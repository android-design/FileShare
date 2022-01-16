package com.fedorov.fileioshare.utils

import kotlinx.coroutines.Dispatchers

class DispatcherProvider {
    val io = Dispatchers.IO
    val background = Dispatchers.Default
    val foreground = Dispatchers.Main
}
