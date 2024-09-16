package de.andrena.codingaider.experimental

import java.io.*
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ShellLikeProcessHandler(vararg command: String?) {
    private val process: Process
    private val reader: BufferedReader
    private val writer: BufferedWriter
    private val executorService: ExecutorService

    init {
        val pb = ProcessBuilder(*command)
        pb.redirectErrorStream(true)


        // Set up environment variables to emulate a shell
        val env = pb.environment()
        env["TERM"] = "xterm-256color" // Support for 256 colors
        env["COLORTERM"] = "truecolor" // Support for true color (if available)
        env["CLICOLOR"] = "1" // Enable color output for ls and other commands
        env["CLICOLOR_FORCE"] = "1" // Force color output even for pipes and redirects

        process = pb.start()
        reader = BufferedReader(InputStreamReader(process.inputStream))
        writer = BufferedWriter(OutputStreamWriter(process.outputStream))
        executorService = Executors.newSingleThreadExecutor()
    }

    fun start() {
        executorService.submit { this.readOutput() }
    }

    private fun readOutput() {
        try {
            var c: Int
            val line = StringBuilder()
            while ((reader.read().also { c = it }) != -1) {
                print(c.toChar())
                line.append(c.toChar())
                if (c == '\n'.code) {
                    handleLine(line.toString().trim { it <= ' ' })
                    line.setLength(0)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @Throws(IOException::class)
    private fun handleLine(line: String) {
        if (line.endsWith("(y/n)")) {
            val response = getUserInput("Enter y or n: ")
            sendInput(response)
        } else if (line.startsWith(">")) {
            val userInput = getUserInput("Your input: ")
            sendInput(userInput)
        }
    }

    @Throws(IOException::class)
    fun sendInput(input: String) {
        writer.write(input)
        writer.newLine()
        writer.flush()
    }

    private fun getUserInput(prompt: String): String {
        print(prompt)
        return Scanner(System.`in`).nextLine()
    }

    fun stop() {
        try {
            process.destroy()
            reader.close()
            writer.close()
            executorService.shutdown()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    companion object {
        @Throws(IOException::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val handler = ShellLikeProcessHandler("aider")
            handler.start()

            try {
                Thread.sleep(Long.MAX_VALUE)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            } finally {
                handler.stop()
            }
        }
    }
}