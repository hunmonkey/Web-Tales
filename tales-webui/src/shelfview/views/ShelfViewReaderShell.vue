<template>
  <v-container
    class="ma-0 pa-0 shelfview-reader-shell"
    :class="{
      'shelfview-reader-shell--search-open': textSearchOpen && textSearchAvailable,
      'shelfview-reader-shell--thumbnails': thumbnailStripVisible,
      'shelfview-reader-shell--chrome-visible': chromeVisible,
    }"
    fluid
    data-testid="shelfview-reader-shell"
  >
    <v-toolbar
      v-show="chromeVisible"
      dense
      elevation="1"
      class="shelfview-reader-shell__toolbar full-width"
      :class="{'shelfview-reader-shell__toolbar--visible': chromeVisible}"
      data-testid="shelfview-reader-toolbar"
    >
      <v-btn
        icon
        :aria-label="$t('shelfview_reader.close')"
        @click="closeBook"
      >
        <v-icon>mdi-arrow-left</v-icon>
      </v-btn>

      <v-toolbar-title class="shelfview-reader-shell__title">{{ bookTitle }}</v-toolbar-title>
      <v-spacer/>

      <div
        v-if="pageControlsVisible"
        class="shelfview-reader-shell__page-controls"
        data-testid="shelfview-reader-page-controls"
      >
        <v-btn
          icon
          :disabled="!canGoPrevious"
          :aria-label="$t('shelfview_reader.previous_page')"
          data-testid="shelfview-reader-previous"
          @click="previousPage"
        >
          <v-icon>mdi-chevron-left</v-icon>
        </v-btn>

        <span
          class="shelfview-reader-shell__page-counter"
          data-testid="shelfview-reader-page-counter"
        >
          {{ currentPageLabel }} / {{ pagesCount }}
        </span>

        <v-btn
          icon
          :disabled="!canGoNext"
          :aria-label="$t('shelfview_reader.next_page')"
          data-testid="shelfview-reader-next"
          @click="nextPage"
        >
          <v-icon>mdi-chevron-right</v-icon>
        </v-btn>

        <v-btn
          v-if="fixedPageControlsVisible"
          text
          small
          class="shelfview-reader-shell__control-toggle"
          :aria-label="pageFitLabel"
          :aria-expanded="openControlMenu === 'fit' ? 'true' : 'false'"
          aria-haspopup="menu"
          data-testid="shelfview-reader-fit-toggle"
          @click="toggleControlMenu('fit', $event)"
        >
          <v-icon small>{{ pageFitIcon }}</v-icon>
          <span class="shelfview-reader-shell__control-toggle-label">{{ pageFitLabel }}</span>
          <v-icon small>mdi-menu-down</v-icon>
        </v-btn>

        <v-tooltip
          v-if="fullscreenAvailable"
          bottom
        >
          <template v-slot:activator="{ on }">
            <v-btn
              icon
              :aria-label="isFullscreen ? $t('shelfview_reader.exit_fullscreen') : $t('shelfview_reader.fullscreen')"
              :aria-pressed="isFullscreen ? 'true' : 'false'"
              data-testid="shelfview-reader-fullscreen-toggle"
              v-on="on"
              @click="toggleFullscreen"
            >
              <v-icon>{{ isFullscreen ? 'mdi-fullscreen-exit' : 'mdi-fullscreen' }}</v-icon>
            </v-btn>
          </template>
          <span>{{ isFullscreen ? $t('shelfview_reader.exit_fullscreen') : $t('shelfview_reader.fullscreen') }}</span>
        </v-tooltip>

        <v-tooltip bottom>
          <template v-slot:activator="{ on }">
            <v-btn
              icon
              :aria-label="$t('shelfview_reader.more_controls')"
              :aria-expanded="overflowMenuOpen ? 'true' : 'false'"
              aria-haspopup="menu"
              data-testid="shelfview-reader-more-toggle"
              v-on="on"
              @click="toggleOverflowMenu"
            >
              <v-icon>mdi-dots-vertical</v-icon>
            </v-btn>
          </template>
          <span>{{ $t('shelfview_reader.more_controls') }}</span>
        </v-tooltip>
      </div>
    </v-toolbar>

    <section
      v-if="pageControlsVisible"
      v-show="chromeVisible && overflowMenuOpen"
      class="shelfview-reader-shell__overflow"
      role="menu"
      :aria-label="$t('shelfview_reader.more_controls')"
      data-testid="shelfview-reader-overflow"
    >
      <div class="shelfview-reader-shell__overflow-actions">
        <v-btn
          text
          small
          class="shelfview-reader-shell__control-toggle"
          :aria-label="readerLayoutLabel"
          :aria-expanded="openControlMenu === 'layout' ? 'true' : 'false'"
          aria-haspopup="menu"
          data-testid="shelfview-reader-layout-toggle"
          @click="toggleControlMenu('layout', $event)"
        >
          <v-icon small>{{ readerLayoutIcon }}</v-icon>
          <span class="shelfview-reader-shell__control-toggle-label">{{ readerLayoutLabel }}</span>
          <v-icon small>mdi-menu-down</v-icon>
        </v-btn>

        <v-btn
          v-if="textControlsVisible"
          text
          small
          class="shelfview-reader-shell__control-toggle"
          :aria-label="textThemeLabel"
          :aria-expanded="openControlMenu === 'text-theme' ? 'true' : 'false'"
          aria-haspopup="menu"
          data-testid="shelfview-reader-text-theme-toggle"
          @click="toggleControlMenu('text-theme', $event)"
        >
          <v-icon small>{{ textThemeIcon }}</v-icon>
          <span class="shelfview-reader-shell__control-toggle-label">{{ textThemeLabel }}</span>
          <v-icon small>mdi-menu-down</v-icon>
        </v-btn>

        <v-btn
          v-if="fixedPageControlsVisible"
          text
          small
          class="shelfview-reader-shell__control-toggle"
          :aria-label="fixedPageRotationLabel"
          :aria-expanded="openControlMenu === 'rotate' ? 'true' : 'false'"
          aria-haspopup="menu"
          data-testid="shelfview-reader-rotate-toggle"
          @click="toggleControlMenu('rotate', $event)"
        >
          <v-icon small>mdi-rotate-right</v-icon>
          <span class="shelfview-reader-shell__control-toggle-label">{{ fixedPageRotationShortLabel }}</span>
          <v-icon small>mdi-menu-down</v-icon>
        </v-btn>

        <v-btn
          v-if="fixedPageControlsVisible"
          text
          small
          class="shelfview-reader-shell__control-toggle"
          :aria-label="readingDirectionLabel"
          :aria-expanded="openControlMenu === 'direction' ? 'true' : 'false'"
          aria-haspopup="menu"
          :disabled="!rtlAvailable"
          data-testid="shelfview-reader-direction-toggle"
          @click="toggleControlMenu('direction', $event)"
        >
          <v-icon small>{{ readingDirectionIcon }}</v-icon>
          <span class="shelfview-reader-shell__control-toggle-label">{{ readingDirectionLabel }}</span>
          <v-icon small>mdi-menu-down</v-icon>
        </v-btn>

        <v-btn
          v-if="fixedPageControlsVisible"
          text
          small
          class="shelfview-reader-shell__control-toggle"
          :aria-label="imageFilterPresetLabel"
          :aria-expanded="openControlMenu === 'filter' ? 'true' : 'false'"
          aria-haspopup="menu"
          :disabled="!imageFiltersAvailable"
          data-testid="shelfview-reader-filter-toggle"
          @click="toggleControlMenu('filter', $event)"
        >
          <v-icon small>{{ imageFilterPresetIcon }}</v-icon>
          <span class="shelfview-reader-shell__control-toggle-label">{{ imageFilterPresetLabel }}</span>
          <v-icon small>mdi-menu-down</v-icon>
        </v-btn>

        <input
          v-if="fixedPageControlsVisible"
          v-model="pageJumpInput"
          class="shelfview-reader-shell__page-jump"
          type="number"
          min="1"
          :max="pagesCount"
          :aria-label="$t('shelfview_reader.page_jump')"
          data-testid="shelfview-reader-page-jump"
          @change="goToPageJumpInput"
          @keyup.enter="goToPageJumpInput"
        />

        <v-tooltip bottom>
          <template v-slot:activator="{ on }">
            <v-btn
              v-if="thumbnailStripAvailable"
              icon
              :aria-label="thumbnailStripOpen ? $t('shelfview_reader.hide_thumbnails') : $t('shelfview_reader.show_thumbnails')"
              :aria-pressed="thumbnailStripOpen ? 'true' : 'false'"
              data-testid="shelfview-reader-thumbnails-toggle"
              v-on="on"
              @click="toggleThumbnailStrip"
            >
              <v-icon>{{ thumbnailStripOpen ? 'mdi-view-carousel' : 'mdi-view-carousel-outline' }}</v-icon>
            </v-btn>
          </template>
          <span>{{ thumbnailStripOpen ? $t('shelfview_reader.hide_thumbnails') : $t('shelfview_reader.show_thumbnails') }}</span>
        </v-tooltip>

        <v-tooltip bottom>
          <template v-slot:activator="{ on }">
            <v-btn
              v-if="fixedPageControlsVisible"
              icon
              :aria-label="$t('shelfview_reader.fixed_settings')"
              data-testid="shelfview-reader-fixed-settings-toggle"
              v-on="on"
              @click="openFixedPageSettings"
            >
              <v-icon>mdi-tune-variant</v-icon>
            </v-btn>
          </template>
          <span>{{ $t('shelfview_reader.fixed_settings') }}</span>
        </v-tooltip>

        <v-tooltip bottom>
          <template v-slot:activator="{ on }">
            <v-btn
              v-if="fixedPageControlsVisible"
              icon
              :aria-label="slideshowLabel"
              data-testid="shelfview-reader-slideshow-toggle"
              v-on="on"
              @click="toggleSlideshow"
            >
              <v-icon>{{ slideshowActive ? 'mdi-pause-circle-outline' : 'mdi-play-circle-outline' }}</v-icon>
            </v-btn>
          </template>
          <span>{{ slideshowLabel }}</span>
        </v-tooltip>

        <v-tooltip bottom>
          <template v-slot:activator="{ on }">
            <v-btn
              v-if="textControlsVisible"
              icon
              :aria-label="$t('shelfview_reader.text_settings')"
              data-testid="shelfview-reader-text-settings-toggle"
              v-on="on"
              @click="openTextSettings"
            >
              <v-icon>mdi-format-size</v-icon>
            </v-btn>
          </template>
          <span>{{ $t('shelfview_reader.text_settings') }}</span>
        </v-tooltip>

        <v-tooltip bottom>
          <template v-slot:activator="{ on }">
            <v-btn
              v-if="textSearchAvailable"
              icon
              :aria-label="$t('shelfview_reader.search')"
              data-testid="shelfview-reader-search-toggle"
              v-on="on"
              @click="toggleTextSearch"
            >
              <v-icon>mdi-magnify</v-icon>
            </v-btn>
          </template>
          <span>{{ $t('shelfview_reader.search') }}</span>
        </v-tooltip>

        <v-tooltip bottom>
          <template v-slot:activator="{ on }">
            <v-btn
              v-if="textToSpeechAvailable"
              icon
              :aria-label="textToSpeechLabel"
              data-testid="shelfview-reader-tts-toggle"
              v-on="on"
              @click="toggleTextToSpeech"
            >
              <v-icon>{{ textToSpeechActive ? 'mdi-volume-off' : 'mdi-volume-high' }}</v-icon>
            </v-btn>
          </template>
          <span>{{ textToSpeechLabel }}</span>
        </v-tooltip>

        <v-tooltip bottom>
          <template v-slot:activator="{ on }">
            <v-btn
              v-if="textControlsVisible"
              icon
              :disabled="!textHighlightAvailable"
              :aria-label="textHighlightLabel"
              data-testid="shelfview-reader-highlight-add"
              v-on="on"
              @click="addTextHighlightForCurrentSelection"
            >
              <v-icon>{{ currentTextParagraphHighlighted ? 'mdi-marker-check' : 'mdi-format-color-highlight' }}</v-icon>
            </v-btn>
          </template>
          <span>{{ textHighlightLabel }}</span>
        </v-tooltip>

        <v-tooltip bottom>
          <template v-slot:activator="{ on }">
            <v-btn
              v-if="textControlsVisible"
              icon
              :aria-label="$t('shelfview_reader.dictionary_lookup')"
              data-testid="shelfview-reader-dictionary-toggle"
              v-on="on"
              @click="openDictionaryLookup"
            >
              <v-icon>mdi-book-search-outline</v-icon>
            </v-btn>
          </template>
          <span>{{ $t('shelfview_reader.dictionary_lookup') }}</span>
        </v-tooltip>

        <v-tooltip bottom>
          <template v-slot:activator="{ on }">
            <v-btn
              v-if="contentsAvailable"
              icon
              :aria-label="$t('shelfview_reader.contents')"
              data-testid="shelfview-reader-contents-toggle"
              v-on="on"
              @click="contentsDrawerOpen = true"
            >
              <v-icon>mdi-table-of-contents</v-icon>
            </v-btn>
          </template>
          <span>{{ $t('shelfview_reader.contents') }}</span>
        </v-tooltip>

        <v-tooltip bottom>
          <template v-slot:activator="{ on }">
            <v-btn
              icon
              :aria-label="$t('shelfview_reader.bookmarks')"
              data-testid="shelfview-reader-bookmarks-toggle"
              v-on="on"
              @click="bookmarksDrawerOpen = true"
            >
              <v-icon>mdi-bookmark-multiple-outline</v-icon>
            </v-btn>
          </template>
          <span>{{ $t('shelfview_reader.bookmarks') }}</span>
        </v-tooltip>

        <v-tooltip bottom>
          <template v-slot:activator="{ on }">
            <v-btn
              icon
              :aria-label="$t('shelfview_reader.add_bookmark')"
              data-testid="shelfview-reader-bookmark-add"
              v-on="on"
              @click="addBookmarkForCurrentPage"
            >
              <v-icon>mdi-bookmark-plus-outline</v-icon>
            </v-btn>
          </template>
          <span>{{ $t('shelfview_reader.add_bookmark') }}</span>
        </v-tooltip>

        <v-tooltip bottom>
          <template v-slot:activator="{ on }">
            <v-btn
              icon
              :aria-label="$t('shelfview_reader.shortcuts')"
              data-testid="shelfview-reader-shortcuts-toggle"
              v-on="on"
              @click="showShortcutHelp = true"
            >
              <v-icon>mdi-keyboard-outline</v-icon>
            </v-btn>
          </template>
          <span>{{ $t('shelfview_reader.shortcuts') }}</span>
        </v-tooltip>

        <v-tooltip bottom>
          <template v-slot:activator="{ on }">
            <v-btn
              icon
              :aria-label="$t('shelfview_reader.open_legacy_reader')"
              :to="legacyReaderLocation"
              v-on="on"
            >
              <v-icon>mdi-book-open-page-variant</v-icon>
            </v-btn>
          </template>
          <span>{{ $t('browse_book.read_book') }}</span>
        </v-tooltip>

        <span
          v-if="readerStateNoticeVisible"
          class="shelfview-reader-shell__sync-status"
          :class="readerStateNoticeClass"
          data-testid="shelfview-reader-sync-status"
        >
          {{ readerStateMessage }}
        </span>

        <v-tooltip bottom>
          <template v-slot:activator="{ on }">
            <v-btn
              icon
              :aria-label="$t('shelfview_reader.close_more_controls')"
              data-testid="shelfview-reader-overflow-close"
              v-on="on"
              @click="closeOverflowMenu"
            >
              <v-icon>mdi-close</v-icon>
            </v-btn>
          </template>
          <span>{{ $t('shelfview_reader.close_more_controls') }}</span>
        </v-tooltip>
      </div>
    </section>

    <ShelfViewControlMenu
      v-if="openControlMenu"
      :menu="openControlMenu"
      :title="controlMenuTitle"
      :left="controlMenuLeft"
      :options="controlMenuOptions"
      @close="closeControlMenu"
      @select="selectControlOption"
    />

    <section
      v-if="textSearchOpen && textSearchAvailable"
      class="shelfview-reader-shell__search-bar"
      data-testid="shelfview-reader-search-bar"
    >
      <v-text-field
        v-model="textSearchQuery"
        dense
        outlined
        hide-details
        prepend-inner-icon="mdi-magnify"
        :label="$t('shelfview_reader.search')"
        data-testid="shelfview-reader-search-input"
        @input="onTextSearchInput"
        @keyup.enter="goToNextTextSearchMatch"
      />
      <span
        class="shelfview-reader-shell__search-status"
        data-testid="shelfview-reader-search-status"
      >
        {{ textSearchStatus }}
      </span>
      <v-btn
        icon
        :disabled="textSearchMatches.length === 0"
        :aria-label="$t('shelfview_reader.search_previous')"
        data-testid="shelfview-reader-search-previous"
        @click="goToPreviousTextSearchMatch"
      >
        <v-icon>mdi-chevron-up</v-icon>
      </v-btn>
      <v-btn
        icon
        :disabled="textSearchMatches.length === 0"
        :aria-label="$t('shelfview_reader.search_next')"
        data-testid="shelfview-reader-search-next"
        @click="goToNextTextSearchMatch"
      >
        <v-icon>mdi-chevron-down</v-icon>
      </v-btn>
    </section>

    <p
      id="shelfview-reader-shortcut-hint"
      class="shelfview-reader-shell__sr-only"
    >
      Use arrow keys, Page Up, Page Down, Home, End and touch swipes to navigate. Press question mark for shortcuts.
    </p>

    <p
      class="shelfview-reader-shell__sr-only"
      aria-live="polite"
      data-testid="shelfview-reader-live-status"
    >
      {{ readerLiveStatus }}
    </p>

    <v-snackbar
      v-model="chromeHintVisible"
      color="rgba(0, 0, 0, 0.82)"
      top
      centered
      :timeout="chromeHintTimeoutMs"
      data-testid="shelfview-reader-chrome-hint"
    >
      <p class="shelfview-reader-shell__chrome-hint-text">
        {{ $t('shelfview_reader.chrome_hint') }}
      </p>
    </v-snackbar>

    <main
      ref="readerStage"
      class="shelfview-reader-shell__stage"
      :class="[stageClasses, textStageThemeClass]"
      :style="readerStageStyle"
      tabindex="0"
      role="application"
      :aria-label="readerAriaLabel"
      aria-describedby="shelfview-reader-shortcut-hint"
      data-testid="shelfview-reader-stage"
      @wheel="onReaderWheel"
      @pointerdown="onReaderPointerDown"
      @pointermove="onReaderPointerMove"
      @pointerup="onReaderPointerUp"
      @pointercancel="onReaderPointerCancel"
      @scroll="onReaderStageScroll"
      @touchstart="onReaderTouchStart"
      @touchend="onReaderTouchEnd"
    >
      <v-progress-circular
        v-if="loading"
        indeterminate
        color="primary"
        data-testid="shelfview-reader-loading"
      />

      <section
        v-else-if="hasDisplayError"
        class="shelfview-reader-shell__status"
        data-testid="shelfview-reader-error"
      >
        <v-icon
          size="36"
          color="error"
        >
          mdi-alert-circle-outline
        </v-icon>
        <h2>{{ displayErrorTitle }}</h2>
        <p>{{ displayErrorMessage }}</p>
        <div class="shelfview-reader-shell__status-actions">
          <v-btn
            color="primary"
            data-testid="shelfview-reader-retry"
            @click="retryReader"
          >
            {{ $t('shelfview_reader.try_again') }}
          </v-btn>
          <v-btn
            v-if="encodingRecoveryVisible"
            data-testid="shelfview-reader-retry-utf8"
            @click="retryTextEncoding('utf-8')"
          >
            UTF-8
          </v-btn>
          <v-btn
            v-if="encodingRecoveryVisible"
            data-testid="shelfview-reader-retry-euckr"
            @click="retryTextEncoding('euc-kr')"
          >
            EUC-KR
          </v-btn>
          <v-btn
            v-if="encodingRecoveryVisible"
            data-testid="shelfview-reader-retry-auto"
            @click="retryTextEncoding()"
          >
            {{ $t('shelfview_reader.encoding_auto') }}
          </v-btn>
          <v-btn
            color="secondary"
            :to="legacyReaderLocation"
            data-testid="shelfview-reader-legacy"
          >
            {{ $t('browse_book.read_book') }}
          </v-btn>
        </div>
        <p
          v-if="encodingRecoveryVisible"
          class="shelfview-reader-shell__status-note"
          data-testid="shelfview-reader-encoding-note"
        >
          {{ $t('shelfview_reader.encoding_current', { encoding: textEncodingLabel }) }}
        </p>
      </section>

      <section
        v-else-if="empty"
        class="shelfview-reader-shell__status"
        data-testid="shelfview-reader-empty"
      >
        <v-icon
          size="36"
          color="grey lighten-1"
        >
          mdi-book-open-blank-variant
        </v-icon>
        <h2>Tales</h2>
        <p>{{ emptyMessage }}</p>
      </section>

      <div
        v-else-if="readerPagesVisible"
        class="shelfview-reader-shell__pages"
        :class="readerPagesClasses"
        data-testid="shelfview-reader-pages"
      >
        <template v-for="page in visiblePageSources">
          <ShelfViewCanvasImagePage
            v-if="canvasImageProcessingEnabled"
            :key="`canvas-page-${page.pageIndex}-${pageImageReloadNonce}`"
            :src="page.url"
            :alt="`${bookTitle} - page ${page.pageIndex + 1}`"
            :page-index="page.pageIndex"
            :reload-nonce="pageImageReloadNonce"
            :page-class="pageImageClass"
            :page-style="imageFilterStyle"
            @error="onPageImageError"
          />
          <img
            v-else
            :key="`page-${page.pageIndex}-${pageImageReloadNonce}`"
            class="shelfview-reader-shell__page shelfview-page-content"
            :class="pageImageClass"
            :style="imageFilterStyle"
            :src="page.url"
            :alt="`${bookTitle} - page ${page.pageIndex + 1}`"
            draggable="false"
            :data-page-index="page.pageIndex"
            :data-page-reload="pageImageReloadNonce"
            data-testid="shelfview-reader-page"
            @error="onPageImageError"
            @dragstart.prevent
          />
        </template>
      </div>

      <article
        v-else-if="readerTextVisible"
        class="shelfview-reader-shell__text"
        :class="textThemeClass"
        :style="textReaderStyle"
        data-testid="shelfview-reader-text"
      >
        <p
          v-for="paragraph in visibleTextParagraphs"
          :key="`paragraph-${paragraph.paragraphIndex}`"
          class="shelfview-reader-shell__text-paragraph"
          :class="textParagraphClass(paragraph.paragraphIndex)"
          :data-text-paragraph-index="paragraph.paragraphIndex"
          data-testid="shelfview-reader-text-paragraph"
        >
          <span
            v-for="segment in textParagraphSegments(paragraph)"
            :key="segment.key"
            :class="textParagraphSegmentClass(segment)"
            :data-testid="segment.highlighted ? 'shelfview-reader-text-highlight' : undefined"
          >{{ segment.text }}</span>
        </p>
      </article>
    </main>

    <section
      v-if="thumbnailStripVisible"
      class="shelfview-reader-shell__thumbnail-strip"
      data-testid="shelfview-reader-thumbnail-strip"
    >
      <button
        v-for="thumbnail in thumbnailPageSources"
        :key="`thumbnail-${thumbnail.pageIndex}`"
        type="button"
        class="shelfview-reader-shell__thumbnail-button"
        :class="{'shelfview-reader-shell__thumbnail-button--active': thumbnail.pageIndex === currentPageIndex}"
        :aria-label="$t('shelfview_reader.page_n', {n: thumbnail.pageIndex + 1})"
        :data-page-index="thumbnail.pageIndex"
        data-testid="shelfview-reader-thumbnail"
        @click="goToPageIndex(thumbnail.pageIndex)"
      >
        <img
          :src="thumbnail.url"
          :alt="$t('shelfview_reader.page_n', {n: thumbnail.pageIndex + 1})"
          draggable="false"
        />
      </button>
    </section>

    <aside
      v-if="contentsDrawerOpen && contentsAvailable"
      class="shelfview-reader-shell__contents-drawer"
      role="dialog"
      aria-modal="true"
      aria-labelledby="shelfview-contents-title"
      data-testid="shelfview-contents-drawer"
    >
      <header class="shelfview-reader-shell__drawer-header">
        <h2 id="shelfview-contents-title">{{ $t('shelfview_reader.contents') }}</h2>
        <v-btn
          icon
          :aria-label="$t('shelfview_reader.close')"
          data-testid="shelfview-contents-close"
          @click="closeContentsDrawer"
        >
          <v-icon>mdi-close</v-icon>
        </v-btn>
      </header>

      <ul
        v-if="manifestContentsItems.length > 0"
        class="shelfview-reader-shell__contents-list"
      >
        <li
          v-for="(item, index) in manifestContentsItems"
          :key="`manifest-contents-${index}`"
          class="shelfview-reader-shell__contents-row"
        >
          <button
            type="button"
            class="shelfview-reader-shell__contents-link"
            :class="{'shelfview-reader-shell__contents-link--active': isCurrentManifestContentsItem(item)}"
            data-testid="shelfview-contents-item"
            @click="goToManifestContentsItem(item)"
          >
            <span class="shelfview-reader-shell__contents-index">{{ index + 1 }}</span>
            <span class="shelfview-reader-shell__contents-label">{{ item.label }}</span>
            <v-icon
              v-if="isCurrentManifestContentsItem(item)"
              small
              class="shelfview-reader-shell__contents-active-icon"
            >mdi-check</v-icon>
          </button>
        </li>
      </ul>
      <ul
        v-else-if="mixedContentsItems.length > 1"
        class="shelfview-reader-shell__contents-list"
      >
        <li
          v-for="item in mixedContentsItems"
          :key="item.itemId"
          class="shelfview-reader-shell__contents-row"
        >
          <button
            type="button"
            class="shelfview-reader-shell__contents-link"
            :class="{'shelfview-reader-shell__contents-link--active': isCurrentMixedItem(item)}"
            data-testid="shelfview-contents-item"
            @click="goToMixedContentItem(item)"
          >
            <span class="shelfview-reader-shell__contents-index">{{ item.index + 1 }}</span>
            <span class="shelfview-reader-shell__contents-label">{{ item.label }}</span>
            <v-icon
              v-if="isCurrentMixedItem(item)"
              small
              class="shelfview-reader-shell__contents-active-icon"
            >mdi-check</v-icon>
          </button>
        </li>
      </ul>
      <ul
        v-else
        class="shelfview-reader-shell__contents-list"
      >
        <li
          v-for="item in textContentsItems"
          :key="`text-contents-${item.paragraphIndex}`"
          class="shelfview-reader-shell__contents-row"
        >
          <button
            type="button"
            class="shelfview-reader-shell__contents-link"
            :class="{'shelfview-reader-shell__contents-link--active': isCurrentTextContentsItem(item)}"
            data-testid="shelfview-contents-item"
            @click="goToTextContentsItem(item)"
          >
            <span class="shelfview-reader-shell__contents-index">{{ item.paragraphIndex + 1 }}</span>
            <span class="shelfview-reader-shell__contents-label">{{ item.label }}</span>
            <v-icon
              v-if="isCurrentTextContentsItem(item)"
              small
              class="shelfview-reader-shell__contents-active-icon"
            >mdi-check</v-icon>
          </button>
        </li>
      </ul>
    </aside>

    <aside
      v-if="dictionaryDrawerOpen"
      class="shelfview-reader-shell__dictionary-drawer"
      role="dialog"
      aria-modal="true"
      aria-labelledby="shelfview-dictionary-title"
      data-testid="shelfview-dictionary-drawer"
    >
      <header class="shelfview-reader-shell__drawer-header">
        <h2 id="shelfview-dictionary-title">{{ $t('shelfview_reader.dictionary') }}</h2>
        <v-btn
          icon
          :aria-label="$t('shelfview_reader.close')"
          data-testid="shelfview-dictionary-close"
          @click="closeDictionaryDrawer"
        >
          <v-icon>mdi-close</v-icon>
        </v-btn>
      </header>

      <v-text-field
        v-model="dictionaryQuery"
        dense
        outlined
        hide-details
        :label="$t('shelfview_reader.dictionary_query')"
        data-testid="shelfview-dictionary-query"
      />

      <div class="shelfview-reader-shell__dictionary-actions">
        <v-btn
          small
          outlined
          data-testid="shelfview-dictionary-import"
          @click="openDictionaryPackImport"
        >
          <v-icon small>mdi-upload</v-icon>
          {{ $t('shelfview_reader.dictionary_pack_import') }}
        </v-btn>
        <input
          ref="dictionaryPackInput"
          class="shelfview-reader-shell__dictionary-file-input"
          type="file"
          accept="application/json,.json"
          data-testid="shelfview-dictionary-import-input"
          @change="onDictionaryPackFileSelected"
        />
        <v-btn
          v-if="dictionaryLocalEntries.length > 0"
          small
          outlined
          data-testid="shelfview-dictionary-clear"
          @click="clearDictionaryPack"
        >
          <v-icon small>mdi-delete-outline</v-icon>
          {{ $t('shelfview_reader.dictionary_pack_clear') }}
        </v-btn>
      </div>

      <p
        v-if="!dictionaryQueryReady"
        class="shelfview-reader-shell__dictionary-empty"
        data-testid="shelfview-dictionary-empty"
      >
        {{ $t('shelfview_reader.dictionary_empty') }}
      </p>

      <ul
        v-else
        class="shelfview-reader-shell__dictionary-results"
        data-testid="shelfview-dictionary-results"
      >
        <li
          v-for="result in dictionaryLookupResults"
          :key="result.key"
          class="shelfview-reader-shell__dictionary-result"
          :data-provider-kind="result.kind"
          data-testid="shelfview-dictionary-result"
        >
          <span class="shelfview-reader-shell__dictionary-provider">{{ result.providerLabel }}</span>
          <strong
            v-if="result.term"
            class="shelfview-reader-shell__dictionary-term"
            data-testid="shelfview-dictionary-term"
          >{{ result.term }}</strong>
          <p
            v-if="result.definition"
            class="shelfview-reader-shell__dictionary-definition"
            data-testid="shelfview-dictionary-definition"
          >{{ result.definition }}</p>
          <span
            v-if="result.sourceName"
            class="shelfview-reader-shell__dictionary-source"
            data-testid="shelfview-dictionary-source"
          >{{ result.sourceName }}</span>
          <a
            v-if="result.url"
            class="shelfview-reader-shell__dictionary-link"
            :href="result.url"
            target="_blank"
            rel="noopener noreferrer"
            data-testid="shelfview-dictionary-search"
          >
            <v-icon small>mdi-open-in-new</v-icon>
            {{ result.label }}
          </a>
        </li>
      </ul>
    </aside>

    <aside
      v-if="bookmarksDrawerOpen"
      class="shelfview-reader-shell__bookmarks-drawer"
      role="dialog"
      aria-modal="true"
      aria-labelledby="shelfview-bookmarks-title"
      data-testid="shelfview-bookmarks-drawer"
    >
      <header class="shelfview-reader-shell__drawer-header">
        <h2 id="shelfview-bookmarks-title">{{ $t('shelfview_reader.bookmarks') }}</h2>
        <v-btn
          icon
          :aria-label="$t('shelfview_reader.close')"
          data-testid="shelfview-bookmarks-close"
          @click="closeBookmarksDrawer"
        >
          <v-icon>mdi-close</v-icon>
        </v-btn>
      </header>

      <v-btn
        block
        color="primary"
        data-testid="shelfview-bookmarks-add"
        @click="addBookmarkForCurrentPage"
      >
        <v-icon left>mdi-bookmark-plus-outline</v-icon>
        {{ $t('shelfview_reader.save_page') }}
      </v-btn>

      <p
        v-if="bookmarks.length === 0"
        class="shelfview-reader-shell__bookmarks-empty"
        data-testid="shelfview-bookmarks-empty"
      >
        {{ $t('shelfview_reader.no_bookmarks') }}
      </p>

      <ul
        v-else
        class="shelfview-reader-shell__bookmarks-list"
      >
        <li
          v-for="bookmark in bookmarks"
          :key="bookmark.id"
          class="shelfview-reader-shell__bookmark-row"
        >
          <div class="shelfview-reader-shell__bookmark-main">
            <button
              type="button"
              class="shelfview-reader-shell__bookmark-link"
              data-testid="shelfview-bookmark-go"
              @click="goToBookmark(bookmark)"
            >
              {{ bookmarkLabel(bookmark) }}
            </button>
            <p
              v-if="bookmark.note && editingBookmarkId !== bookmark.id"
              class="shelfview-reader-shell__bookmark-note"
              data-testid="shelfview-bookmark-note"
            >
              {{ bookmark.note }}
            </p>
            <form
              v-if="editingBookmarkId === bookmark.id"
              class="shelfview-reader-shell__bookmark-note-editor"
              data-testid="shelfview-bookmark-note-editor"
              @submit.prevent="saveBookmarkNote(bookmark)"
            >
              <v-text-field
                v-model="bookmarkNoteDraft"
                dense
                outlined
                hide-details
                :label="$t('shelfview_reader.bookmark_note')"
                data-testid="shelfview-bookmark-note-input"
              />
              <div class="shelfview-reader-shell__bookmark-note-actions">
                <v-btn
                  icon
                  small
                  type="button"
                  :aria-label="$t('shelfview_reader.save_bookmark_note')"
                  data-testid="shelfview-bookmark-note-save"
                  @click="saveBookmarkNote(bookmark)"
                >
                  <v-icon small>mdi-check</v-icon>
                </v-btn>
                <v-btn
                  icon
                  small
                  type="button"
                  :aria-label="$t('shelfview_reader.cancel_bookmark_note')"
                  data-testid="shelfview-bookmark-note-cancel"
                  @click="cancelBookmarkNoteEdit"
                >
                  <v-icon small>mdi-close</v-icon>
                </v-btn>
              </div>
            </form>
          </div>
          <v-btn
            icon
            small
            :aria-label="$t('shelfview_reader.edit_bookmark_note')"
            data-testid="shelfview-bookmark-note-edit"
            @click="startBookmarkNoteEdit(bookmark)"
          >
            <v-icon small>mdi-note-edit-outline</v-icon>
          </v-btn>
          <v-btn
            icon
            small
            :aria-label="$t('shelfview_reader.delete_bookmark')"
            data-testid="shelfview-bookmark-delete"
            @click="deleteBookmark(bookmark)"
          >
            <v-icon small>mdi-delete-outline</v-icon>
          </v-btn>
        </li>
      </ul>
    </aside>

    <aside
      v-if="fixedPageSettingsOpen && fixedPageControlsVisible"
      class="shelfview-reader-shell__text-settings"
      role="dialog"
      aria-modal="true"
      aria-labelledby="shelfview-fixed-settings-title"
      data-testid="shelfview-reader-fixed-settings"
    >
      <header class="shelfview-reader-shell__text-settings-header">
        <h2 id="shelfview-fixed-settings-title">{{ $t('shelfview_reader.fixed_settings') }}</h2>
        <v-btn
          icon
          :aria-label="$t('shelfview_reader.fixed_settings_close')"
          data-testid="shelfview-reader-fixed-settings-close"
          @click="closeFixedPageSettings"
        >
          <v-icon>mdi-close</v-icon>
        </v-btn>
      </header>

      <div class="shelfview-reader-shell__text-settings-body">
        <v-select
          v-model="fixedPageBackground"
          :items="fixedPageBackgroundOptions"
          item-text="text"
          item-value="value"
          dense
          outlined
          hide-details
          :label="$t('shelfview_reader.background')"
          data-testid="shelfview-reader-fixed-background"
          @change="onFixedPageDisplaySettingChanged"
        />

        <div class="shelfview-reader-shell__text-setting-row">
          <label for="shelfview-fixed-page-scale">
            {{ $t('shelfview_reader.page_scale') }}
            <span>{{ fixedPageScale }}%</span>
          </label>
          <v-slider
            id="shelfview-fixed-page-scale"
            v-model.number="fixedPageScale"
            min="50"
            max="300"
            step="5"
            hide-details
            data-testid="shelfview-reader-fixed-scale"
            @input="onFixedPageDisplaySettingChanged"
          />
        </div>

        <div class="shelfview-reader-shell__text-setting-row">
          <label for="shelfview-fixed-page-margin">
            {{ $t('shelfview_reader.margin') }}
            <span>{{ fixedPageMargin }}px</span>
          </label>
          <v-slider
            id="shelfview-fixed-page-margin"
            v-model.number="fixedPageMargin"
            min="0"
            max="96"
            step="4"
            hide-details
            data-testid="shelfview-reader-fixed-margin"
            @input="onFixedPageDisplaySettingChanged"
          />
        </div>

        <div class="shelfview-reader-shell__text-setting-row">
          <label for="shelfview-fixed-page-gap">
            {{ $t('shelfview_reader.page_gap') }}
            <span>{{ fixedPageGap }}px</span>
          </label>
          <v-slider
            id="shelfview-fixed-page-gap"
            v-model.number="fixedPageGap"
            min="0"
            max="64"
            step="2"
            hide-details
            data-testid="shelfview-reader-fixed-gap"
            @input="onFixedPageDisplaySettingChanged"
          />
        </div>

        <div class="shelfview-reader-shell__text-setting-row">
          <label for="shelfview-fixed-page-brightness">
            {{ $t('shelfview_reader.brightness') }}
            <span>{{ fixedPageBrightness.toFixed(2) }}</span>
          </label>
          <v-slider
            id="shelfview-fixed-page-brightness"
            v-model.number="fixedPageBrightness"
            min="0.8"
            max="1.2"
            step="0.02"
            hide-details
            data-testid="shelfview-reader-fixed-brightness"
            @input="onFixedPageDisplaySettingChanged"
          />
        </div>

        <div class="shelfview-reader-shell__text-setting-row">
          <label for="shelfview-fixed-page-contrast">
            {{ $t('shelfview_reader.contrast') }}
            <span>{{ fixedPageContrast.toFixed(2) }}</span>
          </label>
          <v-slider
            id="shelfview-fixed-page-contrast"
            v-model.number="fixedPageContrast"
            min="0.85"
            max="1.35"
            step="0.02"
            hide-details
            data-testid="shelfview-reader-fixed-contrast"
            @input="onFixedPageDisplaySettingChanged"
          />
        </div>

        <v-checkbox
          v-model="fixedPageCoverSingle"
          dense
          hide-details
          :label="$t('shelfview_reader.cover_single')"
          data-testid="shelfview-reader-fixed-cover-single"
          @change="onFixedPageDisplaySettingChanged"
        />

        <v-btn
          block
          color="secondary"
          data-testid="shelfview-reader-fixed-settings-reset"
          @click="resetFixedPageDisplaySettings"
        >
          {{ $t('shelfview_reader.reset') }}
        </v-btn>
      </div>
    </aside>

    <aside
      v-if="textSettingsOpen && textControlsVisible"
      class="shelfview-reader-shell__text-settings"
      role="dialog"
      aria-modal="true"
      aria-labelledby="shelfview-text-settings-title"
      data-testid="shelfview-reader-text-settings"
    >
      <header class="shelfview-reader-shell__text-settings-header">
        <h2 id="shelfview-text-settings-title">{{ $t('shelfview_reader.text_settings') }}</h2>
        <v-btn
          icon
          :aria-label="$t('shelfview_reader.text_settings_close')"
          data-testid="shelfview-reader-text-settings-close"
          @click="closeTextSettings"
        >
          <v-icon>mdi-close</v-icon>
        </v-btn>
      </header>

      <div class="shelfview-reader-shell__text-settings-body">
        <div
          class="shelfview-reader-shell__text-settings-primary"
          data-testid="shelfview-reader-text-settings-primary"
        >
          <div class="shelfview-reader-shell__text-setting-group">
            <span class="shelfview-reader-shell__text-setting-group-label">{{ $t('shelfview_reader.control_text_theme') }}</span>
            <div
              class="shelfview-reader-shell__text-theme-chips"
              data-testid="shelfview-reader-text-theme-chips"
            >
              <v-btn
                v-for="option in textThemeOptions"
                :key="`text-theme-chip-${option.value}`"
                small
                :color="option.active ? 'primary' : undefined"
                :outlined="!option.active"
                :aria-pressed="option.active ? 'true' : 'false'"
                :data-testid="`shelfview-reader-text-theme-chip-${option.value}`"
                @click="selectTextThemeOption(option.value)"
              >
                <v-icon
                  left
                  small
                >{{ option.icon }}</v-icon>
                <span>{{ option.label }}</span>
              </v-btn>
            </div>
          </div>

          <div class="shelfview-reader-shell__text-setting-row shelfview-reader-shell__text-setting-row--stepper">
            <span class="shelfview-reader-shell__text-setting-row-label">{{ $t('shelfview_reader.font_size') }}</span>
            <div class="shelfview-reader-shell__text-font-size-stepper">
              <v-btn
                icon
                small
                :aria-label="$t('shelfview_reader.font_size_decrease')"
                data-testid="shelfview-reader-text-font-size-decrease"
                @click="adjustTextFontSize(-1)"
              >
                <span aria-hidden="true">A-</span>
              </v-btn>
              <span
                class="shelfview-reader-shell__text-font-size-value"
                data-testid="shelfview-reader-text-font-size"
              >{{ textFontSize }}px</span>
              <v-btn
                icon
                small
                :aria-label="$t('shelfview_reader.font_size_increase')"
                data-testid="shelfview-reader-text-font-size-increase"
                @click="adjustTextFontSize(1)"
              >
                <span aria-hidden="true">A+</span>
              </v-btn>
            </div>
          </div>

          <div class="shelfview-reader-shell__text-setting-row">
            <label for="shelfview-text-line-height">
              {{ $t('shelfview_reader.line_height') }}
              <span>{{ textLineHeight.toFixed(2) }}</span>
            </label>
            <v-slider
              id="shelfview-text-line-height"
              v-model.number="textLineHeight"
              min="1.35"
              max="2.4"
              step="0.05"
              hide-details
              data-testid="shelfview-reader-text-line-height"
              @input="onTextDisplaySettingChanged"
            />
          </div>
        </div>

        <v-expansion-panels
          accordion
          dark
          flat
          class="shelfview-reader-shell__text-settings-advanced"
          data-testid="shelfview-reader-text-settings-advanced"
        >
          <v-expansion-panel>
            <v-expansion-panel-header data-testid="shelfview-reader-text-settings-more">
              {{ $t('shelfview_reader.text_settings_more') }}
            </v-expansion-panel-header>
            <v-expansion-panel-content>
              <div class="shelfview-reader-shell__text-settings-advanced-content">
                <v-select
                  v-model="textFontFamily"
                  :items="textFontFamilyOptions"
                  item-text="text"
                  item-value="value"
                  dense
                  outlined
                  hide-details
                  :label="$t('shelfview_reader.font')"
                  data-testid="shelfview-reader-text-font-family"
                  @change="onTextDisplaySettingChanged"
                />

                <v-select
                  v-model="textAlign"
                  :items="textAlignOptions"
                  item-text="text"
                  item-value="value"
                  dense
                  outlined
                  hide-details
                  :label="$t('shelfview_reader.align')"
                  data-testid="shelfview-reader-text-align"
                  @change="onTextDisplaySettingChanged"
                />

                <div class="shelfview-reader-shell__text-setting-row">
                  <label for="shelfview-text-letter-spacing">
                    {{ $t('shelfview_reader.letter_spacing') }}
                    <span>{{ textLetterSpacing.toFixed(1) }}px</span>
                  </label>
                  <v-slider
                    id="shelfview-text-letter-spacing"
                    v-model.number="textLetterSpacing"
                    min="0"
                    max="2.5"
                    step="0.1"
                    hide-details
                    data-testid="shelfview-reader-text-letter-spacing"
                    @input="onTextDisplaySettingChanged"
                  />
                </div>

                <div class="shelfview-reader-shell__text-setting-row">
                  <label for="shelfview-text-word-spacing">
                    {{ $t('shelfview_reader.word_spacing') }}
                    <span>{{ textWordSpacing.toFixed(1) }}px</span>
                  </label>
                  <v-slider
                    id="shelfview-text-word-spacing"
                    v-model.number="textWordSpacing"
                    min="0"
                    max="8"
                    step="0.5"
                    hide-details
                    data-testid="shelfview-reader-text-word-spacing"
                    @input="onTextDisplaySettingChanged"
                  />
                </div>

                <div class="shelfview-reader-shell__text-setting-row">
                  <label for="shelfview-text-content-width">
                    {{ $t('shelfview_reader.width') }}
                    <span>{{ textContentWidth }}px</span>
                  </label>
                  <v-slider
                    id="shelfview-text-content-width"
                    v-model.number="textContentWidth"
                    min="520"
                    max="1180"
                    step="20"
                    hide-details
                    data-testid="shelfview-reader-text-content-width"
                    @input="onTextDisplaySettingChanged"
                  />
                </div>

                <div class="shelfview-reader-shell__text-setting-row">
                  <label for="shelfview-text-page-margin">
                    {{ $t('shelfview_reader.margin') }}
                    <span>{{ textPageMargin }}px</span>
                  </label>
                  <v-slider
                    id="shelfview-text-page-margin"
                    v-model.number="textPageMargin"
                    min="16"
                    max="120"
                    step="4"
                    hide-details
                    data-testid="shelfview-reader-text-page-margin"
                    @input="onTextDisplaySettingChanged"
                  />
                </div>

                <div class="shelfview-reader-shell__text-setting-row">
                  <label for="shelfview-text-paragraph-spacing">
                    {{ $t('shelfview_reader.paragraph') }}
                    <span>{{ textParagraphSpacing.toFixed(2) }}em</span>
                  </label>
                  <v-slider
                    id="shelfview-text-paragraph-spacing"
                    v-model.number="textParagraphSpacing"
                    min="0.6"
                    max="2.6"
                    step="0.05"
                    hide-details
                    data-testid="shelfview-reader-text-paragraph-spacing"
                    @input="onTextDisplaySettingChanged"
                  />
                </div>

                <div class="shelfview-reader-shell__text-setting-row">
                  <label for="shelfview-text-font-weight">
                    {{ $t('shelfview_reader.weight') }}
                    <span>{{ textFontWeight }}</span>
                  </label>
                  <v-slider
                    id="shelfview-text-font-weight"
                    v-model.number="textFontWeight"
                    min="300"
                    max="700"
                    step="50"
                    hide-details
                    data-testid="shelfview-reader-text-font-weight"
                    @input="onTextDisplaySettingChanged"
                  />
                </div>

                <div class="shelfview-reader-shell__text-setting-row">
                  <label for="shelfview-text-brightness">
                    {{ $t('shelfview_reader.brightness') }}
                    <span>{{ textBrightness.toFixed(2) }}</span>
                  </label>
                  <v-slider
                    id="shelfview-text-brightness"
                    v-model.number="textBrightness"
                    min="0.8"
                    max="1.2"
                    step="0.02"
                    hide-details
                    data-testid="shelfview-reader-text-brightness"
                    @input="onTextDisplaySettingChanged"
                  />
                </div>

                <div class="shelfview-reader-shell__text-setting-row">
                  <label for="shelfview-text-tts-rate">
                    {{ $t('shelfview_reader.read_aloud_speed') }}
                    <span>{{ textToSpeechRate.toFixed(1) }}x</span>
                  </label>
                  <v-slider
                    id="shelfview-text-tts-rate"
                    v-model.number="textToSpeechRate"
                    min="0.7"
                    max="1.4"
                    step="0.1"
                    hide-details
                    data-testid="shelfview-reader-tts-rate"
                    @input="onTextToSpeechRateChanged"
                  />
                </div>

                <v-select
                  v-if="textToSpeechVoiceOptions.length > 1"
                  v-model="textToSpeechVoiceURI"
                  :items="textToSpeechVoiceOptions"
                  item-text="text"
                  item-value="value"
                  dense
                  outlined
                  hide-details
                  :label="$t('shelfview_reader.read_aloud_voice')"
                  data-testid="shelfview-reader-tts-voice"
                />
              </div>
            </v-expansion-panel-content>
          </v-expansion-panel>
        </v-expansion-panels>

        <v-btn
          block
          color="secondary"
          data-testid="shelfview-reader-text-settings-reset"
          @click="resetTextDisplaySettings"
        >
          {{ $t('shelfview_reader.reset') }}
        </v-btn>
      </div>
    </aside>

    <section
      v-if="showShortcutHelp"
      class="shelfview-reader-shell__shortcut-dialog"
      role="dialog"
      aria-modal="true"
      aria-labelledby="shelfview-reader-shortcuts-title"
      data-testid="shelfview-reader-shortcuts"
    >
      <div class="shelfview-reader-shell__shortcut-panel">
        <h2 id="shelfview-reader-shortcuts-title">{{ $t('shelfview_reader.shortcuts') }}</h2>
        <dl>
          <template v-for="shortcut in shortcutRows">
            <dt :key="`${shortcut.keys}-keys`">{{ shortcut.keys }}</dt>
            <dd :key="`${shortcut.keys}-label`">{{ shortcut.label }}</dd>
          </template>
        </dl>
        <v-btn
          color="primary"
          data-testid="shelfview-reader-shortcuts-close"
          @click="closeShortcutHelp"
        >
          {{ $t('shelfview_reader.shortcuts_close') }}
        </v-btn>
      </div>
    </section>

    <section
      v-if="endOfBookPromptOpen"
      class="shelfview-reader-shell__end-dialog"
      role="dialog"
      aria-modal="true"
      aria-labelledby="shelfview-reader-end-title"
      data-testid="shelfview-reader-end-prompt"
    >
      <div class="shelfview-reader-shell__end-panel">
        <h2 id="shelfview-reader-end-title">{{ $t('shelfview_reader.end_of_book_title') }}</h2>
        <p>{{ $t('shelfview_reader.end_of_book_message') }}</p>
        <p
          v-if="siblingNext"
          class="shelfview-reader-shell__end-next-title"
          data-testid="shelfview-reader-end-next-title"
        >
          {{ siblingNextTitle }}
        </p>
        <p
          v-else
          class="shelfview-reader-shell__end-no-next"
          data-testid="shelfview-reader-end-no-next"
        >
          {{ $t('shelfview_reader.no_next_book') }}
        </p>
        <div class="shelfview-reader-shell__end-actions">
          <v-btn
            v-if="siblingNext"
            color="primary"
            data-testid="shelfview-reader-end-next"
            @click="goToNextBook"
          >
            {{ $t('shelfview_reader.read_next_book') }}
          </v-btn>
          <v-btn
            color="secondary"
            data-testid="shelfview-reader-end-exit"
            @click="exitEndOfBookPrompt"
          >
            {{ $t('shelfview_reader.exit_reader') }}
          </v-btn>
          <v-btn
            text
            data-testid="shelfview-reader-end-close"
            @click="closeEndOfBookPrompt"
          >
            {{ $t('shelfview_reader.shortcuts_close') }}
          </v-btn>
        </div>
      </div>
    </section>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import screenfull from 'screenfull'
