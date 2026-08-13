package com.example.uepakexplorer

object NativePak {
    init { System.loadLibrary("uepak") }

    external fun openPak(
        fd: Int,
        aesKeyHex: String?
    ): String

    external fun closePak()

    external fun search(
        query: String,
        extension: String?
    ): String

    /*
     * Legacy single-file API.
     *
     * Kept for compatibility with existing callers.
     */
    external fun extract(
        path: String,
        outputFd: Int
    ): String

    /*
     * Safe extraction APIs.
     *
     * Rust writes only to normal app-private filesystem paths.
     * Android SAF is handled afterwards by Kotlin.
     */
    external fun extractToPath(
        path: String,
        outputPath: String
    ): String

    external fun extractBatch(
        pathsJson: String,
        outputRoot: String
    ): String
}
