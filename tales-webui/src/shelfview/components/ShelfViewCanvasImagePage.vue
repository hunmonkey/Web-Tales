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
  SHELFVIEW_CANVAS_CPU_MAX_PIXELS,
  SHELFVIEW_CANVAS_IMAGE_MAX_PIXELS,
  ShelfViewCanvasImageProcessingMode,
  shelfViewAdaptiveMonochromeImageData,
  shelfViewBackgroundCleanupImageData,
  shelfViewCanvasImageWithinPixelBudget,
  shelfViewRenderWebglSharpen,
  shelfViewSharpenImageData,
} from '@/shelfview/functions/canvas-image-processing'

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
  },
  methods: {
    async process() {
      const token = ++this.processingToken
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

        // Everything below here is a synchronous per-pixel pass on the main
        // thread, so a large page falls back to the untouched original rather
        // than stalling the page turn.
        if (!shelfViewCanvasImageWithinPixelBudget(width, height, SHELFVIEW_CANVAS_CPU_MAX_PIXELS)) {
          throw new Error('Canvas image is over the CPU filter budget')
        }

        canvas.width = width
        canvas.height = height
        const context = canvas.getContext('2d', {willReadFrequently: true})
        if (!context) throw new Error('Canvas 2D context is unavailable')

        context.drawImage(image, 0, 0)
        const imageData = context.getImageData(0, 0, width, height)
        switch (this.mode) {
          case 'WEBGL_SHARPEN':
          case 'SHARPEN':
            context.putImageData(shelfViewSharpenImageData(imageData), 0, 0)
            break
          case 'BACKGROUND_CLEANUP':
            context.putImageData(shelfViewBackgroundCleanupImageData(imageData), 0, 0)
            break
          case 'ADAPTIVE_MONO':
            context.putImageData(shelfViewAdaptiveMonochromeImageData(imageData), 0, 0)
            break
        }
        this.$emit('processed')
      } catch (_error) {
        if (token !== this.processingToken) return
        this.fallback = true
        this.$emit('processing-failed')
      }
    },
    loadImage(src: string): Promise<HTMLImageElement> {
      return new Promise((resolve, reject) => {
        const image = new Image()
        image.onload = () => resolve(image)
        image.onerror = reject
        image.crossOrigin = 'use-credentials'
        image.decoding = 'async'
        image.src = src
      })
    },
  },
})
</script>