import {RawLocation} from 'vue-router/types/router'
import {BookDto} from '@/types/tales-books'
import {SeriesDto} from '@/types/tales-series'
import {Context, ContextOrigin} from '@/types/context'
import {getBookTitleCompact} from '@/functions/book-title'
import {getBookReadRouteFromMedia, getLegacyBookReadRouteFromMedia} from '@/functions/book-format'
import {
  ShelfViewManifest,
  ShelfViewManifestContentsItem,
  ShelfViewManifestItem,
} from '@/shelfview/models/shelfview-manifest'
import {ShelfViewApiError} from '@/shelfview/models/shelfview-errors'
import {ReaderAdapter} from '@/shelfview/adapters/reader-adapter'
import {FixedPageAdapter} from '@/shelfview/adapters/fixed-page-adapter'
import {TextReflowAdapter} from '@/shelfview/adapters/text-reflow-adapter'
import {MixedContentAdapter} from '@/shelfview/adapters/mixed-content-adapter'
import ShelfViewControlMenu from '@/shelfview/components/ShelfViewControlMenu.vue'
import ShelfViewCanvasImagePage from '@/shelfview/components/ShelfViewCanvasImagePage.vue'
import {ShelfViewControlMenuOption} from '@/shelfview/models/shelfview-control-menu'
import {
  SHELFVIEW_LOCATOR_SCHEMA_VERSION,
  ShelfViewLocator,
  ShelfViewPageLocator,
  ShelfViewTextLocator,
} from '@/shelfview/models/shelfview-locator'
import {ShelfViewTextParagraph} from '@/shelfview/models/shelfview-text'
import {
  SHELFVIEW_HIGHLIGHT_SELECTOR_SCHEMA_VERSION,
  ShelfViewBookmarkDto,
  ShelfViewFixedPagePreferenceSettings,
  ShelfViewHighlightColor,
  ShelfViewHighlightDto,
  ShelfViewHighlightSelector,
  ShelfViewHighlightUpdateDto,
  ShelfViewPreferenceFormat,
  ShelfViewPreferenceResponseDto,
  ShelfViewPositionDto,
  ShelfViewTextRangeHighlightSelector,
} from '@/shelfview/models/shelfview-state'
import {
  SHELFVIEW_FIXED_PAGE_FITS,
  SHELFVIEW_FIXED_PAGE_BACKGROUNDS,
  SHELFVIEW_FIXED_PAGE_ROTATIONS,
  ShelfViewFixedPageBackground,
  ShelfViewFixedPageFit,
  ShelfViewFixedPageLayout,
  ShelfViewFixedPageReadingDirection,
  ShelfViewFixedPageRotation,
  normalizeFixedPageRotation,
  shelfViewFixedPageBackground,
  shelfViewCanGoNext,
  shelfViewCanGoPrevious,
  shelfViewNextFixedPageRotation,
  shelfViewNextPageIndex,
  shelfViewPreviousPageIndex,
  shelfViewVisiblePageIndexes,
} from '@/shelfview/functions/fixed-page-view'
import {
  SHELFVIEW_IMAGE_FILTER_PRIMARY_PRESETS,
  ShelfViewImageFilterPreset,
  shelfViewImageFilterPrimaryPreset,
  shelfViewImageFilterPreset,
  shelfViewImageFilterStyle,
  shelfViewImageFilterUsesCanvas,
} from '@/shelfview/functions/image-filter-presets'
import {
  ShelfViewReaderInputAction,
  ShelfViewTouchPoint,
  shelfViewReaderKeyboardAction,
  shelfViewShouldIgnoreShortcutTarget,
  shelfViewTapAction,
  shelfViewTouchAction,
} from '@/shelfview/functions/reader-input'
import {
  SHELFVIEW_FIXED_PAGE_DISPLAY_DEFAULTS,
  SHELFVIEW_TEXT_DISPLAY_DEFAULTS,
  shelfViewNormalizeFixedPageDisplaySettings,
  shelfViewNormalizeFixedPageScale,
  shelfViewNormalizeTextDisplaySettings,
} from '@/shelfview/functions/reader-display-settings'
import {
  ShelfViewDictionaryPack,
  ShelfViewDictionaryLocalEntry,
  ShelfViewDictionaryLookupResult,
  shelfViewDictionaryLookupResults,
  shelfViewNormalizeDictionaryQuery,
  shelfViewParseDictionaryPack,
  shelfViewWebDictionaryUrl,
} from '@/shelfview/functions/dictionary-lookup'
import {
  ShelfViewPanStart,
  shelfViewAnchoredZoomScroll,
  shelfViewPanScroll,
  shelfViewShouldStartPanning,
  shelfViewTextCharacterOffsetForScroll,
  shelfViewTextScrollTopForOffset,
} from '@/shelfview/functions/reader-viewport'
import {shelfViewThumbnailUrl} from '@/shelfview/functions/shelfview-urls'
import {
  SHELFVIEW_TEXT_ALIGNS,
  SHELFVIEW_TEXT_FONT_FAMILIES,
  SHELFVIEW_TEXT_THEMES,
  ShelfViewTextAlign,
  ShelfViewTextFontFamily,
  ShelfViewTextTheme,
  shelfViewTextAlign,
  shelfViewTextFontFamily,
  shelfViewTextTheme,
} from '@/shelfview/functions/text-reflow-view'

