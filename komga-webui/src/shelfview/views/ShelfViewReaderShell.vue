<template>
  <v-container
    class="ma-0 pa-0 shelfview-reader-shell"
    :class="{
      'shelfview-reader-shell--search-open': textSearchOpen && textSearchAvailable,
      'shelfview-reader-shell--thumbnails': thumbnailStripVisible,
    }"
    fluid
    data-testid="shelfview-reader-shell"
  >
    <v-toolbar
      dense
      elevation="1"
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
      <v-spacer/>

      <div
        v-if="pageControlsVisible"
        class="shelfview-reader-shell__page-controls"
        data-testid="shelfview-reader-page-controls"
      >
        <v-tooltip bottom>
          <template v-slot:activator="{ on }">
            <v-btn
              icon
              :aria-label="readerLayoutLabel"
              data-testid="shelfview-reader-layout-toggle"
              v-on="on"
              @click="cycleReaderLayout"
            >
              <v-icon>{{ readerLayoutIcon }}</v-icon>
            </v-btn>
          </template>
          <span>{{ readerLayoutLabel }}</span>
        </v-tooltip>

        <v-tooltip bottom>
          <template v-slot:activator="{ on }">
            <v-btn
              v-if="textControlsVisible"
              icon
              :aria-label="textThemeLabel"
              data-testid="shelfview-reader-text-theme-toggle"
              v-on="on"
              @click="cycleTextTheme"
            >
              <v-icon>{{ textThemeIcon }}</v-icon>
            </v-btn>
          </template>
          <span>{{ textThemeLabel }}</span>
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
              :aria-label="fixedPageRotationLabel"
              data-testid="shelfview-reader-rotate-toggle"
              v-on="on"
              @click="cycleFixedPageRotation"
            >
              <v-icon>mdi-rotate-right</v-icon>
            </v-btn>
          </template>
          <span>{{ fixedPageRotationLabel }}</span>
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
              v-if="fixedPageControlsVisible"
              icon
              :aria-label="readingDirectionLabel"
              :disabled="!rtlAvailable"
              data-testid="shelfview-reader-direction-toggle"
              v-on="on"
              @click="toggleReadingDirection"
            >
              <v-icon>{{ readingDirectionIcon }}</v-icon>
            </v-btn>
          </template>
          <span>{{ readingDirectionLabel }}</span>
        </v-tooltip>

        <v-tooltip bottom>
          <template v-slot:activator="{ on }">
            <v-btn
              v-if="fixedPageControlsVisible"
              icon
              :aria-label="pageFitLabel"
              data-testid="shelfview-reader-fit-toggle"
              v-on="on"
              @click="cyclePageFit"
            >
              <v-icon>{{ pageFitIcon }}</v-icon>
            </v-btn>
          </template>
          <span>{{ pageFitLabel }}</span>
        </v-tooltip>

        <v-tooltip bottom>
          <template v-slot:activator="{ on }">
            <v-btn
              v-if="fixedPageControlsVisible"
              icon
              :aria-label="imageFilterPresetLabel"
              :disabled="!imageFiltersAvailable"
              data-testid="shelfview-reader-filter-toggle"
              v-on="on"
              @click="cycleImageFilterPreset"
            >
              <v-icon>{{ imageFilterPresetIcon }}</v-icon>
            </v-btn>
          </template>
          <span>{{ imageFilterPresetLabel }}</span>
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

        <v-btn
          icon
          :disabled="!canGoNext"
          :aria-label="$t('shelfview_reader.next_page')"
          data-testid="shelfview-reader-next"
          @click="nextPage"
        >
          <v-icon>mdi-chevron-right</v-icon>
        </v-btn>
      </div>

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
            :aria-label="$t('shelfview_reader.open_legacy_reader')"
            :to="legacyReaderLocation"
            v-on="on"
          >
            <v-icon>mdi-book-open-page-variant</v-icon>
          </v-btn>
        </template>
        <span>{{ $t('browse_book.read_book') }}</span>
      </v-tooltip>
    </v-toolbar>

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
      @keydown="onReaderKeydown"
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
        <img
          v-for="page in visiblePageSources"
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
          {{ paragraph.text }}
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
      v-if="bookmarksDrawerOpen"
      class="shelfview-reader-shell__bookmarks-drawer"
      role="dialog"
      aria-modal="true"
      aria-labelledby="shelfview-bookmarks-title"
      data-testid="shelfview-bookmarks-drawer"
    >
      <header class="shelfview-reader-shell__bookmarks-header">
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
          <button
            type="button"
            class="shelfview-reader-shell__bookmark-link"
            data-testid="shelfview-bookmark-go"
            @click="goToBookmark(bookmark)"
          >
            {{ bookmarkLabel(bookmark) }}
          </button>
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
          <label for="shelfview-text-font-size">
            {{ $t('shelfview_reader.font_size') }}
            <span>{{ textFontSize }}px</span>
          </label>
          <v-slider
            id="shelfview-text-font-size"
            v-model.number="textFontSize"
            min="14"
            max="32"
            step="1"
            hide-details
            data-testid="shelfview-reader-text-font-size"
            @input="onTextDisplaySettingChanged"
          />
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
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import {RawLocation} from 'vue-router/types/router'
import {BookDto} from '@/types/komga-books'
import {SeriesDto} from '@/types/komga-series'
import {Context, ContextOrigin} from '@/types/context'
import {getBookTitleCompact} from '@/functions/book-title'
import {getLegacyBookReadRouteFromMedia} from '@/functions/book-format'
import {ShelfViewManifest} from '@/shelfview/models/shelfview-manifest'
import {ShelfViewApiError} from '@/shelfview/models/shelfview-errors'
import {ReaderAdapter} from '@/shelfview/adapters/reader-adapter'
import {FixedPageAdapter} from '@/shelfview/adapters/fixed-page-adapter'
import {TextReflowAdapter} from '@/shelfview/adapters/text-reflow-adapter'
import {MixedContentAdapter} from '@/shelfview/adapters/mixed-content-adapter'
import {ShelfViewLocator, ShelfViewTextLocator} from '@/shelfview/models/shelfview-locator'
import {ShelfViewTextParagraph} from '@/shelfview/models/shelfview-text'
import {
  ShelfViewBookmarkDto,
  ShelfViewFixedPagePreferenceSettings,
  ShelfViewPreferenceFormat,
  ShelfViewPositionDto,
} from '@/shelfview/models/shelfview-state'
import {
  SHELFVIEW_FIXED_PAGE_FITS,
  SHELFVIEW_FIXED_PAGE_BACKGROUNDS,
  SHELFVIEW_FIXED_PAGE_LAYOUTS,
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
  ShelfViewImageFilterPreset,
  shelfViewImageFilterPreset,
  shelfViewImageFilterStyle,
  shelfViewNextImageFilterPreset,
} from '@/shelfview/functions/image-filter-presets'
import {
  ShelfViewReaderInputAction,
  ShelfViewTouchPoint,
  shelfViewReaderKeyboardAction,
  shelfViewShouldIgnoreShortcutTarget,
  shelfViewTouchAction,
} from '@/shelfview/functions/reader-input'
import {shelfViewThumbnailUrl} from '@/shelfview/functions/shelfview-urls'
import {
  SHELFVIEW_TEXT_ALIGNS,
  SHELFVIEW_TEXT_FONT_FAMILIES,
  SHELFVIEW_TEXT_THEMES,
  ShelfViewTextAlign,
  ShelfViewTextFontFamily,
  ShelfViewTextTheme,
  shelfViewNextTextTheme,
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

interface ShelfViewFixedPagePanStart {
  pointerId: number,
  x: number,
  y: number,
  scrollLeft: number,
  scrollTop: number,
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
const FIXED_PAGE_SCALE_MIN = 50
const FIXED_PAGE_SCALE_MAX = 300
const FIXED_PAGE_WHEEL_SCALE_STEP = 10

const TEXT_DISPLAY_DEFAULTS = {
  textTheme: 'PAPER' as ShelfViewTextTheme,
  textFontFamily: 'SYSTEM' as ShelfViewTextFontFamily,
  textFontSize: 20,
  textLineHeight: 1.75,
  textLetterSpacing: 0,
  textWordSpacing: 0,
  textContentWidth: 760,
  textPageMargin: 56,
  textParagraphSpacing: 1.35,
  textAlign: 'LEFT' as ShelfViewTextAlign,
  textFontWeight: 400,
  textBrightness: 1,
}

const FIXED_PAGE_DISPLAY_DEFAULTS = {
  fixedPageRotation: 0 as ShelfViewFixedPageRotation,
  fixedPageCoverSingle: false,
  fixedPageScale: 100,
  fixedPageGap: 12,
  fixedPageMargin: 16,
  fixedPageBackground: 'DARK' as ShelfViewFixedPageBackground,
  fixedPageBrightness: 1,
  fixedPageContrast: 1,
}

export default Vue.extend({
  name: 'ShelfViewReaderShell',
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
    fixedPageRotation: FIXED_PAGE_DISPLAY_DEFAULTS.fixedPageRotation,
    fixedPageCoverSingle: FIXED_PAGE_DISPLAY_DEFAULTS.fixedPageCoverSingle,
    fixedPageScale: FIXED_PAGE_DISPLAY_DEFAULTS.fixedPageScale,
    fixedPageGap: FIXED_PAGE_DISPLAY_DEFAULTS.fixedPageGap,
    fixedPageMargin: FIXED_PAGE_DISPLAY_DEFAULTS.fixedPageMargin,
    fixedPageBackground: FIXED_PAGE_DISPLAY_DEFAULTS.fixedPageBackground,
    fixedPageBrightness: FIXED_PAGE_DISPLAY_DEFAULTS.fixedPageBrightness,
    fixedPageContrast: FIXED_PAGE_DISPLAY_DEFAULTS.fixedPageContrast,
    textTheme: TEXT_DISPLAY_DEFAULTS.textTheme,
    textFontFamily: TEXT_DISPLAY_DEFAULTS.textFontFamily,
    textFontSize: TEXT_DISPLAY_DEFAULTS.textFontSize,
    textLineHeight: TEXT_DISPLAY_DEFAULTS.textLineHeight,
    textLetterSpacing: TEXT_DISPLAY_DEFAULTS.textLetterSpacing,
    textWordSpacing: TEXT_DISPLAY_DEFAULTS.textWordSpacing,
    textContentWidth: TEXT_DISPLAY_DEFAULTS.textContentWidth,
    textPageMargin: TEXT_DISPLAY_DEFAULTS.textPageMargin,
    textParagraphSpacing: TEXT_DISPLAY_DEFAULTS.textParagraphSpacing,
    textAlign: TEXT_DISPLAY_DEFAULTS.textAlign,
    textFontWeight: TEXT_DISPLAY_DEFAULTS.textFontWeight,
    textBrightness: TEXT_DISPLAY_DEFAULTS.textBrightness,
    fixedPageSettingsOpen: false,
    textSettingsOpen: false,
    textSearchOpen: false,
    textSearchQuery: '',
    textSearchMatchIndex: 0,
    pageJumpInput: '1',
    slideshowActive: false,
    slideshowTimer: undefined as number | undefined,
    showShortcutHelp: false,
    bookmarks: [] as ShelfViewBookmarkDto[],
    bookmarksDrawerOpen: false,
    readerStateStatus: 'IDLE' as ShelfViewReaderStateStatus,
    readerStateMessage: '',
    positionSaveTimer: undefined as number | undefined,
    preferenceSaveTimer: undefined as number | undefined,
    touchStart: undefined as ShelfViewTouchPoint | undefined,
    fixedPagePanStart: undefined as ShelfViewFixedPagePanStart | undefined,
    fixedPagePanning: false,
    setupRequestId: 0,
  }),
  async mounted() {
    window.addEventListener('online', this.onReaderOnline)
    window.addEventListener('offline', this.onReaderOffline)
    window.addEventListener('resize', this.onReaderViewportChanged)
    window.addEventListener('orientationchange', this.onReaderViewportChanged)
    window.addEventListener('pagehide', this.onPageHide)
    document.addEventListener('visibilitychange', this.onVisibilityChange)
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
    document.removeEventListener('visibilitychange', this.onVisibilityChange)
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
  computed: {
    bookTitle(): string {
      if (this.book && this.series) {
        return getBookTitleCompact(this.book.metadata.title, this.series.metadata.title, this.book.oneshot ? undefined : this.book.metadata.number)
      }
      return 'Tales'
    },
    pageControlsVisible(): boolean {
      return !!this.adapter && !this.hasDisplayError && !this.empty && this.pagesCount > 0
    },
    fixedPageControlsVisible(): boolean {
      return this.manifest?.kind === 'FIXED_PAGE_SEQUENCE' || !!this.mixedContentAdapter?.isCurrentFixedPage()
    },
    textControlsVisible(): boolean {
      return this.manifest?.kind === 'TEXT_REFLOW' || !!this.mixedContentAdapter?.isCurrentText()
    },
    textSearchAvailable(): boolean {
      return this.textControlsVisible && !!this.manifest?.capabilities.search
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
        ...(this.fixedPageControlsVisible ? [{keys: this.$t('shelfview_reader.shortcut_keys_rotate') as string, label: this.$t('shelfview_reader.shortcut_rotate') as string}] : []),
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
    thumbnailStripVisible(): boolean {
      return this.fixedPageControlsVisible && this.thumbnailPageSources.length > 1
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
    imageFilterStyle(): Record<string, string> {
      const style = this.imageFiltersAvailable ? shelfViewImageFilterStyle(this.imageFilterPreset) : shelfViewImageFilterStyle('ORIGINAL')
      style['--sv-brightness'] = roundNumber((Number(style['--sv-brightness']) || 1) * this.fixedPageBrightness, 4).toString()
      style['--sv-contrast'] = roundNumber((Number(style['--sv-contrast']) || 1) * this.fixedPageContrast, 4).toString()
      return style
    },
    imageFilterPresetIcon(): string {
      return shelfViewImageFilterPreset(this.imageFilterPreset).icon
    },
    imageFilterPresetLabel(): string {
      return this.$t(`shelfview_reader.filter_${this.imageFilterPreset.toLowerCase()}`) as string
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
      switch (this.readerLayout) {
        case 'DOUBLE_PAGE':
          return 'mdi-book-open-variant'
        case 'CONTINUOUS_SCROLL':
          return 'mdi-view-sequential'
        default:
          return 'mdi-page-layout-body'
      }
    },
    readerLayoutLabel(): string {
      switch (this.readerLayout) {
        case 'DOUBLE_PAGE':
          return this.$t('shelfview_reader.layout_double') as string
        case 'CONTINUOUS_SCROLL':
          return this.$t('shelfview_reader.layout_continuous') as string
        default:
          return this.$t('shelfview_reader.layout_single') as string
      }
    },
    readingDirectionIcon(): string {
      return this.readingDirection === 'RTL' ? 'mdi-format-textdirection-r-to-l' : 'mdi-format-textdirection-l-to-r'
    },
    readingDirectionLabel(): string {
      return this.readingDirection === 'RTL'
        ? this.$t('shelfview_reader.direction_rtl') as string
        : this.$t('shelfview_reader.direction_ltr') as string
    },
    fixedPageRotationLabel(): string {
      return this.$t('shelfview_reader.rotate_degrees', {degrees: this.fixedPageRotation}) as string
    },
    pageFitIcon(): string {
      switch (this.pageFit) {
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
    pageFitLabel(): string {
      switch (this.pageFit) {
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
        query: this.$route.query,
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
      this.bookmarksDrawerOpen = false
      this.textSearchOpen = false
      this.textSearchQuery = ''
      this.textSearchMatchIndex = 0
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
        this.book = await this.$komgaBooks.getBook(bookId)
        if (requestId !== this.setupRequestId) return

        this.series = await this.$komgaSeries.getOneSeries(this.book.seriesId)
        if (requestId !== this.setupRequestId) return

        this.book.context = this.context
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
    async loadShelfViewState(
      bookId: string,
      preferenceFormats: ShelfViewPreferenceFormat[],
    ): Promise<ShelfViewPositionDto | undefined> {
      try {
        const [savedPosition, bookmarks, preferences] = await Promise.all([
          this.$shelfViewState.getPosition(bookId),
          this.$shelfViewState.getBookmarks(bookId),
          Promise.all(preferenceFormats.map(format =>
            this.$shelfViewState.getPreference({
              scopeType: 'BOOK',
              scopeId: bookId,
              format,
            }),
          )),
        ])

        this.bookmarks = bookmarks
        preferences.forEach(preference => this.applyPreferenceSettings(preference.effectiveSettings, false, preference.format))
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
    cycleReaderLayout() {
      const layouts = this.availableReaderLayouts.length > 0 ? this.availableReaderLayouts : SHELFVIEW_FIXED_PAGE_LAYOUTS
      const currentIndex = layouts.indexOf(this.readerLayout)
      this.readerLayout = layouts[(currentIndex + 1) % layouts.length]
      this.syncAdapterState()
      this.$nextTick(() => this.restoreCurrentTextScrollPosition())
      this.schedulePreferenceSave()
    },
    toggleReadingDirection() {
      if (!this.rtlAvailable) return

      this.readingDirection = this.readingDirection === 'RTL' ? 'LTR' : 'RTL'
      this.syncAdapterState()
      this.schedulePreferenceSave()
    },
    cyclePageFit() {
      const currentIndex = SHELFVIEW_FIXED_PAGE_FITS.indexOf(this.pageFit)
      this.pageFit = SHELFVIEW_FIXED_PAGE_FITS[(currentIndex + 1) % SHELFVIEW_FIXED_PAGE_FITS.length]
      this.schedulePreferenceSave()
    },
    cycleFixedPageRotation() {
      if (!this.fixedPageControlsVisible) return

      this.fixedPageRotation = shelfViewNextFixedPageRotation(this.fixedPageRotation)
      this.$nextTick(() => this.restoreCurrentVisibleContent())
      this.schedulePreferenceSave()
    },
    cycleImageFilterPreset() {
      if (!this.imageFiltersAvailable) return

      this.imageFilterPreset = shelfViewNextImageFilterPreset(this.imageFilterPreset)
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
      this.applyFixedPagePreferenceSettings(FIXED_PAGE_DISPLAY_DEFAULTS)
      this.onFixedPageDisplaySettingChanged()
    },
    onFixedPageDisplaySettingChanged() {
      this.normalizeFixedPageDisplaySettings()
      this.$nextTick(() => this.restoreCurrentVisibleContent())
      this.schedulePreferenceSave()
    },
    cycleTextTheme() {
      this.textTheme = shelfViewNextTextTheme(this.textTheme)
      this.$nextTick(() => this.restoreCurrentTextScrollPosition())
      this.schedulePreferenceSave()
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
      this.applyTextPreferenceSettings(TEXT_DISPLAY_DEFAULTS)
      this.onTextDisplaySettingChanged()
    },
    onTextDisplaySettingChanged() {
      this.normalizeTextDisplaySettings()
      this.$nextTick(() => this.restoreCurrentTextScrollPosition())
      this.schedulePreferenceSave()
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
          this.imageFilterPreset = settings.imageFilterPreset
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
      this.fixedPageScale = this.normalizedFixedPageScale(this.fixedPageScale)
      this.fixedPageGap = Math.round(clampNumber(this.fixedPageGap, 0, 64) / 2) * 2
      this.fixedPageMargin = Math.round(clampNumber(this.fixedPageMargin, 0, 96) / 4) * 4
      if (!SHELFVIEW_FIXED_PAGE_BACKGROUNDS.some(option => option.id === this.fixedPageBackground)) {
        this.fixedPageBackground = FIXED_PAGE_DISPLAY_DEFAULTS.fixedPageBackground
      }
      this.fixedPageRotation = normalizeFixedPageRotation(this.fixedPageRotation)
      this.fixedPageBrightness = roundNumber(clampNumber(this.fixedPageBrightness, 0.8, 1.2), 2)
      this.fixedPageContrast = roundNumber(clampNumber(this.fixedPageContrast, 0.85, 1.35), 2)
    },
    normalizedFixedPageScale(value: number): number {
      return Math.round(clampNumber(value, FIXED_PAGE_SCALE_MIN, FIXED_PAGE_SCALE_MAX) / 5) * 5
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
      if (!SHELFVIEW_TEXT_THEMES.includes(this.textTheme)) this.textTheme = TEXT_DISPLAY_DEFAULTS.textTheme
      if (!SHELFVIEW_TEXT_FONT_FAMILIES.includes(this.textFontFamily)) this.textFontFamily = TEXT_DISPLAY_DEFAULTS.textFontFamily
      if (!SHELFVIEW_TEXT_ALIGNS.includes(this.textAlign)) this.textAlign = TEXT_DISPLAY_DEFAULTS.textAlign
      this.textFontSize = Math.round(clampNumber(this.textFontSize, 14, 32))
      this.textLineHeight = roundNumber(clampNumber(this.textLineHeight, 1.35, 2.4), 2)
      this.textLetterSpacing = roundNumber(clampNumber(this.textLetterSpacing, 0, 2.5), 1)
      this.textWordSpacing = roundNumber(clampNumber(this.textWordSpacing, 0, 8), 1)
      this.textContentWidth = Math.round(clampNumber(this.textContentWidth, 520, 1180))
      this.textPageMargin = Math.round(clampNumber(this.textPageMargin, 16, 120))
      this.textParagraphSpacing = roundNumber(clampNumber(this.textParagraphSpacing, 0.6, 2.6), 2)
      this.textFontWeight = Math.round(clampNumber(this.textFontWeight, 300, 700) / 50) * 50
      this.textBrightness = roundNumber(clampNumber(this.textBrightness, 0.8, 1.2), 2)
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
    textParagraphClass(paragraphIndex: number): object {
      return {
        'shelfview-reader-shell__text-paragraph--search-current': this.currentTextSearchMatch?.paragraphIndex === paragraphIndex,
      }
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
        this.stopSlideshow()
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
    closeShortcutHelp() {
      this.showShortcutHelp = false
      this.$nextTick(() => this.focusReaderStage())
    },
    closeBookmarksDrawer() {
      this.bookmarksDrawerOpen = false
      this.$nextTick(() => this.focusReaderStage())
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
        this.setReaderStateStatus('SAVED', this.$t('shelfview_reader.status_bookmark_deleted') as string)
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

      const action = shelfViewReaderKeyboardAction(e.key, this.readingDirection)
      if (!action) return

      e.preventDefault()
      await this.runReaderInputAction(action)
    },
    onReaderWheel(e: WheelEvent) {
      if (!this.fixedPageControlsVisible || this.hasDisplayError || this.empty) return
      if (this.readerLayout === 'CONTINUOUS_SCROLL' && !e.ctrlKey && !e.metaKey) return
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
      const stageX = e.clientX - rect.left
      const stageY = e.clientY - rect.top
      const contentX = readerStage.scrollLeft + stageX
      const contentY = readerStage.scrollTop + stageY

      this.fixedPageScale = nextScale
      this.$nextTick(() => {
        const scaleRatio = this.fixedPageScale / previousScale
        readerStage.scrollLeft = Math.max(0, (contentX * scaleRatio) - stageX)
        readerStage.scrollTop = Math.max(0, (contentY * scaleRatio) - stageY)
      })
      this.schedulePreferenceSave()
    },
    onReaderPointerDown(e: PointerEvent) {
      if (!this.fixedPageControlsVisible || e.pointerType !== 'mouse' || e.button !== 0) return
      if (shelfViewShouldIgnoreShortcutTarget(e.target)) return

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
      if (!this.fixedPagePanStart || e.pointerId !== this.fixedPagePanStart.pointerId) return

      const readerStage = this.$refs.readerStage as HTMLElement | undefined
      if (!readerStage) return

      if ((e.buttons & 1) !== 1) {
        this.finishFixedPagePan(e)
        return
      }

      const deltaX = e.clientX - this.fixedPagePanStart.x
      const deltaY = e.clientY - this.fixedPagePanStart.y
      if (!this.fixedPagePanning && Math.abs(deltaX) + Math.abs(deltaY) > 3) {
        this.fixedPagePanning = true
      }

      if (this.fixedPagePanning) {
        readerStage.scrollLeft = Math.max(0, this.fixedPagePanStart.scrollLeft - deltaX)
        readerStage.scrollTop = Math.max(0, this.fixedPagePanStart.scrollTop - deltaY)
      }
      if (e.cancelable) e.preventDefault()
    },
    onReaderPointerUp(e: PointerEvent) {
      this.finishFixedPagePan(e)
    },
    onReaderPointerCancel(e: PointerEvent) {
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

      const action = shelfViewTouchAction(
        this.touchStart,
        {x: touch.clientX, y: touch.clientY},
        this.readingDirection,
      )
      this.touchStart = undefined

      if (!action) return

      if (e.cancelable) e.preventDefault()
      await this.runReaderInputAction(action)
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
      const offsetRatio = rect.height > 0
        ? clampNumber((stageTop - rect.top) / rect.height, 0, 1)
        : 0

      return {
        ...currentTextLocator,
        paragraph,
        characterOffset: Math.floor(textLength * offsetRatio),
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
      const offsetRatio = paragraphLength > 0
        ? clampNumber(currentTextLocator.characterOffset / paragraphLength, 0, 1)
        : 0

      readerStage.scrollTop = Math.max(0, paragraph.offsetTop + (paragraph.offsetHeight * offsetRatio))
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
  top: 48px;
  z-index: 2;
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
  padding: 56px 16px 16px;
  width: 100%;
}

.shelfview-reader-shell--search-open .shelfview-reader-shell__stage {
  padding-top: 104px;
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
  padding: 56px var(--sv-fixed-page-margin, 16px) var(--sv-fixed-page-margin, 16px);
}

.shelfview-reader-shell--search-open .shelfview-reader-shell__stage--fixed {
  padding-top: 104px;
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

.shelfview-reader-shell__pages--continuous .shelfview-reader-shell__page--fit-screen,
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

.shelfview-reader-shell__text-paragraph--search-current {
  background: rgba(255, 193, 7, 0.24);
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

.shelfview-reader-shell__bookmarks-header {
  align-items: center;
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
}

.shelfview-reader-shell__bookmarks-header h2 {
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
  align-items: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
  display: flex;
  gap: 8px;
  min-height: 44px;
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

.shelfview-reader-shell__shortcut-dialog {
  align-items: center;
  background: rgba(0, 0, 0, 0.72);
  display: flex;
  inset: 0;
  justify-content: center;
  padding: 16px;
  position: fixed;
  z-index: 4;
}

.shelfview-reader-shell__shortcut-panel {
  background: #202020;
  border-radius: 8px;
  color: #f5f5f5;
  max-width: 420px;
  padding: 20px;
  width: 100%;
}

.shelfview-reader-shell__shortcut-panel h2 {
  font-size: 20px;
  font-weight: 500;
  margin: 0 0 16px;
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
    max-width: 62vw;
  }

  .shelfview-reader-shell__stage {
    padding: 56px 8px 8px;
  }

  .shelfview-reader-shell__stage--fixed {
    padding: 56px var(--sv-fixed-page-margin, 8px) var(--sv-fixed-page-margin, 8px);
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
