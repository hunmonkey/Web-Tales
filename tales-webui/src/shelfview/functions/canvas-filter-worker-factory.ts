/**
 * Isolated so that nothing but a browser ever loads it.
 *
 * `import.meta.url` is how webpack 5 discovers a worker entry, and it is a
 * syntax error once Jest's transform rewrites a module to CommonJS. The runner
 * reaches this file through a dynamic import that only runs when `Worker`
 * exists, so under jsdom the file is never loaded and never parsed.
 */
export function createCanvasFilterWorker(): Worker {
  return new Worker(new URL('./canvas-filter.worker.ts', import.meta.url))
}
