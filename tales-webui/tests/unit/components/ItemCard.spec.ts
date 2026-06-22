import {shallowMount} from '@vue/test-utils'
import ItemCard from '@/components/ItemCard.vue'
import {BookDto} from '@/types/tales-books'

jest.mock('@/i18n', () => ({
  __esModule: true,
  default: {
    locale: 'en',
    t: (key: string) => key,
    tc: (key: string, count: number) => `${key}:${count}`,
  },
}))

const readyBook = (overrides: Partial<BookDto> = {}): BookDto => ({
  id: 'book-1',
  seriesId: 'series-1',
  libraryId: 'library-1',
  name: 'Book One',
  url: '',
  created: new Date('2026-06-22T00:00:00Z'),
  lastModified: new Date('2026-06-22T00:00:00Z'),
  fileLastModified: new Date('2026-06-22T00:00:00Z'),
  sizeBytes: 100,
  size: '100 B',
  number: 1,
  oneshot: false,
  deleted: false,
  media: {
    status: 'READY',
    mediaType: 'application/zip',
    mediaProfile: 'DIVINA',
    pagesCount: 12,
    comment: '',
    epubDivinaCompatible: false,
    epubIsKepub: false,
  },
  metadata: {
    title: 'Book One',
    titleLock: false,
    summary: '',
    summaryLock: false,
    number: '1',
    numberLock: false,
    numberSort: 1,
    numberSortLock: false,
    releaseDate: '',
    releaseDateLock: false,
    authors: [],
    authorsLock: false,
    tags: [],
    tagsLock: false,
    isbn: '',
    isbnLock: false,
    links: [],
    linksLock: false,
    created: '',
    lastModified: '',
  },
  readProgress: undefined,
  context: undefined,
  ...overrides,
} as unknown as BookDto)

describe('ItemCard', () => {
  const originalReaderFlag = process.env.VUE_APP_SHELFVIEW_READER

  afterEach(() => {
    process.env.VUE_APP_SHELFVIEW_READER = originalReaderFlag
  })

  it('routes ready readable book cards to the reader', () => {
    process.env.VUE_APP_SHELFVIEW_READER = 'true'
    const push = jest.fn()
    const wrapper = mountItemCard(readyBook(), push)
    const vm = wrapper.vm as any

    vm.goto()

    expect(push).toHaveBeenCalledWith({
      name: 'read-book-v2',
      params: {bookId: 'book-1'},
      query: {context: undefined, contextId: undefined},
    })
  })

  it('keeps unsupported or unavailable book cards on the detail page', () => {
    const push = jest.fn()
    const wrapper = mountItemCard(readyBook({
      media: {
        ...readyBook().media,
        status: 'ERROR',
      },
    }), push)
    const vm = wrapper.vm as any

    vm.goto()

    expect(push).toHaveBeenCalledWith({
      name: 'browse-book',
      params: {bookId: 'book-1'},
      query: {context: undefined, contextId: undefined},
    })
  })
})

function mountItemCard(
  item: BookDto,
  push: jest.Mock,
) {
  return shallowMount(ItemCard, {
    propsData: {item},
    directives: {
      'line-clamp': jest.fn(),
    },
    mocks: {
      $router: {push},
      $store: {
        getters: {
          getClientSettings: {},
          getLibraryById: () => ({unavailable: false}),
          mePageStreaming: true,
        },
      },
      $eventHub: {
        $on: jest.fn(),
        $off: jest.fn(),
      },
      $t: (key: string) => key,
      $_: {random: jest.fn(() => 1)},
      $vuetify: {rtl: false},
    },
    stubs: {
      'v-hover': {template: '<div><slot :hover="false"></slot></div>'},
      'v-card': {template: '<div v-bind="$attrs" v-on="$listeners"><slot /></div>'},
      'v-img': {template: '<div><slot /></div>'},
      'v-tooltip': {template: '<div><slot name="activator" :on="{}" /><slot /></div>'},
      'v-chip': {template: '<span><slot /></span>'},
      'v-overlay': {template: '<div><slot /></div>'},
      'v-fade-transition': {template: '<div><slot /></div>'},
      'v-progress-linear': {template: '<div />'},
      'v-btn': {template: '<button v-bind="$attrs" v-on="$listeners"><slot /></button>'},
      'v-icon': {template: '<i><slot /></i>'},
      'v-card-subtitle': {template: '<div><slot /></div>'},
      'v-card-text': {template: '<div><slot /></div>'},
      'router-link': {template: '<a><slot /></a>'},
    },
  })
}
