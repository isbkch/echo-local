package com.isbkch.echolocal.domain

data class KokoroVoice(
    val id: String,
    val name: String,
    val character: String,
    val region: String,
)

object VoiceCatalog {
    val curated: List<KokoroVoice> = listOf(
        KokoroVoice("af_heart", "Heart", "Balanced and intimate", "American"),
        KokoroVoice("af_bella", "Bella", "Warm and expressive", "American"),
        KokoroVoice("am_michael", "Michael", "Clear and grounded", "American"),
        KokoroVoice("bf_emma", "Emma", "Polished and composed", "British"),
        KokoroVoice("bm_george", "George", "Measured and articulate", "British"),
    )

    fun voice(id: String): KokoroVoice = curated.firstOrNull { it.id == id } ?: curated.first()
}
