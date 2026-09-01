package com.isbkch.echolocal.domain

object ParagraphPlanner {
    fun segments(source: String, paragraphPauseSeconds: Double): List<SpeechSegment> {
        val paragraphs = source
            .lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toList()

        return paragraphs.mapIndexed { index, paragraph ->
            SpeechSegment(
                text = paragraph,
                pauseAfterSeconds = if (index == paragraphs.lastIndex) 0.0 else paragraphPauseSeconds,
            )
        }
    }
}