interface ShelfViewPageSource {
  pageIndex: number,
  url: string,
}

interface ShelfViewTextSearchMatch {
  paragraphIndex: number,
  characterOffset: number,
}

interface ShelfViewTextContentsItem {
  paragraphIndex: number,
  label: string,
}

interface ShelfViewReaderSelectOption {
  text: string,
  value: string,
}

interface ShelfViewTextParagraphSegment {
  key: string,
  text: string,
  highlighted: boolean,
  color?: ShelfViewHighlightColor,
}

interface ShelfViewTextHighlightRange {
  start: number,
  end: number,
  color: ShelfViewHighlightColor,
}

interface ShelfViewTextSelectionEndpoint {
  paragraphIndex: number,
  characterOffset: number,
}

interface ShelfViewPointerTapStart extends ShelfViewTouchPoint {
  pointerId: number,
}

interface ShelfViewSelectedTextRange {
  startParagraph: number,
  startCharacterOffset: number,
  endParagraph: number,
  endCharacterOffset: number,
  textQuote?: string,
}

interface ShelfViewDictionaryLookupDisplayResult extends ShelfViewDictionaryLookupResult {
  providerLabel: string,
  label: string,
}

type ShelfViewReaderStateStatus =
  | 'IDLE'
  | 'PENDING'
  | 'SAVING'
  | 'SAVED'
  | 'ERROR'
  | 'OFFLINE'

const SHELFVIEW_FIXED_PAGE_FORMAT: ShelfViewPreferenceFormat = 'FIXED_PAGE_SEQUENCE'
const SHELFVIEW_TEXT_FORMAT: ShelfViewPreferenceFormat = 'TEXT_REFLOW'
const POSITION_SAVE_DEBOUNCE_MS = 2000
const PREFERENCE_SAVE_DEBOUNCE_MS = 600
const SLIDESHOW_INTERVAL_MS = 5000
const FIXED_PAGE_WHEEL_SCALE_STEP = 10
const TEXT_CONTENTS_LABEL_MAX_LENGTH = 80
const TEXT_CONTENTS_HEADING_MAX_LENGTH = 96
const TEXT_HIGHLIGHT_QUOTE_MAX_LENGTH = 500
const CHROME_HINT_TIMEOUT_MS = 3000
const CHROME_AUTO_HIDE_DELAY_MS = 2500
const TEXT_TO_SPEECH_RATE_DEFAULT = 1
const TEXT_TO_SPEECH_RATE_MIN = 0.7
const TEXT_TO_SPEECH_RATE_MAX = 1.4
const DICTIONARY_PACK_STORAGE_KEY = 'shelfview.dictionaryPack.v1'
const CHROME_HINT_STORAGE_KEY = 'shelfview.chromeHintSeen.v1'

