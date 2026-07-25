import {shallowMount} from '@vue/test-utils'
import ShelfViewLocalReader from '@/shelfview/views/ShelfViewLocalReader.vue'
import {ShelfViewLocalArchiveError, openShelfViewLocalArchiveFile} from '@/shelfview/functions/local-archive'

jest.mock('@/shelfview/functions/local-archive', () => ({
  ...jest.requireActual('@/shelfview/functions/local-archive'),
  openShelfViewLocalArchiveFile: jest.fn(),
}))

const wait = (ms: number) => new Promise(resolve => setTimeout(resolve, ms))
const flushPromises = () => wait(0)

describe('ShelfViewLocalReader', () => {
  const openLocalArchive = openShelfViewLocalArchiveFile as jest.Mock

  beforeEach(() => {
    jest.clearAllMocks()
  })

  it('opens a browser-local archive and pages through images', async () => {
    const close = jest.fn()
    openLocalArchive.mockResolvedValue({
      title: 'Local Book',
      pages: [
        {index: 0, name: 'page-1.jpg', url: 'blob:page-1', size: 1, mimeType: 'image/jpeg'},
        {index: 1, name: 'page-2.jpg', url: 'blob:page-2', size: 1, mimeType: 'image/jpeg'},
      ],
      close,
    })
    const wrapper = mountLocalReader()

    await (wrapper.vm as any).openFile(file('book.cbz'))
    await flushPromises()
    await wrapper.vm.$nextTick()

    expect(openShelfViewLocalArchiveFile).toHaveBeenCalledWith(expect.objectContaining({name: 'book.cbz'}))
    expect(wrapper.find('[data-testid="shelfview-local-reader-title"]').text()).toBe('Local Book')
    expect(wrapper.find('[data-testid="shelfview-local-reader-counter"]').text()).toBe('1 / 2')
    expect(wrapper.find('[data-testid="shelfview-local-reader-page"]').attributes('src')).toBe('blob:page-1')

    const viewModel = wrapper.vm as any

    viewModel.nextPage()
    await wrapper.vm.$nextTick()
    expect(wrapper.find('[data-testid="shelfview-local-reader-counter"]').text()).toBe('2 / 2')
    expect(wrapper.find('[data-testid="shelfview-local-reader-page"]').attributes('src')).toBe('blob:page-2')

    viewModel.previousPage()
    await wrapper.vm.$nextTick()
    expect(wrapper.find('[data-testid="shelfview-local-reader-counter"]').text()).toBe('1 / 2')

    viewModel.clearArchive()
    await wrapper.vm.$nextTick()
    expect(close).toHaveBeenCalledTimes(1)
    expect(wrapper.find('[data-testid="shelfview-local-reader-page"]').exists()).toBe(false)
  })

  it('shows a translated error instead of the raw archive message', async () => {
    openLocalArchive.mockRejectedValue(new Error('bad archive'))
    const wrapper = mountLocalReader()

    await (wrapper.vm as any).openFile(file('broken.cbz'))
    await flushPromises()
    await wrapper.vm.$nextTick()

    const error = wrapper.find('[data-testid="shelfview-local-reader-error"]')
    expect(error.text()).toContain('shelfview_local_reader.error_open')
    expect(error.text()).not.toContain('bad archive')
    expect(wrapper.find('[data-testid="shelfview-local-reader-page"]').exists()).toBe(false)
  })

  it('explains why a local archive was rejected using its error code', async () => {
    openLocalArchive.mockRejectedValue(
      new ShelfViewLocalArchiveError('NO_SUPPORTED_PAGES', 'The archive does not contain supported image pages.'),
    )
    const wrapper = mountLocalReader()

    await (wrapper.vm as any).openFile(file('text-only.cbz'))
    await flushPromises()
    await wrapper.vm.$nextTick()

    const error = wrapper.find('[data-testid="shelfview-local-reader-error"]')
    expect(error.text()).toContain('shelfview_local_reader.error_no_pages')
    expect(error.text()).not.toContain('supported image pages')
  })

  it('cleans up the opened archive on destroy', async () => {
    const close = jest.fn()
    openLocalArchive.mockResolvedValue({
      title: 'Local Book',
      pages: [
        {index: 0, name: 'page-1.jpg', url: 'blob:page-1', size: 1, mimeType: 'image/jpeg'},
      ],
      close,
    })
    const wrapper = mountLocalReader()

    await (wrapper.vm as any).openFile(file('book.cbz'))
    await flushPromises()

    wrapper.destroy()
    expect(close).toHaveBeenCalledTimes(1)
  })
})

function mountLocalReader() {
  return shallowMount(ShelfViewLocalReader, {
    mocks: {
      $t: (key: string) => key,
    },
    stubs: [
      'v-alert',
      'v-btn',
      'v-icon',
      'v-progress-circular',
      'v-spacer',
      'v-toolbar',
    ],
  })
}

function file(name: string): File {
  return new File(['zip'], name, {type: 'application/zip'})
}
