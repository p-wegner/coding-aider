package de.andrena.codingaider.services.sidecar

import reactor.core.publisher.FluxSink

interface AiderOutputParser {
    fun writeCommandAndReadResults(command: String, sink: FluxSink<String>)
}