package com.fedorov.fileioshare.view

import moxy.MvpView
import moxy.viewstate.strategy.alias.OneExecution
import java.io.File

@OneExecution
interface MainView : MvpView {
    fun showError(errorMsg: String)
    fun showMsg(msg: String)
    fun startForegroundService(file: File)
    fun handleIntent()
    fun finishActivity()
}