export default Vue.extend({
  name: 'ShelfViewReaderShell',
  components: {
    ShelfViewCanvasImagePage,
    ShelfViewControlMenu,
  },
  props: {
    bookId: {
      type: String,
      required: true,
    },
  },
  data: () => ({
    book: undefined as unknown as BookDto,
    series: undefined as unknown as SeriesDto,
    manifest: undefined as unknown as ShelfViewManifest,
    adapter: undefined as ReaderAdapter | undefined,
    currentLocator: undefined as ShelfViewLocator | undefined,
    siblingNext: undefined as BookDto | undefined,
    context: {} as Context,
    error: undefined as Error | ShelfViewApiError | undefined,
    loading: true,
    pageImageFailed: false,
    pageImageReloadNonce: 0,
    textEncodingOverride: undefined as string | undefined,
    readerLayout: 'SINGLE_PAGE' as ShelfViewFixedPageLayout,
    readingDirection: 'LTR' as ShelfViewFixedPageReadingDirection,
    pageFit: 'SCREEN' as ShelfViewFixedPageFit,
    imageFilterPreset: 'ORIGINAL' as ShelfViewImageFilterPreset,
    fixedPageRotation: SHELFVIEW_FIXED_PAGE_DISPLAY_DEFAULTS.fixedPageRotation,
    fixedPageCoverSingle: SHELFVIEW_FIXED_PAGE_DISPLAY_DEFAULTS.fixedPageCoverSingle,
    fixedPageScale: SHELFVIEW_FIXED_PAGE_DISPLAY_DEFAULTS.fixedPageScale,
    fixedPageGap: SHELFVIEW_FIXED_PAGE_DISPLAY_DEFAULTS.fixedPageGap,
    fixedPageMargin: SHELFVIEW_FIXED_PAGE_DISPLAY_DEFAULTS.fixedPageMargin,
    fixedPageBackground: SHELFVIEW_FIXED_PAGE_DISPLAY_DEFAULTS.fixedPageBackground,
    fixedPageBrightness: SHELFVIEW_FIXED_PAGE_DISPLAY_DEFAULTS.fixedPageBrightness,
    fixedPageContrast: SHELFVIEW_FIXED_PAGE_DISPLAY_DEFAULTS.fixedPageContrast,
    textTheme: SHELFVIEW_TEXT_DISPLAY_DEFAULTS.textTheme,
    textFontFamily: SHELFVIEW_TEXT_DISPLAY_DEFAULTS.textFontFamily,
    textFontSize: SHELFVIEW_TEXT_DISPLAY_DEFAULTS.textFontSize,
    textLineHeight: SHELFVIEW_TEXT_DISPLAY_DEFAULTS.textLineHeight,
    textLetterSpacing: SHELFVIEW_TEXT_DISPLAY_DEFAULTS.textLetterSpacing,
    textWordSpacing: SHELFVIEW_TEXT_DISPLAY_DEFAULTS.textWordSpacing,
    textContentWidth: SHELFVIEW_TEXT_DISPLAY_DEFAULTS.textContentWidth,
    textPageMargin: SHELFVIEW_TEXT_DISPLAY_DEFAULTS.textPageMargin,
    textParagraphSpacing: SHELFVIEW_TEXT_DISPLAY_DEFAULTS.textParagraphSpacing,
    textAlign: SHELFVIEW_TEXT_DISPLAY_DEFAULTS.textAlign,
    textFontWeight: SHELFVIEW_TEXT_DISPLAY_DEFAULTS.textFontWeight,
    textBrightness: SHELFVIEW_TEXT_DISPLAY_DEFAULTS.textBrightness,
    fixedPageSettingsOpen: false,
    textSettingsOpen: false,
    openControlMenu: null as string | null,
    controlMenuLeft: 0,
    chromeVisible: false,
    overflowMenuOpen: false,
    thumbnailStripOpen: false,
    chromeHintVisible: false,
    chromeHintTimeoutMs: CHROME_HINT_TIMEOUT_MS,
    chromeAutoHideDelayMs: CHROME_AUTO_HIDE_DELAY_MS,
    chromeIdleTimer: undefined as number | undefined,
    textSearchOpen: false,
    textSearchQuery: '',
    textSearchMatchIndex: 0,
    textToSpeechActive: false,
    textToSpeechRate: TEXT_TO_SPEECH_RATE_DEFAULT,
    textToSpeechVoiceURI: '',
    textToSpeechVoices: [] as SpeechSynthesisVoice[],
    textToSpeechUtterance: undefined as SpeechSynthesisUtterance | undefined,
    pageJumpInput: '1',
    slideshowActive: false,
    slideshowTimer: undefined as number | undefined,
    showShortcutHelp: false,
    fullscreenAvailable: false,
    isFullscreen: false,
    endOfBookPromptOpen: false,
    bookmarks: [] as ShelfViewBookmarkDto[],
    highlights: [] as ShelfViewHighlightDto[],
    bookmarksDrawerOpen: false,
    contentsDrawerOpen: false,
    dictionaryDrawerOpen: false,
    dictionaryQuery: '',
    dictionaryLocalEntries: [] as ShelfViewDictionaryLocalEntry[],
    editingBookmarkId: null as string | null,
    bookmarkNoteDraft: '',
    readerStateStatus: 'IDLE' as ShelfViewReaderStateStatus,
    readerStateMessage: '',
    positionSaveTimer: undefined as number | undefined,
    preferenceSaveTimer: undefined as number | undefined,
    touchStart: undefined as ShelfViewTouchPoint | undefined,
    readerPointerTapStart: undefined as ShelfViewPointerTapStart | undefined,
    fixedPagePanStart: undefined as ShelfViewPanStart | undefined,
    fixedPagePanning: false,
    setupRequestId: 0,
  }),
  async mounted() {
    window.addEventListener('online', this.onReaderOnline)
    window.addEventListener('offline', this.onReaderOffline)
    window.addEventListener('resize', this.onReaderViewportChanged)
    window.addEventListener('orientationchange', this.onReaderViewportChanged)
    window.addEventListener('pagehide', this.onPageHide)
    window.addEventListener('keydown', this.onReaderKeydown)
    document.addEventListener('visibilitychange', this.onVisibilityChange)
    if (screenfull.isEnabled) {
      this.fullscreenAvailable = true
      this.isFullscreen = screenfull.isFullscreen
      screenfull.on('change', this.onFullscreenChange)
    }
    this.refreshTextToSpeechVoices()
    if (this.browserTextToSpeechVoiceListAvailable()) {
      window.speechSynthesis.addEventListener('voiceschanged', this.refreshTextToSpeechVoices)
    }
    await this.setup(this.bookId)
  },
  beforeDestroy() {
    this.setupRequestId += 1
    void this.flushReaderState()
    this.clearReaderStateTimers()
    this.stopSlideshow()
    window.removeEventListener('online', this.onReaderOnline)
    window.removeEventListener('offline', this.onReaderOffline)
    window.removeEventListener('resize', this.onReaderViewportChanged)
    window.removeEventListener('orientationchange', this.onReaderViewportChanged)
    window.removeEventListener('pagehide', this.onPageHide)
    window.removeEventListener('keydown', this.onReaderKeydown)
    document.removeEventListener('visibilitychange', this.onVisibilityChange)
    if (screenfull.isEnabled) {
      screenfull.off('change', this.onFullscreenChange)
      if (screenfull.isFullscreen) void screenfull.exit()
    }
    if (this.browserTextToSpeechVoiceListAvailable()) {
      window.speechSynthesis.removeEventListener('voiceschanged', this.refreshTextToSpeechVoices)
    }
    this.destroyAdapter()
  },
  async beforeRouteUpdate(to, from, next) {
    if (to.params.bookId !== from.params.bookId) {
      this.textEncodingOverride = undefined
      await this.flushReaderState()
      await this.setup(to.params.bookId)
    }
    next()
  },
  watch: {
    chromeVisible(visible: boolean) {
      if (visible) {
        this.scheduleChromeAutoHide()
      } else {
        this.clearChromeAutoHide()
      }
    },
    anyChromePanelOpen(open: boolean) {
      if (open) {
        this.clearChromeAutoHide()
      } else {
        this.scheduleChromeAutoHide()
      }
    },
  },
  computed: {
    bookTitle(): string {
      if (this.book && this.series) {
        return getBookTitleCompact(this.book.metadata.title, this.series.metadata.title, this.book.oneshot ? undefined : this.book.metadata.number)
      }
      return 'Tales'
    },
    siblingNextTitle(): string {
      if (!this.siblingNext) return ''
      return this.siblingNext.metadata?.title || this.siblingNext.name || this.$t('shelfview_reader.read_next_book') as string
    },
    pageControlsVisible(): boolean {
      return !!this.adapter && !this.hasDisplayError && !this.empty && this.pagesCount > 0
    },
    anyChromePanelOpen(): boolean {
      return !!this.openControlMenu ||
        this.textSearchOpen ||
        this.fixedPageSettingsOpen ||
        this.textSettingsOpen ||
        this.showShortcutHelp ||
        this.overflowMenuOpen ||
        this.endOfBookPromptOpen ||
        this.bookmarksDrawerOpen ||
        this.contentsDrawerOpen ||
        this.dictionaryDrawerOpen
    },
    fixedPageControlsVisible(): boolean {
      return this.manifest?.kind === 'FIXED_PAGE_SEQUENCE' || !!this.mixedContentAdapter?.isCurrentFixedPage()
    },
    textControlsVisible(): boolean {
      return this.manifest?.kind === 'TEXT_REFLOW' || !!this.mixedContentAdapter?.isCurrentText()
    },
    tapPagingEnabled(): boolean {
      return this.fixedPageControlsVisible &&
        this.readerLayout !== 'CONTINUOUS_SCROLL' &&
        this.fixedPageScale <= 100
    },
    textSearchAvailable(): boolean {
      return this.textControlsVisible && !!this.manifest?.capabilities.search
    },
    textToSpeechAvailable(): boolean {
      return this.textControlsVisible && this.currentTextToSpeechText.length > 0 && this.browserTextToSpeechAvailable()
    },
    textToSpeechLabel(): string {
      return this.textToSpeechActive
        ? this.$t('shelfview_reader.stop_read_aloud') as string
        : this.$t('shelfview_reader.read_aloud') as string
    },
    textToSpeechVoiceOptions(): ShelfViewReaderSelectOption[] {
      return [
        {
          text: this.$t('shelfview_reader.read_aloud_voice_default') as string,
          value: '',
        },
        ...this.textToSpeechVoices.map(voice => ({
          text: voice.lang ? `${voice.name} (${voice.lang})` : voice.name,
          value: voice.voiceURI,
        })),
      ]
    },
    currentTextToSpeechText(): string {
      const locator = this.currentTextLocator()
      if (!locator) return ''

      return this.visibleTextParagraphs
        .find(paragraph => paragraph.paragraphIndex === locator.paragraph)
        ?.text
        .trim() ?? ''
    },
    currentTextParagraphHighlighted(): boolean {
      const locator = this.currentTextLocator()
      return !!locator && (this.textParagraphBookmarked(locator.paragraph) || this.textParagraphHasDedicatedHighlight(locator.paragraph))
    },
    textHighlightAvailable(): boolean {
      return !!this.currentTextLocator()
    },
    textHighlightLabel(): string {
      return this.$t('shelfview_reader.highlight_selection') as string
    },
    dictionaryQueryReady(): boolean {
      return this.normalizedDictionaryQuery(this.dictionaryQuery).length > 0
    },
    dictionarySearchUrl(): string {
      return shelfViewWebDictionaryUrl(this.dictionaryQuery)
    },
    dictionaryLookupResults(): ShelfViewDictionaryLookupDisplayResult[] {
      return shelfViewDictionaryLookupResults(this.dictionaryQuery, this.dictionaryLocalEntries)
        .map(result => ({
          ...result,
          providerLabel: this.$t(result.providerLabelKey) as string,
          label: result.actionLabelKey ? this.$t(result.actionLabelKey) as string : result.term ?? '',
        }))
    },
    contentsAvailable(): boolean {
      return this.manifestContentsItems.length > 0 || this.mixedContentsItems.length > 1 || this.textContentsItems.length > 1
    },
    manifestContentsItems(): ShelfViewManifestContentsItem[] {
      return this.manifest?.contents ?? []
    },
    mixedContentsItems(): ShelfViewManifestItem[] {
      if (this.manifest?.kind !== 'MIXED_CONTENT_SEQUENCE') return []
      return [...(this.manifest.items ?? [])].sort((a, b) => a.index - b.index)
    },
    textContentsItems(): ShelfViewTextContentsItem[] {
      if (!this.textControlsVisible || this.mixedContentsItems.length > 1) return []

      return this.textContentsParagraphs().map(paragraph => ({
        paragraphIndex: paragraph.paragraphIndex,
        label: this.textContentsLabel(paragraph),
      }))
    },
    readerStateNoticeVisible(): boolean {
      return this.readerStateStatus !== 'IDLE' && this.readerStateMessage.length > 0
    },
    readerStateNoticeClass(): object {
      return {
        'shelfview-reader-shell__sync-status--error': this.readerStateStatus === 'ERROR' || this.readerStateStatus === 'OFFLINE',
        'shelfview-reader-shell__sync-status--saved': this.readerStateStatus === 'SAVED',
      }
    },
    readerIncognito(): boolean {
      return this.$route.query.incognito?.toString().toLowerCase() === 'true'
    },
    availableReaderLayouts(): ShelfViewFixedPageLayout[] {
      if (this.manifest?.kind === 'TEXT_REFLOW' || this.mixedContentAdapter?.isCurrentText()) {
        const layouts: ShelfViewFixedPageLayout[] = ['SINGLE_PAGE']
        if (this.manifest.capabilities.continuousScroll) layouts.push('CONTINUOUS_SCROLL')
        return layouts
      }

      const layouts: ShelfViewFixedPageLayout[] = ['SINGLE_PAGE']
      if (this.manifest?.capabilities.doublePage) layouts.push('DOUBLE_PAGE')
      if (this.manifest?.capabilities.continuousScroll) layouts.push('CONTINUOUS_SCROLL')
      return layouts
    },
    fixedPageAdapter(): FixedPageAdapter | undefined {
      return this.adapter instanceof FixedPageAdapter ? this.adapter : undefined
    },
    textAdapter(): TextReflowAdapter | undefined {
      return this.adapter instanceof TextReflowAdapter ? this.adapter : undefined
    },
    mixedContentAdapter(): MixedContentAdapter | undefined {
      return this.adapter instanceof MixedContentAdapter ? this.adapter : undefined
    },
    pagesCount(): number {
      if (this.fixedPageAdapter) return this.fixedPageAdapter.getPagesCount()
      if (this.textAdapter) return this.textAdapter.getParagraphsCount()
      if (this.mixedContentAdapter) return this.mixedContentAdapter.getCurrentContentCount()
      return this.manifest?.pageCount || 0
    },
    currentPageIndex(): number {
      if (this.currentLocator?.kind === 'PAGE') return this.currentLocator.page
      if (this.currentLocator?.kind === 'TEXT') return this.currentLocator.paragraph
      if (this.currentLocator?.kind === 'MIXED') {
        return this.currentLocator.itemLocator.kind === 'PAGE'
          ? this.currentLocator.itemLocator.page
          : this.currentLocator.itemLocator.paragraph
      }
      return 0
    },
    currentPageNumber(): number {
      return this.currentPageIndex + 1
    },
    currentPageLabel(): string {
      const visiblePages = this.visiblePageSources.map(page => page.pageIndex + 1)
      if (visiblePages.length > 1 && this.readerLayout === 'DOUBLE_PAGE') {
        return `${Math.min(...visiblePages)}-${Math.max(...visiblePages)}`
      }
      return this.currentPageNumber.toString()
    },
    readerAriaLabel(): string {
      return `${this.bookTitle} reader, ${this.currentLocationUnit} ${this.currentPageLabel} of ${this.pagesCount}`
    },
    readerLiveStatus(): string {
      return `${this.currentLocationUnitLabel} ${this.currentPageLabel} of ${this.pagesCount}`
    },
    currentLocationUnit(): string {
      return this.textControlsVisible ? 'paragraph' : 'page'
    },
    currentLocationUnitLabel(): string {
      return this.textControlsVisible ? 'Paragraph' : 'Page'
    },
    shortcutRows(): Array<{ keys: string, label: string }> {
      return [
        {keys: this.$t('shelfview_reader.shortcut_keys_move') as string, label: this.$t('shelfview_reader.shortcut_move_pages') as string},
        {keys: this.$t('shelfview_reader.shortcut_keys_page') as string, label: this.$t('shelfview_reader.shortcut_move_pages') as string},
        {keys: this.$t('shelfview_reader.shortcut_keys_space') as string, label: this.$t('shelfview_reader.shortcut_next_page') as string},
        {keys: this.$t('shelfview_reader.shortcut_keys_home_end') as string, label: this.$t('shelfview_reader.shortcut_first_last') as string},
        {keys: this.$t('shelfview_reader.shortcut_keys_escape') as string, label: this.$t('shelfview_reader.shortcut_toggle_chrome') as string},
        ...(this.fixedPageControlsVisible ? [{keys: this.$t('shelfview_reader.shortcut_keys_side_tap') as string, label: this.$t('shelfview_reader.shortcut_side_tap_pages') as string}] : []),
        {keys: this.$t('shelfview_reader.shortcut_keys_center_tap') as string, label: this.$t('shelfview_reader.shortcut_center_tap_chrome') as string},
        ...(this.fixedPageControlsVisible ? [{keys: this.$t('shelfview_reader.shortcut_keys_rotate') as string, label: this.$t('shelfview_reader.shortcut_rotate') as string}] : []),
        ...(this.fullscreenAvailable ? [{keys: this.$t('shelfview_reader.shortcut_keys_fullscreen') as string, label: this.$t('shelfview_reader.shortcut_fullscreen') as string}] : []),
        {keys: this.$t('shelfview_reader.shortcut_keys_help') as string, label: this.$t('shelfview_reader.shortcut_show') as string},
      ]
    },
    canGoPrevious(): boolean {
      if (this.mixedContentAdapter) return this.mixedContentAdapter.canGoPrevious()
      if (this.textAdapter) return this.textAdapter.canGoPrevious()
      return !!this.adapter && shelfViewCanGoPrevious(this.currentPageIndex, this.pagesCount, this.readerLayout, this.fixedPageCoverSingle)
    },
    canGoNext(): boolean {
      if (this.mixedContentAdapter) return this.mixedContentAdapter.canGoNext()
      if (this.textAdapter) return this.textAdapter.canGoNext()
      return !!this.adapter && shelfViewCanGoNext(this.currentPageIndex, this.pagesCount, this.readerLayout, this.fixedPageCoverSingle)
    },
    rtlAvailable(): boolean {
      return !!this.manifest?.capabilities.rtl
    },
    visiblePageSources(): ShelfViewPageSource[] {
      if (!this.fixedPageAdapter && !this.mixedContentAdapter?.isCurrentFixedPage()) return []

      return shelfViewVisiblePageIndexes(
        this.currentPageIndex,
        this.pagesCount,
        this.readerLayout,
        this.readingDirection,
        this.fixedPageCoverSingle,
      )
        .map(pageIndex => ({
          pageIndex,
          url: this.fixedPageAdapter?.getPageUrl(pageIndex) ?? this.mixedContentAdapter?.getPageUrl(pageIndex),
        }))
        .filter((page): page is ShelfViewPageSource => !!page.url)
    },
    readerPagesVisible(): boolean {
      return this.visiblePageSources.length > 0
    },
    thumbnailPageSources(): ShelfViewPageSource[] {
      if (!this.fixedPageControlsVisible || this.pagesCount < 1) return []

      return Array.from({length: this.pagesCount}, (_, pageIndex) => ({
        pageIndex,
        url: this.fixedPageAdapter
          ? shelfViewThumbnailUrl(this.manifest, pageIndex)
          : this.mixedContentAdapter?.getPageUrl(pageIndex),
      }))
        .filter((page): page is ShelfViewPageSource => !!page.url)
    },
    thumbnailStripAvailable(): boolean {
      return this.fixedPageControlsVisible && this.thumbnailPageSources.length > 1
    },
    thumbnailStripVisible(): boolean {
      return this.chromeVisible && this.thumbnailStripOpen && this.thumbnailStripAvailable
    },
    visibleTextParagraphs(): ShelfViewTextParagraph[] {
      return this.textAdapter?.getVisibleParagraphs(this.readerLayout) ?? this.mixedContentAdapter?.getVisibleParagraphs(this.readerLayout) ?? []
    },
    textSearchMatches(): ShelfViewTextSearchMatch[] {
      const query = this.textSearchQuery.trim().toLocaleLowerCase()
      if (!query || !this.textSearchAvailable) return []

      const matches: ShelfViewTextSearchMatch[] = []
      this.searchableTextParagraphs().forEach(paragraph => {
        const text = paragraph.text.toLocaleLowerCase()
        let index = text.indexOf(query)
        while (index >= 0) {
          matches.push({
            paragraphIndex: paragraph.paragraphIndex,
            characterOffset: index,
          })
          index = text.indexOf(query, index + query.length)
        }
      })
      return matches
    },
    currentTextSearchMatch(): ShelfViewTextSearchMatch | undefined {
      return this.textSearchMatches[this.textSearchMatchIndex]
    },
    textSearchStatus(): string {
      if (!this.textSearchQuery.trim()) return this.$t('shelfview_reader.search_empty') as string
      if (this.textSearchMatches.length === 0) return this.$t('shelfview_reader.search_no_matches') as string
      return this.$t('shelfview_reader.search_match_status', {
        current: this.textSearchMatchIndex + 1,
        total: this.textSearchMatches.length,
      }) as string
    },
    readerTextVisible(): boolean {
      return this.visibleTextParagraphs.length > 0
    },
    stageClasses(): object {
      return {
        'shelfview-reader-shell__stage--continuous': this.readerLayout === 'CONTINUOUS_SCROLL',
        'shelfview-reader-shell__stage--fixed': this.fixedPageControlsVisible,
        'shelfview-reader-shell__stage--panning': this.fixedPagePanning,
        'shelfview-reader-shell__stage--text': this.textControlsVisible,
      }
    },
    textStageThemeClass(): string {
      return this.textControlsVisible ? `shelfview-reader-shell__stage--theme-${this.textTheme.toLowerCase()}` : ''
    },
    readerPagesClasses(): object {
      return {
        'shelfview-reader-shell__pages--single': this.readerLayout === 'SINGLE_PAGE',
        'shelfview-reader-shell__pages--double': this.readerLayout === 'DOUBLE_PAGE',
        'shelfview-reader-shell__pages--continuous': this.readerLayout === 'CONTINUOUS_SCROLL',
      }
    },
    pageImageClass(): object {
      return {
        'shelfview-reader-shell__page--fit-screen': this.pageFit === 'SCREEN',
        'shelfview-reader-shell__page--fit-width': this.pageFit === 'WIDTH',
        'shelfview-reader-shell__page--fit-height': this.pageFit === 'HEIGHT',
        'shelfview-reader-shell__page--fit-original': this.pageFit === 'ORIGINAL',
      }
    },
    imageFiltersAvailable(): boolean {
      return !!this.manifest?.capabilities.imageFilters
    },
    canvasImageProcessingEnabled(): boolean {
      return this.imageFiltersAvailable &&
        this.readerLayout !== 'CONTINUOUS_SCROLL' &&
        shelfViewImageFilterUsesCanvas(this.imageFilterPreset)
    },
    imageFilterStyle(): Record<string, string> {
      const style = this.imageFiltersAvailable ? shelfViewImageFilterStyle(this.imageFilterPreset) : shelfViewImageFilterStyle('ORIGINAL')
      style['--sv-brightness'] = roundNumber((Number(style['--sv-brightness']) || 1) * this.fixedPageBrightness, 4).toString()
      style['--sv-contrast'] = roundNumber((Number(style['--sv-contrast']) || 1) * this.fixedPageContrast, 4).toString()
      return style
    },
    imageFilterPresetIcon(): string {
      return shelfViewImageFilterPreset(this.visibleImageFilterPreset).icon
    },
    imageFilterPresetLabel(): string {
      return this.$t(`shelfview_reader.filter_${this.visibleImageFilterPreset.toLowerCase()}`) as string
    },
    visibleImageFilterPreset(): ShelfViewImageFilterPreset {
      return shelfViewImageFilterPrimaryPreset(this.imageFilterPreset)
    },
    textThemeIcon(): string {
      return shelfViewTextTheme(this.textTheme).icon
    },
    textThemeLabel(): string {
      return this.$t(`shelfview_reader.theme_${this.textTheme.toLowerCase()}`) as string
    },
    textThemeClass(): string {
      return shelfViewTextTheme(this.textTheme).className
    },
    textFontFamilyOptions(): Array<{ text: string, value: ShelfViewTextFontFamily }> {
      return SHELFVIEW_TEXT_FONT_FAMILIES.map(value => ({
        text: this.$t(`shelfview_reader.font_${value.toLowerCase()}`) as string,
        value,
      }))
    },
    textAlignOptions(): Array<{ text: string, value: ShelfViewTextAlign }> {
      return SHELFVIEW_TEXT_ALIGNS.map(value => ({
        text: this.$t(`shelfview_reader.align_${value.toLowerCase()}`) as string,
        value,
      }))
    },
    fixedPageBackgroundOptions(): Array<{ text: string, value: ShelfViewFixedPageBackground }> {
      return SHELFVIEW_FIXED_PAGE_BACKGROUNDS.map(option => ({
        text: this.$t(`shelfview_reader.background_${option.id.toLowerCase()}`) as string,
        value: option.id,
      }))
    },
    readerStageStyle(): Record<string, string> {
      if (!this.fixedPageControlsVisible) return {}

      return {
        '--sv-fixed-page-scale': (this.fixedPageScale / 100).toString(),
        '--sv-fixed-page-rotation': `${this.fixedPageRotation}deg`,
        '--sv-fixed-page-gap': `${this.fixedPageGap}px`,
        '--sv-fixed-page-margin': `${this.fixedPageMargin}px`,
        '--sv-fixed-background': shelfViewFixedPageBackground(this.fixedPageBackground).cssValue,
      }
    },
    textReaderStyle(): Record<string, string> {
      return {
        '--sv-text-font-family': shelfViewTextFontFamily(this.textFontFamily).cssValue,
        '--sv-text-font-size': `${this.textFontSize}px`,
        '--sv-text-line-height': this.textLineHeight.toString(),
        '--sv-text-letter-spacing': `${this.textLetterSpacing}px`,
        '--sv-text-word-spacing': `${this.textWordSpacing}px`,
        '--sv-text-content-width': `${this.textContentWidth}px`,
        '--sv-text-page-margin-x': `${this.textPageMargin}px`,
        '--sv-text-page-margin-y': `${Math.max(18, Math.round(this.textPageMargin * 0.75))}px`,
        '--sv-text-paragraph-spacing': `${this.textParagraphSpacing}em`,
        '--sv-text-align': shelfViewTextAlign(this.textAlign).cssValue,
        '--sv-text-font-weight': this.textFontWeight.toString(),
        '--sv-text-brightness': this.textBrightness.toString(),
      }
    },
    slideshowLabel(): string {
      return this.slideshowActive
        ? this.$t('shelfview_reader.slideshow_stop') as string
        : this.$t('shelfview_reader.slideshow_start') as string
    },
    readerLayoutIcon(): string {
      return this.readerLayoutIconFor(this.readerLayout)
    },
    readerLayoutLabel(): string {
      return this.readerLayoutLabelFor(this.readerLayout)
    },
    readingDirectionIcon(): string {
      return this.readingDirectionIconFor(this.readingDirection)
    },
    readingDirectionLabel(): string {
      return this.readingDirectionLabelFor(this.readingDirection)
    },
    fixedPageRotationLabel(): string {
      return this.$t('shelfview_reader.rotate_degrees', {degrees: this.fixedPageRotation}) as string
    },
    fixedPageRotationShortLabel(): string {
      return `${this.fixedPageRotation}°`
    },
    pageFitIcon(): string {
      return this.pageFitIconFor(this.pageFit)
    },
    pageFitLabel(): string {
      return this.pageFitLabelFor(this.pageFit)
    },
    readerLayoutOptions(): ShelfViewControlMenuOption[] {
      return this.availableReaderLayouts.map(value => ({
        value,
        icon: this.readerLayoutIconFor(value),
        label: this.readerLayoutLabelFor(value),
        active: value === this.readerLayout,
      }))
    },
    pageFitOptions(): ShelfViewControlMenuOption[] {
      return SHELFVIEW_FIXED_PAGE_FITS.map(value => ({
        value,
        icon: this.pageFitIconFor(value),
        label: this.pageFitLabelFor(value),
        active: value === this.pageFit,
      }))
    },
    readingDirectionOptions(): ShelfViewControlMenuOption[] {
      return (['LTR', 'RTL'] as ShelfViewFixedPageReadingDirection[]).map(value => ({
        value,
        icon: this.readingDirectionIconFor(value),
        label: this.readingDirectionLabelFor(value),
        active: value === this.readingDirection,
      }))
    },
    fixedPageRotationOptions(): ShelfViewControlMenuOption[] {
      return SHELFVIEW_FIXED_PAGE_ROTATIONS.map(value => ({
        value,
        icon: 'mdi-rotate-right',
        label: `${value}°`,
        active: value === this.fixedPageRotation,
      }))
    },
    imageFilterPresetOptions(): ShelfViewControlMenuOption[] {
      return SHELFVIEW_IMAGE_FILTER_PRIMARY_PRESETS.map(option => ({
        value: option.id,
        icon: option.icon,
        label: this.$t(`shelfview_reader.filter_${option.id.toLowerCase()}`) as string,
        active: option.id === this.visibleImageFilterPreset,
      }))
    },
    textThemeOptions(): ShelfViewControlMenuOption[] {
      return SHELFVIEW_TEXT_THEMES.map(value => ({
        value,
        icon: shelfViewTextTheme(value).icon,
        label: this.$t(`shelfview_reader.theme_${value.toLowerCase()}`) as string,
        active: value === this.textTheme,
      }))
    },
    controlMenuOptions(): ShelfViewControlMenuOption[] {
      switch (this.openControlMenu) {
        case 'layout':
          return this.readerLayoutOptions
        case 'fit':
          return this.pageFitOptions
        case 'direction':
          return this.readingDirectionOptions
        case 'rotate':
          return this.fixedPageRotationOptions
        case 'filter':
          return this.imageFilterPresetOptions
        case 'text-theme':
          return this.textThemeOptions
        default:
          return []
      }
    },
    controlMenuTitle(): string {
      switch (this.openControlMenu) {
        case 'layout':
          return this.$t('shelfview_reader.control_layout') as string
        case 'fit':
          return this.$t('shelfview_reader.control_fit') as string
        case 'direction':
          return this.$t('shelfview_reader.control_direction') as string
        case 'rotate':
          return this.$t('shelfview_reader.control_rotate') as string
        case 'filter':
          return this.$t('shelfview_reader.control_filter') as string
        case 'text-theme':
          return this.$t('shelfview_reader.control_text_theme') as string
        default:
          return ''
      }
    },
    empty(): boolean {
      return !!this.manifest && !this.readerPagesVisible && !this.readerTextVisible
    },
    emptyMessage(): string {
      if (this.manifest?.kind === 'TEXT_REFLOW') {
        return this.$t('shelfview_reader.empty_text') as string
      }
      return this.$t('shelfview_reader.empty_pages') as string
    },
    hasDisplayError(): boolean {
      return !!this.error || this.pageImageFailed
    },
    encodingRecoveryVisible(): boolean {
      return !!this.error && this.textRecoveryAvailable && this.errorLooksEncodingRelated(this.error)
    },
    textEncodingLabel(): string {
      return this.textEncodingOverride?.toUpperCase() ?? this.$t('shelfview_reader.encoding_auto') as string
    },
    displayErrorTitle(): string {
      if (this.pageImageFailed) return this.$t('shelfview_reader.error_page_failed_title') as string
      if (this.error instanceof ShelfViewApiError) {
        switch (this.error.code) {
          case 'FORBIDDEN':
            return this.$t('shelfview_reader.error_forbidden_title') as string
          case 'NOT_FOUND':
            return this.$t('shelfview_reader.error_not_found_title') as string
          case 'PAYLOAD_TOO_LARGE':
            return this.$t('shelfview_reader.error_too_large_title') as string
          case 'UNAVAILABLE':
            return this.$t('shelfview_reader.error_unavailable_title') as string
          case 'UNSUPPORTED_SCHEMA':
            return this.$t('shelfview_reader.error_unsupported_schema_title') as string
          default:
            return this.errorLooksEncodingRelated(this.error)
              ? this.$t('shelfview_reader.error_encoding_title') as string
              : this.$t('shelfview_reader.error_generic_title') as string
        }
      }
      return this.$t('shelfview_reader.error_generic_title') as string
    },
    displayErrorMessage(): string {
      if (this.pageImageFailed) {
        return this.$t('shelfview_reader.error_page_failed_message') as string
      }
      if (this.error instanceof ShelfViewApiError) {
        switch (this.error.code) {
          case 'FORBIDDEN':
            return this.$t('shelfview_reader.error_forbidden_message') as string
          case 'NOT_FOUND':
            return this.$t('shelfview_reader.error_not_found_message') as string
          case 'PAYLOAD_TOO_LARGE':
            return this.$t('shelfview_reader.error_too_large_message') as string
          case 'UNAVAILABLE':
            return this.browserOffline()
              ? this.$t('shelfview_reader.error_unavailable_offline_message') as string
              : this.$t('shelfview_reader.error_unavailable_message') as string
          case 'UNSUPPORTED_SCHEMA':
            return this.$t('shelfview_reader.error_unsupported_schema_message') as string
          default:
            if (this.errorLooksEncodingRelated(this.error)) {
              return this.$t('shelfview_reader.error_encoding_message') as string
            }
            if (this.error.code === 'BAD_REQUEST') {
              return this.$t('shelfview_reader.error_bad_request_message') as string
            }
        }
      }
      if (this.error instanceof Error && this.error.message.toLowerCase().includes('different book version')) {
        return this.$t('shelfview_reader.error_book_changed_message') as string
      }
      return this.error?.message || this.$t('shelfview_reader.error_generic_message') as string
    },
    legacyReaderLocation(): RawLocation {
      return {
        name: this.book?.media ? getLegacyBookReadRouteFromMedia(this.book.media) : 'read-book',
        params: {bookId: this.bookId},
        query: {...this.$route.query, forceLegacy: 'true'},
      }
    },
    textRecoveryAvailable(): boolean {
      return this.manifest?.kind === 'TEXT_REFLOW' || this.manifest?.kind === 'MIXED_CONTENT_SEQUENCE'
    },
  },
  methods: {
    async setup(bookId: string) {
      const requestId = this.setupRequestId + 1
      this.setupRequestId = requestId
      this.loading = true
      this.error = undefined
      this.manifest = undefined as unknown as ShelfViewManifest
      this.bookmarks = []
      this.highlights = []
      this.bookmarksDrawerOpen = false
      this.contentsDrawerOpen = false
      this.dictionaryDrawerOpen = false
      this.dictionaryQuery = ''
      this.dictionaryLocalEntries = []
      this.loadDictionaryPackFromBrowserStorage()
      this.textSearchOpen = false
      this.textSearchQuery = ''
      this.textSearchMatchIndex = 0
      this.chromeVisible = false
      this.overflowMenuOpen = false
      this.thumbnailStripOpen = false
      this.maybeShowChromeHint()
      this.siblingNext = undefined
      this.endOfBookPromptOpen = false
      this.stopSlideshow()
      this.clearReaderStateTimers()
      this.setReaderStateStatus('IDLE', '')
      this.destroyAdapter()
      this.pageImageFailed = false

      this.context = {} as Context
      if (this.$route.query.contextId && this.$route.query.context &&
        Object.values(ContextOrigin).includes(this.$route.query.context as ContextOrigin)) {
        this.context = {
          origin: this.$route.query.context as ContextOrigin,
          id: this.$route.query.contextId as string,
        }
      }

      try {
        this.book = await this.$talesBooks.getBook(bookId)
        if (requestId !== this.setupRequestId) return

        this.series = await this.$talesSeries.getOneSeries(this.book.seriesId)
        if (requestId !== this.setupRequestId) return

        this.book.context = this.context
        void this.loadSiblingNext(bookId, requestId)
        this.manifest = await this.$shelfViewManifest.getManifest(bookId)
        if (requestId !== this.setupRequestId) return

        if (this.manifest.kind === 'FIXED_PAGE_SEQUENCE' || this.manifest.kind === 'TEXT_REFLOW' || this.manifest.kind === 'MIXED_CONTENT_SEQUENCE') {
          const savedPosition = await this.loadShelfViewState(bookId, this.preferenceFormatsForManifest())
          if (requestId !== this.setupRequestId) return

          if (this.manifest.kind === 'FIXED_PAGE_SEQUENCE') {
            await this.openFixedPageManifest(this.manifest, savedPosition)
          } else if (this.manifest.kind === 'TEXT_REFLOW') {
            await this.openTextManifest(this.manifest, savedPosition)
          } else {
            await this.openMixedManifest(this.manifest, savedPosition)
          }
        }
        if (requestId !== this.setupRequestId) return

        document.title = `Tales - ${this.bookTitle}`
      } catch (e) {
        if (requestId === this.setupRequestId) this.error = e
      } finally {
        if (requestId === this.setupRequestId) this.loading = false
      }
    },
    async loadSiblingNext(
      bookId: string,
      requestId: number,
    ) {
      try {
        const siblingNext = this.context.origin === ContextOrigin.READLIST
          ? await this.$talesReadLists.getBookSiblingNext(this.context.id, bookId)
          : await this.$talesBooks.getBookSiblingNext(bookId)
        if (requestId === this.setupRequestId) this.siblingNext = siblingNext
      } catch (e) {
        if (requestId === this.setupRequestId) this.siblingNext = undefined
      }
    },
    async loadShelfViewState(
      bookId: string,
      preferenceFormats: ShelfViewPreferenceFormat[],
    ): Promise<ShelfViewPositionDto | undefined> {
      try {
        const [savedPosition, bookmarks, highlights, preferences] = await Promise.all([
          this.$shelfViewState.getPosition(bookId),
          this.$shelfViewState.getBookmarks(bookId),
          this.$shelfViewState.getHighlights(bookId),
          Promise.all(preferenceFormats.map(format =>
            this.$shelfViewState.getPreference({
              scopeType: 'BOOK',
              scopeId: bookId,
              format,
            }),
          )),
        ])

        this.bookmarks = bookmarks
        this.highlights = highlights
        preferences.forEach(preference => {
          this.applyPreferenceSettings(this.settingsWithFormatDefaults(preference), false, preference.format)
        })
        this.setReaderStateStatus('SAVED', this.$t('shelfview_reader.status_restored') as string)

        return savedPosition
      } catch (e) {
        this.setReaderStateStatus(this.browserOffline() ? 'OFFLINE' : 'ERROR', this.$t('shelfview_reader.status_unavailable') as string)
        return undefined
      }
    },
    preferenceFormatsForManifest(): ShelfViewPreferenceFormat[] {
      switch (this.manifest?.kind) {
        case 'TEXT_REFLOW':
          return [SHELFVIEW_TEXT_FORMAT]
        case 'MIXED_CONTENT_SEQUENCE':
          return [SHELFVIEW_FIXED_PAGE_FORMAT, SHELFVIEW_TEXT_FORMAT]
        case 'FIXED_PAGE_SEQUENCE':
          return [SHELFVIEW_FIXED_PAGE_FORMAT]
        default:
          return []
      }
    },
    settingsWithFormatDefaults(
      preference: ShelfViewPreferenceResponseDto,
    ): ShelfViewFixedPagePreferenceSettings {
      const settings = {...preference.effectiveSettings}
      if (preference.format === SHELFVIEW_FIXED_PAGE_FORMAT && !this.preferenceHasSavedPageFit(preference)) {
        settings.pageFit = 'SCREEN'
      }
      return settings
    },
    preferenceHasSavedPageFit(preference: ShelfViewPreferenceResponseDto): boolean {
      return preference.sources.some(source => !!source.settings.pageFit)
    },
    async openFixedPageManifest(
      manifest: ShelfViewManifest,
      savedPosition?: ShelfViewPositionDto,
    ) {
      const adapter = new FixedPageAdapter()
      await adapter.open(manifest)
      this.adapter = adapter
      this.normalizeReaderLayout()
      await this.restoreSavedPosition(savedPosition)
      this.syncAdapterState()
      this.$nextTick(() => this.focusReaderStage())
    },
    async openTextManifest(
      manifest: ShelfViewManifest,
      savedPosition?: ShelfViewPositionDto,
    ) {
      const adapter = new TextReflowAdapter(textManifest => this.loadTextContent(textManifest))
      await adapter.open(manifest)
      this.adapter = adapter
      this.normalizeReaderLayout()
      await this.restoreSavedPosition(savedPosition)
      this.syncAdapterState()
      this.$nextTick(() => {
        this.restoreCurrentTextScrollPosition()
        this.focusReaderStage()
      })
    },
    async openMixedManifest(
      manifest: ShelfViewManifest,
      savedPosition?: ShelfViewPositionDto,
    ) {
      const adapter = new MixedContentAdapter(textManifest => this.loadTextContent(textManifest))
      await adapter.open(manifest)
      this.adapter = adapter
      this.normalizeReaderLayout()
      await this.restoreSavedPosition(savedPosition)
      this.syncAdapterState()
      this.$nextTick(() => {
        this.restoreCurrentTextScrollPosition()
        this.focusReaderStage()
      })
    },
    async restoreSavedPosition(savedPosition?: ShelfViewPositionDto) {
      if (!this.adapter || !savedPosition) return

      try {
        await this.adapter.goTo(savedPosition.locator)
      } catch (e) {
        this.setReaderStateStatus('ERROR', this.savedLocationErrorMessage(e))
      }
    },
    savedLocationErrorMessage(e: unknown): string {
      if (e instanceof Error && e.message.toLowerCase().includes('different book version')) {
        return this.$t('shelfview_reader.status_book_changed') as string
      }
      return this.$t('shelfview_reader.status_saved_location_unavailable') as string
    },
    loadTextContent(manifest: ShelfViewManifest) {
      return this.textEncodingOverride
        ? this.$shelfViewText.getTextContent(manifest, this.textEncodingOverride)
        : this.$shelfViewText.getTextContent(manifest)
    },
    retryReader() {
      if (this.pageImageFailed && !this.error) {
        this.retryPageImage()
        return
      }

      void this.setup(this.bookId)
    },
    retryPageImage() {
      this.pageImageFailed = false
      this.pageImageReloadNonce += 1
      this.$nextTick(() => this.focusReaderStage())
    },
    retryTextEncoding(encoding?: string) {
      this.textEncodingOverride = encoding
      this.pageImageFailed = false
      void this.setup(this.bookId)
    },
    onPageImageError() {
      this.pageImageFailed = true
    },
    errorLooksEncodingRelated(error: Error | ShelfViewApiError): boolean {
      return error.message.toLowerCase().includes('encoding')
    },
    destroyAdapter() {
      this.stopTextToSpeech()
      this.adapter?.destroy()
      this.adapter = undefined
      this.currentLocator = undefined
    },
    syncAdapterState() {
      if (!this.adapter) {
        this.currentLocator = undefined
        return
      }

      this.currentLocator = this.adapter.getCurrentLocator()
      this.pageJumpInput = this.currentPageNumber.toString()
    },
    async previousPage() {
      if (!this.adapter) return

      if (this.mixedContentAdapter) {
        await this.mixedContentAdapter.previous()
        this.normalizeReaderLayout()
        this.syncAdapterState()
        this.$nextTick(() => this.restoreCurrentTextScrollPosition())
        this.schedulePositionSave()
        return
      }

      if (this.textAdapter) {
        await this.textAdapter.previous()
        this.syncAdapterState()
        this.schedulePositionSave()
        return
      }

      await this.goToPageIndex(shelfViewPreviousPageIndex(this.currentPageIndex, this.pagesCount, this.readerLayout, this.fixedPageCoverSingle))
    },
    async nextPage() {
      if (!this.adapter) return

      if (!this.canGoNext) {
        this.openEndOfBookPrompt()
        return
      }

      if (this.mixedContentAdapter) {
        await this.mixedContentAdapter.next()
        this.normalizeReaderLayout()
        this.syncAdapterState()
        this.$nextTick(() => this.restoreCurrentTextScrollPosition())
        this.schedulePositionSave()
        return
      }

      if (this.textAdapter) {
        await this.textAdapter.next()
        this.syncAdapterState()
        this.schedulePositionSave()
        return
      }

      await this.goToPageIndex(shelfViewNextPageIndex(this.currentPageIndex, this.pagesCount, this.readerLayout, this.fixedPageCoverSingle))
    },
    async goToPageIndex(pageIndex: number) {
      if (!this.adapter) return

      this.pageImageFailed = false
      if (this.mixedContentAdapter) {
        await this.mixedContentAdapter.goToCurrentItemContentIndex(pageIndex)
        this.syncAdapterState()
        this.schedulePositionSave()
        return
      }

      const currentLocator = this.adapter.getCurrentLocator()
      if (currentLocator.kind === 'PAGE') {
        await this.adapter.goTo({
          ...currentLocator,
          page: pageIndex,
        })
      } else {
        await this.adapter.goTo({
          ...currentLocator,
          paragraph: pageIndex,
        })
      }
      this.syncAdapterState()
      this.schedulePositionSave()
    },
    normalizeReaderLayout() {
      if (!this.availableReaderLayouts.includes(this.readerLayout)) {
        this.readerLayout = 'SINGLE_PAGE'
      }
    },
    readerLayoutIconFor(layout: ShelfViewFixedPageLayout): string {
      switch (layout) {
        case 'DOUBLE_PAGE':
          return 'mdi-book-open-variant'
        case 'CONTINUOUS_SCROLL':
          return 'mdi-view-sequential'
        default:
          return 'mdi-page-layout-body'
      }
    },
    readerLayoutLabelFor(layout: ShelfViewFixedPageLayout): string {
      switch (layout) {
        case 'DOUBLE_PAGE':
          return this.$t('shelfview_reader.layout_double') as string
        case 'CONTINUOUS_SCROLL':
          return this.$t('shelfview_reader.layout_continuous') as string
        default:
          return this.$t('shelfview_reader.layout_single') as string
      }
    },
    pageFitIconFor(fit: ShelfViewFixedPageFit): string {
      switch (fit) {
        case 'WIDTH':
          return 'mdi-arrow-expand-horizontal'
        case 'HEIGHT':
          return 'mdi-arrow-expand-vertical'
        case 'ORIGINAL':
          return 'mdi-image-size-select-large'
        default:
          return 'mdi-fit-to-screen-outline'
      }
    },
    pageFitLabelFor(fit: ShelfViewFixedPageFit): string {
      switch (fit) {
        case 'WIDTH':
          return this.$t('shelfview_reader.fit_width') as string
        case 'HEIGHT':
          return this.$t('shelfview_reader.fit_height') as string
        case 'ORIGINAL':
          return this.$t('shelfview_reader.fit_original') as string
        default:
          return this.$t('shelfview_reader.fit_screen') as string
      }
    },
    readingDirectionIconFor(direction: ShelfViewFixedPageReadingDirection): string {
      return direction === 'RTL' ? 'mdi-format-textdirection-r-to-l' : 'mdi-format-textdirection-l-to-r'
    },
    readingDirectionLabelFor(direction: ShelfViewFixedPageReadingDirection): string {
      return direction === 'RTL'
        ? this.$t('shelfview_reader.direction_rtl') as string
        : this.$t('shelfview_reader.direction_ltr') as string
    },
    toggleControlMenu(menu: string, event: MouseEvent) {
      if (this.openControlMenu === menu) {
        this.closeControlMenu()
        return
      }

      const target = event.currentTarget as HTMLElement | null
      const rect = target?.getBoundingClientRect()
      const menuWidth = 240
      const left = rect ? Math.min(rect.left, window.innerWidth - menuWidth - 8) : 8
      this.controlMenuLeft = Math.max(8, left)
      this.openControlMenu = menu
    },
    closeControlMenu() {
      this.openControlMenu = null
    },
    selectControlOption(value: string | number) {
      switch (this.openControlMenu) {
        case 'layout':
          this.setReaderLayout(value as ShelfViewFixedPageLayout)
          break
        case 'fit':
          this.setPageFit(value as ShelfViewFixedPageFit)
          break
        case 'direction':
          this.setReadingDirection(value as ShelfViewFixedPageReadingDirection)
          break
        case 'rotate':
          this.setFixedPageRotation(Number(value))
          break
        case 'filter':
          this.setImageFilterPreset(value as ShelfViewImageFilterPreset)
          break
        case 'text-theme':
          this.setTextTheme(value as ShelfViewTextTheme)
          break
      }
      this.closeControlMenu()
    },
    setReaderLayout(layout: ShelfViewFixedPageLayout) {
      if (!this.availableReaderLayouts.includes(layout)) return

      this.readerLayout = layout
      this.syncAdapterState()
      this.$nextTick(() => this.restoreCurrentTextScrollPosition())
      this.schedulePreferenceSave()
    },
    setReadingDirection(direction: ShelfViewFixedPageReadingDirection) {
      if (!this.rtlAvailable) return

      this.readingDirection = direction
      this.syncAdapterState()
      this.schedulePreferenceSave()
    },
    setPageFit(fit: ShelfViewFixedPageFit) {
      this.pageFit = fit
      this.schedulePreferenceSave()
    },
    setFixedPageRotation(rotation: number) {
      if (!this.fixedPageControlsVisible) return

      this.fixedPageRotation = normalizeFixedPageRotation(rotation)
      this.$nextTick(() => this.restoreCurrentVisibleContent())
      this.schedulePreferenceSave()
    },
    cycleFixedPageRotation() {
      this.setFixedPageRotation(shelfViewNextFixedPageRotation(this.fixedPageRotation))
    },
    setImageFilterPreset(preset: ShelfViewImageFilterPreset) {
      if (!this.imageFiltersAvailable) return

      this.imageFilterPreset = preset
      this.schedulePreferenceSave()
    },
    openFixedPageSettings() {
      this.textSettingsOpen = false
      this.fixedPageSettingsOpen = true
    },
    closeFixedPageSettings() {
      this.fixedPageSettingsOpen = false
      this.$nextTick(() => this.focusReaderStage())
    },
    resetFixedPageDisplaySettings() {
      this.applyFixedPagePreferenceSettings(SHELFVIEW_FIXED_PAGE_DISPLAY_DEFAULTS)
      this.onFixedPageDisplaySettingChanged()
    },
    onFixedPageDisplaySettingChanged() {
      this.normalizeFixedPageDisplaySettings()
      this.$nextTick(() => this.restoreCurrentVisibleContent())
      this.schedulePreferenceSave()
    },
    setTextTheme(theme: ShelfViewTextTheme) {
      this.textTheme = theme
      this.$nextTick(() => this.restoreCurrentTextScrollPosition())
      this.schedulePreferenceSave()
    },
    selectTextThemeOption(value: string | number) {
      this.setTextTheme(value as ShelfViewTextTheme)
    },
    adjustTextFontSize(delta: number) {
      this.textFontSize += delta
      this.onTextDisplaySettingChanged()
    },
    openTextSettings() {
      this.fixedPageSettingsOpen = false
      this.textSettingsOpen = true
    },
    closeTextSettings() {
      this.textSettingsOpen = false
      this.$nextTick(() => this.focusReaderStage())
    },
    resetTextDisplaySettings() {
      this.applyTextPreferenceSettings(SHELFVIEW_TEXT_DISPLAY_DEFAULTS)
      this.onTextDisplaySettingChanged()
    },
    onTextDisplaySettingChanged() {
      this.normalizeTextDisplaySettings()
      this.$nextTick(() => this.restoreCurrentTextScrollPosition())
      this.schedulePreferenceSave()
    },
    onTextToSpeechRateChanged() {
      const rate = Number(this.textToSpeechRate)
      this.textToSpeechRate = Number.isFinite(rate)
        ? roundNumber(clampNumber(rate, TEXT_TO_SPEECH_RATE_MIN, TEXT_TO_SPEECH_RATE_MAX), 1)
        : TEXT_TO_SPEECH_RATE_DEFAULT
    },
    applyPreferenceSettings(
      settings: ShelfViewFixedPagePreferenceSettings,
      persist = false,
      format?: ShelfViewPreferenceFormat,
    ) {
      if (format !== SHELFVIEW_TEXT_FORMAT) {
        if (settings.layout && this.availableReaderLayouts.includes(settings.layout)) {
          this.readerLayout = settings.layout
        }
        if (settings.readingDirection && (settings.readingDirection === 'LTR' || (settings.readingDirection === 'RTL' && this.rtlAvailable))) {
          this.readingDirection = settings.readingDirection
        }
        if (settings.pageFit && SHELFVIEW_FIXED_PAGE_FITS.includes(settings.pageFit)) {
          this.pageFit = settings.pageFit
        }
        if (settings.imageFilterPreset && this.imageFiltersAvailable) {
          this.imageFilterPreset = shelfViewImageFilterPrimaryPreset(settings.imageFilterPreset)
        }
        if (isFiniteNumber(settings.fixedPageRotation)) {
          this.fixedPageRotation = normalizeFixedPageRotation(settings.fixedPageRotation)
        }
        if (!this.imageFiltersAvailable) this.imageFilterPreset = 'ORIGINAL'
        this.applyFixedPagePreferenceSettings(settings)
      }

      this.applyTextPreferenceSettings(settings)
      if (format === SHELFVIEW_TEXT_FORMAT && settings.layout && this.manifest?.kind === 'TEXT_REFLOW' && this.availableReaderLayouts.includes(settings.layout)) {
        this.readerLayout = settings.layout
      }

      this.normalizeReaderLayout()
      if (persist) this.schedulePreferenceSave()
    },
    applyFixedPagePreferenceSettings(settings: ShelfViewFixedPagePreferenceSettings) {
      if (isFiniteNumber(settings.fixedPageRotation)) this.fixedPageRotation = normalizeFixedPageRotation(settings.fixedPageRotation)
      if (typeof settings.fixedPageCoverSingle === 'boolean') this.fixedPageCoverSingle = settings.fixedPageCoverSingle
      if (isFiniteNumber(settings.fixedPageScale)) this.fixedPageScale = settings.fixedPageScale
      if (isFiniteNumber(settings.fixedPageGap)) this.fixedPageGap = settings.fixedPageGap
      if (isFiniteNumber(settings.fixedPageMargin)) this.fixedPageMargin = settings.fixedPageMargin
      if (settings.fixedPageBackground && SHELFVIEW_FIXED_PAGE_BACKGROUNDS.some(option => option.id === settings.fixedPageBackground)) {
        this.fixedPageBackground = settings.fixedPageBackground
      }
      if (isFiniteNumber(settings.fixedPageBrightness)) this.fixedPageBrightness = settings.fixedPageBrightness
      if (isFiniteNumber(settings.fixedPageContrast)) this.fixedPageContrast = settings.fixedPageContrast
      this.normalizeFixedPageDisplaySettings()
    },
    normalizeFixedPageDisplaySettings() {
      const settings = shelfViewNormalizeFixedPageDisplaySettings({
        fixedPageRotation: this.fixedPageRotation,
        fixedPageCoverSingle: this.fixedPageCoverSingle,
        fixedPageScale: this.fixedPageScale,
        fixedPageGap: this.fixedPageGap,
        fixedPageMargin: this.fixedPageMargin,
        fixedPageBackground: this.fixedPageBackground,
        fixedPageBrightness: this.fixedPageBrightness,
        fixedPageContrast: this.fixedPageContrast,
      })

      this.fixedPageRotation = settings.fixedPageRotation
      this.fixedPageCoverSingle = settings.fixedPageCoverSingle
      this.fixedPageScale = settings.fixedPageScale
      this.fixedPageGap = settings.fixedPageGap
      this.fixedPageMargin = settings.fixedPageMargin
      this.fixedPageBackground = settings.fixedPageBackground
      this.fixedPageBrightness = settings.fixedPageBrightness
      this.fixedPageContrast = settings.fixedPageContrast
    },
    normalizedFixedPageScale(value: number): number {
      return shelfViewNormalizeFixedPageScale(value)
    },
    applyTextPreferenceSettings(settings: ShelfViewFixedPagePreferenceSettings) {
      if (settings.textTheme && SHELFVIEW_TEXT_THEMES.includes(settings.textTheme)) this.textTheme = settings.textTheme
      if (settings.textFontFamily && SHELFVIEW_TEXT_FONT_FAMILIES.includes(settings.textFontFamily)) this.textFontFamily = settings.textFontFamily
      if (settings.textAlign && SHELFVIEW_TEXT_ALIGNS.includes(settings.textAlign)) this.textAlign = settings.textAlign
      if (isFiniteNumber(settings.textFontSize)) this.textFontSize = settings.textFontSize
      if (isFiniteNumber(settings.textLineHeight)) this.textLineHeight = settings.textLineHeight
      if (isFiniteNumber(settings.textLetterSpacing)) this.textLetterSpacing = settings.textLetterSpacing
      if (isFiniteNumber(settings.textWordSpacing)) this.textWordSpacing = settings.textWordSpacing
      if (isFiniteNumber(settings.textContentWidth)) this.textContentWidth = settings.textContentWidth
      if (isFiniteNumber(settings.textPageMargin)) this.textPageMargin = settings.textPageMargin
      if (isFiniteNumber(settings.textParagraphSpacing)) this.textParagraphSpacing = settings.textParagraphSpacing
      if (isFiniteNumber(settings.textFontWeight)) this.textFontWeight = settings.textFontWeight
      if (isFiniteNumber(settings.textBrightness)) this.textBrightness = settings.textBrightness
      this.normalizeTextDisplaySettings()
    },
    normalizeTextDisplaySettings() {
      const settings = shelfViewNormalizeTextDisplaySettings({
        textTheme: this.textTheme,
        textFontFamily: this.textFontFamily,
        textFontSize: this.textFontSize,
        textLineHeight: this.textLineHeight,
        textLetterSpacing: this.textLetterSpacing,
        textWordSpacing: this.textWordSpacing,
        textContentWidth: this.textContentWidth,
        textPageMargin: this.textPageMargin,
        textParagraphSpacing: this.textParagraphSpacing,
        textAlign: this.textAlign,
        textFontWeight: this.textFontWeight,
        textBrightness: this.textBrightness,
      })

      this.textTheme = settings.textTheme
      this.textFontFamily = settings.textFontFamily
      this.textFontSize = settings.textFontSize
      this.textLineHeight = settings.textLineHeight
      this.textLetterSpacing = settings.textLetterSpacing
      this.textWordSpacing = settings.textWordSpacing
      this.textContentWidth = settings.textContentWidth
      this.textPageMargin = settings.textPageMargin
      this.textParagraphSpacing = settings.textParagraphSpacing
      this.textAlign = settings.textAlign
      this.textFontWeight = settings.textFontWeight
      this.textBrightness = settings.textBrightness
    },
    currentPreferenceSettings(format?: ShelfViewPreferenceFormat): ShelfViewFixedPagePreferenceSettings {
      if ((format ?? this.currentPreferenceFormat()) === SHELFVIEW_TEXT_FORMAT) {
        return {
          layout: this.readerLayout,
          textTheme: this.textTheme,
          textFontFamily: this.textFontFamily,
          textFontSize: this.textFontSize,
          textLineHeight: this.textLineHeight,
          textLetterSpacing: this.textLetterSpacing,
          textWordSpacing: this.textWordSpacing,
          textContentWidth: this.textContentWidth,
          textPageMargin: this.textPageMargin,
          textParagraphSpacing: this.textParagraphSpacing,
          textAlign: this.textAlign,
          textFontWeight: this.textFontWeight,
          textBrightness: this.textBrightness,
        }
      }

      return {
        layout: this.readerLayout,
        readingDirection: this.readingDirection,
        pageFit: this.pageFit,
        imageFilterPreset: this.imageFilterPreset,
        fixedPageRotation: this.fixedPageRotation,
        fixedPageCoverSingle: this.fixedPageCoverSingle,
        fixedPageScale: this.fixedPageScale,
        fixedPageGap: this.fixedPageGap,
        fixedPageMargin: this.fixedPageMargin,
        fixedPageBackground: this.fixedPageBackground,
        fixedPageBrightness: this.fixedPageBrightness,
        fixedPageContrast: this.fixedPageContrast,
      }
    },
    currentPreferenceFormat(): ShelfViewPreferenceFormat | undefined {
      if (!this.manifest) return undefined
      if (this.textControlsVisible) return SHELFVIEW_TEXT_FORMAT
      if (this.fixedPageControlsVisible) return SHELFVIEW_FIXED_PAGE_FORMAT
      return undefined
    },
    schedulePositionSave() {
      if (!this.adapter || !this.currentLocator) return

      if (this.positionSaveTimer !== undefined) window.clearTimeout(this.positionSaveTimer)
      this.setReaderStateStatus('PENDING', this.$t('shelfview_reader.status_saving') as string)
      this.positionSaveTimer = window.setTimeout(() => {
        void this.flushPositionSave()
      }, POSITION_SAVE_DEBOUNCE_MS)
    },
    schedulePreferenceSave() {
      if (!this.bookId || !this.currentPreferenceFormat()) return

      if (this.preferenceSaveTimer !== undefined) window.clearTimeout(this.preferenceSaveTimer)
      this.setReaderStateStatus('PENDING', this.$t('shelfview_reader.status_saving') as string)
      this.preferenceSaveTimer = window.setTimeout(() => {
        void this.flushPreferenceSave()
      }, PREFERENCE_SAVE_DEBOUNCE_MS)
    },
    async flushReaderState() {
      await Promise.all([
        this.flushPositionSave(),
        this.flushPreferenceSave(),
      ])
    },
    async flushPositionSave() {
      if (this.positionSaveTimer !== undefined) {
        window.clearTimeout(this.positionSaveTimer)
        this.positionSaveTimer = undefined
      }
      if (!this.adapter || !this.currentLocator) return
      if (this.browserOffline()) {
        this.setReaderStateStatus('OFFLINE', this.$t('shelfview_reader.status_offline') as string)
        return
      }

      try {
        this.setReaderStateStatus('SAVING', this.$t('shelfview_reader.status_saving') as string)
        await this.$shelfViewState.savePosition(this.bookId, {
          locator: this.currentLocator,
          progression: this.adapter.getProgression(),
        })
        this.setReaderStateStatus('SAVED', this.$t('shelfview_reader.status_saved') as string)
      } catch (e) {
        this.setReaderStateStatus(this.browserOffline() ? 'OFFLINE' : 'ERROR', this.$t('shelfview_reader.status_save_failed') as string)
      }
    },
    async flushPreferenceSave() {
      if (this.preferenceSaveTimer !== undefined) {
        window.clearTimeout(this.preferenceSaveTimer)
        this.preferenceSaveTimer = undefined
      }
      const format = this.currentPreferenceFormat()
      if (!this.bookId || !format) return
      if (this.browserOffline()) {
        this.setReaderStateStatus('OFFLINE', this.$t('shelfview_reader.status_offline') as string)
        return
      }

      try {
        this.setReaderStateStatus('SAVING', this.$t('shelfview_reader.status_saving') as string)
        await this.$shelfViewState.savePreference({
          scopeType: 'BOOK',
          scopeId: this.bookId,
          format,
          settings: this.currentPreferenceSettings(format),
        })
        this.setReaderStateStatus('SAVED', this.$t('shelfview_reader.status_saved') as string)
      } catch (e) {
        this.setReaderStateStatus(this.browserOffline() ? 'OFFLINE' : 'ERROR', this.$t('shelfview_reader.status_save_failed') as string)
      }
    },
    clearReaderStateTimers() {
      if (this.positionSaveTimer !== undefined) {
        window.clearTimeout(this.positionSaveTimer)
        this.positionSaveTimer = undefined
      }
      if (this.preferenceSaveTimer !== undefined) {
        window.clearTimeout(this.preferenceSaveTimer)
        this.preferenceSaveTimer = undefined
      }
      this.clearChromeAutoHide()
    },
    clearChromeAutoHide() {
      if (this.chromeIdleTimer !== undefined) {
        window.clearTimeout(this.chromeIdleTimer)
        this.chromeIdleTimer = undefined
      }
    },
    scheduleChromeAutoHide() {
      this.clearChromeAutoHide()
      if (!this.chromeVisible || this.anyChromePanelOpen) return

      this.chromeIdleTimer = window.setTimeout(() => {
        this.hideChromeAfterIdle()
      }, this.chromeAutoHideDelayMs)
    },
    hideChromeAfterIdle() {
      this.clearChromeAutoHide()
      if (this.anyChromePanelOpen) return

      this.chromeVisible = false
      this.closeOverflowMenu()
    },
    noteChromeActivity() {
      if (this.chromeVisible) this.scheduleChromeAutoHide()
    },
    setReaderStateStatus(
      status: ShelfViewReaderStateStatus,
      message: string,
    ) {
      this.readerStateStatus = status
      this.readerStateMessage = message
    },
    browserOffline(): boolean {
      return typeof navigator !== 'undefined' && navigator.onLine === false
    },
    focusReaderStage() {
      const readerStage = this.$refs.readerStage as HTMLElement | undefined
      readerStage?.focus()
    },
    toggleTextSearch() {
      this.textSearchOpen = !this.textSearchOpen
      this.$nextTick(() => this.focusReaderStage())
    },
    toggleTextToSpeech() {
      if (this.textToSpeechActive) {
        this.stopTextToSpeech()
        return
      }

      this.startTextToSpeech()
    },
    startTextToSpeech() {
      if (!this.textToSpeechAvailable) return

      const text = this.selectedTextToSpeechText() || this.currentTextToSpeechText
      if (!text) return

      this.stopTextToSpeech()

      const utterance = new window.SpeechSynthesisUtterance(text)
      utterance.rate = this.textToSpeechRate
      const voice = this.selectedTextToSpeechVoice()
      if (voice) utterance.voice = voice
      utterance.onend = () => this.clearTextToSpeechState(utterance)
      utterance.onerror = () => this.clearTextToSpeechState(utterance)
      this.textToSpeechUtterance = utterance
      this.textToSpeechActive = true
      window.speechSynthesis.speak(utterance)
    },
    selectedTextToSpeechText(): string {
      const selection = window.getSelection?.()
      if (
        !selection ||
        typeof selection.rangeCount !== 'number' ||
        typeof selection.getRangeAt !== 'function' ||
        selection.rangeCount === 0 ||
        selection.isCollapsed
      ) return ''

      const range = selection.getRangeAt(0)
      const start = this.textSelectionEndpoint(range.startContainer, range.startOffset)
      const end = this.textSelectionEndpoint(range.endContainer, range.endOffset)
      if (!start || !end) return ''

      return selection.toString().replace(/\s+/g, ' ').trim()
    },
    refreshTextToSpeechVoices() {
      if (!this.browserTextToSpeechVoiceListAvailable()) {
        this.textToSpeechVoices = []
        this.textToSpeechVoiceURI = ''
        return
      }

      this.textToSpeechVoices = window.speechSynthesis.getVoices()
      if (this.textToSpeechVoiceURI && !this.selectedTextToSpeechVoice()) {
        this.textToSpeechVoiceURI = ''
      }
    },
    selectedTextToSpeechVoice(): SpeechSynthesisVoice | undefined {
      if (!this.textToSpeechVoiceURI) return undefined
      return this.textToSpeechVoices.find(voice => voice.voiceURI === this.textToSpeechVoiceURI)
    },
    stopTextToSpeech() {
      if (this.browserTextToSpeechAvailable()) window.speechSynthesis.cancel()
      this.textToSpeechUtterance = undefined
      this.textToSpeechActive = false
    },
    clearTextToSpeechState(utterance: SpeechSynthesisUtterance) {
      if (this.textToSpeechUtterance !== utterance) return
      this.textToSpeechUtterance = undefined
      this.textToSpeechActive = false
    },
    browserTextToSpeechAvailable(): boolean {
      return typeof window !== 'undefined' &&
        !!window.speechSynthesis &&
        typeof window.speechSynthesis.speak === 'function' &&
        typeof window.speechSynthesis.cancel === 'function' &&
        typeof window.SpeechSynthesisUtterance === 'function'
    },
    browserTextToSpeechVoiceListAvailable(): boolean {
      return typeof window !== 'undefined' &&
        !!window.speechSynthesis &&
        typeof window.speechSynthesis.getVoices === 'function' &&
        typeof window.speechSynthesis.addEventListener === 'function' &&
        typeof window.speechSynthesis.removeEventListener === 'function'
    },
    onTextSearchInput() {
      this.textSearchMatchIndex = 0
      if (this.textSearchMatches.length > 0) void this.goToTextSearchMatch(0)
    },
    async goToNextTextSearchMatch() {
      if (this.textSearchMatches.length === 0) return
      await this.goToTextSearchMatch((this.textSearchMatchIndex + 1) % this.textSearchMatches.length)
    },
    async goToPreviousTextSearchMatch() {
      if (this.textSearchMatches.length === 0) return
      await this.goToTextSearchMatch((this.textSearchMatchIndex + this.textSearchMatches.length - 1) % this.textSearchMatches.length)
    },
    async goToTextSearchMatch(index: number) {
      const matches = this.textSearchMatches
      const match = matches[index]
      const currentTextLocator = this.currentTextLocator()
      if (!match || !currentTextLocator) return

      this.textSearchMatchIndex = index
      await this.goToTextLocator({
        ...currentTextLocator,
        paragraph: match.paragraphIndex,
        characterOffset: match.characterOffset,
      })
      this.$nextTick(() => this.restoreCurrentTextScrollPosition())
    },
    searchableTextParagraphs(): ShelfViewTextParagraph[] {
      return this.textAdapter?.getVisibleParagraphs('CONTINUOUS_SCROLL') ?? this.mixedContentAdapter?.getVisibleParagraphs('CONTINUOUS_SCROLL') ?? []
    },
    textContentsParagraphs(): ShelfViewTextParagraph[] {
      const paragraphs = this.searchableTextParagraphs()
      const headings = paragraphs.filter(paragraph => this.textParagraphLooksLikeHeading(paragraph))
      return headings.length > 1 ? headings : paragraphs
    },
    textContentsLabel(paragraph: ShelfViewTextParagraph): string {
      const label = this.normalizedTextContentsLabel(paragraph.text)
      if (!label) return this.$t('shelfview_reader.bookmark_paragraph', {n: paragraph.paragraphIndex + 1}) as string
      return label.length > TEXT_CONTENTS_LABEL_MAX_LENGTH
        ? `${label.slice(0, TEXT_CONTENTS_LABEL_MAX_LENGTH)}...`
        : label
    },
    normalizedTextContentsLabel(text: string): string {
      return text.replace(/\s+/g, ' ').trim().replace(/^#{1,6}\s+/, '')
    },
    textParagraphLooksLikeHeading(paragraph: ShelfViewTextParagraph): boolean {
      const original = paragraph.text.replace(/\s+/g, ' ').trim()
      const label = this.normalizedTextContentsLabel(paragraph.text)
      if (!label || label.length > TEXT_CONTENTS_HEADING_MAX_LENGTH) return false

      return /^#{1,6}\s+\S+/.test(original) ||
        /^(chapter|part|book)\s+[\divxlcdm]+(?:\s|$|[.:)-])/i.test(label) ||
        /^제\s*\d+\s*(장|부|화|편)(?:\s|$|[.:：-])/.test(label) ||
        /^\d+(?:\.\d+)*[.)]?\s+\S+/.test(label)
    },
    textParagraphClass(paragraphIndex: number): object {
      return {
        'shelfview-reader-shell__text-paragraph--highlighted': this.textParagraphBookmarked(paragraphIndex),
        'shelfview-reader-shell__text-paragraph--search-current': this.currentTextSearchMatch?.paragraphIndex === paragraphIndex,
      }
    },
    textParagraphSegments(paragraph: ShelfViewTextParagraph): ShelfViewTextParagraphSegment[] {
      const ranges = this.textDedicatedHighlightRangesForParagraph(paragraph.paragraphIndex, paragraph.text.length)
      if (ranges.length === 0) {
        return [{
          key: `${paragraph.paragraphIndex}-plain`,
          text: paragraph.text,
          highlighted: false,
        }]
      }

      const segments: ShelfViewTextParagraphSegment[] = []
      let cursor = 0

      ranges
        .sort((a, b) => a.start - b.start || a.end - b.end)
        .forEach((range, rangeIndex) => {
          if (range.end <= cursor) return

          const start = Math.max(cursor, range.start)
          if (start > cursor) {
            segments.push({
              key: `${paragraph.paragraphIndex}-plain-${cursor}-${start}`,
              text: paragraph.text.slice(cursor, start),
              highlighted: false,
            })
          }

          segments.push({
            key: `${paragraph.paragraphIndex}-highlight-${rangeIndex}-${start}-${range.end}`,
            text: paragraph.text.slice(start, range.end),
            highlighted: true,
            color: range.color,
          })
          cursor = range.end
        })

      if (cursor < paragraph.text.length) {
        segments.push({
          key: `${paragraph.paragraphIndex}-plain-${cursor}-end`,
          text: paragraph.text.slice(cursor),
          highlighted: false,
        })
      }

      return segments
    },
    textParagraphSegmentClass(segment: ShelfViewTextParagraphSegment): object {
      const classes: Record<string, boolean> = {
        'shelfview-reader-shell__text-highlight': segment.highlighted,
      }
      if (segment.highlighted && segment.color) {
        classes[`shelfview-reader-shell__text-highlight--${segment.color.toLowerCase()}`] = true
      }
      return classes
    },
    textParagraphHasDedicatedHighlight(paragraphIndex: number): boolean {
      return this.textDedicatedHighlightRangesForParagraph(paragraphIndex, this.currentTextParagraphLength(paragraphIndex)).length > 0
    },
    textDedicatedHighlightRangesForParagraph(
      paragraphIndex: number,
      paragraphLength: number,
    ): ShelfViewTextHighlightRange[] {
      if (paragraphLength <= 0) return []

      return this.highlights
        .map(highlight => {
          const selector = this.textHighlightSelectorForCurrentContent(highlight)
          if (!selector) return undefined
          if (paragraphIndex < selector.startParagraph || paragraphIndex > selector.endParagraph) return undefined

          const start = selector.startParagraph === paragraphIndex ? selector.startCharacterOffset : 0
          const end = selector.endParagraph === paragraphIndex ? selector.endCharacterOffset : paragraphLength
          const clampedStart = Math.round(clampNumber(start, 0, paragraphLength))
          const clampedEnd = Math.round(clampNumber(end, 0, paragraphLength))
          if (clampedEnd <= clampedStart) return undefined

          return {
            start: clampedStart,
            end: clampedEnd,
            color: highlight.color,
          }
        })
        .filter((range): range is ShelfViewTextHighlightRange => !!range)
    },
    textHighlightSelectorForCurrentContent(highlight: ShelfViewHighlightDto): ShelfViewTextRangeHighlightSelector | undefined {
      if (highlight.selector.kind === 'TEXT_RANGE') {
        if (this.currentLocator?.kind !== 'TEXT' || highlight.locator.kind !== 'TEXT') return undefined
        return highlight.selector
      }

      if (highlight.selector.kind !== 'MIXED_TEXT_RANGE') return undefined
      if (this.currentLocator?.kind !== 'MIXED' || highlight.locator.kind !== 'MIXED') return undefined
      if (highlight.selector.itemId !== this.currentLocator.itemId || highlight.selector.itemIndex !== this.currentLocator.itemIndex) return undefined
      if (highlight.locator.itemId !== this.currentLocator.itemId || highlight.locator.itemIndex !== this.currentLocator.itemIndex) return undefined
      return highlight.selector.itemSelector
    },
    textParagraphBookmarked(paragraphIndex: number): boolean {
      return this.bookmarks.some(bookmark => this.bookmarkMatchesCurrentTextParagraph(bookmark, paragraphIndex))
    },
    bookmarkMatchesCurrentTextParagraph(
      bookmark: ShelfViewBookmarkDto,
      paragraphIndex: number,
    ): boolean {
      if (this.currentLocator?.kind === 'MIXED') {
        return bookmark.locator.kind === 'MIXED' &&
          bookmark.locator.itemId === this.currentLocator.itemId &&
          bookmark.locator.itemLocator.kind === 'TEXT' &&
          bookmark.locator.itemLocator.paragraph === paragraphIndex
      }

      return bookmark.locator.kind === 'TEXT' && bookmark.locator.paragraph === paragraphIndex
    },
    async goToPageJumpInput() {
      const pageNumber = Number(this.pageJumpInput)
      if (!Number.isFinite(pageNumber)) {
        this.pageJumpInput = this.currentPageNumber.toString()
        return
      }

      await this.goToPageIndex(Math.round(clampNumber(pageNumber, 1, this.pagesCount)) - 1)
      this.$nextTick(() => this.restoreCurrentVisibleContent())
    },
    toggleSlideshow() {
      if (this.slideshowActive) {
        this.stopSlideshow()
        return
      }

      if (!this.fixedPageControlsVisible || this.pagesCount < 2) return
      this.slideshowActive = true
      this.slideshowTimer = window.setInterval(() => {
        void this.advanceSlideshow()
      }, SLIDESHOW_INTERVAL_MS)
    },
    async advanceSlideshow() {
      if (!this.slideshowActive || !this.fixedPageControlsVisible) {
        this.stopSlideshow()
        return
      }

      if (!this.canGoNext) {
        this.openEndOfBookPrompt()
        return
      }

      await this.nextPage()
    },
    stopSlideshow() {
      if (this.slideshowTimer !== undefined) {
        window.clearInterval(this.slideshowTimer)
        this.slideshowTimer = undefined
      }
      this.slideshowActive = false
    },
    openEndOfBookPrompt() {
      this.stopSlideshow()
      this.endOfBookPromptOpen = true
    },
    closeEndOfBookPrompt() {
      this.endOfBookPromptOpen = false
      this.$nextTick(() => this.focusReaderStage())
    },
    async goToNextBook() {
      if (!this.siblingNext) {
        await this.exitEndOfBookPrompt()
        return
      }

      this.endOfBookPromptOpen = false
      await this.completeCurrentBookFromEndPrompt()
      this.$router.push({
        name: getBookReadRouteFromMedia(this.siblingNext.media),
        params: {bookId: this.siblingNext.id.toString()},
        query: this.nextBookRouteQuery(),
      })
    },
    async exitEndOfBookPrompt() {
      this.endOfBookPromptOpen = false
      await this.completeCurrentBookFromEndPrompt()
      await this.closeBook()
    },
    async completeCurrentBookFromEndPrompt() {
      await this.flushReaderState()
      await this.markCurrentBookCompleted()
    },
    async markCurrentBookCompleted() {
      if (this.readerIncognito) return

      try {
        await this.$talesBooks.updateReadProgress(this.bookId, {completed: true})
      } catch (e) {
        this.setReaderStateStatus(this.browserOffline() ? 'OFFLINE' : 'ERROR', this.$t('shelfview_reader.status_save_failed') as string)
      }
    },
    nextBookRouteQuery(): Record<string, string | undefined> {
      return {
        context: this.context.origin,
        contextId: this.context.id,
        incognito: this.readerIncognito ? 'true' : undefined,
      }
    },
    closeShortcutHelp() {
      this.showShortcutHelp = false
      this.$nextTick(() => this.focusReaderStage())
    },
    toggleChrome() {
      this.chromeVisible = !this.chromeVisible
      if (!this.chromeVisible) this.closeOverflowMenu()
    },
    showChromeFromReaderTap() {
      if (!this.chromeVisible) {
        this.chromeVisible = true
        return
      }

      this.scheduleChromeAutoHide()
    },
    toggleOverflowMenu() {
      this.overflowMenuOpen = !this.overflowMenuOpen
    },
    closeOverflowMenu() {
      this.overflowMenuOpen = false
    },
    toggleThumbnailStrip() {
      this.thumbnailStripOpen = !this.thumbnailStripOpen
      this.noteChromeActivity()
    },
    maybeShowChromeHint() {
      const storage = this.browserStorage()
      try {
        if (storage?.getItem(CHROME_HINT_STORAGE_KEY)) return
        storage?.setItem(CHROME_HINT_STORAGE_KEY, 'true')
      } catch (e) {
        // Ignore private-mode storage failures; the hint is only a first-run aid.
      }
      this.chromeHintVisible = true
    },
    toggleFullscreen() {
      if (!screenfull.isEnabled) return
      if (screenfull.isFullscreen) {
        void screenfull.exit()
      } else {
        void screenfull.request(document.documentElement, {navigationUI: 'hide'})
      }
    },
    onFullscreenChange() {
      this.isFullscreen = screenfull.isEnabled && screenfull.isFullscreen
      if (this.isFullscreen) this.chromeVisible = false
    },
    closeBookmarksDrawer() {
      this.bookmarksDrawerOpen = false
      this.cancelBookmarkNoteEdit()
      this.$nextTick(() => this.focusReaderStage())
    },
    closeContentsDrawer() {
      this.contentsDrawerOpen = false
      this.$nextTick(() => this.focusReaderStage())
    },
    closeDictionaryDrawer() {
      this.dictionaryDrawerOpen = false
      this.$nextTick(() => this.focusReaderStage())
    },
    openDictionaryLookup() {
      const selectedText = this.normalizedDictionaryQuery(window.getSelection?.()?.toString() ?? '')
      if (selectedText) this.dictionaryQuery = selectedText
      this.dictionaryDrawerOpen = true
    },
    openDictionaryPackImport() {
      const input = this.$refs.dictionaryPackInput as HTMLInputElement | undefined
      input?.click()
    },
    async onDictionaryPackFileSelected(event: Event) {
      const input = event.target as HTMLInputElement | null
      const file = input?.files?.[0]
      if (!file) return

      try {
        await this.importDictionaryPackJson(await file.text())
      } catch (e) {
        this.setReaderStateStatus('ERROR', this.$t('shelfview_reader.status_dictionary_pack_failed') as string)
      } finally {
        if (input) input.value = ''
      }
    },
    async importDictionaryPackJson(rawJson: string) {
      const pack = shelfViewParseDictionaryPack(rawJson)
      this.applyDictionaryPack(pack)
      this.saveDictionaryPackToBrowserStorage(pack)
      this.setReaderStateStatus('SAVED', this.$t('shelfview_reader.status_dictionary_pack_loaded') as string)
    },
    applyDictionaryPack(pack: ShelfViewDictionaryPack) {
      this.dictionaryLocalEntries = pack.entries.map(entry => ({
        ...entry,
        sourceName: entry.sourceName ?? pack.metadata.sourceName,
        sourceUrl: entry.sourceUrl ?? pack.metadata.sourceUrl,
        licenseName: entry.licenseName ?? pack.metadata.licenseName,
      }))
    },
    loadDictionaryPackFromBrowserStorage() {
      const storage = this.browserStorage()
      if (!storage) return

      const storedPack = storage.getItem(DICTIONARY_PACK_STORAGE_KEY)
      if (!storedPack) return

      try {
        this.applyDictionaryPack(shelfViewParseDictionaryPack(storedPack))
      } catch (e) {
        storage.removeItem(DICTIONARY_PACK_STORAGE_KEY)
      }
    },
    saveDictionaryPackToBrowserStorage(pack: ShelfViewDictionaryPack) {
      const storage = this.browserStorage()
      if (!storage) return

      try {
        storage.setItem(DICTIONARY_PACK_STORAGE_KEY, JSON.stringify(pack))
      } catch (e) {
        storage.removeItem(DICTIONARY_PACK_STORAGE_KEY)
      }
    },
    clearDictionaryPack() {
      const storage = this.browserStorage()
      storage?.removeItem(DICTIONARY_PACK_STORAGE_KEY)
      this.dictionaryLocalEntries = []
      this.setReaderStateStatus('SAVED', this.$t('shelfview_reader.status_dictionary_pack_cleared') as string)
    },
    browserStorage(): Storage | undefined {
      try {
        return typeof window !== 'undefined' ? window.localStorage : undefined
      } catch (e) {
        return undefined
      }
    },
    normalizedDictionaryQuery(text: string): string {
      return shelfViewNormalizeDictionaryQuery(text)
    },
    isCurrentManifestContentsItem(item: ShelfViewManifestContentsItem): boolean {
      return this.sameReaderTarget(item.locator, this.currentLocator)
    },
    isCurrentMixedItem(item: ShelfViewManifestItem): boolean {
      return this.currentLocator?.kind === 'MIXED' && this.currentLocator.itemId === item.itemId
    },
    isCurrentTextContentsItem(item: ShelfViewTextContentsItem): boolean {
      return this.currentTextLocator()?.paragraph === item.paragraphIndex
    },
    async goToManifestContentsItem(item: ShelfViewManifestContentsItem) {
      if (!this.adapter) return

      await this.adapter.goTo(item.locator)
      this.normalizeReaderLayout()
      this.syncAdapterState()
      this.closeContentsDrawer()
      this.$nextTick(() => this.restoreCurrentVisibleContent())
      this.schedulePositionSave()
    },
    async goToMixedContentItem(item: ShelfViewManifestItem) {
      if (!this.adapter || this.manifest?.kind !== 'MIXED_CONTENT_SEQUENCE') return

      const itemIndex = this.mixedContentsItems.findIndex(existing => existing.itemId === item.itemId)
      if (itemIndex < 0) return

      const itemLocator = this.firstLocatorForMixedContentItem(item)

      await this.adapter.goTo({
        schemaVersion: SHELFVIEW_LOCATOR_SCHEMA_VERSION,
        kind: 'MIXED',
        itemId: item.itemId,
        itemIndex,
        itemLocator,
        contentFingerprint: this.manifest.contentFingerprint,
      })
      this.normalizeReaderLayout()
      this.syncAdapterState()
      this.closeContentsDrawer()
      this.$nextTick(() => this.restoreCurrentTextScrollPosition())
      this.schedulePositionSave()
    },
    firstLocatorForMixedContentItem(item: ShelfViewManifestItem): ShelfViewPageLocator | ShelfViewTextLocator {
      if (item.kind === 'TEXT_REFLOW') {
        return {
          schemaVersion: SHELFVIEW_LOCATOR_SCHEMA_VERSION,
          kind: 'TEXT',
          paragraph: 0,
          characterOffset: 0,
          progression: 0,
          normalizationVersion: item.text?.normalizationVersion ?? 1,
          contentFingerprint: item.contentFingerprint,
        }
      }

      return {
        schemaVersion: SHELFVIEW_LOCATOR_SCHEMA_VERSION,
        kind: 'PAGE',
        page: 0,
        offset: 0,
        contentFingerprint: item.contentFingerprint,
      }
    },
    async goToTextContentsItem(item: ShelfViewTextContentsItem) {
      const currentTextLocator = this.currentTextLocator()
      if (!currentTextLocator) return

      await this.goToTextLocator({
        ...currentTextLocator,
        paragraph: item.paragraphIndex,
        characterOffset: 0,
      })
      this.closeContentsDrawer()
      this.$nextTick(() => this.restoreCurrentTextScrollPosition())
    },
    async addBookmarkForCurrentPage() {
      if (!this.currentLocator) return
      if (this.browserOffline()) {
        this.setReaderStateStatus('OFFLINE', this.$t('shelfview_reader.status_offline') as string)
        return
      }

      try {
        const bookmark = await this.$shelfViewState.createBookmark(this.bookId, {
          locator: this.currentLocator,
          label: (this.textControlsVisible
            ? this.$t('shelfview_reader.bookmark_paragraph', {n: this.currentPageNumber})
            : this.$t('shelfview_reader.bookmark_page', {n: this.currentPageNumber})) as string,
        })
        this.bookmarks = [
          ...this.bookmarks.filter(existing => existing.id !== bookmark.id),
          bookmark,
        ]
        this.setReaderStateStatus('SAVED', this.$t('shelfview_reader.status_bookmark_saved') as string)
      } catch (e) {
        this.setReaderStateStatus(this.browserOffline() ? 'OFFLINE' : 'ERROR', this.$t('shelfview_reader.status_bookmark_failed') as string)
      }
    },
    async addTextHighlightForCurrentSelection() {
      const selectedRange = this.selectedTextHighlightRange() ?? this.currentParagraphHighlightRange()
      const locator = selectedRange ? this.textHighlightLocatorForRange(selectedRange) : undefined
      const selector = selectedRange ? this.textHighlightSelectorForRange(selectedRange) : undefined
      if (!locator || !selector) return
      if (this.browserOffline()) {
        this.setReaderStateStatus('OFFLINE', this.$t('shelfview_reader.status_offline') as string)
        return
      }

      try {
        const update: ShelfViewHighlightUpdateDto = {
          locator,
          selector,
          color: 'YELLOW',
        }
        if (selectedRange?.textQuote) update.textQuote = selectedRange.textQuote

        const highlight = await this.$shelfViewState.createHighlight(this.bookId, update)
        this.highlights = [
          ...this.highlights.filter(existing => existing.id !== highlight.id),
          highlight,
        ]
        window.getSelection?.()?.removeAllRanges()
        this.setReaderStateStatus('SAVED', this.$t('shelfview_reader.status_highlight_saved') as string)
      } catch (e) {
        this.setReaderStateStatus(this.browserOffline() ? 'OFFLINE' : 'ERROR', this.$t('shelfview_reader.status_highlight_failed') as string)
      }
    },
    selectedTextHighlightRange(): ShelfViewSelectedTextRange | undefined {
      const selection = window.getSelection?.()
      if (!selection || selection.rangeCount === 0 || selection.isCollapsed) return undefined

      const range = selection.getRangeAt(0)
      const start = this.textSelectionEndpoint(range.startContainer, range.startOffset)
      const end = this.textSelectionEndpoint(range.endContainer, range.endOffset)
      if (!start || !end) return undefined
      if (end.paragraphIndex < start.paragraphIndex) return undefined
      if (end.paragraphIndex === start.paragraphIndex && end.characterOffset <= start.characterOffset) return undefined

      return {
        startParagraph: start.paragraphIndex,
        startCharacterOffset: start.characterOffset,
        endParagraph: end.paragraphIndex,
        endCharacterOffset: end.characterOffset,
        textQuote: this.normalizedTextQuote(selection.toString()),
      }
    },
    textSelectionEndpoint(
      container: Node,
      offset: number,
    ): ShelfViewTextSelectionEndpoint | undefined {
      const element = container.nodeType === Node.ELEMENT_NODE
        ? container as Element
        : container.parentElement
      const paragraph = element?.closest<HTMLElement>('[data-text-paragraph-index]')
      if (!paragraph) return undefined

      const paragraphIndex = Number(paragraph.dataset.textParagraphIndex)
      if (!Number.isFinite(paragraphIndex)) return undefined

      const range = document.createRange()
      range.selectNodeContents(paragraph)
      try {
        range.setEnd(container, offset)
      } catch (e) {
        return undefined
      }

      const characterOffset = Math.round(clampNumber(
        range.toString().length,
        0,
        this.currentTextParagraphLength(paragraphIndex),
      ))

      return {
        paragraphIndex,
        characterOffset,
      }
    },
    currentParagraphHighlightRange(): ShelfViewSelectedTextRange | undefined {
      const locator = this.currentTextLocator()
      if (!locator) return undefined

      const paragraph = this.visibleTextParagraphs.find(existing => existing.paragraphIndex === locator.paragraph)
      const paragraphLength = paragraph?.text.length ?? this.currentTextParagraphLength(locator.paragraph)
      if (paragraphLength <= 0) return undefined

      return {
        startParagraph: locator.paragraph,
        startCharacterOffset: 0,
        endParagraph: locator.paragraph,
        endCharacterOffset: paragraphLength,
        textQuote: this.normalizedTextQuote(paragraph?.text ?? ''),
      }
    },
    textHighlightLocatorForRange(range: ShelfViewSelectedTextRange): ShelfViewLocator | undefined {
      const currentTextLocator = this.currentTextLocator()
      if (!currentTextLocator || !this.currentLocator) return undefined

      const textLocator: ShelfViewTextLocator = {
        ...currentTextLocator,
        paragraph: range.startParagraph,
        characterOffset: range.startCharacterOffset,
        progression: 0,
      }

      if (this.currentLocator?.kind === 'MIXED') {
        return {
          ...this.currentLocator,
          itemLocator: textLocator,
        }
      }

      return textLocator
    },
    textHighlightSelectorForRange(range: ShelfViewSelectedTextRange): ShelfViewHighlightSelector | undefined {
      const currentTextLocator = this.currentTextLocator()
      if (!currentTextLocator) return undefined

      const textSelector: ShelfViewTextRangeHighlightSelector = {
        schemaVersion: SHELFVIEW_HIGHLIGHT_SELECTOR_SCHEMA_VERSION,
        kind: 'TEXT_RANGE',
        startParagraph: range.startParagraph,
        startCharacterOffset: range.startCharacterOffset,
        endParagraph: range.endParagraph,
        endCharacterOffset: range.endCharacterOffset,
        normalizationVersion: currentTextLocator.normalizationVersion,
      }

      if (this.currentLocator?.kind === 'MIXED') {
        return {
          schemaVersion: SHELFVIEW_HIGHLIGHT_SELECTOR_SCHEMA_VERSION,
          kind: 'MIXED_TEXT_RANGE',
          itemId: this.currentLocator.itemId,
          itemIndex: this.currentLocator.itemIndex,
          itemSelector: textSelector,
        }
      }

      return textSelector
    },
    normalizedTextQuote(text: string): string {
      return text.replace(/\s+/g, ' ').trim().slice(0, TEXT_HIGHLIGHT_QUOTE_MAX_LENGTH)
    },
    async goToBookmark(bookmark: ShelfViewBookmarkDto) {
      if (!this.adapter) return

      try {
        await this.adapter.goTo(bookmark.locator)
        this.syncAdapterState()
        this.schedulePositionSave()
        this.closeBookmarksDrawer()
      } catch (e) {
        this.setReaderStateStatus('ERROR', this.$t('shelfview_reader.status_bookmark_unavailable') as string)
      }
    },
    async deleteBookmark(bookmark: ShelfViewBookmarkDto) {
      if (this.browserOffline()) {
        this.setReaderStateStatus('OFFLINE', this.$t('shelfview_reader.status_offline') as string)
        return
      }

      try {
        await this.$shelfViewState.deleteBookmark(bookmark.id)
        this.bookmarks = this.bookmarks.filter(existing => existing.id !== bookmark.id)
        if (this.editingBookmarkId === bookmark.id) this.cancelBookmarkNoteEdit()
        this.setReaderStateStatus('SAVED', this.$t('shelfview_reader.status_bookmark_deleted') as string)
      } catch (e) {
        this.setReaderStateStatus(this.browserOffline() ? 'OFFLINE' : 'ERROR', this.$t('shelfview_reader.status_bookmark_failed') as string)
      }
    },
    startBookmarkNoteEdit(bookmark: ShelfViewBookmarkDto) {
      this.editingBookmarkId = bookmark.id
      this.bookmarkNoteDraft = bookmark.note ?? ''
    },
    cancelBookmarkNoteEdit() {
      this.editingBookmarkId = null
      this.bookmarkNoteDraft = ''
    },
    async saveBookmarkNote(bookmark: ShelfViewBookmarkDto) {
      if (this.browserOffline()) {
        this.setReaderStateStatus('OFFLINE', this.$t('shelfview_reader.status_offline') as string)
        return
      }

      const note = this.bookmarkNoteDraft.trim() || undefined

      try {
        const updatedBookmark = await this.$shelfViewState.updateBookmark(bookmark.id, {
          locator: bookmark.locator,
          label: bookmark.label,
          note,
        })
        this.bookmarks = this.bookmarks.map(existing => existing.id === updatedBookmark.id ? updatedBookmark : existing)
        this.cancelBookmarkNoteEdit()
        this.setReaderStateStatus('SAVED', this.$t('shelfview_reader.status_bookmark_saved') as string)
      } catch (e) {
        this.setReaderStateStatus(this.browserOffline() ? 'OFFLINE' : 'ERROR', this.$t('shelfview_reader.status_bookmark_failed') as string)
      }
    },
    bookmarkLabel(bookmark: ShelfViewBookmarkDto): string {
      if (bookmark.label) return bookmark.label
      if (bookmark.locator.kind === 'PAGE') return this.$t('shelfview_reader.bookmark_page', {n: bookmark.locator.page + 1}) as string
      if (bookmark.locator.kind === 'MIXED') return this.mixedBookmarkLabel(bookmark.locator)
      return this.$t('shelfview_reader.bookmark_paragraph', {n: bookmark.locator.paragraph + 1}) as string
    },
    mixedBookmarkLabel(locator: ShelfViewLocator): string {
      if (locator.kind !== 'MIXED') return this.$t('shelfview_reader.bookmark_item') as string

      const itemNumber = locator.itemIndex + 1
      if (locator.itemLocator.kind === 'PAGE') return this.$t('shelfview_reader.bookmark_mixed_item_page', {item: itemNumber, n: locator.itemLocator.page + 1}) as string
      return this.$t('shelfview_reader.bookmark_mixed_item_paragraph', {item: itemNumber, n: locator.itemLocator.paragraph + 1}) as string
    },
    onReaderOnline() {
      if (this.readerStateStatus === 'OFFLINE') {
        this.setReaderStateStatus('PENDING', this.$t('shelfview_reader.status_saving') as string)
        void this.flushReaderState()
      }
    },
    onReaderOffline() {
      this.setReaderStateStatus('OFFLINE', this.$t('shelfview_reader.status_offline') as string)
    },
    onVisibilityChange() {
      if (document.visibilityState === 'hidden') void this.flushReaderState()
    },
    onReaderViewportChanged() {
      this.$nextTick(() => this.restoreCurrentVisibleContent())
    },
    onPageHide() {
      void this.flushReaderState()
    },
    async onReaderKeydown(e: KeyboardEvent) {
      if (e.ctrlKey || e.altKey || e.metaKey) return
      if (shelfViewShouldIgnoreShortcutTarget(e.target)) return
      this.noteChromeActivity()

      if (this.endOfBookPromptOpen) {
        if (e.key === 'Escape') {
          e.preventDefault()
          this.closeEndOfBookPrompt()
        } else if (e.key === 'ArrowRight' && this.siblingNext) {
          e.preventDefault()
          await this.goToNextBook()
        }
        return
      }

      if (e.key === 'Escape') {
        if (this.anyChromePanelOpen) return

        e.preventDefault()
        this.toggleChrome()
        return
      }

      const action = shelfViewReaderKeyboardAction(e.key, this.readingDirection)
      if (!action) return

      e.preventDefault()
      await this.runReaderInputAction(action)
    },
    onReaderWheel(e: WheelEvent) {
      if (this.readerLayout === 'CONTINUOUS_SCROLL' && !e.ctrlKey && !e.metaKey) {
        this.openEndPromptForContinuousScrollEnd(e)
        return
      }
      if (!this.fixedPageControlsVisible || this.hasDisplayError || this.empty) return
      if (shelfViewShouldIgnoreShortcutTarget(e.target)) return
      if (!Number.isFinite(e.deltaY) || e.deltaY === 0) return

      const readerStage = this.$refs.readerStage as HTMLElement | undefined
      if (!readerStage) return

      const nextScale = this.normalizedFixedPageScale(
        this.fixedPageScale + (e.deltaY < 0 ? FIXED_PAGE_WHEEL_SCALE_STEP : -FIXED_PAGE_WHEEL_SCALE_STEP),
      )
      if (nextScale === this.fixedPageScale) return

      if (e.cancelable) e.preventDefault()

      const previousScale = this.fixedPageScale
      const rect = readerStage.getBoundingClientRect()
      const nextScroll = shelfViewAnchoredZoomScroll({
        previousScale,
        nextScale,
        scrollLeft: readerStage.scrollLeft,
        scrollTop: readerStage.scrollTop,
        stageX: e.clientX - rect.left,
        stageY: e.clientY - rect.top,
      })

      this.fixedPageScale = nextScale
      this.$nextTick(() => {
        readerStage.scrollLeft = nextScroll.scrollLeft
        readerStage.scrollTop = nextScroll.scrollTop
      })
      this.schedulePreferenceSave()
    },
    onReaderPointerDown(e: PointerEvent) {
      if (e.pointerType !== 'mouse' || e.button !== 0) return
      if (shelfViewShouldIgnoreShortcutTarget(e.target)) return

      this.readerPointerTapStart = {
        pointerId: e.pointerId,
        x: e.clientX,
        y: e.clientY,
      }

      if (!this.fixedPageControlsVisible) return

      const readerStage = this.$refs.readerStage as HTMLElement | undefined
      if (!readerStage) return

      this.fixedPagePanStart = {
        pointerId: e.pointerId,
        x: e.clientX,
        y: e.clientY,
        scrollLeft: readerStage.scrollLeft,
        scrollTop: readerStage.scrollTop,
      }
      this.fixedPagePanning = false
      readerStage.setPointerCapture?.(e.pointerId)
      if (e.cancelable) e.preventDefault()
    },
    onReaderPointerMove(e: PointerEvent) {
      this.noteChromeActivity()
      if (!this.fixedPagePanStart || e.pointerId !== this.fixedPagePanStart.pointerId) return

      const readerStage = this.$refs.readerStage as HTMLElement | undefined
      if (!readerStage) return

      if ((e.buttons & 1) !== 1) {
        this.finishFixedPagePan(e)
        return
      }

      if (!this.fixedPagePanning && shelfViewShouldStartPanning(this.fixedPagePanStart, e.clientX, e.clientY)) {
        this.fixedPagePanning = true
      }

      if (this.fixedPagePanning) {
        const nextScroll = shelfViewPanScroll(this.fixedPagePanStart, e.clientX, e.clientY)
        readerStage.scrollLeft = nextScroll.scrollLeft
        readerStage.scrollTop = nextScroll.scrollTop
      }
      if (e.cancelable) e.preventDefault()
    },
    async onReaderPointerUp(e: PointerEvent) {
      const tapStart = this.readerPointerTapStart
      const wasPanning = this.fixedPagePanning
      let action: ShelfViewReaderInputAction | undefined

      if (tapStart && e.pointerId === tapStart.pointerId && !wasPanning && !this.readerPointerSelectionActive()) {
        action = this.readerTapAction(tapStart, {x: e.clientX, y: e.clientY})
      }

      this.readerPointerTapStart = undefined
      this.finishFixedPagePan(e)

      if (!action) return

      if (e.cancelable) e.preventDefault()
      await this.runReaderInputAction(action)
    },
    onReaderPointerCancel(e: PointerEvent) {
      if (this.readerPointerTapStart?.pointerId === e.pointerId) this.readerPointerTapStart = undefined
      this.finishFixedPagePan(e)
    },
    finishFixedPagePan(e?: PointerEvent) {
      if (!this.fixedPagePanStart) return
      if (e && e.pointerId !== this.fixedPagePanStart.pointerId) return

      const readerStage = this.$refs.readerStage as HTMLElement | undefined
      readerStage?.releasePointerCapture?.(this.fixedPagePanStart.pointerId)
      this.fixedPagePanStart = undefined
      this.fixedPagePanning = false
    },
    onReaderTouchStart(e: TouchEvent) {
      const touch = e.changedTouches[0]
      if (!touch) return

      this.touchStart = {
        x: touch.clientX,
        y: touch.clientY,
      }
    },
    async onReaderTouchEnd(e: TouchEvent) {
      const touch = e.changedTouches[0]
      if (!this.touchStart || !touch) return

      if (this.openEndPromptForContinuousTouchEnd(e, touch)) {
        this.touchStart = undefined
        return
      }

      const action = shelfViewTouchAction(
        this.touchStart,
        {x: touch.clientX, y: touch.clientY},
        this.readingDirection,
      ) ?? this.readerTapAction(this.touchStart, {x: touch.clientX, y: touch.clientY})
      this.touchStart = undefined

      if (!action) return

      if (e.cancelable) e.preventDefault()
      await this.runReaderInputAction(action)
    },
    openEndPromptForContinuousScrollEnd(e: WheelEvent) {
      if (!this.adapter || this.hasDisplayError || this.empty || this.endOfBookPromptOpen) return
      if (shelfViewShouldIgnoreShortcutTarget(e.target)) return
      if (!Number.isFinite(e.deltaY) || e.deltaY <= 0) return
      if (this.canGoNext || !this.readerStageAtVerticalEnd()) return

      if (e.cancelable) e.preventDefault()
      this.openEndOfBookPrompt()
    },
    openEndPromptForContinuousTouchEnd(
      e: TouchEvent,
      touch: Touch,
    ): boolean {
      if (!this.adapter || this.readerLayout !== 'CONTINUOUS_SCROLL' || this.hasDisplayError || this.empty || this.endOfBookPromptOpen) return false
      if (shelfViewShouldIgnoreShortcutTarget(e.target)) return false
      if (this.canGoNext || !this.readerStageAtVerticalEnd()) return false
      if (!this.touchStart) return false

      const deltaX = touch.clientX - this.touchStart.x
      const deltaY = touch.clientY - this.touchStart.y
      const absX = Math.abs(deltaX)
      const absY = Math.abs(deltaY)
      const swipedUpToScrollDown = deltaY < -48 && absY > absX * 1.2
      if (!swipedUpToScrollDown) return false

      if (e.cancelable) e.preventDefault()
      this.openEndOfBookPrompt()
      return true
    },
    readerStageAtVerticalEnd(): boolean {
      const readerStage = this.$refs.readerStage as HTMLElement | undefined
      if (!readerStage) return false

      return readerStage.scrollHeight - readerStage.scrollTop - readerStage.clientHeight <= 1
    },
    readerTapAction(
      start: ShelfViewTouchPoint,
      end: ShelfViewTouchPoint,
    ): ShelfViewReaderInputAction | undefined {
      if (!this.adapter || this.hasDisplayError || this.empty || this.endOfBookPromptOpen) return undefined

      const readerStage = this.$refs.readerStage as HTMLElement | undefined
      if (!readerStage) return undefined

      const rect = readerStage.getBoundingClientRect()
      return shelfViewTapAction(
        {x: start.x - rect.left, y: start.y - rect.top},
        {x: end.x - rect.left, y: end.y - rect.top},
        rect.width || readerStage.clientWidth,
        this.readingDirection,
        this.tapPagingEnabled,
      )
    },
    readerPointerSelectionActive(): boolean {
      return !!window.getSelection?.()?.toString()
    },
    onReaderStageScroll() {
      if (!this.adapter) return

      if (this.textAdapter || this.mixedContentAdapter?.isCurrentText()) {
        const locator = this.nearestTextLocator()
        if (!locator || this.sameTextLocator(locator, this.currentLocator)) return

        void this.goToTextLocator(locator)
        return
      }

      if (this.readerLayout !== 'CONTINUOUS_SCROLL') return

      const pageIndex = this.nearestContinuousContentIndex()
      if (pageIndex === undefined || pageIndex === this.currentPageIndex) return

      void this.goToPageIndex(pageIndex)
    },
    async goToTextLocator(locator: ShelfViewTextLocator) {
      if (this.mixedContentAdapter?.isCurrentText()) {
        await this.mixedContentAdapter.goToCurrentTextLocator(locator)
        this.syncAdapterState()
        this.schedulePositionSave()
        return
      }
      if (!this.textAdapter) return

      await this.textAdapter.goTo(locator)
      this.syncAdapterState()
      this.schedulePositionSave()
    },
    nearestTextLocator(): ShelfViewTextLocator | undefined {
      const currentTextLocator = this.currentTextLocator()
      if (!currentTextLocator) return undefined

      const readerStage = this.$refs.readerStage as HTMLElement | undefined
      if (!readerStage) return undefined

      const paragraphs = Array.from(readerStage.querySelectorAll<HTMLElement>('[data-text-paragraph-index]'))
      if (paragraphs.length === 0) return undefined

      const stageTop = readerStage.getBoundingClientRect().top
      let closestParagraph: HTMLElement | undefined
      let closestDistance = Number.POSITIVE_INFINITY

      paragraphs.forEach(paragraph => {
        const rect = paragraph.getBoundingClientRect()
        const distance = rect.top <= stageTop && rect.bottom >= stageTop
          ? 0
          : Math.abs(rect.top - stageTop)

        if (distance < closestDistance) {
          closestDistance = distance
          closestParagraph = paragraph
        }
      })

      if (!closestParagraph) return undefined

      const paragraph = Number(closestParagraph.dataset.textParagraphIndex)
      if (!Number.isFinite(paragraph)) return undefined

      const rect = closestParagraph.getBoundingClientRect()
      const textLength = this.currentTextParagraphLength(paragraph)

      return {
        ...currentTextLocator,
        paragraph,
        characterOffset: shelfViewTextCharacterOffsetForScroll({
          stageTop,
          paragraphTop: rect.top,
          paragraphHeight: rect.height,
          textLength,
        }),
      }
    },
    sameTextLocator(
      locator: ShelfViewTextLocator,
      currentLocator?: ShelfViewLocator,
    ): boolean {
      const currentTextLocator = currentLocator?.kind === 'MIXED' ? currentLocator.itemLocator : currentLocator

      return currentTextLocator?.kind === 'TEXT' &&
        locator.paragraph === currentTextLocator.paragraph &&
        locator.characterOffset === currentTextLocator.characterOffset
    },
    sameReaderTarget(
      locator: ShelfViewLocator,
      currentLocator?: ShelfViewLocator,
    ): boolean {
      if (!currentLocator || locator.kind !== currentLocator.kind) return false
      if (locator.contentFingerprint !== currentLocator.contentFingerprint) return false

      if (locator.kind === 'PAGE' && currentLocator.kind === 'PAGE') {
        return locator.page === currentLocator.page
      }

      if (locator.kind === 'TEXT' && currentLocator.kind === 'TEXT') {
        return locator.paragraph === currentLocator.paragraph
      }

      if (locator.kind === 'MIXED' && currentLocator.kind === 'MIXED') {
        return locator.itemId === currentLocator.itemId &&
          locator.itemIndex === currentLocator.itemIndex &&
          this.sameReaderTarget(locator.itemLocator, currentLocator.itemLocator)
      }

      return false
    },
    restoreCurrentTextScrollPosition() {
      const currentTextLocator = this.currentTextLocator()
      if (!currentTextLocator) return

      const readerStage = this.$refs.readerStage as HTMLElement | undefined
      if (!readerStage) return

      const paragraph = Array
        .from(readerStage.querySelectorAll<HTMLElement>('[data-text-paragraph-index]'))
        .find(x => Number(x.dataset.textParagraphIndex) === currentTextLocator.paragraph)
      if (!paragraph) return

      const paragraphLength = this.currentTextParagraphLength(currentTextLocator.paragraph)
      readerStage.scrollTop = shelfViewTextScrollTopForOffset({
        paragraphOffsetTop: paragraph.offsetTop,
        paragraphHeight: paragraph.offsetHeight,
        characterOffset: currentTextLocator.characterOffset,
        textLength: paragraphLength,
      })
    },
    restoreCurrentVisibleContent() {
      if (!this.adapter || this.hasDisplayError) return

      if (this.textAdapter || this.mixedContentAdapter?.isCurrentText()) {
        this.restoreCurrentTextScrollPosition()
        return
      }

      if (this.readerLayout === 'CONTINUOUS_SCROLL') this.scrollCurrentPageIntoView()
    },
    scrollCurrentPageIntoView() {
      const readerStage = this.$refs.readerStage as HTMLElement | undefined
      const page = readerStage?.querySelector<HTMLElement>(`[data-page-index="${this.currentPageIndex}"]`)
      if (!readerStage || !page) return

      if (typeof page.scrollIntoView === 'function') {
        page.scrollIntoView({block: 'start', inline: 'nearest'})
      } else {
        readerStage.scrollTop = page.offsetTop
      }
    },
    currentTextLocator(): ShelfViewTextLocator | undefined {
      if (this.currentLocator?.kind === 'TEXT') return this.currentLocator
      if (this.currentLocator?.kind === 'MIXED' && this.currentLocator.itemLocator.kind === 'TEXT') return this.currentLocator.itemLocator
      return undefined
    },
    currentTextParagraphLength(paragraphIndex: number): number {
      return this.textAdapter?.getParagraphLength(paragraphIndex) ?? this.mixedContentAdapter?.getParagraphLength(paragraphIndex) ?? 0
    },
    nearestContinuousContentIndex(): number | undefined {
      const readerStage = this.$refs.readerStage as HTMLElement | undefined
      if (!readerStage) return undefined

      const dataAttribute = this.textControlsVisible ? 'textParagraphIndex' : 'pageIndex'
      const selector = this.textControlsVisible ? '[data-text-paragraph-index]' : '[data-page-index]'
      const pages = Array.from(readerStage.querySelectorAll<HTMLElement>(selector))
      if (pages.length === 0) return undefined

      const stageTop = readerStage.getBoundingClientRect().top
      let closestPageIndex: number | undefined
      let closestDistance = Number.POSITIVE_INFINITY

      pages.forEach(page => {
        const pageIndex = Number(page.dataset[dataAttribute])
        if (!Number.isFinite(pageIndex)) return

        const distance = Math.abs(page.getBoundingClientRect().top - stageTop)
        if (distance < closestDistance) {
          closestDistance = distance
          closestPageIndex = pageIndex
        }
      })

      return closestPageIndex
    },
    async runReaderInputAction(action: ShelfViewReaderInputAction) {
      switch (action) {
        case 'NEXT':
          await this.nextPage()
          break
        case 'PREVIOUS':
          await this.previousPage()
          break
        case 'FIRST':
          await this.goToPageIndex(0)
          break
        case 'LAST':
          await this.goToPageIndex(this.pagesCount - 1)
          break
        case 'ROTATE':
          this.cycleFixedPageRotation()
          break
        case 'TOGGLE_FULLSCREEN':
          this.toggleFullscreen()
          break
        case 'TOGGLE_CHROME':
          this.showChromeFromReaderTap()
          break
        case 'SHOW_SHORTCUTS':
          this.showShortcutHelp = true
          break
      }
    },
    async closeBook() {
      await this.flushReaderState()

      if (this.book) {
        this.$router.push({
          name: this.book.oneshot ? 'browse-oneshot' : 'browse-book',
          params: {bookId: this.bookId, seriesId: this.book.seriesId},
          query: {context: this.context.origin, contextId: this.context.id},
        })
      } else {
        this.$router.push({name: 'browse-book', params: {bookId: this.bookId}})
      }
    },
  },
})

