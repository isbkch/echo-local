package com.isbkch.echolocal.domain

import java.text.BreakIterator
import java.util.Locale

object ParagraphPlanner {
    fun segments(
        source: String,
        paragraphPauseSeconds: Double,
        maximumCharacters: Int = 700,
    ): List<SpeechSegment> {
        val boundedMaximum = maximumCharacters.coerceAtLeast(1)
        val paragraphs = source
            .lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toList()

        val output = buildList {
            paragraphs.forEach { paragraph ->
                val pieces = sentencePieces(paragraph, boundedMaximum)
                pieces.forEachIndexed { index, piece ->
                    add(
                        SpeechSegment(
                            text = piece,
                            pauseAfterSeconds = if (index == pieces.lastIndex) paragraphPauseSeconds else 0.12,
                        ),
                    )
                }
            }
        }

        return if (output.isEmpty()) output else output.dropLast(1) + output.last().copy(pauseAfterSeconds = 0.0)
    }

    private fun sentencePieces(paragraph: String, maximumCharacters: Int): List<String> {
        val iterator = BreakIterator.getSentenceInstance(Locale.ENGLISH).apply { setText(paragraph) }
        val sentences = buildList {
            var start = iterator.first()
            var end = iterator.next()
            while (end != BreakIterator.DONE) {
                paragraph.substring(start, end).trim().takeIf(String::isNotEmpty)?.let(::add)
                start = end
                end = iterator.next()
            }
        }.ifEmpty { listOf(paragraph) }

        val pieces = mutableListOf<String>()
        var current = ""
        sentences.flatMap { splitOversized(it, maximumCharacters) }.forEach { sentence ->
            val candidate = if (current.isEmpty()) sentence else "$current $sentence"
            if (candidate.length <= maximumCharacters) {
                current = candidate
            } else {
                if (current.isNotEmpty()) pieces += current
                current = sentence
            }
        }
        if (current.isNotEmpty()) pieces += current
        return pieces
    }

    private fun splitOversized(text: String, maximumCharacters: Int): List<String> {
        if (text.length <= maximumCharacters) return listOf(text)

        val pieces = mutableListOf<String>()
        var current = ""
        text.split(Regex("\\s+")).filter(String::isNotEmpty).forEach { oversizedWord ->
            oversizedWord.chunked(maximumCharacters).forEach { word ->
                val candidate = if (current.isEmpty()) word else "$current $word"
                if (candidate.length <= maximumCharacters) {
                    current = candidate
                } else {
                    if (current.isNotEmpty()) pieces += current
                    current = word
                }
            }
        }
        if (current.isNotEmpty()) pieces += current
        return pieces
    }
}
