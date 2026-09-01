package com.isbkch.echolocal.model

import okhttp3.HttpUrl.Companion.toHttpUrl

object KokoroModelManifest {
    private const val REVISION = "2895b2025f1046fad6b51f8773debc3da8ba05df"

    val current = ModelManifest(
        version = 1,
        revision = REVISION,
        assets = listOf(
            asset("kokoro-e2e-realtime.onnx", 2_880_477, "52e0206f7cfdd2c622dd782de62215cba79057b6a29f02185906b3f497f1978b"),
            asset("kokoro-e2e.onnx.data", 162_095_104, "272bbac32e99c25a7fc59131371e94907b4ecbaa1b4f67a66fcfb2825e6f927e"),
            asset("vocab_index.json", 2_501, "d392acfec384f050de86c7ac4ab56833cf56a6268315f4d6aeb350e9627404b8"),
            asset("us_gold.json", 3_000_469, "dc414872a49a28ae6c141463d502fd945f3b2fde040484fdc47d00cc4612686f"),
            asset("us_silver.json", 3_099_517, "de8f67be911bb6c659187b4a65fd966b6a30e56350e0f790d763210b053ac475"),
            asset("dict_fr.json", 51_497, "445e45ae84d8a779d22b4df3b90e169ee6e737856765a812abfbbfddd08b4488"),
            asset("dict_es.json", 5_077, "0011eaaccbffb825d6d3c48543ee176053ee224036e09873170c0b89be2faea4"),
            asset("dict_it.json", 3_926, "9e920c5e24b9aea14642b0b53e2788c69c0f36b492f408bcbe586233dfb3a316"),
            asset("dict_pt.json", 37_438, "8c4f5ac84fbf822e8558ac28c4291dbceb754aadc0fad3f6124b528512129ea1"),
            asset("dict_hi.json", 3_983, "6cb736986de8966f87779e9d89438f07a9c0c935a4a975be080b87a251d37a6f"),
            asset("voices/af_heart.bin", 1_024, "3bc1d7444ad012b5c78f3fe7546e7bfcc505a487debf24c799cf97ecf34dc191"),
            asset("voices/af_bella.bin", 1_024, "64e11e8e3f5aa126915f109cecaef1fc9e2e3abd9ae545cd395eec2774509ef7"),
            asset("voices/am_michael.bin", 1_024, "f7bbad342014cea4183055d31b08912bc45042673b4c478d87f00421f3534883"),
            asset("voices/bf_emma.bin", 1_024, "857b4c91068f78bfd6ef609953ee9c09e2ba35f09589d961efd55b2895f6372c"),
            asset("voices/bm_george.bin", 1_024, "3644b5ee56b6ee914dde17e9e8662ddc486fa23efb83773be135640b88d9820f"),
        ),
    )

    private fun asset(relativePath: String, sizeBytes: Long, sha256: String): ModelAsset = ModelAsset(
        relativePath = relativePath,
        sizeBytes = sizeBytes,
        sha256 = sha256,
        url = "https://huggingface.co/soniqo/Kokoro-82M-ONNX/resolve/$REVISION/$relativePath".toHttpUrl(),
    )
}
