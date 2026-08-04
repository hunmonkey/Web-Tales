<template>
  <v-container
    class="ma-0 pa-0 shelfview-reader-shell"
    :class="{
      'shelfview-reader-shell--search-open': textSearchOpen && textSearchAvailable,
      'shelfview-reader-shell--thumbnails': thumbnailStripReserved,
      'shelfview-reader-shell--page-dock': pageDockReserved,
      'shelfview-reader-shell--chrome-visible': chromeVisible,
    }"
    dark
    fluid
    data-testid="shelfview-reader-shell"
  >
    <v-toolbar
      v-show="chromeVisible"
      dark
      dense
      flat
      class="shelfview-reader-shell__toolbar full-width"
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

      <!-- A failed save has to reach the reader without opening anything. The
           quiet states stay inside the settings panel so the toolbar keeps
           still while reading. -->
      <span
        v-if="readerStateFailureVisible"
        class="shelfview-reader-shell__sync-status shelfview-reader-shell__sync-status--error shelfview-reader-shell__toolbar-status"
        role="status"
        data-testid="shelfview-reader-toolbar-status"
      >
        <v-icon small>mdi-alert-circle-outline</v-icon>
        {{ readerStateMessage }}
      </span>

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

        <span
          v-if="progressPercentLabel"
          class="shelfview-reader-shell__progress"
          data-testid="shelfview-reader-progress"
        >{{ progressPercentLabel }}</span>

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
          <v-icon small>mdi-chevron-down</v-icon>
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

        <v-btn
          text
          small
          class="shelfview-reader-shell__control-toggle shelfview-reader-shell__settings-button"
          :class="{'shelfview-reader-shell__settings-button--open': overflowMenuOpen}"
          :aria-label="$t('shelfview_reader.settings')"
          :aria-expanded="overflowMenuOpen ? 'true' : 'false'"
          aria-haspopup="dialog"
          data-testid="shelfview-reader-more-toggle"
          @click="toggleOverflowMenu"
        >
          <v-icon small>mdi-tune-variant</v-icon>
          <span class="shelfview-reader-shell__control-toggle-label">{{ $t('shelfview_reader.settings') }}</span>
        </v-btn>
      </div>
    </v-toolbar>

    <section
      v-if="pageControlsVisible"
      v-show="chromeVisible && overflowMenuOpen"
      class="shelfview-reader-shell__overflow"
      role="dialog"
      :aria-label="$t('shelfview_reader.settings')"
      data-testid="shelfview-reader-overflow"
    >
      <header class="shelfview-reader-shell__overflow-header">
        <h2>{{ $t('shelfview_reader.settings') }}</h2>
        <span
          v-if="readerStateNoticeVisible"
          class="shelfview-reader-shell__sync-status"
          :class="readerStateNoticeClass"
          data-testid="shelfview-reader-sync-status"
        >
          {{ readerStateMessage }}
        </span>
        <v-btn
          icon
          small
          :aria-label="$t('shelfview_reader.settings_close')"
          data-testid="shelfview-reader-overflow-close"
          @click="closeOverflowMenu"
        >
          <v-icon>mdi-close</v-icon>
        </v-btn>
      </header>

      <div class="shelfview-reader-shell__overflow-actions">
        <section class="shelfview-reader-shell__setting-group">
          <h3 class="shelfview-reader-shell__setting-group-title">{{ $t('shelfview_reader.group_view') }}</h3>

          <!-- The toolbar drops its label on a narrow screen, so the current fit
               value has to be readable here as well (Rule_UIUX 1-8). -->
          <button
            v-if="fixedPageControlsVisible"
            type="button"
            class="shelfview-reader-shell__setting-row"
            :aria-expanded="openControlMenu === 'fit' ? 'true' : 'false'"
            aria-haspopup="menu"
            data-testid="shelfview-reader-fit-row"
            @click="toggleControlMenu('fit', $event)"
          >
            <v-icon small>{{ pageFitIcon }}</v-icon>
            <span class="shelfview-reader-shell__setting-row-label">{{ $t('shelfview_reader.control_fit') }}</span>
            <span class="shelfview-reader-shell__setting-row-value">{{ pageFitLabel }}</span>
            <v-icon small>mdi-chevron-down</v-icon>
          </button>

          <button
            type="button"
            class="shelfview-reader-shell__setting-row"
            :aria-expanded="openControlMenu === 'layout' ? 'true' : 'false'"
            aria-haspopup="menu"
            data-testid="shelfview-reader-layout-toggle"
            @click="toggleControlMenu('layout', $event)"
          >
            <v-icon small>{{ readerLayoutIcon }}</v-icon>
            <span class="shelfview-reader-shell__setting-row-label">{{ $t('shelfview_reader.control_layout') }}</span>
            <span class="shelfview-reader-shell__setting-row-value">{{ readerLayoutLabel }}</span>
            <v-icon small>mdi-chevron-down</v-icon>
          </button>

          <button
            v-if="fixedPageControlsVisible"
            type="button"
            class="shelfview-reader-shell__setting-row"
            :aria-label="fixedPageRotationLabel"
            :aria-expanded="openControlMenu === 'rotate' ? 'true' : 'false'"
            aria-haspopup="menu"
            data-testid="shelfview-reader-rotate-toggle"
            @click="toggleControlMenu('rotate', $event)"
          >
            <v-icon small>mdi-rotate-right</v-icon>
            <span class="shelfview-reader-shell__setting-row-label">{{ $t('shelfview_reader.control_rotate') }}</span>
            <span class="shelfview-reader-shell__setting-row-value">{{ fixedPageRotationShortLabel }}</span>
            <v-icon small>mdi-chevron-down</v-icon>
          </button>

          <button
            v-if="fixedPageControlsVisible"
            type="button"
            class="shelfview-reader-shell__setting-row"
            :aria-expanded="openControlMenu === 'direction' ? 'true' : 'false'"
            aria-haspopup="menu"
            :disabled="!rtlAvailable"
            data-testid="shelfview-reader-direction-toggle"
            @click="toggleControlMenu('direction', $event)"
          >
            <v-icon small>{{ readingDirectionIcon }}</v-icon>
            <span class="shelfview-reader-shell__setting-row-label">{{ $t('shelfview_reader.control_direction') }}</span>
            <span class="shelfview-reader-shell__setting-row-value">{{ readingDirectionLabel }}</span>
            <v-icon small>mdi-chevron-down</v-icon>
          </button>

          <button
            v-if="fixedPageControlsVisible"
            type="button"
            class="shelfview-reader-shell__setting-row"
            :aria-expanded="openControlMenu === 'filter' ? 'true' : 'false'"
            aria-haspopup="menu"
            :disabled="!imageFiltersAvailable"
            data-testid="shelfview-reader-filter-toggle"
            @click="toggleControlMenu('filter', $event)"
          >
            <v-icon small>{{ imageFilterPresetIcon }}</v-icon>
            <span class="shelfview-reader-shell__setting-row-label">{{ $t('shelfview_reader.control_filter') }}</span>
            <span class="shelfview-reader-shell__setting-row-value">{{ imageFilterPresetLabel }}</span>
            <v-icon small>mdi-chevron-down</v-icon>
          </button>

          <button
            v-if="fixedPageControlsVisible"
            type="button"
            class="shelfview-reader-shell__setting-row"
            data-testid="shelfview-reader-fixed-settings-toggle"
            @click="openFixedPageSettings"
          >
            <v-icon small>mdi-image-edit-outline</v-icon>
            <span class="shelfview-reader-shell__setting-row-label">{{ $t('shelfview_reader.fixed_settings') }}</span>
            <v-icon small>mdi-chevron-right</v-icon>
          </button>
        </section>

        <section
          v-if="fixedPageControlsVisible || thumbnailStripAvailable"
          class="shelfview-reader-shell__setting-group"
        >
          <h3 class="shelfview-reader-shell__setting-group-title">{{ $t('shelfview_reader.group_page') }}</h3>

          <div
            v-if="fixedPageControlsVisible"
            class="shelfview-reader-shell__setting-row shelfview-reader-shell__setting-row--static"
          >
            <v-icon small>mdi-numeric</v-icon>
            <label
              class="shelfview-reader-shell__setting-row-label"
              for="shelfview-reader-page-jump-input"
            >{{ $t('shelfview_reader.page_jump') }}</label>
            <input
              id="shelfview-reader-page-jump-input"
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
          </div>

          <button
            v-if="thumbnailStripAvailable"
            type="button"
            class="shelfview-reader-shell__setting-row"
            :aria-label="thumbnailStripOpen ? $t('shelfview_reader.hide_thumbnails') : $t('shelfview_reader.show_thumbnails')"
            :aria-pressed="thumbnailStripOpen ? 'true' : 'false'"
            data-testid="shelfview-reader-thumbnails-toggle"
            @click="toggleThumbnailStrip"
          >
            <v-icon small>mdi-view-carousel-outline</v-icon>
            <span class="shelfview-reader-shell__setting-row-label">{{ $t('shelfview_reader.thumbnail_strip') }}</span>
            <span class="shelfview-reader-shell__setting-row-state">{{ thumbnailStripOpen ? $t('shelfview_reader.state_on') : $t('shelfview_reader.state_off') }}</span>
          </button>

          <button
            v-if="fixedPageControlsVisible"
            type="button"
            class="shelfview-reader-shell__setting-row"
            :aria-label="slideshowLabel"
            :aria-pressed="slideshowActive ? 'true' : 'false'"
            data-testid="shelfview-reader-slideshow-toggle"
            @click="toggleSlideshow"
          >
            <v-icon small>{{ slideshowActive ? 'mdi-pause-circle-outline' : 'mdi-play-circle-outline' }}</v-icon>
            <span class="shelfview-reader-shell__setting-row-label">{{ $t('shelfview_reader.slideshow') }}</span>
            <span class="shelfview-reader-shell__setting-row-state">{{ slideshowActive ? $t('shelfview_reader.state_on') : $t('shelfview_reader.state_off') }}</span>
          </button>
        </section>

        <section
          v-if="textControlsVisible"
          class="shelfview-reader-shell__setting-group"
        >
          <h3 class="shelfview-reader-shell__setting-group-title">{{ $t('shelfview_reader.group_text') }}</h3>

          <button
            type="button"
            class="shelfview-reader-shell__setting-row"
            :aria-expanded="openControlMenu === 'text-theme' ? 'true' : 'false'"
            aria-haspopup="menu"
            data-testid="shelfview-reader-text-theme-toggle"
            @click="toggleControlMenu('text-theme', $event)"
          >
            <v-icon small>{{ textThemeIcon }}</v-icon>
            <span class="shelfview-reader-shell__setting-row-label">{{ $t('shelfview_reader.control_text_theme') }}</span>
            <span class="shelfview-reader-shell__setting-row-value">{{ textThemeLabel }}</span>
            <v-icon small>mdi-chevron-down</v-icon>
          </button>

          <button
            type="button"
            class="shelfview-reader-shell__setting-row"
            data-testid="shelfview-reader-text-settings-toggle"
            @click="openTextSettings"
          >
            <v-icon small>mdi-format-size</v-icon>
            <span class="shelfview-reader-shell__setting-row-label">{{ $t('shelfview_reader.text_settings') }}</span>
            <v-icon small>mdi-chevron-right</v-icon>
          </button>

          <button
            v-if="textToSpeechAvailable"
            type="button"
            class="shelfview-reader-shell__setting-row"
            :aria-label="textToSpeechLabel"
            :aria-pressed="textToSpeechActive ? 'true' : 'false'"
            data-testid="shelfview-reader-tts-toggle"
            @click="toggleTextToSpeech"
          >
            <v-icon small>{{ textToSpeechActive ? 'mdi-volume-off' : 'mdi-volume-high' }}</v-icon>
            <span class="shelfview-reader-shell__setting-row-label">{{ $t('shelfview_reader.read_aloud') }}</span>
            <span class="shelfview-reader-shell__setting-row-state">{{ textToSpeechStateLabel }}</span>
          </button>

          <button
            v-if="textToSpeechActive && textToSpeechPauseAvailable"
            type="button"
            class="shelfview-reader-shell__setting-row"
            :aria-pressed="textToSpeechPaused ? 'true' : 'false'"
            data-testid="shelfview-reader-tts-pause"
            @click="toggleTextToSpeechPause"
          >
            <v-icon small>{{ textToSpeechPaused ? 'mdi-play-circle-outline' : 'mdi-pause-circle-outline' }}</v-icon>
            <span class="shelfview-reader-shell__setting-row-label">
              {{ textToSpeechPaused ? $t('shelfview_reader.resume_read_aloud') : $t('shelfview_reader.pause_read_aloud') }}
            </span>
          </button>

          <button
            type="button"
            class="shelfview-reader-shell__setting-row"
            :disabled="!textHighlightAvailable"
            data-testid="shelfview-reader-highlight-add"
            @click="addTextHighlightForCurrentSelection"
          >
            <v-icon small>{{ currentTextParagraphHighlighted ? 'mdi-marker-check' : 'mdi-format-color-highlight' }}</v-icon>
            <span class="shelfview-reader-shell__setting-row-label">{{ textHighlightLabel }}</span>
          </button>

          <button
            type="button"
            class="shelfview-reader-shell__setting-row"
            data-testid="shelfview-reader-highlights-toggle"
            @click="highlightsDrawerOpen = true"
          >
            <v-icon small>mdi-format-color-highlight</v-icon>
            <span class="shelfview-reader-shell__setting-row-label">{{ $t('shelfview_reader.highlights') }}</span>
            <span class="shelfview-reader-shell__setting-row-value">{{ highlights.length }}</span>
            <v-icon small>mdi-chevron-right</v-icon>
          </button>

          <button
            type="button"
            class="shelfview-reader-shell__setting-row"
            data-testid="shelfview-reader-dictionary-toggle"
            @click="openDictionaryLookup"
          >
            <v-icon small>mdi-book-search-outline</v-icon>
            <span class="shelfview-reader-shell__setting-row-label">{{ $t('shelfview_reader.dictionary_lookup') }}</span>
            <v-icon small>mdi-chevron-right</v-icon>
          </button>
        </section>

        <section class="shelfview-reader-shell__setting-group">
          <h3 class="shelfview-reader-shell__setting-group-title">{{ $t('shelfview_reader.group_tools') }}</h3>

          <button
            v-if="contentsAvailable"
            type="button"
            class="shelfview-reader-shell__setting-row"
            data-testid="shelfview-reader-contents-toggle"
            @click="contentsDrawerOpen = true"
          >
            <v-icon small>mdi-format-list-bulleted</v-icon>
            <span class="shelfview-reader-shell__setting-row-label">{{ $t('shelfview_reader.contents') }}</span>
            <v-icon small>mdi-chevron-right</v-icon>
          </button>

          <button
            type="button"
            class="shelfview-reader-shell__setting-row"
            data-testid="shelfview-reader-bookmarks-toggle"
            @click="bookmarksDrawerOpen = true"
          >
            <v-icon small>mdi-bookmark-multiple-outline</v-icon>
            <span class="shelfview-reader-shell__setting-row-label">{{ $t('shelfview_reader.bookmarks') }}</span>
            <v-icon small>mdi-chevron-right</v-icon>
          </button>

          <button
            type="button"
            class="shelfview-reader-shell__setting-row"
            data-testid="shelfview-reader-bookmark-add"
            @click="addBookmarkForCurrentPage"
          >
            <v-icon small>mdi-bookmark-plus-outline</v-icon>
            <span class="shelfview-reader-shell__setting-row-label">{{ $t('shelfview_reader.add_bookmark') }}</span>
          </button>

          <button
            v-if="textSearchAvailable"
            type="button"
            class="shelfview-reader-shell__setting-row"
            :aria-pressed="textSearchOpen ? 'true' : 'false'"
            data-testid="shelfview-reader-search-toggle"
            @click="toggleTextSearch"
          >
            <v-icon small>mdi-magnify</v-icon>
            <span class="shelfview-reader-shell__setting-row-label">{{ $t('shelfview_reader.search') }}</span>
            <span class="shelfview-reader-shell__setting-row-state">{{ textSearchOpen ? $t('shelfview_reader.state_on') : $t('shelfview_reader.state_off') }}</span>
          </button>

          <button
            type="button"
            class="shelfview-reader-shell__setting-row"
            data-testid="shelfview-reader-shortcuts-toggle"
            @click="showShortcutHelp = true"
          >
            <v-icon small>mdi-keyboard-outline</v-icon>
            <span class="shelfview-reader-shell__setting-row-label">{{ $t('shelfview_reader.shortcuts') }}</span>
            <v-icon small>mdi-chevron-right</v-icon>
          </button>
        </section>
      </div>
    </section>

    <ShelfViewControlMenu
      v-if="openControlMenu"
      :menu="openControlMenu"
      :title="controlMenuTitle"
      :left="controlMenuLeft"
      :top="controlMenuTop"
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
        @keyup.enter="applyTextSearchQueryAndGoToNext"
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
      {{ $t('shelfview_reader.shortcut_hint') }}
    </p>

    <p
      class="shelfview-reader-shell__sr-only"
      aria-live="polite"
      data-testid="shelfview-reader-live-status"
    >
      {{ readerLiveStatus }}
    </p>

    <main
      ref="readerStage"
      class="shelfview-reader-shell__stage"
      :class="[stageClasses, textStageThemeClass]"
      :style="readerStageStyle"
      tabindex="0"
      :role="readerStageRole"
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
        class="shelfview-reader-shell__action shelfview-reader-shell__action--accent"
        data-testid="shelfview-reader-loading"
      />

      <section
        v-else-if="hasDisplayError"
        class="shelfview-reader-shell__status"
        data-testid="shelfview-reader-error"
      >
        <v-icon
          size="36"
          class="shelfview-reader-shell__icon--danger"
        >
          mdi-alert-circle-outline
        </v-icon>
        <h2>{{ displayErrorTitle }}</h2>
        <p>{{ displayErrorMessage }}</p>
        <div class="shelfview-reader-shell__status-actions">
          <!-- Retrying an expired session only repeats the same failure, so the
               one action offered there is the one that can actually fix it. -->
          <v-btn
            v-if="signInRecoveryVisible"
            class="shelfview-reader-shell__action shelfview-reader-shell__action--accent"
            data-testid="shelfview-reader-sign-in"
            @click="goToSignIn"
          >
            {{ $t('shelfview_reader.sign_in_again') }}
          </v-btn>
          <v-btn
            v-else
            class="shelfview-reader-shell__action shelfview-reader-shell__action--accent"
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
        v-else-if="epubBridgeVisible"
        class="shelfview-reader-shell__status"
        data-testid="shelfview-reader-epub-bridge"
      >
        <v-icon
          size="36"
          class="shelfview-reader-shell__icon--muted"
        >
          mdi-book-open-page-variant-outline
        </v-icon>
        <h2>{{ $t('shelfview_reader.epub_bridge_title') }}</h2>
        <p>{{ $t('shelfview_reader.epub_bridge_message') }}</p>
        <div class="shelfview-reader-shell__status-actions">
          <v-btn
            class="shelfview-reader-shell__action shelfview-reader-shell__action--accent"
            data-testid="shelfview-reader-epub-open"
            @click="openEpubFallbackReader"
          >
            <v-icon left>mdi-book-open-page-variant-outline</v-icon>
            {{ $t('shelfview_reader.epub_bridge_open') }}
          </v-btn>
        </div>
      </section>

      <section
        v-else-if="empty"
        class="shelfview-reader-shell__status"
        data-testid="shelfview-reader-empty"
      >
        <v-icon
          size="36"
          class="shelfview-reader-shell__icon--muted"
        >
          mdi-book-open-page-variant-outline
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
          <div
            v-if="pageImageFailedAt(page.pageIndex)"
            :key="`page-failed-${page.pageIndex}`"
            class="shelfview-reader-shell__page-failed"
            data-testid="shelfview-reader-page-failed"
          >
            <v-icon>mdi-image-broken-variant</v-icon>
            <span>{{ $t('shelfview_reader.page_failed_n', {n: page.pageIndex + 1}) }}</span>
            <v-btn
              small
              text
              data-testid="shelfview-reader-page-retry"
              @click="retryPageImage"
            >
              {{ $t('shelfview_reader.try_again') }}
            </v-btn>
          </div>
          <ShelfViewCanvasImagePage
            v-else-if="canvasImageProcessingEnabled"
            :key="`canvas-page-${page.pageIndex}-${pageImageReloadNonce}`"
            :src="page.url"
            :alt="`${bookTitle} - ${$t('shelfview_reader.page_n', {n: page.pageIndex + 1})}`"
            :page-index="page.pageIndex"
            :reload-nonce="pageImageReloadNonce"
            :page-class="pageImageClass"
            :page-style="imageFilterStyle"
            :mode="canvasImageProcessingMode"
            @error="onPageImageError(page.pageIndex)"
          />
          <img
            v-else
            :key="`page-${page.pageIndex}-${pageImageReloadNonce}`"
            class="shelfview-reader-shell__page shelfview-page-content"
            :class="[pageImageClass, {'shelfview-reader-shell__page--pending': !pageImageLoadedAt(page.pageIndex)}]"
            :style="imageFilterStyle"
            :src="page.url"
            :alt="`${bookTitle} - ${$t('shelfview_reader.page_n', {n: page.pageIndex + 1})}`"
            draggable="false"
            :loading="pageImageLoading"
            decoding="async"
            :data-page-index="page.pageIndex"
            :data-page-reload="pageImageReloadNonce"
            data-testid="shelfview-reader-page"
            @load="onPageImageLoaded(page.pageIndex, $event)"
            @error="onPageImageError(page.pageIndex)"
            @dragstart.prevent
          />
        </template>
      </div>

      <article
        v-else-if="readerTextVisible"
        class="shelfview-reader-shell__text"
        :class="[textThemeClass, {'shelfview-reader-shell__text--unwrapped': textUnwrapLines}]"
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

    <!-- Highlighting, looking a word up and reading a sentence aloud all read the
         live selection, but their buttons sat two panels away and the selection
         was gone by the time the reader got there. This puts them beside the
         selection, and mousedown.prevent keeps the selection alive. -->
    <div
      v-if="selectionActionsVisible"
      class="shelfview-reader-shell__selection-actions"
      :style="selectionActionsStyle"
      role="toolbar"
      :aria-label="$t('shelfview_reader.selection_actions')"
      data-testid="shelfview-reader-selection-actions"
    >
      <v-btn
        small
        text
        data-testid="shelfview-reader-selection-highlight"
        @mousedown.prevent
        @click="addTextHighlightForCurrentSelection"
      >
        <v-icon small left>mdi-format-color-highlight</v-icon>
        {{ $t('shelfview_reader.highlight_selection') }}
      </v-btn>
      <v-btn
        small
        text
        data-testid="shelfview-reader-selection-dictionary"
        @mousedown.prevent
        @click="openDictionaryLookup"
      >
        <v-icon small left>mdi-book-search-outline</v-icon>
        {{ $t('shelfview_reader.dictionary_lookup') }}
      </v-btn>
      <v-btn
        v-if="textToSpeechAvailable"
        small
        text
        data-testid="shelfview-reader-selection-read-aloud"
        @mousedown.prevent
        @click="startTextToSpeech"
      >
        <v-icon small left>mdi-volume-high</v-icon>
        {{ $t('shelfview_reader.read_aloud') }}
      </v-btn>
    </div>

    <!-- On a phone the toolbar sits out of thumb reach, so paging is mirrored at
         the bottom. It follows the same show/hide rules as the rest of the
         chrome and never covers the page while reading. -->
    <nav
      v-if="pageDockVisible"
      class="shelfview-reader-shell__page-dock"
      :aria-label="$t('shelfview_reader.group_page')"
      data-testid="shelfview-reader-page-dock"
    >
      <v-btn
        icon
        :disabled="!canGoPrevious"
        :aria-label="$t('shelfview_reader.previous_page')"
        data-testid="shelfview-reader-dock-previous"
        @click="previousPage"
      >
        <v-icon>mdi-chevron-left</v-icon>
      </v-btn>

      <span
        class="shelfview-reader-shell__page-counter"
        data-testid="shelfview-reader-dock-counter"
      >
        {{ currentPageLabel }} / {{ pagesCount }}
      </span>

      <!-- The page number changes with the font size on a TXT, so a percentage
           is the only figure a reader can carry between sessions. -->
      <span
        v-if="progressPercentLabel"
        class="shelfview-reader-shell__progress"
        data-testid="shelfview-reader-dock-progress"
      >{{ progressPercentLabel }}</span>

      <v-btn
        icon
        :disabled="!canGoNext"
        :aria-label="$t('shelfview_reader.next_page')"
        data-testid="shelfview-reader-dock-next"
        @click="nextPage"
      >
        <v-icon>mdi-chevron-right</v-icon>
      </v-btn>

      <!-- On a narrow screen the toolbar controls scroll sideways behind a
           hidden scrollbar and the settings button is the one that falls off the
           end, so the dock carries a second way in. -->
      <v-btn
        v-if="pageControlsVisible"
        icon
        :aria-label="$t('shelfview_reader.settings')"
        :aria-expanded="overflowMenuOpen ? 'true' : 'false'"
        aria-haspopup="dialog"
        data-testid="shelfview-reader-dock-settings"
        @click="toggleOverflowMenu"
      >
        <v-icon>mdi-tune-variant</v-icon>
      </v-btn>
    </nav>

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
        :aria-current="thumbnail.pageIndex === currentPageIndex ? 'true' : undefined"
        :data-page-index="thumbnail.pageIndex"
        data-testid="shelfview-reader-thumbnail"
        @click="goToPageIndex(thumbnail.pageIndex)"
      >
        <img
          :src="thumbnail.url"
          :alt="$t('shelfview_reader.page_n', {n: thumbnail.pageIndex + 1})"
          draggable="false"
          loading="lazy"
          decoding="async"
          data-testid="shelfview-reader-thumbnail-image"
        />
      </button>
    </section>

    <transition name="shelfview-panel">
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
    </transition>

    <transition name="shelfview-panel">
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
            v-if="dictionaryLocalEntries.length > 0 || dictionaryServerProviders.length > 0"
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
    </transition>

    <transition name="shelfview-panel">
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
          class="shelfview-reader-shell__action shelfview-reader-shell__action--accent"
          data-testid="shelfview-bookmarks-add"
          @click="addBookmarkForCurrentPage"
        >
          <v-icon left>mdi-bookmark-plus-outline</v-icon>
          {{ $t('shelfview_reader.add_bookmark') }}
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
    </transition>

    <transition name="shelfview-panel">
      <aside
        v-if="highlightsDrawerOpen"
        class="shelfview-reader-shell__bookmarks-drawer"
        role="dialog"
        aria-modal="true"
        aria-labelledby="shelfview-highlights-title"
        data-testid="shelfview-highlights-drawer"
      >
        <header class="shelfview-reader-shell__drawer-header">
          <h2 id="shelfview-highlights-title">{{ $t('shelfview_reader.highlights') }}</h2>
          <v-btn
            icon
            :aria-label="$t('shelfview_reader.close')"
            data-testid="shelfview-highlights-close"
            @click="closeHighlightsDrawer"
          >
            <v-icon>mdi-close</v-icon>
          </v-btn>
        </header>

        <p
          v-if="highlights.length === 0"
          class="shelfview-reader-shell__bookmarks-empty"
          data-testid="shelfview-highlights-empty"
        >
          {{ $t('shelfview_reader.no_highlights') }}
        </p>

        <ul
          v-else
          class="shelfview-reader-shell__bookmarks-list"
        >
          <li
            v-for="highlight in highlights"
            :key="highlight.id"
            class="shelfview-reader-shell__bookmark-row"
          >
            <div class="shelfview-reader-shell__bookmark-main">
              <button
                type="button"
                class="shelfview-reader-shell__bookmark-link"
                data-testid="shelfview-highlight-go"
                @click="goToHighlight(highlight)"
              >
                {{ highlightLabel(highlight) }}
              </button>

              <div class="shelfview-reader-shell__highlight-colors">
                <button
                  v-for="color in highlightColorOptions"
                  :key="color"
                  type="button"
                  class="shelfview-reader-shell__highlight-color"
                  :class="[
                    `shelfview-reader-shell__highlight-color--${color.toLowerCase()}`,
                    {'shelfview-reader-shell__highlight-color--selected': highlight.color === color},
                  ]"
                  :aria-label="$t(`shelfview_reader.highlight_color_${color.toLowerCase()}`)"
                  :aria-pressed="highlight.color === color ? 'true' : 'false'"
                  data-testid="shelfview-highlight-color"
                  @click="setHighlightColor(highlight, color)"
                />
              </div>

              <p
                v-if="highlight.note && editingHighlightId !== highlight.id"
                class="shelfview-reader-shell__bookmark-note"
                data-testid="shelfview-highlight-note"
              >
                {{ highlight.note }}
              </p>
              <form
                v-if="editingHighlightId === highlight.id"
                class="shelfview-reader-shell__bookmark-note-editor"
                data-testid="shelfview-highlight-note-editor"
                @submit.prevent="saveHighlightNote(highlight)"
              >
                <v-text-field
                  v-model="highlightNoteDraft"
                  dense
                  outlined
                  hide-details
                  :label="$t('shelfview_reader.bookmark_note')"
                  data-testid="shelfview-highlight-note-input"
                />
                <div class="shelfview-reader-shell__bookmark-note-actions">
                  <v-btn
                    icon
                    small
                    type="button"
                    :aria-label="$t('shelfview_reader.save_bookmark_note')"
                    data-testid="shelfview-highlight-note-save"
                    @click="saveHighlightNote(highlight)"
                  >
                    <v-icon small>mdi-check</v-icon>
                  </v-btn>
                  <v-btn
                    icon
                    small
                    type="button"
                    :aria-label="$t('shelfview_reader.cancel_bookmark_note')"
                    data-testid="shelfview-highlight-note-cancel"
                    @click="cancelHighlightNoteEdit"
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
              data-testid="shelfview-highlight-note-edit"
              @click="startHighlightNoteEdit(highlight)"
            >
              <v-icon small>mdi-note-edit-outline</v-icon>
            </v-btn>
            <v-btn
              icon
              small
              :aria-label="$t('shelfview_reader.delete_highlight')"
              data-testid="shelfview-highlight-delete"
              @click="deleteHighlight(highlight)"
            >
              <v-icon small>mdi-delete-outline</v-icon>
            </v-btn>
          </li>
        </ul>
      </aside>
    </transition>

    <transition name="shelfview-panel">
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

          <v-select
            v-model="imageFilterPreset"
            :items="allImageFilterPresetOptions"
            item-text="text"
            item-value="value"
            dense
            outlined
            hide-details
            :label="$t('shelfview_reader.control_filter')"
            data-testid="shelfview-reader-fixed-image-processing"
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
            class="shelfview-reader-shell__action"
            data-testid="shelfview-reader-fixed-settings-reset"
            @click="resetFixedPageDisplaySettings"
          >
            {{ $t('shelfview_reader.reset') }}
          </v-btn>
        </div>
      </aside>
    </transition>

    <transition name="shelfview-panel">
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
                      max="2560"
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
                      max="240"
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
                    <label for="shelfview-text-unwrap">
                      {{ $t('shelfview_reader.unwrap_lines') }}
                      <span>{{ textUnwrapLines ? $t('shelfview_reader.state_on') : $t('shelfview_reader.state_off') }}</span>
                    </label>
                    <!-- Old Korean TXT files are hard-wrapped at a fixed column,
                         which stops them reflowing with the font size. -->
                    <v-switch
                      id="shelfview-text-unwrap"
                      v-model="textUnwrapLines"
                      dense
                      hide-details
                      :label="$t('shelfview_reader.unwrap_lines_hint')"
                      data-testid="shelfview-reader-text-unwrap"
                      @change="onTextDisplaySettingChanged"
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
                      min="0.5"
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
            class="shelfview-reader-shell__action"
            data-testid="shelfview-reader-text-settings-reset"
            @click="resetTextDisplaySettings"
          >
            {{ $t('shelfview_reader.reset') }}
          </v-btn>
        </div>
      </aside>
    </transition>

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
          class="shelfview-reader-shell__action shelfview-reader-shell__action--accent"
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
            class="shelfview-reader-shell__action shelfview-reader-shell__action--accent"
            data-testid="shelfview-reader-end-next"
            @click="goToNextBook"
          >
            {{ $t('shelfview_reader.read_next_book') }}
          </v-btn>
          <v-btn
            class="shelfview-reader-shell__action"
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
import {EpubWebPubAdapter} from '@/shelfview/adapters/epub-webpub-adapter'
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
  SHELFVIEW_IMAGE_FILTER_PRESETS,
  ShelfViewImageFilterPreset,
  shelfViewImageFilterPreset,
  shelfViewImageFilterProcessingMode,
  shelfViewImageFilterStyle,
  shelfViewImageFilterUsesCanvas,
} from '@/shelfview/functions/image-filter-presets'
import {
  ShelfViewReaderInputAction,
  ShelfViewTouchPoint,
  ShelfViewTouchStageState,
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
  ShelfViewDictionaryProviderDto,
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
import {shelfViewEstimateCharsPerPage} from '@/shelfview/functions/text-pagination'

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
const SHELFVIEW_GLOBAL_SCOPE_ID = 'GLOBAL'
const POSITION_SAVE_DEBOUNCE_MS = 2000
const TEXT_SEARCH_DEBOUNCE_MS = 250
const PREFERENCE_SAVE_DEBOUNCE_MS = 600
const SLIDESHOW_INTERVAL_MS = 5000
const FIXED_PAGE_WHEEL_SCALE_STEP = 10
const TEXT_CONTENTS_LABEL_MAX_LENGTH = 80
const TEXT_CONTENTS_HEADING_MAX_LENGTH = 96
const TEXT_HIGHLIGHT_QUOTE_MAX_LENGTH = 500
const TEXT_TO_SPEECH_RATE_DEFAULT = 1
const TEXT_TO_SPEECH_RATE_MIN = 0.7
const TEXT_TO_SPEECH_RATE_MAX = 1.4
const DICTIONARY_PACK_STORAGE_KEY = 'shelfview.dictionaryPack.v1'

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
    epubBridge: undefined as EpubWebPubAdapter | undefined,
    currentLocator: undefined as ShelfViewLocator | undefined,
    siblingNext: undefined as BookDto | undefined,
    context: {} as Context,
    error: undefined as Error | ShelfViewApiError | undefined,
    loading: true,
    failedPageIndexes: [] as number[],
    loadedPageKeys: [] as string[],
    widePageIndexes: [] as number[],
    mixedContentIndex: 0,
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
    textUnwrapLines: SHELFVIEW_TEXT_DISPLAY_DEFAULTS.textUnwrapLines,
    fixedPageSettingsOpen: false,
    textSettingsOpen: false,
    canReturnToPreviousScreen: false,
    openControlMenu: null as string | null,
    controlMenuLeft: 0,
    controlMenuTop: 50,
    // The toolbar is the reader's only way in to every control, so it stays up
    // until the reader deliberately hides it.
    chromeVisible: true,
    overflowMenuOpen: false,
    thumbnailStripOpen: false,
    textSearchOpen: false,
    textSearchQuery: '',
    textSearchQueryApplied: '',
    textSearchTimer: undefined as number | undefined,
    textSearchMatchIndex: 0,
    textToSpeechActive: false,
    textToSpeechRate: TEXT_TO_SPEECH_RATE_DEFAULT,
    textToSpeechVoiceURI: '',
    textToSpeechVoices: [] as SpeechSynthesisVoice[],
    textToSpeechUtterance: undefined as SpeechSynthesisUtterance | undefined,
    textToSpeechParagraphIndex: undefined as number | undefined,
    textToSpeechPaused: false,
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
    highlightsDrawerOpen: false,
    editingHighlightId: '',
    highlightNoteDraft: '',
    contentsDrawerOpen: false,
    dictionaryDrawerOpen: false,
    dictionaryQuery: '',
    dictionaryLocalEntries: [] as ShelfViewDictionaryLocalEntry[],
    dictionaryServerProviders: [] as ShelfViewDictionaryProviderDto[],
    dictionaryServerResults: [] as ShelfViewDictionaryLookupResult[],
    dictionaryLookupRequestId: 0,
    editingBookmarkId: null as string | null,
    bookmarkNoteDraft: '',
    readerStateStatus: 'IDLE' as ShelfViewReaderStateStatus,
    readerStateMessage: '',
    positionSaveTimer: undefined as number | undefined,
    preferenceSaveTimer: undefined as number | undefined,
    globalPageFitPending: false,
    touchStart: undefined as ShelfViewTouchPoint | undefined,
    touchStartScrollLeft: undefined as number | undefined,
    selectionRect: undefined as {top: number, left: number} | undefined,
    appliedCharsPerPage: undefined as number | undefined,
    scrollFrameHandle: undefined as number | undefined,
    viewportFrameHandle: undefined as number | undefined,
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
    document.addEventListener('selectionchange', this.refreshSelectionActions)
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
    this.cancelReaderFrames()
    this.stopSlideshow()
    window.removeEventListener('online', this.onReaderOnline)
    window.removeEventListener('offline', this.onReaderOffline)
    window.removeEventListener('resize', this.onReaderViewportChanged)
    window.removeEventListener('orientationchange', this.onReaderViewportChanged)
    window.removeEventListener('pagehide', this.onPageHide)
    window.removeEventListener('keydown', this.onReaderKeydown)
    document.removeEventListener('visibilitychange', this.onVisibilityChange)
    document.removeEventListener('selectionchange', this.refreshSelectionActions)
    if (screenfull.isEnabled) {
      screenfull.off('change', this.onFullscreenChange)
      if (screenfull.isFullscreen) void screenfull.exit()
    }
    if (this.browserTextToSpeechVoiceListAvailable()) {
      window.speechSynthesis.removeEventListener('voiceschanged', this.refreshTextToSpeechVoices)
    }
    this.destroyAdapter()
  },
  // Remember whether the reader was opened from somewhere inside the app, so
  // leaving it can return to that exact screen instead of always landing on the
  // book detail page.
  beforeRouteEnter(to, from, next) {
    const cameFromInsideTheApp = !!from.name && from.name !== to.name
    next((vm: any) => {
      vm.canReturnToPreviousScreen = cameFromInsideTheApp
    })
  },
  async beforeRouteUpdate(to, from, next) {
    if (to.params.bookId !== from.params.bookId) {
      // Moving to another book inside the reader makes the previous history
      // entry another book, so going back is no longer the way out.
      this.canReturnToPreviousScreen = false
      this.textEncodingOverride = undefined
      await this.flushReaderState()
      await this.setup(to.params.bookId)
    }
    next()
  },
  watch: {
    dictionaryQuery() {
      void this.refreshDictionaryServerLookup()
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
        this.highlightsDrawerOpen ||
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
        this.readerLayout !== 'CONTINUOUS_SCROLL'
    },
    textSearchAvailable(): boolean {
      return this.textControlsVisible && !!this.manifest?.capabilities.search
    },
    textToSpeechAvailable(): boolean {
      return this.textControlsVisible && this.currentTextToSpeechText.length > 0 && this.browserTextToSpeechAvailable()
    },
    textToSpeechStateLabel(): string {
      if (!this.textToSpeechActive) return this.$t('shelfview_reader.state_off') as string
      return this.textToSpeechPaused
        ? this.$t('shelfview_reader.state_paused') as string
        : this.$t('shelfview_reader.state_on') as string
    },
    textToSpeechPauseAvailable(): boolean {
      return this.browserTextToSpeechPauseAvailable()
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
      return shelfViewDictionaryLookupResults(this.dictionaryQuery, this.dictionaryLocalEntries, this.dictionaryServerResults)
        .map(result => ({
          ...result,
          providerLabel: result.providerLabel ?? this.$t(result.providerLabelKey ?? 'shelfview_reader.dictionary_provider_local') as string,
          label: result.actionLabelKey ? this.$t(result.actionLabelKey) as string : result.term ?? '',
        }))
    },
    contentsAvailable(): boolean {
      if (this.epubBridgeVisible) return false
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
    readerStateFailureVisible(): boolean {
      return (this.readerStateStatus === 'ERROR' || this.readerStateStatus === 'OFFLINE') &&
        this.readerStateMessage.length > 0
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
      if (this.textAdapter) {
        return this.readerLayout === 'SINGLE_PAGE'
          ? this.textAdapter.getPageCount()
          : this.textAdapter.getParagraphsCount()
      }
      if (this.mixedContentAdapter) return this.mixedContentAdapter.getCurrentContentCount()
      return this.manifest?.pageCount || 0
    },
    currentPageIndex(): number {
      if (this.currentLocator?.kind === 'PAGE') return this.currentLocator.page
      if (this.currentLocator?.kind === 'TEXT') {
        return this.textAdapter && this.readerLayout === 'SINGLE_PAGE'
          ? this.textAdapter.getCurrentPageIndex()
          : this.currentLocator.paragraph
      }
      if (this.currentLocator?.kind === 'MIXED') {
        // The mixed adapter reports pages for a paginated text item and the raw
        // paragraph otherwise, so the counter matches what pagesCount counts.
        return this.mixedContentIndex
      }
      return 0
    },
    currentPageNumber(): number {
      return this.currentPageIndex + 1
    },
    highlightColorOptions(): ShelfViewHighlightColor[] {
      // The backend and the paragraph styling have carried all five since
      // SV-930; only the UI to reach them was missing.
      return ['YELLOW', 'GREEN', 'BLUE', 'PINK', 'UNDERLINE']
    },
    selectionActionsVisible(): boolean {
      return this.textControlsVisible && !!this.selectionRect && !this.anyChromePanelOpen
    },
    selectionActionsStyle(): Record<string, string> {
      return {
        top: `${this.selectionRect?.top ?? 0}px`,
        left: `${this.selectionRect?.left ?? 0}px`,
      }
    },
    progressPercentLabel(): string {
      if (!this.adapter || this.pagesCount < 1) return ''

      const progression = this.adapter.getProgression()
      if (!Number.isFinite(progression)) return ''
      return `${Math.round(Math.max(0, Math.min(1, progression)) * 100)}%`
    },
    currentPageLabel(): string {
      const visiblePages = this.visiblePageSources.map(page => page.pageIndex + 1)
      if (visiblePages.length > 1 && this.readerLayout === 'DOUBLE_PAGE') {
        return `${Math.min(...visiblePages)}-${Math.max(...visiblePages)}`
      }
      return this.currentPageNumber.toString()
    },
    /**
     * `application` hands every key to the reader, which is right for a page of
     * images but wrong for prose: it switches a screen reader out of browse mode
     * and the shell only handles nine keys, so the book text becomes unreachable.
     * Text keeps the plain `main` semantics and still pages from the keyboard,
     * because the handler is on `window` rather than on this element.
     */
    readerStageRole(): string | undefined {
      return this.textControlsVisible ? undefined : 'application'
    },
    readerAriaLabel(): string {
      return this.$t('shelfview_reader.reader_aria_label', {
        title: this.bookTitle,
        unit: this.currentLocationUnit,
        position: this.currentPageLabel,
        total: this.pagesCount,
      }) as string
    },
    readerLiveStatus(): string {
      return this.$t('shelfview_reader.reader_live_status', {
        unit: this.currentLocationUnit,
        position: this.currentPageLabel,
        total: this.pagesCount,
      }) as string
    },
    currentLocationUnit(): string {
      return this.textControlsVisible
        ? this.$t('shelfview_reader.unit_paragraph') as string
        : this.$t('shelfview_reader.unit_page') as string
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
        ...(this.contentsAvailable ? [{keys: 'T', label: this.$t('shelfview_reader.shortcut_contents') as string}] : []),
        {keys: 'B', label: this.$t('shelfview_reader.shortcut_bookmarks') as string},
        {keys: 'S', label: this.$t('shelfview_reader.shortcut_settings') as string},
        ...(this.fixedPageControlsVisible ? [
          {keys: 'G', label: this.$t('shelfview_reader.shortcut_page_jump') as string},
          {keys: this.$t('shelfview_reader.shortcut_keys_zoom') as string, label: this.$t('shelfview_reader.shortcut_zoom') as string},
        ] : []),
        // A bluetooth page turner sends these keys, so it already works; saying
        // so saves the reader from having to find out by trial.
        {keys: this.$t('shelfview_reader.shortcut_keys_remote') as string, label: this.$t('shelfview_reader.shortcut_remote') as string},
      ]
    },
    canGoPrevious(): boolean {
      if (this.mixedContentAdapter) return this.mixedContentAdapter.canGoPrevious()
      if (this.textAdapter) {
        return this.readerLayout === 'SINGLE_PAGE'
          ? this.textAdapter.canGoPreviousPage()
          : this.textAdapter.canGoPrevious()
      }
      return !!this.adapter && shelfViewCanGoPrevious(this.currentPageIndex, this.pagesCount, this.readerLayout, this.fixedPageCoverSingle)
    },
    canGoNext(): boolean {
      if (this.mixedContentAdapter) return this.mixedContentAdapter.canGoNext()
      if (this.textAdapter) {
        return this.readerLayout === 'SINGLE_PAGE'
          ? this.textAdapter.canGoNextPage()
          : this.textAdapter.canGoNext()
      }
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
        new Set(this.widePageIndexes),
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
    pageDockVisible(): boolean {
      return this.chromeVisible && this.pageControlsVisible && this.$vuetify.breakpoint.smAndDown
    },
    // The bottom chrome hides along with the toolbar, but the space it occupies
    // stays reserved either way, so toggling the toolbar cannot resize the page.
    thumbnailStripReserved(): boolean {
      return this.thumbnailStripOpen && this.thumbnailStripAvailable
    },
    pageDockReserved(): boolean {
      return this.pageControlsVisible && this.$vuetify.breakpoint.smAndDown
    },
    visibleTextParagraphs(): ShelfViewTextParagraph[] {
      return this.textAdapter?.getVisibleParagraphs(this.readerLayout) ?? this.mixedContentAdapter?.getVisibleParagraphs(this.readerLayout) ?? []
    },
    textSearchMatches(): ShelfViewTextSearchMatch[] {
      const query = this.textSearchQueryApplied.trim().toLocaleLowerCase()
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
      if (!this.textSearchQueryApplied.trim()) return this.$t('shelfview_reader.search_empty') as string
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
        'shelfview-reader-shell__pages--rotated-quarter': this.fixedPageRotation === 90 || this.fixedPageRotation === 270,
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
    // Continuous scroll renders every page at once, so let the browser defer the
    // off-screen requests. Single and double page always sit in the viewport and
    // must not wait for the lazy-loading check.
    pageImageLoading(): string {
      return this.readerLayout === 'CONTINUOUS_SCROLL' ? 'lazy' : 'eager'
    },
    imageFiltersAvailable(): boolean {
      return !!this.manifest?.capabilities.imageFilters
    },
    canvasImageProcessingEnabled(): boolean {
      return this.imageFiltersAvailable &&
        this.readerLayout !== 'CONTINUOUS_SCROLL' &&
        shelfViewImageFilterUsesCanvas(this.imageFilterPreset)
    },
    canvasImageProcessingMode(): string {
      return shelfViewImageFilterProcessingMode(this.imageFilterPreset) ?? 'SHARPEN'
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
      return shelfViewImageFilterPreset(this.imageFilterPreset).id
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
      // Brightness belongs to the whole reading surface. Applied to the article
      // alone it only darkened the glyphs, which raised contrast on the paper
      // theme instead of lowering it.
      if (this.textControlsVisible) return {'--sv-text-brightness': this.textBrightness.toString()}
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
      const presets = SHELFVIEW_IMAGE_FILTER_PRIMARY_PRESETS.some(option => option.id === this.visibleImageFilterPreset)
        ? SHELFVIEW_IMAGE_FILTER_PRIMARY_PRESETS
        : [
          ...SHELFVIEW_IMAGE_FILTER_PRIMARY_PRESETS,
          ...SHELFVIEW_IMAGE_FILTER_PRESETS.filter(option => option.id === this.visibleImageFilterPreset),
        ]

      return presets.map(option => ({
        value: option.id,
        icon: option.icon,
        label: this.$t(`shelfview_reader.filter_${option.id.toLowerCase()}`) as string,
        active: option.id === this.visibleImageFilterPreset,
      }))
    },
    allImageFilterPresetOptions(): ShelfViewReaderSelectOption[] {
      return SHELFVIEW_IMAGE_FILTER_PRESETS.map(option => ({
        text: this.$t(`shelfview_reader.filter_${option.id.toLowerCase()}`) as string,
        value: option.id,
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
    epubBridgeVisible(): boolean {
      return !!this.epubBridge && this.manifest?.kind === 'EPUB_WEBPUB' && !this.hasDisplayError
    },
    empty(): boolean {
      return !!this.manifest && !this.readerPagesVisible && !this.readerTextVisible && !this.epubBridgeVisible
    },
    emptyMessage(): string {
      if (this.manifest?.kind === 'TEXT_REFLOW') {
        return this.$t('shelfview_reader.empty_text') as string
      }
      return this.$t('shelfview_reader.empty_pages') as string
    },
    /**
     * True only when nothing readable is left on screen. A single broken entry
     * inside an archive now stops at its own page, so a spread keeps its other
     * half and continuous scroll keeps the reader's place instead of throwing
     * the whole stage away.
     */
    pageImageFailed(): boolean {
      const visible = this.visiblePageSources
      return visible.length > 0 && visible.every(page => this.pageImageFailedAt(page.pageIndex))
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
          case 'UNAUTHENTICATED':
            return this.$t('shelfview_reader.error_unauthenticated_title') as string
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
          case 'UNAUTHENTICATED':
            return this.$t('shelfview_reader.error_unauthenticated_message') as string
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
      // Raw server and browser messages are English developer text, so they must
      // not reach the reader (Rule_UIUX 6-1).
      return this.$t('shelfview_reader.error_generic_message') as string
    },
    signInRecoveryVisible(): boolean {
      return this.error instanceof ShelfViewApiError && this.error.code === 'UNAUTHENTICATED'
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
      this.dictionaryServerProviders = []
      this.dictionaryServerResults = []
      this.dictionaryLookupRequestId += 1
      this.loadDictionaryPackFromBrowserStorage()
      void this.loadDictionaryProviders()
      this.textSearchOpen = false
      this.textSearchQuery = ''
      this.textSearchQueryApplied = ''
      this.textSearchMatchIndex = 0
      this.chromeVisible = true
      this.overflowMenuOpen = false
      this.thumbnailStripOpen = false
      this.siblingNext = undefined
      this.endOfBookPromptOpen = false
      this.stopSlideshow()
      this.clearReaderStateTimers()
      this.setReaderStateStatus('IDLE', '')
      this.cancelReaderFrames()
      this.appliedCharsPerPage = undefined
      this.destroyAdapter()
      this.failedPageIndexes = []
      this.widePageIndexes = []

      this.context = {} as Context
      if (this.$route.query.contextId && this.$route.query.context &&
        Object.values(ContextOrigin).includes(this.$route.query.context as ContextOrigin)) {
        this.context = {
          origin: this.$route.query.context as ContextOrigin,
          id: this.$route.query.contextId as string,
        }
      }

      try {
        // Opening a book used to cost four round trips in a row before the
        // first page image was even requested. The manifest is all the adapter
        // needs, and the book only feeds the toolbar title, so the two run
        // together; the series title and the next-book lookup arrive later
        // because `bookTitle` already tolerates them being absent.
        const [book, manifest] = await Promise.all([
          this.$talesBooks.getBook(bookId),
          this.$shelfViewManifest.getManifest(bookId),
        ])
        if (requestId !== this.setupRequestId) return

        book.context = this.context
        this.book = book
        this.manifest = manifest
        void this.loadSeries(book.seriesId, requestId)
        void this.loadSiblingNext(bookId, requestId)

        if (this.manifest.kind === 'EPUB_WEBPUB') {
          await this.openEpubBridgeManifest(this.manifest)
        } else if (this.manifest.kind === 'FIXED_PAGE_SEQUENCE' || this.manifest.kind === 'TEXT_REFLOW' || this.manifest.kind === 'MIXED_CONTENT_SEQUENCE') {
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
    async loadSeries(
      seriesId: string,
      requestId: number,
    ) {
      try {
        const series = await this.$talesSeries.getOneSeries(seriesId)
        if (requestId === this.setupRequestId) this.series = series
      } catch (e) {
        // The title falls back to the book name; this must not block reading.
        if (requestId === this.setupRequestId) this.series = undefined
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
      this.recomputeTextPagination()
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
    async openEpubBridgeManifest(
      manifest: ShelfViewManifest,
    ) {
      const epubBridge = new EpubWebPubAdapter()
      await epubBridge.open(manifest)
      this.epubBridge = epubBridge
      this.currentLocator = undefined
      this.$nextTick(() => this.focusReaderStage())
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
    goToSignIn() {
      void this.$router.push({
        name: 'login',
        query: {redirect: this.$route.fullPath},
      })
    },
    retryPageImage() {
      this.failedPageIndexes = []
      this.pageImageReloadNonce += 1
      this.$nextTick(() => this.focusReaderStage())
    },
    retryTextEncoding(encoding?: string) {
      this.textEncodingOverride = encoding
      this.failedPageIndexes = []
      void this.setup(this.bookId)
    },
    pageImageFailedAt(pageIndex: number): boolean {
      return this.failedPageIndexes.includes(pageIndex)
    },
    pageImageLoadedAt(pageIndex: number): boolean {
      return this.loadedPageKeys.includes(this.pageImageKey(pageIndex))
    },
    /**
     * Records a page that is wider than it is tall, which in a comic means a
     * double-spread illustration. The manifest carries no page sizes, so the
     * only place to learn this is the image once it has decoded.
     */
    notePageAspect(
      pageIndex: number,
      e?: Event,
    ) {
      const image = e?.target as HTMLImageElement | undefined
      const width = image?.naturalWidth ?? 0
      const height = image?.naturalHeight ?? 0
      if (width < 1 || height < 1) return

      const wide = width > height
      const known = this.widePageIndexes.includes(pageIndex)
      if (wide === known) return

      this.widePageIndexes = wide
        ? [...this.widePageIndexes, pageIndex]
        : this.widePageIndexes.filter(index => index !== pageIndex)
    },
    pageImageKey(pageIndex: number): string {
      return `${pageIndex}-${this.pageImageReloadNonce}`
    },
    onPageImageLoaded(
      pageIndex: number,
      e?: Event,
    ) {
      this.notePageAspect(pageIndex, e)

      const key = this.pageImageKey(pageIndex)
      if (this.loadedPageKeys.includes(key)) return

      // Only the pages in flight matter, so the list stays short instead of
      // growing for the length of a thousand-page book.
      this.loadedPageKeys = [...this.loadedPageKeys.slice(-11), key]
    },
    onPageImageError(pageIndex?: number) {
      if (typeof pageIndex !== 'number') return
      if (this.failedPageIndexes.includes(pageIndex)) return

      this.failedPageIndexes = [...this.failedPageIndexes, pageIndex]
    },
    errorLooksEncodingRelated(error: Error | ShelfViewApiError): boolean {
      return error.message.toLowerCase().includes('encoding')
    },
    destroyAdapter() {
      this.stopTextToSpeech()
      this.adapter?.destroy()
      this.epubBridge?.destroy()
      this.adapter = undefined
      this.epubBridge = undefined
      this.currentLocator = undefined
    },
    syncAdapterState() {
      if (!this.adapter) {
        this.currentLocator = undefined
        this.mixedContentIndex = 0
        return
      }

      this.currentLocator = this.adapter.getCurrentLocator()
      // Read once here rather than from a computed: switching mixed items
      // destroys one item adapter before the next one has opened, and a
      // computed can be evaluated inside that gap.
      this.mixedContentIndex = this.mixedContentAdapter?.getCurrentContentIndex() ?? 0
      this.pageJumpInput = this.currentPageNumber.toString()
    },
    recomputeTextPagination() {
      // A TXT reached through a mixed archive is the same reading experience as
      // a standalone TXT, so it gets the same page budget (ADR-0009 revisit
      // trigger). Anything but single-page keeps the budget at zero, which the
      // pagination helper reads as one paragraph per page.
      if (!this.textAdapter && !this.mixedContentAdapter?.isCurrentText()) return

      const stage = this.$refs.readerStage as HTMLElement | undefined
      if (!stage) return

      const charsPerPage = this.readerLayout === 'SINGLE_PAGE'
        ? this.estimatedCharsPerPage(stage)
        : 0
      if (charsPerPage === undefined) return
      // Rebuilding the page table walks every paragraph, and a resize burst
      // recomputes the same budget over and over, so an unchanged budget is
      // dropped here rather than inside the adapters.
      if (charsPerPage === this.appliedCharsPerPage) return

      this.appliedCharsPerPage = charsPerPage
      this.textAdapter?.setPagination(charsPerPage)
      this.mixedContentAdapter?.setPagination(charsPerPage)
    },
    estimatedCharsPerPage(stage: HTMLElement): number | undefined {
      const stageWidth = stage.clientWidth
      const stageHeight = stage.clientHeight - this.readerStageVerticalPadding(stage)
      if (stageWidth <= 0 || stageHeight <= 0) return undefined

      return shelfViewEstimateCharsPerPage({
        columnWidth: Math.min(stageWidth, this.textContentWidth),
        viewportHeight: stageHeight,
        fontSize: this.textFontSize,
        lineHeight: this.textLineHeight,
        marginX: this.textPageMargin,
        marginY: Math.max(18, Math.round(this.textPageMargin * 0.75)),
      })
    },
    // clientHeight counts the stage padding, and the toolbar reservation makes
    // that padding big enough to matter, so the page budget is measured against
    // the height the text can actually use rather than the whole stage.
    readerStageVerticalPadding(stage: HTMLElement): number {
      const styles = window.getComputedStyle?.(stage)
      if (!styles) return 0

      const top = Number.parseFloat(styles.paddingTop)
      const bottom = Number.parseFloat(styles.paddingBottom)
      return (Number.isFinite(top) ? top : 0) + (Number.isFinite(bottom) ? bottom : 0)
    },
    async previousPage() {
      if (!this.adapter) return

      this.failedPageIndexes = []
      if (this.mixedContentAdapter) {
        await this.mixedContentAdapter.previous()
        this.normalizeReaderLayout()
        this.syncAdapterState()
        this.$nextTick(() => this.restoreCurrentTextScrollPosition())
        this.schedulePositionSave()
        return
      }

      if (this.textAdapter) {
        if (this.readerLayout === 'SINGLE_PAGE') {
          await this.textAdapter.previousPage()
          this.syncAdapterState()
          this.$nextTick(() => this.restoreCurrentTextScrollPosition())
        } else {
          await this.textAdapter.previous()
          this.syncAdapterState()
        }
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

      this.failedPageIndexes = []
      if (this.mixedContentAdapter) {
        await this.mixedContentAdapter.next()
        this.normalizeReaderLayout()
        this.syncAdapterState()
        this.$nextTick(() => this.restoreCurrentTextScrollPosition())
        this.schedulePositionSave()
        return
      }

      if (this.textAdapter) {
        if (this.readerLayout === 'SINGLE_PAGE') {
          await this.textAdapter.nextPage()
          this.syncAdapterState()
          this.$nextTick(() => this.restoreCurrentTextScrollPosition())
        } else {
          await this.textAdapter.next()
          this.syncAdapterState()
        }
        this.schedulePositionSave()
        return
      }

      await this.goToPageIndex(shelfViewNextPageIndex(this.currentPageIndex, this.pagesCount, this.readerLayout, this.fixedPageCoverSingle))
    },
    async goToPageIndex(pageIndex: number) {
      if (!this.adapter) return

      this.failedPageIndexes = []
      if (this.mixedContentAdapter) {
        await this.mixedContentAdapter.goToCurrentItemContentIndex(pageIndex)
        this.syncAdapterState()
        this.schedulePositionSave()
        return
      }

      if (this.textAdapter && this.readerLayout === 'SINGLE_PAGE') {
        await this.textAdapter.goToPage(pageIndex)
        this.syncAdapterState()
        this.$nextTick(() => this.restoreCurrentTextScrollPosition())
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
      // mdi-format-textdirection-* was dropped from Material Design Icons and
      // rendered as an empty box in the bundled 7.x font.
      return direction === 'RTL' ? 'mdi-format-pilcrow-arrow-left' : 'mdi-format-pilcrow-arrow-right'
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
      const menuHeight = 280
      const left = rect ? Math.min(rect.left, window.innerWidth - menuWidth - 8) : 8
      // Rows now sit inside the settings panel, so the menu opens next to the
      // row that was clicked instead of always hanging below the toolbar.
      const top = rect ? Math.min(rect.bottom + 6, window.innerHeight - menuHeight - 8) : 50
      this.controlMenuLeft = Math.max(8, left)
      this.controlMenuTop = Math.max(50, top)
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
      this.recomputeTextPagination()
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
      if (!SHELFVIEW_FIXED_PAGE_FITS.includes(fit)) return

      this.pageFit = fit
      this.globalPageFitPending = true
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
      this.recomputeTextPagination()
      this.syncAdapterState()
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
          this.imageFilterPreset = shelfViewImageFilterPreset(settings.imageFilterPreset).id
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
      if (typeof settings.textUnwrapLines === 'boolean') this.textUnwrapLines = settings.textUnwrapLines
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
        textUnwrapLines: this.textUnwrapLines,
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
      this.textUnwrapLines = settings.textUnwrapLines
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
          textUnwrapLines: this.textUnwrapLines,
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
    /**
     * Saves the position from `pagehide` or a hide `visibilitychange`, where the
     * ordinary XHR would be cancelled with the page. Nothing is awaited and no
     * status is set, because the reader is on its way out.
     */
    flushPositionSaveOnUnload() {
      if (this.positionSaveTimer !== undefined) {
        window.clearTimeout(this.positionSaveTimer)
        this.positionSaveTimer = undefined
      }
      if (!this.adapter || !this.currentLocator || this.browserOffline()) return false

      return this.$shelfViewState.savePositionOnUnload(this.bookId, {
        locator: this.currentLocator,
        progression: this.adapter.getProgression(),
      })
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
        const settings = this.currentPreferenceSettings(format)
        await this.$shelfViewState.savePreference({
          scopeType: 'BOOK',
          scopeId: this.bookId,
          format,
          settings,
        })
        if (format === SHELFVIEW_FIXED_PAGE_FORMAT && this.globalPageFitPending && settings.pageFit) {
          await this.$shelfViewState.savePreference({
            scopeType: 'GLOBAL',
            scopeId: SHELFVIEW_GLOBAL_SCOPE_ID,
            format,
            settings: {pageFit: settings.pageFit},
          })
          this.globalPageFitPending = false
        }
        this.setReaderStateStatus('SAVED', this.$t('shelfview_reader.status_saved') as string)
      } catch (e) {
        this.setReaderStateStatus(this.browserOffline() ? 'OFFLINE' : 'ERROR', this.$t('shelfview_reader.status_save_failed') as string)
      }
    },
    /** Enter searches at once rather than waiting out the debounce. */
    async applyTextSearchQueryAndGoToNext() {
      if (this.textSearchTimer !== undefined) {
        window.clearTimeout(this.textSearchTimer)
        this.textSearchTimer = undefined
        this.applyTextSearchQuery()
        return
      }

      await this.goToNextTextSearchMatch()
    },
    cancelReaderFrames() {
      if (this.textSearchTimer !== undefined) {
        window.clearTimeout(this.textSearchTimer)
        this.textSearchTimer = undefined
      }
      if (typeof window.cancelAnimationFrame === 'function') {
        if (this.scrollFrameHandle !== undefined) window.cancelAnimationFrame(this.scrollFrameHandle)
        if (this.viewportFrameHandle !== undefined) window.cancelAnimationFrame(this.viewportFrameHandle)
      }
      this.scrollFrameHandle = undefined
      this.viewportFrameHandle = undefined
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
    toggleTextToSpeechPause() {
      if (!this.textToSpeechActive || !this.browserTextToSpeechPauseAvailable()) return

      if (this.textToSpeechPaused) {
        window.speechSynthesis.resume()
        this.textToSpeechPaused = false
        return
      }

      window.speechSynthesis.pause()
      this.textToSpeechPaused = true
    },
    startTextToSpeech() {
      if (!this.textToSpeechAvailable) return

      // A deliberate selection is read once and nothing more; without one the
      // reader wants the book read to them, so playback carries on paragraph by
      // paragraph and turns the page at the end of a screenful.
      const selectedText = this.selectedTextToSpeechText()
      if (selectedText) {
        this.stopTextToSpeech()
        this.speakTextToSpeech(selectedText, undefined)
        return
      }

      const locator = this.currentTextLocator()
      if (!locator) return

      this.stopTextToSpeech()
      this.speakTextToSpeechParagraph(locator.paragraph)
    },
    speakTextToSpeechParagraph(paragraphIndex: number): boolean {
      const text = this.visibleTextParagraphs
        .find(paragraph => paragraph.paragraphIndex === paragraphIndex)
        ?.text
        .trim() ?? ''
      if (!text) return false

      this.speakTextToSpeech(text, paragraphIndex)
      return true
    },
    speakTextToSpeech(
      text: string,
      paragraphIndex: number | undefined,
    ) {
      const utterance = new window.SpeechSynthesisUtterance(text)
      utterance.rate = this.textToSpeechRate
      const voice = this.selectedTextToSpeechVoice()
      if (voice) utterance.voice = voice
      utterance.onend = () => {
        if (this.textToSpeechUtterance !== utterance) return
        if (paragraphIndex === undefined) {
          this.clearTextToSpeechState(utterance)
          return
        }
        void this.advanceTextToSpeech(paragraphIndex)
      }
      utterance.onerror = () => this.clearTextToSpeechState(utterance)
      this.textToSpeechUtterance = utterance
      this.textToSpeechParagraphIndex = paragraphIndex
      this.textToSpeechActive = true
      this.textToSpeechPaused = false
      window.speechSynthesis.speak(utterance)
    },
    async advanceTextToSpeech(spokenParagraphIndex: number) {
      const remaining = this.visibleTextParagraphs
        .filter(paragraph => paragraph.paragraphIndex > spokenParagraphIndex)
        .sort((a, b) => a.paragraphIndex - b.paragraphIndex)[0]
      if (remaining && this.speakTextToSpeechParagraph(remaining.paragraphIndex)) return

      if (!this.canGoNext) {
        this.stopTextToSpeech()
        return
      }

      await this.nextPage()
      // The prompt at the end of a book replaces the page, so reading stops
      // rather than talking over a dialog the reader has to answer.
      if (!this.textToSpeechActive || this.endOfBookPromptOpen) {
        this.stopTextToSpeech()
        return
      }

      const first = [...this.visibleTextParagraphs].sort((a, b) => a.paragraphIndex - b.paragraphIndex)[0]
      if (!first || !this.speakTextToSpeechParagraph(first.paragraphIndex)) this.stopTextToSpeech()
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
      this.textToSpeechParagraphIndex = undefined
      this.textToSpeechActive = false
      this.textToSpeechPaused = false
    },
    clearTextToSpeechState(utterance: SpeechSynthesisUtterance) {
      if (this.textToSpeechUtterance !== utterance) return
      this.textToSpeechUtterance = undefined
      this.textToSpeechParagraphIndex = undefined
      this.textToSpeechActive = false
      this.textToSpeechPaused = false
    },
    browserTextToSpeechPauseAvailable(): boolean {
      return this.browserTextToSpeechAvailable() &&
        typeof window.speechSynthesis.pause === 'function' &&
        typeof window.speechSynthesis.resume === 'function'
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
    /**
     * Searching walks every paragraph in the book, and the backend allows a TXT
     * of up to 5MB, so running it on each keystroke stalls typing. The query is
     * applied once the reader pauses instead.
     */
    onTextSearchInput() {
      if (this.textSearchTimer !== undefined) window.clearTimeout(this.textSearchTimer)

      this.textSearchTimer = window.setTimeout(() => {
        this.textSearchTimer = undefined
        this.applyTextSearchQuery()
      }, TEXT_SEARCH_DEBOUNCE_MS)
    },
    applyTextSearchQuery() {
      this.textSearchQueryApplied = this.textSearchQuery
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
        'shelfview-reader-shell__text-paragraph--speaking': this.textToSpeechParagraphIndex === paragraphIndex,
      }
    },
    /**
     * Folds the single newlines inside a paragraph into spaces so a hard-wrapped
     * TXT reflows with the column instead of keeping the line breaks the file was
     * saved with. It is a one-for-one replacement done at render time only:
     * bookmarks and highlights count characters in the original text, so the
     * length must not change and the stored text must not be touched.
     */
    displayParagraphText(text: string): string {
      return this.textUnwrapLines ? text.split('\n').join(' ') : text
    },
    textParagraphSegments(paragraph: ShelfViewTextParagraph): ShelfViewTextParagraphSegment[] {
      const ranges = this.textDedicatedHighlightRangesForParagraph(paragraph.paragraphIndex, paragraph.text.length)
      if (ranges.length === 0) {
        return [{
          key: `${paragraph.paragraphIndex}-plain`,
          text: this.displayParagraphText(paragraph.text),
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
              text: this.displayParagraphText(paragraph.text.slice(cursor, start)),
              highlighted: false,
            })
          }

          segments.push({
            key: `${paragraph.paragraphIndex}-highlight-${rangeIndex}-${start}-${range.end}`,
            text: this.displayParagraphText(paragraph.text.slice(start, range.end)),
            highlighted: true,
            color: range.color,
          })
          cursor = range.end
        })

      if (cursor < paragraph.text.length) {
        segments.push({
          key: `${paragraph.paragraphIndex}-plain-${cursor}-end`,
          text: this.displayParagraphText(paragraph.text.slice(cursor)),
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
    openEpubFallbackReader() {
      this.$router.push({
        name: this.book?.media ? getLegacyBookReadRouteFromMedia(this.book.media) : 'read-epub',
        params: {bookId: this.bookId},
        query: this.nextBookRouteQuery(),
      })
    },
    closeShortcutHelp() {
      this.showShortcutHelp = false
      this.$nextTick(() => this.focusReaderStage())
    },
    /**
     * Closes the innermost open reader panel and reports whether it closed one.
     * The order runs from the panel drawn on top down to the settings panel that
     * opens the rest, so repeated Escapes retrace the way the reader came in.
     */
    closeTopmostChromePanel(): boolean {
      if (this.openControlMenu) {
        this.closeControlMenu()
        return true
      }
      if (this.showShortcutHelp) {
        this.closeShortcutHelp()
        return true
      }
      if (this.dictionaryDrawerOpen) {
        this.closeDictionaryDrawer()
        return true
      }
      if (this.highlightsDrawerOpen) {
        this.closeHighlightsDrawer()
        return true
      }
      if (this.bookmarksDrawerOpen) {
        this.closeBookmarksDrawer()
        return true
      }
      if (this.contentsDrawerOpen) {
        this.closeContentsDrawer()
        return true
      }
      if (this.fixedPageSettingsOpen) {
        this.closeFixedPageSettings()
        return true
      }
      if (this.textSettingsOpen) {
        this.closeTextSettings()
        return true
      }
      if (this.textSearchOpen) {
        this.toggleTextSearch()
        return true
      }
      if (this.overflowMenuOpen) {
        this.closeOverflowMenu()
        this.$nextTick(() => this.focusReaderStage())
        return true
      }
      return false
    },
    toggleChrome() {
      this.chromeVisible = !this.chromeVisible
      if (!this.chromeVisible) this.closeOverflowMenu()
    },
    toggleOverflowMenu() {
      this.overflowMenuOpen = !this.overflowMenuOpen
    },
    closeOverflowMenu() {
      this.overflowMenuOpen = false
    },
    toggleThumbnailStrip() {
      this.thumbnailStripOpen = !this.thumbnailStripOpen
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
      try {
        await this.$shelfViewState.importDictionaryLocalPack(pack)
        await this.loadDictionaryProviders()
        await this.refreshDictionaryServerLookup()
      } catch (e) {
        // Browser-local import remains the fallback when account sync is unavailable.
      }
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
    async clearDictionaryPack() {
      const storage = this.browserStorage()
      storage?.removeItem(DICTIONARY_PACK_STORAGE_KEY)
      this.dictionaryLocalEntries = []
      const providers = [...this.dictionaryServerProviders]
      this.dictionaryServerProviders = []
      this.dictionaryServerResults = []
      try {
        await Promise.all(providers.map(provider => this.$shelfViewState.deleteDictionaryProvider(provider.id)))
      } catch (e) {
        this.dictionaryServerProviders = providers
        await this.refreshDictionaryServerLookup()
        this.setReaderStateStatus('ERROR', this.$t('shelfview_reader.status_dictionary_pack_failed') as string)
        return
      }
      this.setReaderStateStatus('SAVED', this.$t('shelfview_reader.status_dictionary_pack_cleared') as string)
    },
    async loadDictionaryProviders() {
      try {
        this.dictionaryServerProviders = await this.$shelfViewState.getDictionaryProviders()
      } catch (e) {
        this.dictionaryServerProviders = []
      }
    },
    async refreshDictionaryServerLookup() {
      const requestId = this.dictionaryLookupRequestId + 1
      this.dictionaryLookupRequestId = requestId
      const query = this.normalizedDictionaryQuery(this.dictionaryQuery)
      if (!query) {
        this.dictionaryServerResults = []
        return
      }

      try {
        const results = await this.$shelfViewState.lookupDictionary(query)
        if (requestId === this.dictionaryLookupRequestId) {
          this.dictionaryServerResults = results
        }
      } catch (e) {
        if (requestId === this.dictionaryLookupRequestId) {
          this.dictionaryServerResults = []
        }
      }
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
    closeHighlightsDrawer() {
      this.highlightsDrawerOpen = false
      this.cancelHighlightNoteEdit()
      this.$nextTick(() => this.focusReaderStage())
    },
    highlightLabel(highlight: ShelfViewHighlightDto): string {
      const quote = highlight.textQuote?.trim()
      if (quote) return quote.length > 80 ? `${quote.slice(0, 80)}…` : quote

      const locator = highlight.locator
      const paragraph = locator.kind === 'TEXT'
        ? locator.paragraph
        : locator.kind === 'MIXED' && locator.itemLocator.kind === 'TEXT'
          ? locator.itemLocator.paragraph
          : undefined
      return paragraph === undefined
        ? this.$t('shelfview_reader.highlights') as string
        : this.$t('shelfview_reader.bookmark_paragraph', {n: paragraph + 1}) as string
    },
    async goToHighlight(highlight: ShelfViewHighlightDto) {
      if (!this.adapter) return

      try {
        await this.adapter.goTo(highlight.locator)
        this.syncAdapterState()
        this.schedulePositionSave()
        this.closeHighlightsDrawer()
      } catch (e) {
        this.setReaderStateStatus('ERROR', this.$t('shelfview_reader.status_bookmark_unavailable') as string)
      }
    },
    async setHighlightColor(
      highlight: ShelfViewHighlightDto,
      color: ShelfViewHighlightColor,
    ) {
      if (highlight.color === color) return
      await this.updateHighlight(highlight, {color, note: highlight.note})
    },
    startHighlightNoteEdit(highlight: ShelfViewHighlightDto) {
      this.editingHighlightId = highlight.id
      this.highlightNoteDraft = highlight.note ?? ''
    },
    cancelHighlightNoteEdit() {
      this.editingHighlightId = ''
      this.highlightNoteDraft = ''
    },
    async saveHighlightNote(highlight: ShelfViewHighlightDto) {
      const note = this.highlightNoteDraft.trim() || undefined
      if (await this.updateHighlight(highlight, {color: highlight.color, note})) this.cancelHighlightNoteEdit()
    },
    async updateHighlight(
      highlight: ShelfViewHighlightDto,
      changes: {color: ShelfViewHighlightColor, note?: string},
    ): Promise<boolean> {
      if (this.browserOffline()) {
        this.setReaderStateStatus('OFFLINE', this.$t('shelfview_reader.status_offline') as string)
        return false
      }

      try {
        const updated = await this.$shelfViewState.updateHighlight(highlight.id, changes)
        this.highlights = this.highlights.map(existing => existing.id === updated.id ? updated : existing)
        this.setReaderStateStatus('SAVED', this.$t('shelfview_reader.status_saved') as string)
        return true
      } catch (e) {
        this.setReaderStateStatus(this.browserOffline() ? 'OFFLINE' : 'ERROR', this.$t('shelfview_reader.status_highlight_failed') as string)
        return false
      }
    },
    async deleteHighlight(highlight: ShelfViewHighlightDto) {
      if (this.browserOffline()) {
        this.setReaderStateStatus('OFFLINE', this.$t('shelfview_reader.status_offline') as string)
        return
      }

      try {
        await this.$shelfViewState.deleteHighlight(highlight.id)
        this.highlights = this.highlights.filter(existing => existing.id !== highlight.id)
        if (this.editingHighlightId === highlight.id) this.cancelHighlightNoteEdit()
        this.setReaderStateStatus('SAVED', this.$t('shelfview_reader.status_highlight_deleted') as string)
      } catch (e) {
        this.setReaderStateStatus(this.browserOffline() ? 'OFFLINE' : 'ERROR', this.$t('shelfview_reader.status_highlight_failed') as string)
      }
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
      if (document.visibilityState !== 'hidden') return

      // A hidden tab can be discarded without ever firing pagehide, so this
      // takes the same keepalive path rather than an XHR that may not finish.
      if (this.flushPositionSaveOnUnload()) {
        void this.flushPreferenceSave()
        return
      }
      void this.flushReaderState()
    },
    /**
     * A mobile address bar collapsing fires resize repeatedly, and repaginating
     * rebuilds the paragraph length table each time, so the bursts are collapsed
     * into one frame the same way scrolling is.
     */
    onReaderViewportChanged() {
      if (this.viewportFrameHandle !== undefined) return
      if (typeof window.requestAnimationFrame !== 'function') {
        this.applyReaderViewportChange()
        return
      }

      this.viewportFrameHandle = window.requestAnimationFrame(() => {
        this.viewportFrameHandle = undefined
        this.applyReaderViewportChange()
      })
    },
    applyReaderViewportChange() {
      this.recomputeTextPagination()
      this.$nextTick(() => this.restoreCurrentVisibleContent())
    },
    onPageHide() {
      if (this.flushPositionSaveOnUnload()) {
        void this.flushPreferenceSave()
        return
      }
      void this.flushReaderState()
    },
    async onReaderKeydown(e: KeyboardEvent) {
      if (e.ctrlKey || e.altKey || e.metaKey) return
      if (shelfViewShouldIgnoreShortcutTarget(e.target)) return

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
        e.preventDefault()
        // Escape unwinds one layer at a time: the innermost open panel first,
        // and only the chrome itself once nothing is left open.
        if (!this.closeTopmostChromePanel()) this.toggleChrome()
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
      // Zoom is the modifier gesture, as it already was in continuous scroll. A
      // plain wheel has to stay with the stage, otherwise a width-fitted page
      // that runs off the bottom of the screen cannot be scrolled at all.
      if (!e.ctrlKey && !e.metaKey) return

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
      // A page that already fits has nothing to pan, and treating the drag as a
      // pan there would swallow the centre tap that brings the chrome back.
      if (!this.readerStageCanPan(readerStage)) return

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
    readerStageCanPan(readerStage: HTMLElement): boolean {
      return readerStage.scrollWidth > readerStage.clientWidth ||
        readerStage.scrollHeight > readerStage.clientHeight
    },
    onReaderPointerMove(e: PointerEvent) {
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
      // A second finger means a pinch or a browser gesture, never a page turn.
      // Dropping the start point here also covers touchend, which fires once per
      // finger and would otherwise read the released finger as a swipe.
      if ((e.touches?.length ?? 0) > 1) {
        this.touchStart = undefined
        this.touchStartScrollLeft = undefined
        return
      }

      const touch = e.changedTouches[0]
      if (!touch) return

      this.touchStart = {
        x: touch.clientX,
        y: touch.clientY,
      }
      this.touchStartScrollLeft = (this.$refs.readerStage as HTMLElement | undefined)?.scrollLeft
    },
    async onReaderTouchEnd(e: TouchEvent) {
      const touch = e.changedTouches[0]
      if (!this.touchStart || !touch) return

      if ((e.touches?.length ?? 0) > 0) {
        this.touchStart = undefined
        this.touchStartScrollLeft = undefined
        return
      }

      if (this.openEndPromptForContinuousTouchEnd(e, touch)) {
        this.touchStart = undefined
        this.touchStartScrollLeft = undefined
        return
      }

      const action = shelfViewTouchAction(
        this.touchStart,
        {x: touch.clientX, y: touch.clientY},
        this.readingDirection,
        this.readerTouchStageState(),
      ) ?? this.readerTapAction(this.touchStart, {x: touch.clientX, y: touch.clientY})
      this.touchStart = undefined
      this.touchStartScrollLeft = undefined

      if (!action) return

      if (e.cancelable) e.preventDefault()
      await this.runReaderInputAction(action)
    },
    /**
     * Places the selection toolbar just above the selected text, or clears it
     * when the selection is empty or outside the book text.
     */
    refreshSelectionActions() {
      if (!this.textControlsVisible) {
        this.selectionRect = undefined
        return
      }

      const selection = window.getSelection?.()
      if (!selection || selection.isCollapsed || selection.rangeCount === 0 || !this.selectedTextHighlightRange()) {
        this.selectionRect = undefined
        return
      }

      const rect = selection.getRangeAt(0).getBoundingClientRect?.()
      if (!rect || (rect.width === 0 && rect.height === 0)) {
        this.selectionRect = undefined
        return
      }

      this.selectionRect = {
        top: Math.max(8, rect.top - 48),
        left: Math.max(8, rect.left),
      }
    },
    readerTouchStageState(): ShelfViewTouchStageState {
      const readerStage = this.$refs.readerStage as HTMLElement | undefined
      if (!readerStage) return {canPanHorizontally: false, scrolledHorizontally: false}

      return {
        canPanHorizontally: readerStage.scrollWidth > readerStage.clientWidth,
        scrolledHorizontally: this.touchStartScrollLeft !== undefined &&
          readerStage.scrollLeft !== this.touchStartScrollLeft,
      }
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
      // A page wider than the stage is panned with the side zones, so tapping
      // them must not also turn the page. Measuring the real overflow keeps
      // paging alive for a page that merely got taller, which a scale threshold
      // used to switch off without telling anyone.
      const pagingEnabled = this.tapPagingEnabled &&
        readerStage.scrollWidth <= readerStage.clientWidth

      return shelfViewTapAction(
        {x: start.x - rect.left, y: start.y - rect.top},
        {x: end.x - rect.left, y: end.y - rect.top},
        rect.width || readerStage.clientWidth,
        this.readingDirection,
        pagingEnabled,
      )
    },
    readerPointerSelectionActive(): boolean {
      return !!window.getSelection?.()?.toString()
    },
    /**
     * Scroll fires far faster than the screen repaints, and the work behind it
     * measures every paragraph or page element in the document. Collapsing the
     * bursts into one frame keeps a long TXT and a long comic scrolling smoothly
     * without changing what the reader ends up looking at.
     */
    onReaderStageScroll() {
      if (this.scrollFrameHandle !== undefined) return
      if (typeof window.requestAnimationFrame !== 'function') {
        this.applyReaderStageScroll()
        return
      }

      this.scrollFrameHandle = window.requestAnimationFrame(() => {
        this.scrollFrameHandle = undefined
        this.applyReaderStageScroll()
      })
    },
    applyReaderStageScroll() {
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
          this.toggleChrome()
          break
        case 'SHOW_SHORTCUTS':
          this.showShortcutHelp = true
          break
        case 'TOGGLE_CONTENTS':
          if (this.contentsAvailable) this.contentsDrawerOpen = !this.contentsDrawerOpen
          break
        case 'TOGGLE_BOOKMARKS':
          this.bookmarksDrawerOpen = !this.bookmarksDrawerOpen
          break
        case 'TOGGLE_SETTINGS':
          this.toggleOverflowMenu()
          break
        case 'FOCUS_PAGE_JUMP':
          this.focusPageJumpInput()
          break
        case 'ZOOM_IN':
          this.stepFixedPageScale(FIXED_PAGE_WHEEL_SCALE_STEP)
          break
        case 'ZOOM_OUT':
          this.stepFixedPageScale(-FIXED_PAGE_WHEEL_SCALE_STEP)
          break
        case 'ZOOM_RESET':
          this.stepFixedPageScale(undefined)
          break
      }
    },
    /**
     * Steps the page scale, or returns it to 100% when no step is given.
     * Zooming used to be reachable only through the mouse wheel and a slider
     * three panels deep, which left trackpad and keyboard readers with no way
     * to enlarge a panel.
     */
    stepFixedPageScale(step: number | undefined) {
      if (!this.fixedPageControlsVisible) return

      const nextScale = step === undefined
        ? SHELFVIEW_FIXED_PAGE_DISPLAY_DEFAULTS.fixedPageScale
        : this.normalizedFixedPageScale(this.fixedPageScale + step)
      if (nextScale === this.fixedPageScale) return

      this.fixedPageScale = nextScale
      this.schedulePreferenceSave()
    },
    focusPageJumpInput() {
      if (!this.fixedPageControlsVisible) return

      this.chromeVisible = true
      this.overflowMenuOpen = true
      this.$nextTick(() => {
        const input = this.$el.querySelector('#shelfview-reader-page-jump-input') as HTMLInputElement | undefined
        input?.focus()
        input?.select()
      })
    },
    async closeBook() {
      await this.flushReaderState()

      // The router restores savedPosition, so going back returns the reader to
      // the originating list at the row they were on.
      if (this.canReturnToPreviousScreen) {
        this.$router.back()
        return
      }

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
  src: url("~@/shelfview/assets/fonts/GowunDodum-Regular.woff2") format("woff2");
}

@font-face {
  font-display: swap;
  font-family: "ShelfView Gowun Batang";
  font-style: normal;
  font-weight: 400;
  src: url("~@/shelfview/assets/fonts/GowunBatang-Regular.woff2") format("woff2");
}

.shelfview-reader-shell {
  background: var(--sv-chrome-bg);
  height: 100vh;
  overflow: hidden;
  width: 100%;

  /* How much vertical space the fit rules must leave for the chrome. The 72px
     baseline covers the toolbar and the default 16px stage padding; the
     variable adds whatever the bottom dock and the thumbnail strip reserve on
     top of that padding, so "fit to screen" keeps meaning it. */
  --sv-stage-bottom-chrome: 0px;
}

.shelfview-reader-shell--thumbnails {
  --sv-stage-bottom-chrome: 88px;
}

.shelfview-reader-shell--page-dock {
  --sv-stage-bottom-chrome: 56px;
}

.shelfview-reader-shell--page-dock.shelfview-reader-shell--thumbnails {
  --sv-stage-bottom-chrome: 144px;
}

/* The toolbar stays dark in both application themes so the chrome never glows
   next to the page, and it is painted from the shared tokens rather than the
   Vuetify theme surface. It floats over the page instead of reserving space, so
   showing or hiding it never resizes the page underneath. */
.shelfview-reader-shell__toolbar.v-toolbar {
  backdrop-filter: blur(18px);
  background-color: var(--sv-chrome-elevated) !important;
  border-bottom: 1px solid var(--sv-chrome-border);
  color: var(--sv-chrome-text);
  min-width: 0;
  position: fixed;
  top: 0;
  z-index: 2;
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
  border-radius: var(--tales-radius-sm);
  flex: 0 0 auto;
  letter-spacing: 0;
  text-transform: none;
}

.shelfview-reader-shell__control-toggle-label {
  font-size: 12px;
  margin: 0 2px 0 6px;
  max-width: 96px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.shelfview-reader-shell__settings-button--open {
  background: var(--sv-chrome-accent-soft) !important;
  color: var(--sv-chrome-accent) !important;
}

/* The settings panel replaces the old row of unlabelled icons. Every control
   keeps its own data-testid so the toolbar contract in the spec still holds,
   but each one now carries a written label and a written state. */
.shelfview-reader-shell__overflow {
  background: var(--sv-chrome-surface);
  border-inline-start: 1px solid var(--sv-chrome-border);
  bottom: 0;
  box-shadow: var(--sv-chrome-shadow);
  color: var(--sv-chrome-text);
  display: flex;
  flex-direction: column;
  max-width: min(360px, 94vw);
  overflow-y: auto;
  padding: 16px 20px 24px;
  position: fixed;
  right: 0;
  top: 48px;
  width: 360px;
  z-index: 3;
}

.shelfview-reader-shell__overflow-header {
  align-items: center;
  display: flex;
  gap: 10px;
  justify-content: space-between;
  margin-bottom: 12px;
}

.shelfview-reader-shell__overflow-header h2 {
  flex: 1 1 auto;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: -0.01em;
  margin: 0;
}

.shelfview-reader-shell__overflow-actions {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.shelfview-reader-shell__setting-group-title {
  color: var(--sv-chrome-muted);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.14em;
  margin: 0 0 4px;
  padding-inline: 10px;
  text-transform: uppercase;
}

.shelfview-reader-shell__setting-row {
  align-items: center;
  background: transparent;
  border: 0;
  border-radius: var(--tales-radius-sm);
  color: var(--sv-chrome-text);
  cursor: pointer;
  display: flex;
  font: inherit;
  gap: 10px;
  min-height: 44px;
  padding: 8px 10px;
  text-align: left;
  width: 100%;
}

.shelfview-reader-shell__setting-row--static {
  cursor: default;
}

.shelfview-reader-shell__setting-row:hover:not(:disabled):not(.shelfview-reader-shell__setting-row--static) {
  background: var(--sv-chrome-surface-soft);
}

.shelfview-reader-shell__setting-row:focus-visible {
  outline: 2px solid var(--sv-chrome-accent);
  outline-offset: -2px;
}

.shelfview-reader-shell__setting-row:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.shelfview-reader-shell__setting-row-label {
  flex: 1 1 auto;
  font-size: 14px;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.shelfview-reader-shell__setting-row-value {
  color: var(--sv-chrome-muted);
  flex: 0 1 auto;
  font-size: 13px;
  max-width: 44%;
  overflow: hidden;
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Written state, not colour alone, so the on/off reading survives for anyone
   who cannot separate the two accents. */
.shelfview-reader-shell__setting-row-state {
  border: 1px solid var(--sv-chrome-border-strong);
  border-radius: 999px;
  color: var(--sv-chrome-muted);
  flex: 0 0 auto;
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
  padding: 5px 10px;
}

.shelfview-reader-shell__setting-row[aria-pressed="true"] .shelfview-reader-shell__setting-row-state {
  background: var(--sv-chrome-accent-soft);
  border-color: var(--sv-chrome-accent);
  color: var(--sv-chrome-accent);
}

.shelfview-reader-shell__setting-row[aria-pressed="true"] .v-icon,
.shelfview-reader-shell__setting-row[aria-expanded="true"] .v-icon {
  color: var(--sv-chrome-accent);
}

/* The reader chrome is dark in both application themes, so its icons and
   buttons cannot inherit the Vuetify theme colour or they turn invisible on
   the light theme. */
.shelfview-reader-shell__overflow .v-icon,
.shelfview-reader-shell__contents-drawer .v-icon,
.shelfview-reader-shell__dictionary-drawer .v-icon,
.shelfview-reader-shell__bookmarks-drawer .v-icon,
.shelfview-reader-shell__text-settings .v-icon,
.shelfview-reader-shell__shortcut-panel .v-icon,
.shelfview-reader-shell__end-panel .v-icon {
  color: var(--sv-chrome-muted);
}

.shelfview-reader-shell__overflow .v-btn:not(.v-btn--has-bg),
.shelfview-reader-shell__contents-drawer .v-btn:not(.v-btn--has-bg),
.shelfview-reader-shell__dictionary-drawer .v-btn:not(.v-btn--has-bg),
.shelfview-reader-shell__bookmarks-drawer .v-btn:not(.v-btn--has-bg),
.shelfview-reader-shell__text-settings .v-btn:not(.v-btn--has-bg) {
  border-radius: var(--tales-radius-sm);
  color: var(--sv-chrome-text);
  letter-spacing: 0;
  text-transform: none;
}

.shelfview-reader-shell__page-counter {
  font-size: 14px;
  min-width: 56px;
  text-align: center;
}

.shelfview-reader-shell__page-jump {
  background: var(--sv-chrome-surface-soft);
  border: 1px solid var(--sv-chrome-border-strong);
  border-radius: var(--tales-radius-sm);
  color: var(--sv-chrome-text);
  flex: 0 0 auto;
  font-size: 13px;
  height: 34px;
  padding: 0 6px;
  text-align: center;
  width: 82px;
}

.shelfview-reader-shell__page-jump:focus {
  border-color: var(--sv-chrome-accent);
  outline: 2px solid var(--sv-chrome-accent);
  outline-offset: 1px;
}

.shelfview-reader-shell__search-bar {
  align-items: center;
  background: var(--sv-chrome-surface);
  border-bottom: 1px solid var(--sv-chrome-border);
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
  color: var(--sv-chrome-muted);
  font-size: 13px;
  min-width: 72px;
  text-align: center;
}

.shelfview-reader-shell__sync-status {
  border: 1px solid var(--sv-chrome-border-strong);
  border-radius: 999px;
  color: var(--sv-chrome-muted);
  font-size: 12px;
  line-height: 1;
  max-width: 140px;
  overflow: hidden;
  padding: 6px 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.shelfview-reader-shell__sync-status--error {
  border-color: var(--sv-chrome-danger);
  color: var(--sv-chrome-danger);
}

.shelfview-reader-shell__toolbar-status {
  align-items: center;
  display: inline-flex;
  gap: 4px;
  flex: 0 1 auto;
  margin-left: 12px;
}

.shelfview-reader-shell__toolbar-status .v-icon {
  color: inherit;
}

.shelfview-reader-shell__sync-status--saved {
  border-color: var(--sv-chrome-success);
  color: var(--sv-chrome-success);
}

.shelfview-reader-shell__stage {
  align-items: center;
  display: flex;
  height: 100%;
  justify-content: center;
  overflow: hidden;
  /* Without this an edge swipe reaches the browser's back gesture instead of
     turning the page, which throws the reader out of the book. */
  overscroll-behavior: contain;
  padding: 16px;
  touch-action: pan-x pan-y pinch-zoom;
  width: 100%;
}

.shelfview-reader-shell--thumbnails .shelfview-reader-shell__stage {
  padding-bottom: 104px;
}

/* Keep the page clear of the bottom dock rather than letting it sit on top. */
.shelfview-reader-shell--page-dock .shelfview-reader-shell__stage,
.shelfview-reader-shell--page-dock .shelfview-reader-shell__stage--fixed {
  padding-bottom: 72px;
}

.shelfview-reader-shell--page-dock.shelfview-reader-shell--thumbnails .shelfview-reader-shell__stage,
.shelfview-reader-shell--page-dock.shelfview-reader-shell--thumbnails .shelfview-reader-shell__stage--fixed {
  padding-bottom: 160px;
}

.shelfview-reader-shell__stage:focus {
  outline: 2px solid var(--sv-chrome-accent);
  outline-offset: -6px;
}

.shelfview-reader-shell__stage--continuous {
  align-items: stretch;
  justify-content: flex-start;
  overflow-y: auto;
}

.shelfview-reader-shell__stage--fixed {
  background: var(--sv-fixed-background, var(--sv-chrome-bg));
  cursor: grab;
  overflow: auto;
  padding: var(--sv-fixed-page-margin, 16px);
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
  filter: brightness(var(--sv-text-brightness, 1));
  overflow-y: auto;
  padding-left: 0;
  padding-right: 0;
}

/* A page that fits the screen may sit under the toolbar, but content that
   scrolls cannot be pulled back out from under it, so the scrolling layouts
   always reserve the toolbar height. The reservation never depends on the
   toolbar being up, so hiding it does not move the page. Both classes are
   repeated to outrank the shorthand padding these layouts inherit. */
.shelfview-reader-shell__stage.shelfview-reader-shell__stage--continuous,
.shelfview-reader-shell__stage.shelfview-reader-shell__stage--text {
  padding-top: 48px;
}

/* The search bar is another fixed strip below the toolbar, and it covers
   scrolling content the same way, so opening it extends the same reservation.
   Opening search is a deliberate action, unlike the old idle auto-hide, so the
   one-off shift it causes is expected rather than jitter. */
.shelfview-reader-shell--search-open .shelfview-reader-shell__stage--text {
  padding-top: 104px;
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

/* Continuous scroll puts every page in the DOM and loads them lazily, so a page
   that has not arrived yet is zero pixels tall. Holding a rough page height
   keeps the scrollbar from growing under the reader's finger and keeps a
   restored position landing near the right place. The manifest carries no page
   sizes, so this is a placeholder height rather than the real aspect ratio. */
.shelfview-reader-shell__pages--continuous .shelfview-reader-shell__page,
.shelfview-reader-shell__pages--continuous .shelfview-page-content {
  min-height: 60vh;
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

.shelfview-reader-shell__progress {
  color: var(--sv-chrome-muted);
  font-size: 11px;
  margin-left: 6px;
}

/* `rotate` turns the picture but leaves the layout box upright, so a quarter
   turn is still measured against the unrotated width and height and runs off
   the screen. Swapping which side each fit rule constrains puts the page back
   inside the stage. */
.shelfview-reader-shell__pages--rotated-quarter .shelfview-reader-shell__page--fit-screen {
  max-height: calc(100% * var(--sv-fixed-page-scale, 1));
  max-width: calc((100vh - 72px - var(--sv-stage-bottom-chrome, 0px) - (var(--sv-fixed-page-margin, 16px) - 16px) * 2) * var(--sv-fixed-page-scale, 1));
}

.shelfview-reader-shell__pages--rotated-quarter .shelfview-reader-shell__page--fit-width {
  height: calc(100% * var(--sv-fixed-page-scale, 1));
  max-width: none;
  width: auto;
}

.shelfview-reader-shell__pages--rotated-quarter .shelfview-reader-shell__page--fit-height {
  height: auto;
  max-height: 100%;
  width: calc((100vh - 72px - var(--sv-stage-bottom-chrome, 0px) - (var(--sv-fixed-page-margin, 16px) - 16px) * 2) * var(--sv-fixed-page-scale, 1));
}

.shelfview-reader-shell__highlight-colors {
  display: flex;
  gap: 6px;
  margin: 6px 0;
}

.shelfview-reader-shell__highlight-color {
  border: 1px solid var(--sv-chrome-border-strong);
  border-radius: 999px;
  height: 20px;
  width: 20px;
}

.shelfview-reader-shell__highlight-color--selected {
  border-color: var(--sv-chrome-accent);
  border-width: 2px;
}

.shelfview-reader-shell__highlight-color:focus-visible {
  outline: 2px solid var(--sv-chrome-accent);
  outline-offset: 2px;
}

/* Colour is the meaning here, so these literals are the rule 1-9 exception. */
.shelfview-reader-shell__highlight-color--yellow {
  background: rgba(255, 213, 79, 0.85);
}

.shelfview-reader-shell__highlight-color--green {
  background: rgba(129, 199, 132, 0.85);
}

.shelfview-reader-shell__highlight-color--blue {
  background: rgba(100, 181, 246, 0.85);
}

.shelfview-reader-shell__highlight-color--pink {
  background: rgba(240, 148, 191, 0.85);
}

.shelfview-reader-shell__highlight-color--underline {
  background: transparent;
  border-bottom: 3px solid var(--sv-chrome-text);
}

.shelfview-reader-shell__selection-actions {
  background: var(--sv-chrome-elevated);
  border: 1px solid var(--sv-chrome-border);
  border-radius: var(--tales-radius-sm);
  box-shadow: var(--sv-chrome-shadow);
  display: flex;
  gap: 2px;
  padding: 2px;
  position: fixed;
  z-index: 8;
}

.shelfview-reader-shell__text--unwrapped {
  white-space: normal;
}

/* The reader keeps its own accent so the chrome does not change colour with the
   application theme (Rule_UIUX 1-9). */
.shelfview-reader-shell__action.v-btn {
  background: var(--sv-chrome-surface-soft) !important;
  color: var(--sv-chrome-text) !important;
  letter-spacing: 0;
  text-transform: none;
}

.shelfview-reader-shell__action--accent.v-btn {
  background: var(--sv-chrome-accent-soft, var(--sv-chrome-surface-soft)) !important;
  color: var(--sv-chrome-accent) !important;
}

.shelfview-reader-shell__icon--danger.v-icon {
  color: var(--sv-chrome-danger) !important;
}

.shelfview-reader-shell__icon--muted.v-icon {
  color: var(--sv-chrome-muted) !important;
}

/* Holds the place of a page that is still arriving, so a slow load reads as
   loading rather than as a broken screen (Rule_UIUX 1-11). It only paints a
   surface; nothing here looks like it can be pressed. */
.shelfview-reader-shell__page--pending {
  background: var(--sv-chrome-surface-soft);
  min-height: 240px;
  min-width: 160px;
}

/* Stands in for one unreadable page so the pages around it stay put. */
.shelfview-reader-shell__page-failed {
  align-items: center;
  background: var(--sv-chrome-surface-soft);
  border: 1px dashed var(--sv-chrome-border-strong);
  border-radius: var(--tales-radius-sm);
  color: var(--sv-chrome-muted);
  display: flex;
  flex-direction: column;
  gap: 8px;
  justify-content: center;
  min-height: 240px;
  min-width: 200px;
  padding: 24px;
  text-align: center;
}

.shelfview-reader-shell__page-failed .v-icon {
  color: var(--sv-chrome-muted);
}

.shelfview-reader-shell__page--fit-screen {
  max-height: calc((100vh - 72px - var(--sv-stage-bottom-chrome, 0px) - (var(--sv-fixed-page-margin, 16px) - 16px) * 2) * var(--sv-fixed-page-scale, 1));
  max-width: calc(100% * var(--sv-fixed-page-scale, 1));
}

.shelfview-reader-shell__page--fit-width {
  height: auto;
  max-width: none;
  width: calc(100% * var(--sv-fixed-page-scale, 1));
}

.shelfview-reader-shell__page--fit-height {
  height: calc((100vh - 72px - var(--sv-stage-bottom-chrome, 0px) - (var(--sv-fixed-page-margin, 16px) - 16px) * 2) * var(--sv-fixed-page-scale, 1));
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
  color: var(--sv-chrome-text);
  font-family: var(--sv-text-font-family, Pretendard, Arial, sans-serif);
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

/* Marks the paragraph being read aloud so the reader can follow along. */
.shelfview-reader-shell__text-paragraph--speaking {
  box-shadow: inset 3px 0 0 0 var(--sv-chrome-accent);
  padding-left: 10px;
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

.shelfview-reader-shell__page-dock {
  align-items: center;
  backdrop-filter: blur(18px);
  background: var(--sv-chrome-elevated);
  border: 1px solid var(--sv-chrome-border);
  border-radius: 999px;
  bottom: max(16px, env(safe-area-inset-bottom, 0px));
  box-shadow: var(--sv-chrome-shadow);
  color: var(--sv-chrome-text);
  display: flex;
  gap: 4px;
  left: 50%;
  padding: 4px 8px;
  position: fixed;
  transform: translateX(-50%);
  z-index: 2;
}

.shelfview-reader-shell--thumbnails .shelfview-reader-shell__page-dock {
  bottom: calc(88px + max(12px, env(safe-area-inset-bottom, 0px)));
}

.shelfview-reader-shell__page-dock .v-icon {
  color: var(--sv-chrome-text);
}

.shelfview-reader-shell__thumbnail-strip {
  align-items: center;
  backdrop-filter: blur(18px);
  background: var(--sv-chrome-elevated);
  border-top: 1px solid var(--sv-chrome-border);
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
  background: var(--sv-chrome-surface-soft);
  border: 2px solid transparent;
  border-radius: var(--tales-radius-sm);
  cursor: pointer;
  display: flex;
  flex: 0 0 58px;
  height: 70px;
  justify-content: center;
  padding: 3px;
}

/* Colour alone cannot carry "you are here" (Rule_UIUX 1-4), so the current
   thumbnail also gets a thicker frame, and focus is told apart from it. */
.shelfview-reader-shell__thumbnail-button--active {
  border-color: var(--sv-chrome-accent);
  border-width: 2px;
  box-shadow: inset 0 -3px 0 0 var(--sv-chrome-accent);
}

.shelfview-reader-shell__thumbnail-button:focus-visible {
  outline: 2px solid var(--sv-chrome-accent);
  outline-offset: 2px;
}

.shelfview-reader-shell__thumbnail-button img {
  max-height: 100%;
  max-width: 100%;
  object-fit: contain;
}

.shelfview-reader-shell__status {
  align-items: center;
  color: var(--sv-chrome-text);
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
  color: var(--sv-chrome-muted);
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
  color: var(--sv-chrome-muted);
  font-size: 13px;
}

.shelfview-reader-shell__contents-drawer,
.shelfview-reader-shell__dictionary-drawer,
.shelfview-reader-shell__bookmarks-drawer {
  background: var(--sv-chrome-surface);
  border-inline-start: 1px solid var(--sv-chrome-border);
  bottom: 0;
  box-shadow: var(--sv-chrome-shadow);
  color: var(--sv-chrome-text);
  max-width: min(360px, 92vw);
  overflow-y: auto;
  padding: 20px;
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
  border-bottom: 1px solid var(--sv-chrome-border);
}

.shelfview-reader-shell__contents-link {
  align-items: center;
  background: transparent;
  border: 0;
  border-radius: var(--tales-radius-sm);
  color: var(--sv-chrome-text);
  cursor: pointer;
  display: flex;
  font: inherit;
  gap: 10px;
  min-height: 44px;
  padding: 10px 8px;
  text-align: left;
  width: 100%;
}

.shelfview-reader-shell__contents-link:hover {
  background: var(--sv-chrome-surface-soft);
  color: var(--sv-chrome-accent);
}

/* The active contents row already carries the accent colour, so colour alone
   cannot show keyboard focus on the row the reader is currently on. */
.shelfview-reader-shell__contents-link:focus-visible {
  color: var(--sv-chrome-accent);
  outline: 2px solid var(--sv-chrome-accent);
  outline-offset: 2px;
}

.shelfview-reader-shell__contents-link--active {
  background: var(--sv-chrome-accent-soft);
  color: var(--sv-chrome-accent);
}

.shelfview-reader-shell__contents-index {
  color: var(--sv-chrome-muted);
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
  color: var(--sv-chrome-muted);
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
  color: var(--sv-chrome-muted);
  font-size: 13px;
}

.shelfview-reader-shell__dictionary-term {
  color: var(--sv-chrome-text);
  font-size: 15px;
}

.shelfview-reader-shell__dictionary-definition {
  color: var(--sv-chrome-text);
  flex: 1 0 100%;
  font-size: 14px;
  line-height: 1.55;
  margin: 0;
  overflow-wrap: anywhere;
}

.shelfview-reader-shell__dictionary-source {
  color: var(--sv-chrome-muted);
  font-size: 12px;
}

.shelfview-reader-shell__dictionary-link {
  align-items: center;
  background: var(--sv-chrome-accent-soft);
  border: 1px solid var(--sv-chrome-border-strong);
  border-radius: var(--tales-radius-sm);
  color: var(--sv-chrome-accent);
  display: inline-flex;
  font-weight: 600;
  gap: 8px;
  min-height: 36px;
  padding: 0 14px;
  text-decoration: none;
}

.shelfview-reader-shell__dictionary-link:hover {
  border-color: var(--sv-chrome-accent);
}

.shelfview-reader-shell__dictionary-link:focus-visible {
  border-color: var(--sv-chrome-accent);
  outline: 2px solid var(--sv-chrome-accent);
  outline-offset: 2px;
}

.shelfview-reader-shell__text-settings {
  background: var(--sv-chrome-surface);
  border-inline-start: 1px solid var(--sv-chrome-border);
  bottom: 0;
  box-shadow: var(--sv-chrome-shadow);
  color: var(--sv-chrome-text);
  max-width: min(380px, 94vw);
  overflow-y: auto;
  padding: 20px;
  position: fixed;
  right: 0;
  top: 48px;
  width: 380px;
  z-index: 3;
}

/* Every reader surface that has a title bar uses the same header rule, so the
   settings panel, the side drawers and the text settings read as one family. */
.shelfview-reader-shell__text-settings-header {
  align-items: center;
  display: flex;
  gap: 10px;
  justify-content: space-between;
  margin-bottom: 16px;
}

.shelfview-reader-shell__text-settings-header h2 {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: -0.01em;
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
  color: var(--sv-chrome-text);
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
  color: var(--sv-chrome-accent);
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
  color: var(--sv-chrome-accent);
  font-weight: 500;
}

.shelfview-reader-shell__drawer-header {
  align-items: center;
  display: flex;
  gap: 10px;
  justify-content: space-between;
  margin-bottom: 12px;
}

.shelfview-reader-shell__drawer-header h2 {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: -0.01em;
  margin: 0;
}

.shelfview-reader-shell__bookmarks-empty {
  color: var(--sv-chrome-muted);
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
  border-bottom: 1px solid var(--sv-chrome-border);
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
  color: var(--sv-chrome-text);
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
  color: var(--sv-chrome-muted);
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
  background: var(--sv-chrome-scrim);
  display: flex;
  inset: 0;
  justify-content: center;
  padding: 16px;
  position: fixed;
  z-index: 4;
}

.shelfview-reader-shell__shortcut-panel,
.shelfview-reader-shell__end-panel {
  background: var(--sv-chrome-surface);
  border: 1px solid var(--sv-chrome-border);
  border-radius: var(--tales-radius-lg);
  box-shadow: var(--sv-chrome-shadow);
  color: var(--sv-chrome-text);
  max-width: 420px;
  padding: 24px;
  width: 100%;
}

.shelfview-reader-shell__shortcut-panel h2,
.shelfview-reader-shell__end-panel h2 {
  font-size: 20px;
  font-weight: 500;
  margin: 0 0 16px;
}

.shelfview-reader-shell__end-panel p {
  color: var(--sv-chrome-muted);
  line-height: 1.5;
  margin: 0 0 12px;
}

.shelfview-reader-shell__end-next-title {
  background: var(--sv-chrome-surface-soft);
  border-radius: var(--tales-radius-sm);
  color: var(--sv-chrome-text) !important;
  font-weight: 600;
  overflow-wrap: anywhere;
  padding: 10px 12px;
}

.shelfview-reader-shell__end-no-next {
  color: var(--sv-chrome-muted) !important;
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
  color: var(--sv-chrome-accent);
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

/* The right-hand panels all open in the same place, so a short slide makes it
   possible to see which one replaced which (Rule_UIUX 1-6: one property,
   150-200ms). */
.shelfview-panel-enter-active,
.shelfview-panel-leave-active {
  transition: transform 180ms ease;
}

.shelfview-panel-enter,
.shelfview-panel-leave-to {
  transform: translateX(100%);
}

@media (prefers-reduced-motion: reduce) {
  .shelfview-reader-shell * {
    scroll-behavior: auto !important;
  }

  .shelfview-panel-enter-active,
  .shelfview-panel-leave-active {
    transition: none !important;
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

  /* The labels are what push the controls past the edge at 360px, and the
     aria-label on each button already carries the same wording. */
  .shelfview-reader-shell__control-toggle-label {
    display: none;
  }

  .shelfview-reader-shell__overflow {
    max-width: 100vw;
    width: 100vw;
  }

  .shelfview-reader-shell__stage {
    padding: 8px;
  }

  .shelfview-reader-shell__stage--fixed {
    padding: var(--sv-fixed-page-margin, 8px);
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

  /* A failed save must stay visible on a phone; only the quiet states hide. */
  .shelfview-reader-shell__toolbar-status {
    display: inline-flex;
    max-width: 45vw;
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