function clampNumber(
  value: number,
  min: number,
  max: number,
): number {
  return Math.max(min, Math.min(value, max))
}

function roundNumber(
  value: number,
  decimals: number,
): number {
  const factor = 10 ** decimals
  return Math.round(value * factor) / factor
}

function isFiniteNumber(value: unknown): value is number {
  return typeof value === 'number' && Number.isFinite(value)
}
</script>

<style scoped>
@font-face {
  font-display: swap;
  font-family: "ShelfView Gowun Dodum";
  font-style: normal;
  font-weight: 400;
  src: url("~@/shelfview/assets/fonts/GowunDodum-Regular.ttf") format("truetype");
}

@font-face {
  font-display: swap;
  font-family: "ShelfView Gowun Batang";
  font-style: normal;
  font-weight: 400;
  src: url("~@/shelfview/assets/fonts/GowunBatang-Regular.ttf") format("truetype");
}

.shelfview-reader-shell {
  background: #111111;
  height: 100vh;
  overflow: hidden;
  width: 100%;
}

.shelfview-reader-shell__toolbar {
  min-width: 0;
  opacity: 0;
  position: fixed;
  top: 0;
  transform: translateY(-100%);
  transition: opacity 160ms ease, transform 160ms ease;
  z-index: 2;
}

