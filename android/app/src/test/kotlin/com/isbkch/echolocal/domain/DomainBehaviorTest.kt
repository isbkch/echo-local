package com.isbkch.echolocal.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainBehaviorTest {
    @Test
    fun curatedVoicesMatchTheAppleProduct() {
        assertEquals(
            listOf("af_heart", "af_bella", "am_michael", "bf_emma", "bm_george"),
            VoiceCatalog.curated.map { it.id },
        )
        assertEquals(
            listOf("Heart", "Bella", "Michael", "Emma", "George"),
            VoiceCatalog.curated.map { it.name },
        )
    }

    @Test
    fun unknownVoiceFallsBackToHeart() {
        assertSame(VoiceCatalog.curated.first(), VoiceCatalog.voice("not-a-voice"))
    }

    @Test
    fun paragraphsReceivePauseExceptForTheFinalSegment() {
        val result = ParagraphPlanner.segments("First.\n\n Second. ", 0.55)

        assertEquals(listOf("First.", "Second."), result.map { it.text })
        assertEquals(listOf(0.55, 0.0), result.map { it.pauseAfterSeconds })
    }

    @Test
    fun blankTextProducesNoSegments() {
        assertTrue(ParagraphPlanner.segments(" \n \n", 0.42).isEmpty())
    }

    @Test
    fun oversizedParagraphIsBoundedWithoutDroppingWords() {
        val source = List(220) { "carefully" }.joinToString(" ")

        val result = ParagraphPlanner.segments(source, 0.42, maximumCharacters = 180)

        assertTrue(result.size > 1)
        assertTrue(result.all { it.text.length <= 180 })
        assertEquals(source.split(Regex("\\s+")), result.flatMap { it.text.split(Regex("\\s+")) })
        assertTrue(result.dropLast(1).all { it.pauseAfterSeconds == 0.12 })
        assertEquals(0.0, result.last().pauseAfterSeconds, 0.001)
    }

    @Test
    fun oversizedSingleWordIsStillBounded() {
        val source = "a".repeat(401)

        val result = ParagraphPlanner.segments(source, 0.42, maximumCharacters = 100)

        assertEquals(source, result.joinToString(separator = "") { it.text })
        assertTrue(result.all { it.text.length <= 100 })
    }

    @Test
    fun metricsUseTheFixedAppleBaselineAndParagraphPause() {
        val firstParagraph = List(155) { "word" }.joinToString(" ")
        val metrics = TextMetrics.from("$firstParagraph\n\nSecond", paragraphPauseSeconds = 0.42)

        assertEquals(156, metrics.wordCount)
        assertEquals(60.0 + (60.0 / 155.0) + 0.42, metrics.estimatedDurationSeconds, 0.001)
    }
}
