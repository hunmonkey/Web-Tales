<template>
  <div
    class="shelfview-local-reader"
    data-testid="shelfview-local-reader"
    @keydown.left.prevent="previousPage"
    @keydown.right.prevent="nextPage"
  >
    <v-toolbar
      dark
      flat
      dense
      class="shelfview-local-reader__toolbar"
      data-testid="shelfview-local-reader-toolbar"
    >
      <input
        ref="fileInput"
        class="shelfview-local-reader__file-input"
        type="file"
        accept=".zip,.cbz,application/zip"
        data-testid="shelfview-local-reader-file-input"
        @change="onFileChange"
      >
      <v-btn
        icon
        :title="$t('shelfview_local_reader.open').toString()"
        data-testid="shelfview-local-reader-open"
        @click="triggerFilePicker"
        :aria-label="$t('shelfview_local_reader.open')"
      >
        <v-icon>mdi-folder-open-outline</v-icon>
      </v-btn>
      <div
        class="shelfview-local-reader__title text-truncate"
        data-testid="shelfview-local-reader-title"
      >
        {{ title }}
      </div>
      <v-spacer/>
      <v-btn
        icon
        :disabled="!canGoPrevious"
        :title="$t('shelfview_local_reader.previous').toString()"
        data-testid="shelfview-local-reader-previous"
        @click="previousPage"
        :aria-label="$t('shelfview_local_reader.previous')"
      >
        <v-icon>mdi-chevron-left</v-icon>
      </v-btn>
      <div
        class="shelfview-local-reader__counter"
        data-testid="shelfview-local-reader-counter"
      >
        {{ pageCounter }}
      </div>
      <v-btn
        icon
        :disabled="!canGoNext"
        :title="$t('shelfview_local_reader.next').toString()"
        data-testid="shelfview-local-reader-next"
        @click="nextPage"
        :aria-label="$t('shelfview_local_reader.next')"
      >
        <v-icon>mdi-chevron-right</v-icon>
      </v-btn>
      <v-btn
        icon
        :disabled="!archive && !errorMessage"
        :title="$t('shelfview_local_reader.clear').toString()"
        data-testid="shelfview-local-reader-clear"
        @click="clearArchive"
        :aria-label="$t('shelfview_local_reader.clear')"
      >
        <v-icon>mdi-close</v-icon>
      </v-btn>
    </v-toolbar>

    <div
      ref="stage"
      class="shelfview-local-reader__stage"
      tabindex="0"
      data-testid="shelfview-local-reader-stage"
    >
      <v-progress-circular
        v-if="loading"
        indeterminate
        color="primary"
        data-testid="shelfview-local-reader-loading"
      />
      <v-alert
        v-else-if="errorMessage"
        type="error"
        text
        data-testid="shelfview-local-reader-error"
      >
        {{ errorMessage }}
      </v-alert>
      <div
        v-else-if="!currentPage"
        class="shelfview-local-reader__empty"
        data-testid="shelfview-local-reader-empty"
      >
        <v-icon
          size="52"
          class="mb-4"
        >
          mdi-folder-open-outline
        </v-icon>
        <v-btn
          color="primary"
          data-testid="shelfview-local-reader-empty-open"
          @click="triggerFilePicker"
        >
          <v-icon left>mdi-folder-open-outline</v-icon>
          {{ $t('shelfview_local_reader.open') }}
        </v-btn>
        <div class="shelfview-local-reader__empty-text">
          {{ $t('shelfview_local_reader.empty') }}
        </div>
      </div>
      <img
        v-else
        class="shelfview-local-reader__page"
        :src="currentPage.url"
        :alt="currentPage.name"
        :data-page-index="currentPage.index"
        data-testid="shelfview-local-reader-page"
        draggable="false"
        @dragstart.prevent
      >
    </div>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import {
  ShelfViewLocalArchiveBook,
  ShelfViewLocalArchiveError,
  ShelfViewLocalArchivePage,
  openShelfViewLocalArchiveFile,
} from '@/shelfview/functions/local-archive'

