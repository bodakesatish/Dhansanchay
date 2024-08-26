package com.dhansanchay.ui.login

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhansanchay.domain.model.ResponseCode
import com.dhansanchay.domain.model.response.SchemeModel
import com.dhansanchay.domain.usecases.SchemeListCountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ViewModelLogin @Inject constructor(
//    private val loginUseCase: LoginUseCase,
    private val schemeListUseCase: SchemeListCountUseCase
) : ViewModel() {

    private val tag = this.javaClass.simpleName
    val schemeResponse = MutableLiveData<List<SchemeModel>>()

    init {
        Log.d(tag, "In $tag init")
    }

    fun login() {
        Log.d(tag, "In $tag login")
    }

    fun getSchemeList() {
        Log.d(tag, "In $tag getSchemeList")
        viewModelScope.launch(Dispatchers.IO) {
            val response = schemeListUseCase.executeUseCase(SchemeListCountUseCase.Request())
            viewModelScope.launch(Dispatchers.Main) {
              when ( response.getResponseCode() ) {
                  is ResponseCode.Success -> {
                      Log.d(tag, "In $tag getSchemeList SUCCESS")
                      Log.d(tag, "In $tag Response -> ${response.getData()}")
                  }
                  else -> {
                      Log.d(tag, "In $tag getSchemeList ERROR")
                  }
              }
            }
        }
    }

}