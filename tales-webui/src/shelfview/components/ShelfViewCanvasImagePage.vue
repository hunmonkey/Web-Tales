<template>
  <canvas
    v-if="!fallback"
    ref="canvas"
    class="shelfview-reader-shell__page shelfview-page-content"
    :class="pageClass"
    :style="pageStyle"
    role="img"
    :aria-label="alt"
    :data-page-index="pageIndex"
    :data-page-reload="reloadNonce"
    :data-canvas-src="src"
    :data-canvas-mode="mode"
    data-testid="shelfview-reader-page"
    @dragstart.prevent
  />
  <img
    v-else
    class="shelfview-reader-shell__page shelfview-page-content"
    :class="pageClass"
    :style="pageStyle"
    :src="src"
    :alt="alt"
    draggable="false"
    :data-page-index="pageIndex"
    :data-page-reload="reloadNonce"
    :data-canvas-src="src"
    :data-canvas-mode="mode"
    data-canvas-fallback="true"
    data-testid="shelfview-reader-page"
    @error="$emit('error', $event)"
    @dragstart.prevent
  >
</template>

<script lang="ts">
import Vue from 'vue'
import {
  SHELFVIEW_CANVAS_IMAGE_MAX_PIXELS,
  ShelfViewCanvasImageProcessingMode,
  shelfViewCanvasImageWithinPixelBudget,
  shelfViewRenderWebglSharpen,
} from '@/shelfview/functions/canvas-image-processing'
import {shelfViewFilterImageData} from '@/shelfview/functions/canvas-filter-runner'
import {shelfViewPageImageCrossOrigin} from '@/shelfview/functions/shelfview-urls'

export default Vue.extend({
  name: 'ShelfViewCanvasImagePage',
  props: {
    src: {
      type: String,
      required: true,
    },
    alt: {
      type: String,
      required: true,
    },
    pageIndex: {
      type: Number,
      required: true,
    },
    reloadNonce: {
      type: Number,
      required: true,
    },
    pageClass: {
      type: [String, Array, Object],
      default: undefined,
    },
    pageStyle: {
      type: Object,
      required: true,
    },
    mode: {
      type: String as () => ShelfViewCanvasImageProcessingMode,
      default: 'SHARPEN',
    },
    maxPixels: {
      type: Number,
      default: SHELFVIEW_CANVAS_IMAGE_MAX_PIXELS,
    },
  },
  data: () => ({
    fallback: false,
    processingToken: 0,
    filterAbortController: undefined as AbortController | undefined,
  }),
  watch: {
    src: 'process',
    reloadNonce: 'process',
    mode: 'process',
  },
  mounted() {
    this.process()
  },
  beforeDestroy() {
    this.processingToken++
    this.filterAbortController?.abort()
  },
  methods: {
    async process() {
      const token = ++this.processingToken
      this.filterAbortController?.abort()
      const filterAbortController = typeof AbortController === 'undefined' ? undefined : new AbortController()
      this.filterAbortController = filterAbortController
      this.fallback = false
      await this.$nextTick()

      const canvas = this.$refs.canvas as HTMLCanvasElement | undefined
      if (!canvas) return

      try {
        const image = await this.loadImage(this.src)
        if (token !== this.processingToken) return

        const width = image.naturalWidth || image.width
        const height = image.naturalHeight || image.height
        if (!shelfViewCanvasImageWithinPixelBudget(width, height, this.maxPixels)) {
          throw new Error('Canvas image is over the pixel budget')
        }

        if (this.mode === 'WEBGL_SHARPEN' && shelfViewRenderWebglSharpen(canvas, image, width, height)) {
          this.$emit('processed')
          return
        }

        canvas.width = width
        canvas.height = height
        const context = canvas.getContext('2d', {willReadFrequently: true})
        if (!context) throw new Error('Canvas 2D context is unavailable')

        context.drawImage(image, 0, 0)
        // The per-pixel pass runs in a worker where one exists, so the page turn
        // is not held up by it. Without a worker the runner falls back to the
        // main thread and refuses anything over the CPU budget, which is the
        // behaviour this component had before.
        const filtered = await shelfViewFilterImageData(
          this.mode,
          context.getImageData(0, 0, width, height),
          filterAbortController?.signal,
        )
        if (token !== this.processingToken) return

        context.putImageData(filtered, 0, 0)
        this.$emit('processed')
      } catch (_error) {
        if (token !== this.processingToken) return
        this.fallback = true
        this.$emit('processing-failed')
      } finally {
        if (this.filterAbortController === filterAbortController) this.filterAbortController = undefined
      }
    },
    loadImage(src: string): Promise<HTMLImageElement> {
      return new Promise((resolve, reject) => {
        const image = new Image()
        image.onload = () => resolve(image)
        image.onerror = reject
        // Only a cross-origin page needs the CORS mode to leave the canvas
        // readable. Asking for it on a same-origin page would make this a
        // different resource from the prefetched one and fetch the page twice.
        const crossOrigin = shelfViewPageImageCrossOrigin(src)
        if (crossOrigin) image.crossOrigin = crossOrigin
        image.decoding = 'async'
        image.src = src
      })
    },
  },
})
</script>
