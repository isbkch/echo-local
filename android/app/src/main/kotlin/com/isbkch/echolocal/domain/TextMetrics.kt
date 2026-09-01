package com.isbkch.echolocal.domain

data class TextMetrics(
    val wordCount: Int,
    val characterCount: Int,
    val estimatedDurationSeconds: Double,
) {
    companion object {
        private const val WORDS_PER_MINUTE = 155.0

        fun from(source: String, paragraphPauseSeconds: Double = 0.42): TextMetrics {
            val words = source.trim().takeIf(String::isNotEmpty)?.split(Regex("\\s+")) ?: emptyList()
            val paragraphCount = source.lineSequence().count { it.isNotBlank() }
            val speakingSeconds = words.size * 60.0 / WORDS_PER_MINUTE
            val pauseSeconds = (paragraphCount - 1).coerceAtLeast(0) * paragraphPauseSeconds

            return TextMetrics(
                wordCount = words.size,
                characterCount = source.length,
                estimatedDurationSeconds = speakingSeconds + pauseSeconds,
            )
        }
    }
}
