package com.fedorov.fileioshare.view

import moxy.MvpView
import moxy.viewstate.strategy.alias.OneExecution

@OneExecution
interface MainView : MvpView {
    fun handleIntent()
    fun finishActivity()
}