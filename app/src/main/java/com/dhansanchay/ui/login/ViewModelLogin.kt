package com.dhansanchay.ui.login

import android.util.Log
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ViewModelLogin @Inject constructor() : ViewModel() {

    private val tag = this.javaClass.simpleName

    init {
        Log.d(tag, "In $tag init")
    }

}