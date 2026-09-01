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
    fun metricsUseTheFixedAppleBaselineAndParagraphPause() {
        val firstParagraph = List(155) { "word" }.joinToString(" ")
        val metrics = TextMetrics.from("$firstParagraph\n\nSecond", paragraphPauseSeconds = 0.42)

        assertEquals(156, metrics.wordCount)
        assertEquals(60.0 + (60.0 / 155.0) + 0.42, metrics.estimatedDurationSeconds, 0.001)
    }
}
