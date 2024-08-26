package com.dhansanchay.ui.scheme

import android.support.v4.os.IResultReceiver._Parcel
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhansanchay.domain.model.ResponseCode
import com.dhansanchay.domain.model.response.SchemeModel
import com.dhansanchay.domain.usecases.SchemeListUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ViewModelSchemeList @Inject constructor(
    private val getSchemeListUseCase: SchemeListUseCase,
) : ViewModel() {

    private val tag = this.javaClass.simpleName

    private val _schemeResponse = MutableLiveData<List<SchemeModel>>()
    val schemeResponse : LiveData<List<SchemeModel>> = _schemeResponse

    init {
        Log.d(tag, "In $tag init")
    }

    fun getSchemeList() {
        Log.d(tag, "In $tag getSchemeList")
        viewModelScope.launch(Dispatchers.IO) {
            val response = getSchemeListUseCase.executeUseCase(SchemeListUseCase.Request())
            viewModelScope.launch(Dispatchers.Main) {
                when ( response.getResponseCode() ) {
                    is ResponseCode.Success -> {
                        Log.d(tag, "In $tag getSchemeList SUCCESS")
                        Log.d(tag, "In $tag Response -> ${response.getData()}")
                        _schemeResponse.postValue(response.getData())
                    }
                    else -> {
                        Log.d(tag, "In $tag getSchemeList ERROR")
                    }
                }
            }
        }
    }


}