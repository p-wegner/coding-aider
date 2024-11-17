package de.andrena.codingaider.services

import reactor.core.publisher.FluxSink

interface AiderOutputParser {
    fun writeCommandAndReadResults(command: String, sink: FluxSink<String>)
}