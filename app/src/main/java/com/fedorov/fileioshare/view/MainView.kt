package com.fedorov.fileioshare.view

import moxy.MvpView
import moxy.viewstate.strategy.alias.OneExecution
import java.io.File

@OneExecution
interface MainView : MvpView {
    fun handleIntent()
    fun finishActivity()
}