export default Vue.extend({
  name: 'ShelfViewLocalReader',
  data: () => ({
    archive: undefined as ShelfViewLocalArchiveBook | undefined,
    pageIndex: 0,
    loading: false,
    errorMessage: '',
  }),
  computed: {
    title(): string {
      return this.archive?.title ?? this.$t('shelfview_local_reader.title').toString()
    },
    currentPage(): ShelfViewLocalArchivePage | undefined {
      return this.archive?.pages[this.pageIndex]
    },
    pageCounter(): string {
      if (!this.archive) return ''
      return `${this.pageIndex + 1} / ${this.archive.pages.length}`
    },
    canGoPrevious(): boolean {
      return this.pageIndex > 0
    },
    canGoNext(): boolean {
      return !!this.archive && this.pageIndex < this.archive.pages.length - 1
    },
  },
  beforeDestroy() {
    this.closeCurrentArchive()
  },
  methods: {
    triggerFilePicker() {
      (this.$refs.fileInput as HTMLInputElement | undefined)?.click()
    },
    async onFileChange(event: Event) {
      const input = event.target as HTMLInputElement
      const file = input.files?.[0]
      if (!file) return

      await this.openFile(file)
      input.value = ''
    },
    async openFile(file: File) {
      this.loading = true
      this.errorMessage = ''
      this.closeCurrentArchive()

      try {
        this.archive = await openShelfViewLocalArchiveFile(file)
        this.pageIndex = 0
        await this.$nextTick()
        const stage = this.$refs.stage as HTMLElement | undefined
        stage?.focus()
      } catch (error) {
        this.errorMessage = this.localArchiveErrorMessage(error)
      } finally {
        this.loading = false
      }
    },
    // Never surface the raw English archive message to the reader (Rule_UIUX 6-1).
    localArchiveErrorMessage(error: unknown): string {
      if (error instanceof ShelfViewLocalArchiveError) {
        switch (error.code) {
          case 'TOO_MANY_ENTRIES':
            return this.$t('shelfview_local_reader.error_too_many_pages').toString()
          case 'NO_SUPPORTED_PAGES':
            return this.$t('shelfview_local_reader.error_no_pages').toString()
          case 'OVER_SIZE_LIMIT':
            return this.$t('shelfview_local_reader.error_too_large').toString()
          case 'INVALID_IMAGE':
            return this.$t('shelfview_local_reader.error_invalid_image').toString()
        }
      }
      return this.$t('shelfview_local_reader.error_open').toString()
    },
    previousPage() {
      if (!this.canGoPrevious) return
      this.pageIndex -= 1
    },
    nextPage() {
      if (!this.canGoNext) return
      this.pageIndex += 1
    },
    clearArchive() {
      this.errorMessage = ''
      this.closeCurrentArchive()
    },
    closeCurrentArchive() {
      this.archive?.close()
      this.archive = undefined
      this.pageIndex = 0
    },
  },
})
</script>

<style scoped>
.shelfview-local-reader {
  min-height: 100vh;
  background: var(--sv-chrome-bg);
  color: var(--sv-chrome-text);
  display: flex;
  flex-direction: column;
}

/* Kept dark in both application themes, like the main reader chrome. */
.shelfview-local-reader__toolbar.v-toolbar {
  flex: 0 0 auto;
  background-color: var(--sv-chrome-surface) !important;
  border-bottom: 1px solid var(--sv-chrome-border);
  color: var(--sv-chrome-text);
}

.shelfview-local-reader__file-input {
  display: none;
}

.shelfview-local-reader__title {
  min-width: 0;
  max-width: min(50vw, 560px);
  font-weight: 600;
}

.shelfview-local-reader__counter {
  min-width: 72px;
  text-align: center;
  font-variant-numeric: tabular-nums;
}

.shelfview-local-reader__stage {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  outline: none;
}

/* The stage is tabindex="0", so keyboard focus needs a visible ring. Mouse
   clicks do not match :focus-visible, so reading stays undisturbed. */
.shelfview-local-reader__stage:focus-visible {
  outline: 2px solid var(--sv-chrome-accent);
  outline-offset: -2px;
}

.shelfview-local-reader__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  text-align: center;
  color: var(--sv-chrome-muted);
}

.shelfview-local-reader__empty-text {
  max-width: 440px;
  line-height: 1.5;
}

.shelfview-local-reader__page {
  max-width: 100%;
  max-height: calc(100vh - 88px);
  object-fit: contain;
  box-shadow: var(--sv-chrome-shadow);
}
</style>
