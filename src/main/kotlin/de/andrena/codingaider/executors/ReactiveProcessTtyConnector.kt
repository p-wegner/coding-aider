package de.andrena.codingaider.executors

import com.jediterm.terminal.TtyConnector
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.concurrent.atomic.AtomicBoolean

class ReactiveProcessTtyConnector(private val process: Process, private val charset: Charset) : TtyConnector {
    private val input: InputStream = process.inputStream
    private val output: OutputStream = process.outputStream
    private val isConnected = AtomicBoolean(true)

    override fun close() {
        isConnected.set(false)
        process.destroy()
    }

    override fun getName(): String {
        return "Aider"
    }

    @Throws(IOException::class)
    override fun read(buf: CharArray, offset: Int, length: Int): Int {
        val bytes = ByteArray(length)
        val len = input.read(bytes, 0, length)
        if (len == -1) {
            return -1
        }
        val s = String(bytes, 0, len, charset)
        s.toCharArray(buf, offset, 0, s.length)
        return s.length
    }

    @Throws(IOException::class)
    override fun write(bytes: ByteArray) {
        output.write(bytes)
        output.flush()
    }

    override fun isConnected(): Boolean {
        return isConnected.get() && process.isAlive
    }

    @Throws(IOException::class)
    override fun write(string: String) {
        write(string.toByteArray(charset))
    }

    @Throws(InterruptedException::class)
    override fun waitFor(): Int {
        return process.waitFor()
    }

    @Throws(IOException::class)
    override fun ready(): Boolean {
        return input.available() > 0
    }

    fun read(): Int {
        return input.read()
    }
}
