package com.dhansanchay.ui.scheme

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhansanchay.domain.model.ResponseCode
import com.dhansanchay.domain.model.request.PageRequest
import com.dhansanchay.domain.model.response.SchemeModel
import com.dhansanchay.domain.usecases.PaginatedSchemeListUseCase
import com.dhansanchay.domain.usecases.SchemeListCountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ViewModelSchemeList @Inject constructor(
    private val getSchemeListUseCase: SchemeListCountUseCase,
    private val paginatedSchemeListUseCase: PaginatedSchemeListUseCase
) : ViewModel() {

    private val tag = this.javaClass.simpleName

    private val _schemeResponse = MutableLiveData<List<SchemeModel>>()
    val schemeResponse : LiveData<List<SchemeModel>> = _schemeResponse

    private var currentPage = 0
    private val pageSize = 20

    init {
        Log.d(tag, "In $tag init")
    }

    fun getSchemeList() {
        Log.d(tag, "In $tag getSchemeList")
        viewModelScope.launch(Dispatchers.IO) {
            val response = getSchemeListUseCase.executeUseCase(SchemeListCountUseCase.Request())
            viewModelScope.launch(Dispatchers.Main) {
                when ( response.getResponseCode() ) {
                    is ResponseCode.Success -> {
                        Log.d(tag, "In $tag getSchemeList SUCCESS")
                        Log.d(tag, "In $tag Response -> ${response.getData()}")
                        currentPage = 0
                        getPaginatedSchemeList()
                    }
                    else -> {
                        Log.d(tag, "In $tag getSchemeList ERROR")
                    }
                }
            }
        }
    }

    fun getPaginatedSchemeList() {
        Log.d(tag, "In $tag getPaginatedSchemeList")
        viewModelScope.launch(Dispatchers.IO) {
            val request = PaginatedSchemeListUseCase.Request()
            request.setRequestModel(PageRequest(currentPage = currentPage, pageSize = pageSize))
            val response = paginatedSchemeListUseCase.executeUseCase(request)
            viewModelScope.launch(Dispatchers.Main) {
                when ( response.getResponseCode() ) {
                    is ResponseCode.Success -> {
                        currentPage++
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