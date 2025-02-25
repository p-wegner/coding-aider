package de.andrena.codingaider.features.testgeneration

data class TestTypeConfiguration(
    var name: String = "",
    var promptTemplate: String = "",
    var referenceFilePattern: String = "",
    var testFilePattern: String = "*Test.kt",
    var isEnabled: Boolean = true,
    var contextFiles: List<String> = listOf()
)