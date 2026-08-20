package org.tales.shelfview.interfaces.api

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.web.server.ResponseStatusException
import java.nio.ByteBuffer
import java.nio.channels.ClosedChannelException
import java.nio.channels.NonWritableChannelException
import java.nio.channels.SeekableByteChannel

class ShelfViewTextContentProviderTest {
  @Test
  fun `given file exactly at byte limit when reading then returns every byte`() {
    val channel = TestChannel(ByteArray(4) { it.toByte() }, declaredSize = 4)

    val bytes = readShelfViewTextBytes(channel, 4)

    assertThat(bytes).containsExactly(0, 1, 2, 3)
  }

  @Test
  fun `given declared file size over limit when reading then rejects before a read`() {
    val channel = TestChannel(ByteArray(5), declaredSize = 5)

    assertThatThrownBy { readShelfViewTextBytes(channel, 4) }
      .isInstanceOf(ResponseStatusException::class.java)
      .hasMessageContaining("SHELFVIEW_TEXT_TOO_LARGE")
    assertThat(channel.readCount).isZero()
  }

  @Test
  fun `given file grows after size query when reading then consumes only limit plus one`() {
    val channel = TestChannel(ByteArray(5), declaredSize = 1)

    assertThatThrownBy { readShelfViewTextBytes(channel, 4) }
      .isInstanceOf(ResponseStatusException::class.java)
      .hasMessageContaining("SHELFVIEW_TEXT_TOO_LARGE")
    assertThat(channel.bytesRead).isEqualTo(5)
  }

  private class TestChannel(
    private val bytes: ByteArray,
    private val declaredSize: Long,
  ) : SeekableByteChannel {
    private var open = true
    private var offset = 0
    var readCount = 0
      private set
    var bytesRead = 0
      private set

    override fun read(destination: ByteBuffer): Int {
      if (!open) throw ClosedChannelException()
      readCount += 1
      if (offset >= bytes.size) return -1
      val count = minOf(destination.remaining(), bytes.size - offset)
      destination.put(bytes, offset, count)
      offset += count
      bytesRead += count
      return count
    }

    override fun write(source: ByteBuffer): Int = throw NonWritableChannelException()

    override fun position(): Long = offset.toLong()

    override fun position(newPosition: Long): SeekableByteChannel {
      offset = newPosition.toInt()
      return this
    }

    override fun size(): Long = declaredSize

    override fun truncate(size: Long): SeekableByteChannel = throw NonWritableChannelException()

    override fun isOpen(): Boolean = open

    override fun close() {
      open = false
    }
  }
}