.shelfview-reader-shell__toolbar--visible {
  opacity: 1;
  transform: translateY(0);
}

.shelfview-reader-shell__title {
  max-width: min(54vw, 720px);
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.shelfview-reader-shell__page-controls {
  align-items: center;
  display: flex;
  flex: 0 1 auto;
  gap: 4px;
  margin-right: 4px;
  min-width: 0;
  overflow-x: auto;
  scrollbar-width: none;
}

.shelfview-reader-shell__page-controls::-webkit-scrollbar {
  display: none;
}

.shelfview-reader-shell__control-toggle {
  flex: 0 0 auto;
  text-transform: none;
}

.shelfview-reader-shell__control-toggle-label {
  font-size: 12px;
  margin: 0 2px 0 4px;
  max-width: 96px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.shelfview-reader-shell__overflow {
  background: rgba(22, 27, 34, 0.96);
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 8px;
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.35);
  max-width: min(720px, calc(100vw - 24px));
  padding: 8px;
  position: fixed;
  right: 12px;
  top: 56px;
  z-index: 3;
}

.shelfview-reader-shell__overflow-actions {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  justify-content: flex-end;
}

.shelfview-reader-shell__page-counter {
  font-size: 14px;
  min-width: 56px;
  text-align: center;
}

.shelfview-reader-shell__page-jump {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-radius: 4px;
  color: #ffffff;
  font-size: 13px;
  height: 28px;
  padding: 0 4px;
  text-align: center;
  width: 58px;
}

.shelfview-reader-shell__page-jump:focus {
  border-color: #90caf9;
  outline: none;
}

.shelfview-reader-shell__chrome-hint-text {
  font-size: 16px;
  margin: 0;
  text-align: center;
}

.shelfview-reader-shell__search-bar {
  align-items: center;
  background: #1d232b;
  border-bottom: 1px solid rgba(255, 255, 255, 0.14);
  display: grid;
  gap: 8px;
  grid-template-columns: minmax(160px, 1fr) auto auto auto;
  left: 0;
  padding: 8px 12px;
  position: fixed;
  right: 0;
  top: 0;
  z-index: 2;
}

.shelfview-reader-shell--chrome-visible .shelfview-reader-shell__search-bar {
  top: 48px;
}

.shelfview-reader-shell__search-status {
  color: #d9e2ec;
  font-size: 13px;
  min-width: 72px;
  text-align: center;
}

.shelfview-reader-shell__sync-status {
  border: 1px solid rgba(255, 255, 255, 0.24);
  border-radius: 6px;
  color: #f5f5f5;
  font-size: 12px;
  line-height: 1;
  margin-left: 8px;
  max-width: 120px;
  overflow: hidden;
  padding: 5px 7px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.shelfview-reader-shell__sync-status--error {
  border-color: #ef9a9a;
  color: #ffcdd2;
}

.shelfview-reader-shell__sync-status--saved {
  border-color: #a5d6a7;
  color: #c8e6c9;
}

.shelfview-reader-shell__stage {
  align-items: center;
  display: flex;
  height: 100%;
  justify-content: center;
  overflow: hidden;
  padding: 16px;
  width: 100%;
}

.shelfview-reader-shell--chrome-visible .shelfview-reader-shell__stage {
  padding-top: 56px;
}

.shelfview-reader-shell--search-open.shelfview-reader-shell--chrome-visible .shelfview-reader-shell__stage {
  padding-top: 104px;
}

.shelfview-reader-shell--search-open:not(.shelfview-reader-shell--chrome-visible) .shelfview-reader-shell__stage {
  padding-top: 56px;
}

.shelfview-reader-shell--thumbnails .shelfview-reader-shell__stage {
  padding-bottom: 104px;
}

.shelfview-reader-shell__stage:focus {
  outline: 2px solid #90caf9;
  outline-offset: -6px;
}

.shelfview-reader-shell__stage--continuous {
  align-items: stretch;
  justify-content: flex-start;
  overflow-y: auto;
}

.shelfview-reader-shell__stage--fixed {
  background: var(--sv-fixed-background, #111111);
  cursor: grab;
  overflow: auto;
  padding: var(--sv-fixed-page-margin, 16px);
}

.shelfview-reader-shell--chrome-visible .shelfview-reader-shell__stage--fixed {
  padding-top: 56px;
}

.shelfview-reader-shell--search-open.shelfview-reader-shell--chrome-visible .shelfview-reader-shell__stage--fixed {
  padding-top: 104px;
}

.shelfview-reader-shell--search-open:not(.shelfview-reader-shell--chrome-visible) .shelfview-reader-shell__stage--fixed {
  padding-top: 56px;
}

.shelfview-reader-shell--thumbnails .shelfview-reader-shell__stage--fixed {
  padding-bottom: 104px;
}

.shelfview-reader-shell__stage--panning {
  cursor: grabbing;
}

.shelfview-reader-shell__stage--text {
  align-items: stretch;
  cursor: auto;
  overflow-y: auto;
  padding-left: 0;
  padding-right: 0;
}

.shelfview-reader-shell__pages {
  align-items: center;
  display: flex;
  gap: var(--sv-fixed-page-gap, 8px);
  height: 100%;
  justify-content: center;
  user-select: none;
  width: 100%;
}

.shelfview-reader-shell__pages--continuous {
  flex-direction: column;
  height: auto;
  justify-content: flex-start;
  min-height: 100%;
}

.shelfview-reader-shell__page {
  display: block;
  object-fit: contain;
  transform: rotate(var(--sv-fixed-page-rotation, 0deg));
  transform-origin: center center;
  user-select: none;
  -webkit-user-drag: none;
}

.shelfview-page-content {
  filter:
    brightness(var(--sv-brightness, 1))
    contrast(var(--sv-contrast, 1))
    saturate(var(--sv-saturate, 1))
    grayscale(var(--sv-grayscale, 0))
    sepia(var(--sv-sepia, 0))
    invert(var(--sv-invert, 0));
}

.shelfview-reader-shell__page--fit-screen {
  max-height: calc((100vh - 72px) * var(--sv-fixed-page-scale, 1));
  max-width: calc(100% * var(--sv-fixed-page-scale, 1));
}

.shelfview-reader-shell__page--fit-width {
  height: auto;
  max-width: none;
  width: calc(100% * var(--sv-fixed-page-scale, 1));
}

.shelfview-reader-shell__page--fit-height {
  height: calc((100vh - 72px) * var(--sv-fixed-page-scale, 1));
  max-width: 100%;
  width: auto;
}

.shelfview-reader-shell__page--fit-original {
  height: auto;
  max-height: none;
  max-width: none;
  width: auto;
}

.shelfview-reader-shell__pages--double .shelfview-reader-shell__page--fit-screen {
  max-width: calc((50vw - 24px) * var(--sv-fixed-page-scale, 1));
}

.shelfview-reader-shell__pages--double .shelfview-reader-shell__page--fit-width {
  width: calc((50vw - 24px) * var(--sv-fixed-page-scale, 1));
}

.shelfview-reader-shell__pages--continuous .shelfview-reader-shell__page {
  margin-bottom: var(--sv-fixed-page-gap, 12px);
}

.shelfview-reader-shell__pages--continuous .shelfview-reader-shell__page--fit-screen {
  height: auto;
  max-height: none;
  max-width: min(100%, calc(1200px * var(--sv-fixed-page-scale, 1)));
  width: auto;
}

.shelfview-reader-shell__pages--continuous .shelfview-reader-shell__page--fit-width {
  height: auto;
  max-height: none;
  max-width: min(calc(100% * var(--sv-fixed-page-scale, 1)), calc(1200px * var(--sv-fixed-page-scale, 1)));
  width: calc(100% * var(--sv-fixed-page-scale, 1));
}

.shelfview-reader-shell__text {
  color: #e8ebf0;
  filter: brightness(var(--sv-text-brightness, 1));
  font-family: var(--sv-text-font-family, Roboto, Arial, sans-serif);
  font-size: var(--sv-text-font-size, 20px);
  font-weight: var(--sv-text-font-weight, 400);
  letter-spacing: var(--sv-text-letter-spacing, 0);
  line-height: var(--sv-text-line-height, 1.75);
  margin: 0 auto;
  min-height: 100%;
  max-width: min(100%, var(--sv-text-content-width, 760px));
  padding: var(--sv-text-page-margin-y, 42px) var(--sv-text-page-margin-x, 56px) 56px;
  text-align: var(--sv-text-align, left);
  white-space: pre-wrap;
  width: 100%;
  word-spacing: var(--sv-text-word-spacing, 0);
  word-break: keep-all;
  overflow-wrap: anywhere;
}

/* The active text theme paints the whole reading area (stage); the text column
   stays transparent so the page reads as one consistent surface with no
   mismatched gutters. Colors are kept high-contrast for legibility. */
.shelfview-reader-shell__text--paper {
  background: transparent;
  color: #1b1813;
}

.shelfview-reader-shell__text--dark {
  background: transparent;
  color: #f3f6fb;
}

.shelfview-reader-shell__text--oled {
  background: transparent;
  color: #fdfdfd;
}

.shelfview-reader-shell__stage--theme-paper {
  background: #f4ecd9;
}

.shelfview-reader-shell__stage--theme-dark {
  background: #14171c;
}

.shelfview-reader-shell__stage--theme-oled {
  background: #000000;
}

.shelfview-reader-shell__text-paragraph {
  border-radius: 4px;
  margin: 0 0 var(--sv-text-paragraph-spacing, 1.35em);
  padding: 2px 0;
}

.shelfview-reader-shell__text-paragraph--highlighted {
  background: rgba(255, 214, 102, 0.28);
  box-shadow: inset 3px 0 0 rgba(229, 172, 45, 0.78);
  padding-left: 8px;
  padding-right: 8px;
}

.shelfview-reader-shell__text-paragraph--search-current {
  background: rgba(255, 193, 7, 0.24);
}

.shelfview-reader-shell__text-highlight {
  border-radius: 2px;
  padding: 0 1px;
}

.shelfview-reader-shell__text-highlight--yellow {
  background: rgba(255, 214, 102, 0.46);
}

.shelfview-reader-shell__text-highlight--green {
  background: rgba(121, 215, 162, 0.38);
}

.shelfview-reader-shell__text-highlight--blue {
  background: rgba(114, 183, 255, 0.35);
}

.shelfview-reader-shell__text-highlight--pink {
  background: rgba(255, 145, 180, 0.38);
}

.shelfview-reader-shell__text-highlight--underline {
  background: transparent;
  box-shadow: inset 0 -0.16em 0 rgba(229, 172, 45, 0.75);
}

.shelfview-reader-shell__text-paragraph:last-child {
  margin-bottom: 0;
}

.shelfview-reader-shell__thumbnail-strip {
  align-items: center;
  background: rgba(17, 17, 17, 0.94);
  border-top: 1px solid rgba(255, 255, 255, 0.16);
  bottom: 0;
  display: flex;
  gap: 8px;
  height: 88px;
  left: 0;
  overflow-x: auto;
  padding: 8px 12px;
  position: fixed;
  right: 0;
  z-index: 2;
}

.shelfview-reader-shell__thumbnail-button {
  align-items: center;
  background: #20242b;
  border: 2px solid transparent;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  flex: 0 0 58px;
  height: 70px;
  justify-content: center;
  padding: 3px;
}

.shelfview-reader-shell__thumbnail-button--active {
  border-color: #90caf9;
}

.shelfview-reader-shell__thumbnail-button img {
  max-height: 100%;
  max-width: 100%;
  object-fit: contain;
}

.shelfview-reader-shell__status {
  align-items: center;
  color: #f5f5f5;
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-width: 520px;
  text-align: center;
}

.shelfview-reader-shell__status h2 {
  font-size: 20px;
  font-weight: 500;
  margin: 0;
}

.shelfview-reader-shell__status p {
  color: #d0d0d0;
  font-size: 14px;
  line-height: 1.5;
  margin: 0;
}

.shelfview-reader-shell__status-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}

.shelfview-reader-shell__status-note {
  color: #bdbdbd;
  font-size: 13px;
}

.shelfview-reader-shell__contents-drawer,
.shelfview-reader-shell__dictionary-drawer,
.shelfview-reader-shell__bookmarks-drawer {
  background: #202020;
  bottom: 0;
  box-shadow: -8px 0 24px rgba(0, 0, 0, 0.36);
  color: #f5f5f5;
  max-width: min(360px, 92vw);
  overflow-y: auto;
  padding: 16px;
  position: fixed;
  right: 0;
  top: 48px;
  width: 360px;
  z-index: 3;
}

.shelfview-reader-shell__contents-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.shelfview-reader-shell__contents-row {
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
}

.shelfview-reader-shell__contents-link {
  align-items: center;
  background: transparent;
  border: 0;
  color: #f5f5f5;
  cursor: pointer;
  display: flex;
  font: inherit;
  gap: 10px;
  min-height: 44px;
  padding: 10px 0;
  text-align: left;
  width: 100%;
}

.shelfview-reader-shell__contents-link:hover,
.shelfview-reader-shell__contents-link:focus-visible {
  color: #90caf9;
  outline: none;
}

.shelfview-reader-shell__contents-link--active {
  color: #90caf9;
}

.shelfview-reader-shell__contents-index {
  color: #b8d7f1;
  flex: 0 0 28px;
  font-size: 13px;
  text-align: right;
}

.shelfview-reader-shell__contents-label {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.shelfview-reader-shell__contents-active-icon {
  flex: 0 0 auto;
}

.shelfview-reader-shell__dictionary-empty {
  color: #d0d0d0;
  font-size: 14px;
  margin: 16px 0 0;
}

.shelfview-reader-shell__dictionary-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.shelfview-reader-shell__dictionary-file-input {
  display: none;
}

.shelfview-reader-shell__dictionary-results {
  display: grid;
  gap: 10px;
  list-style: none;
  margin: 16px 0 0;
  padding: 0;
}

.shelfview-reader-shell__dictionary-result {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.shelfview-reader-shell__dictionary-provider {
  color: #d0d0d0;
  font-size: 13px;
}

.shelfview-reader-shell__dictionary-term {
  color: #ffffff;
  font-size: 15px;
}

.shelfview-reader-shell__dictionary-definition {
  color: #f1f1f1;
  flex: 1 0 100%;
  font-size: 14px;
  line-height: 1.55;
  margin: 0;
  overflow-wrap: anywhere;
}

.shelfview-reader-shell__dictionary-source {
  color: #aeb7c2;
  font-size: 12px;
}

.shelfview-reader-shell__dictionary-link {
  align-items: center;
  background: #1976d2;
  border-radius: 4px;
  color: #ffffff;
  display: inline-flex;
  gap: 8px;
  min-height: 36px;
  padding: 0 14px;
  text-decoration: none;
}

.shelfview-reader-shell__dictionary-link:hover,
.shelfview-reader-shell__dictionary-link:focus-visible {
  background: #2196f3;
  outline: none;
}

.shelfview-reader-shell__text-settings {
  background: #23272e;
  bottom: 0;
  box-shadow: -8px 0 24px rgba(0, 0, 0, 0.36);
  color: #f5f5f5;
  max-width: min(380px, 94vw);
  overflow-y: auto;
  padding: 16px;
  position: fixed;
  right: 0;
  top: 48px;
  width: 380px;
  z-index: 3;
}

.shelfview-reader-shell__text-settings-header {
  align-items: center;
  display: flex;
  justify-content: space-between;
  margin-bottom: 16px;
}

.shelfview-reader-shell__text-settings-header h2 {
  font-size: 18px;
  font-weight: 500;
  margin: 0;
}

.shelfview-reader-shell__text-settings-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.shelfview-reader-shell__text-settings-primary,
.shelfview-reader-shell__text-settings-advanced-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.shelfview-reader-shell__text-settings-advanced {
  background: transparent;
}

.shelfview-reader-shell__text-setting-group-label,
.shelfview-reader-shell__text-setting-row-label {
  color: #f5f5f5;
  display: block;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 8px;
}

.shelfview-reader-shell__text-theme-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.shelfview-reader-shell__text-setting-row--stepper {
  align-items: center;
  display: flex;
  justify-content: space-between;
}

.shelfview-reader-shell__text-setting-row--stepper .shelfview-reader-shell__text-setting-row-label {
  margin-bottom: 0;
}

.shelfview-reader-shell__text-font-size-stepper {
  align-items: center;
  display: flex;
  gap: 8px;
}

.shelfview-reader-shell__text-font-size-value {
  color: #b8d7f1;
  font-weight: 600;
  min-width: 52px;
  text-align: center;
}

.shelfview-reader-shell__text-setting-row label {
  align-items: baseline;
  display: flex;
  font-size: 13px;
  font-weight: 600;
  justify-content: space-between;
  margin-bottom: 6px;
}

.shelfview-reader-shell__text-setting-row label span {
  color: #b8d7f1;
  font-weight: 500;
}

.shelfview-reader-shell__drawer-header {
  align-items: center;
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
}

.shelfview-reader-shell__drawer-header h2 {
  font-size: 18px;
  font-weight: 500;
  margin: 0;
}

.shelfview-reader-shell__bookmarks-empty {
  color: #d0d0d0;
  font-size: 14px;
  margin: 18px 0 0;
}

.shelfview-reader-shell__bookmarks-list {
  list-style: none;
  margin: 16px 0 0;
  padding: 0;
}

.shelfview-reader-shell__bookmark-row {
  align-items: flex-start;
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
  display: flex;
  gap: 8px;
  min-height: 44px;
  padding: 6px 0;
}

.shelfview-reader-shell__bookmark-main {
  flex: 1;
  min-width: 0;
}

.shelfview-reader-shell__bookmark-link {
  background: transparent;
  border: 0;
  color: #f5f5f5;
  cursor: pointer;
  flex: 1;
  font: inherit;
  min-width: 0;
  overflow: hidden;
  padding: 10px 0;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.shelfview-reader-shell__bookmark-note {
  color: #d7dce2;
  font-size: 13px;
  line-height: 1.4;
  margin: -4px 0 8px;
  overflow-wrap: anywhere;
}

.shelfview-reader-shell__bookmark-note-editor {
  display: flex;
  gap: 6px;
  margin: 4px 0 8px;
}

.shelfview-reader-shell__bookmark-note-editor .v-input {
  flex: 1;
}

.shelfview-reader-shell__bookmark-note-actions {
  display: flex;
  flex: 0 0 auto;
}

.shelfview-reader-shell__shortcut-dialog,
.shelfview-reader-shell__end-dialog {
  align-items: center;
  background: rgba(0, 0, 0, 0.72);
  display: flex;
  inset: 0;
  justify-content: center;
  padding: 16px;
  position: fixed;
  z-index: 4;
}

.shelfview-reader-shell__shortcut-panel,
.shelfview-reader-shell__end-panel {
  background: #202020;
  border-radius: 8px;
  color: #f5f5f5;
  max-width: 420px;
  padding: 20px;
  width: 100%;
}

.shelfview-reader-shell__shortcut-panel h2,
.shelfview-reader-shell__end-panel h2 {
  font-size: 20px;
  font-weight: 500;
  margin: 0 0 16px;
}

.shelfview-reader-shell__end-panel p {
  color: #d7dce2;
  line-height: 1.5;
  margin: 0 0 12px;
}

.shelfview-reader-shell__end-next-title {
  background: rgba(255, 255, 255, 0.08);
  border-radius: 6px;
  color: #ffffff !important;
  font-weight: 600;
  overflow-wrap: anywhere;
  padding: 10px 12px;
}

.shelfview-reader-shell__end-no-next {
  color: #cfd8dc !important;
}

.shelfview-reader-shell__end-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
  margin-top: 18px;
}

.shelfview-reader-shell__shortcut-panel dl {
  display: grid;
  gap: 8px 16px;
  grid-template-columns: auto 1fr;
  margin: 0 0 18px;
}

.shelfview-reader-shell__shortcut-panel dt {
  color: #bbdefb;
  font-weight: 600;
}

.shelfview-reader-shell__shortcut-panel dd {
  margin: 0;
}

.shelfview-reader-shell__sr-only {
  height: 1px;
  left: -10000px;
  overflow: hidden;
  position: absolute;
  top: auto;
  width: 1px;
}

.full-width {
  width: 100%;
}

@media (prefers-reduced-motion: reduce) {
  .shelfview-reader-shell * {
    scroll-behavior: auto !important;
  }
}

@media (max-width: 600px) {
  .shelfview-reader-shell__title {
    max-width: 24vw;
  }

  .shelfview-reader-shell__page-counter {
    font-size: 13px;
    min-width: 44px;
  }

  .shelfview-reader-shell__page-controls {
    gap: 0;
    max-width: 72vw;
  }

  .shelfview-reader-shell__overflow {
    left: 8px;
    max-width: none;
    right: 8px;
  }

  .shelfview-reader-shell__overflow-actions {
    justify-content: flex-start;
  }

  .shelfview-reader-shell__stage {
    padding: 8px;
  }

  .shelfview-reader-shell__stage--fixed {
    padding: var(--sv-fixed-page-margin, 8px);
  }

  .shelfview-reader-shell--chrome-visible .shelfview-reader-shell__stage,
  .shelfview-reader-shell--chrome-visible .shelfview-reader-shell__stage--fixed {
    padding-top: 56px;
  }

  .shelfview-reader-shell--search-open.shelfview-reader-shell--chrome-visible .shelfview-reader-shell__stage,
  .shelfview-reader-shell--search-open.shelfview-reader-shell--chrome-visible .shelfview-reader-shell__stage--fixed {
    padding-top: 104px;
  }

  .shelfview-reader-shell--search-open:not(.shelfview-reader-shell--chrome-visible) .shelfview-reader-shell__stage,
  .shelfview-reader-shell--search-open:not(.shelfview-reader-shell--chrome-visible) .shelfview-reader-shell__stage--fixed {
    padding-top: 56px;
  }

  .shelfview-reader-shell__pages {
    gap: var(--sv-fixed-page-gap, 4px);
  }

  .shelfview-reader-shell__pages--double .shelfview-reader-shell__page--fit-screen {
    max-width: calc((50vw - 12px) * var(--sv-fixed-page-scale, 1));
  }

  .shelfview-reader-shell__pages--double .shelfview-reader-shell__page--fit-width {
    width: calc((50vw - 12px) * var(--sv-fixed-page-scale, 1));
  }

  .shelfview-reader-shell__sync-status {
    display: none;
    max-width: 72px;
    padding: 5px;
  }

  .shelfview-reader-shell__text {
    padding:
      max(20px, calc(var(--sv-text-page-margin-y, 24px) * 0.66))
      min(var(--sv-text-page-margin-x, 18px), 28px)
      40px;
  }

  .shelfview-reader-shell__text-settings {
    max-width: 100vw;
    width: 100vw;
  }
}
</style>
