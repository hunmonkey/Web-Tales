import {
  SHELFVIEW_CANVAS_CPU_MAX_PIXELS,
  ShelfViewCanvasImageProcessingMode,
  shelfViewAdaptiveMonochromeImageData,
  shelfViewBackgroundCleanupImageData,
  shelfViewCanvasImageWithinPixelBudget,
  shelfViewSharpenImageData,
} from '@/shelfview/functions/canvas-image-processing'
import type {ShelfViewCanvasFilterResponse} from '@/shelfview/functions/canvas-filter.worker'

/**
 * Runs an image filter off the main thread when the browser has workers, and on
 * it when it does not.
 *
 * The filters are per-pixel passes. On the main thread a page anywhere near the
 * reader's overall size budget freezes the tab for hundreds of milliseconds on
 * every page turn, which is why the synchronous path gives up above
 * [SHELFVIEW_CANVAS_CPU_MAX_PIXELS] and shows the page unfiltered. A worker has
 * no such cost to the page turn, so it keeps working up to the reader's own
 * budget - pages between the two limits are filtered now where before they were
 * silently left alone.
 *
 * Pixels move both ways as a transferred ArrayBuffer, so neither hand-off
 * copies the image.
 */

let worker: Worker | undefined
let workerCreation: Promise<Worker | undefined> | undefined
let workerUnusable = false
let nextToken = 0
let activeJob: CanvasFilterJob | undefined
const queuedJobs: CanvasFilterJob[] = []

interface CanvasFilterJob {
  token: number
  mode: ShelfViewCanvasImageProcessingMode
  width: number
  height: number
  pixels: ArrayBuffer
  signal?: AbortSignal
  abortListener?: () => void
  resolve: (imageData: ImageData) => void
  reject: (error: Error) => void
}

async function ensureWorker(): Promise<Worker | undefined> {
  if (workerUnusable) return undefined
  if (worker) return worker
  if (workerCreation) return workerCreation
  if (typeof Worker === 'undefined') {
    workerUnusable = true
    return undefined
  }

  workerCreation = createWorker()
  return workerCreation
}

async function createWorker(): Promise<Worker | undefined> {
  try {
    const {createCanvasFilterWorker} = await import(
      /* webpackChunkName: "canvas-filter-worker-factory" */
      '@/shelfview/functions/canvas-filter-worker-factory'
    )
    const created = createCanvasFilterWorker()
    created.addEventListener('message', event => handleWorkerMessage(created, event as MessageEvent<ShelfViewCanvasFilterResponse>))
    created.addEventListener('error', () => failWorker(created, new Error('Canvas filter worker failed')))
    created.addEventListener('messageerror', () => failWorker(created, new Error('Canvas filter worker returned an unreadable message')))
    worker = created
    return created
  } catch (_error) {
    workerUnusable = true
    return undefined
  } finally {
    workerCreation = undefined
  }
}

function filterOnWorker(
  active: Worker,
  mode: ShelfViewCanvasImageProcessingMode,
  imageData: ImageData,
  signal?: AbortSignal,
): Promise<ImageData> {
  if (signal?.aborted) return Promise.reject(canvasFilterAbortError())

  const token = ++nextToken
  const width = imageData.width
  const height = imageData.height
  const pixels = imageData.data.buffer as ArrayBuffer

  return new Promise<ImageData>((resolve, reject) => {
    const job: CanvasFilterJob = {
      token,
      mode,
      width,
      height,
      pixels,
      signal,
      resolve,
      reject,
    }
    if (signal) {
      job.abortListener = () => abortJob(job)
      signal.addEventListener('abort', job.abortListener, {once: true})
    }

    queuedJobs.push(job)
    if (worker === active) startNextJob(active)
    else void restartQueuedJobs()
  })
}

function startNextJob(active: Worker) {
  if (worker !== active || activeJob) return

  while (queuedJobs.length > 0) {
    const job = queuedJobs.shift()!
    if (job.signal?.aborted) {
      rejectJob(job, canvasFilterAbortError())
      continue
    }

    activeJob = job
    try {
      active.postMessage({
        token: job.token,
        mode: job.mode,
        width: job.width,
        height: job.height,
        pixels: job.pixels,
      }, [job.pixels])
    } catch (error) {
      failWorker(active, error instanceof Error ? error : new Error(String(error)))
    }
    return
  }
}

