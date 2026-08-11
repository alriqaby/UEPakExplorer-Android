package com.example.uepakexplorer

object NativePak {
    init { System.loadLibrary("uepak") }
    external fun openPak(fd: Int, aesKeyHex: String?): String
    external fun closePak()
    external fun search(query: String, extension: String?): String
    external fun extract(path: String, outputFd: Int): String
}