function handleWorkerMessage(
  active: Worker,
  event: MessageEvent<ShelfViewCanvasFilterResponse>,
) {
  if (worker !== active) return
  const job = activeJob
  if (!job || event.data.token !== job.token) {
    failWorker(active, new Error('Canvas filter worker returned an unexpected response'))
    return
  }

  activeJob = undefined
  if (!event.data.pixels) {
    rejectJob(job, new Error(event.data.error ?? 'Canvas filter worker returned no pixels'))
  } else {
    try {
      resolveJob(job, new ImageData(new Uint8ClampedArray(event.data.pixels), job.width, job.height))
    } catch (error) {
      rejectJob(job, error instanceof Error ? error : new Error(String(error)))
    }
  }
  startNextJob(active)
}

function abortJob(job: CanvasFilterJob) {
  const queuedIndex = queuedJobs.indexOf(job)
  if (queuedIndex >= 0) {
    queuedJobs.splice(queuedIndex, 1)
    rejectJob(job, canvasFilterAbortError())
    return
  }
  if (activeJob !== job) return

  const active = worker
  activeJob = undefined
  rejectJob(job, canvasFilterAbortError())

  // A worker cannot receive a cancellation message while a synchronous pixel
  // loop is running. Termination is the only way to stop that CPU work and the
  // already transferred buffer. Queued jobs still own their buffers, so they
  // can continue on a fresh worker.
  worker = undefined
  try {
    active?.terminate()
  } catch (_error) {
    // The job is already cancelled; a failed terminate has nothing to settle.
  }
  if (queuedJobs.length > 0) void restartQueuedJobs()
}

async function restartQueuedJobs() {
  const active = await ensureWorker()
  if (active) {
    startNextJob(active)
    return
  }
  rejectQueuedJobs(new Error('Canvas filter worker is unavailable'))
}

function failWorker(active: Worker, error: Error) {
  if (worker !== active) return

  workerUnusable = true
  worker = undefined
  try {
    active.terminate()
  } catch (_error) {
    // Rejection below is the observable failure; termination is best effort.
  }

  const jobs = activeJob ? [activeJob, ...queuedJobs.splice(0)] : queuedJobs.splice(0)
  activeJob = undefined
  jobs.forEach(job => rejectJob(job, error))
}

function rejectQueuedJobs(error: Error) {
  queuedJobs.splice(0).forEach(job => rejectJob(job, error))
}

function resolveJob(job: CanvasFilterJob, imageData: ImageData) {
  removeAbortListener(job)
  job.resolve(imageData)
}

function rejectJob(job: CanvasFilterJob, error: Error) {
  removeAbortListener(job)
  job.reject(error)
}

function removeAbortListener(job: CanvasFilterJob) {
  if (job.signal && job.abortListener) job.signal.removeEventListener('abort', job.abortListener)
  job.abortListener = undefined
}

function canvasFilterAbortError(): Error {
  const error = new Error('Canvas filter job was cancelled')
  error.name = 'AbortError'
  return error
}

function filterSynchronously(
  mode: ShelfViewCanvasImageProcessingMode,
  imageData: ImageData,
): ImageData {
  if (!shelfViewCanvasImageWithinPixelBudget(imageData.width, imageData.height, SHELFVIEW_CANVAS_CPU_MAX_PIXELS)) {
    throw new Error('Canvas image is over the CPU filter budget')
  }

  switch (mode) {
    case 'BACKGROUND_CLEANUP':
      return shelfViewBackgroundCleanupImageData(imageData)
    case 'ADAPTIVE_MONO':
      return shelfViewAdaptiveMonochromeImageData(imageData)
    case 'WEBGL_SHARPEN':
    case 'SHARPEN':
      return shelfViewSharpenImageData(imageData)
  }
}

export async function shelfViewFilterImageData(
  mode: ShelfViewCanvasImageProcessingMode,
  imageData: ImageData,
  signal?: AbortSignal,
): Promise<ImageData> {
  if (signal?.aborted) throw canvasFilterAbortError()

  const active = await ensureWorker()
  if (active) return filterOnWorker(active, mode, imageData, signal)

  if (signal?.aborted) throw canvasFilterAbortError()
  return filterSynchronously(mode, imageData)
}
