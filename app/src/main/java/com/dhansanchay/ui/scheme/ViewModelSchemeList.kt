package com.dhansanchay.ui.scheme

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.dhansanchay.domain.model.ResponseCode
import com.dhansanchay.domain.model.request.PageRequest
import com.dhansanchay.domain.model.response.SchemeModel
import com.dhansanchay.domain.usecases.PaginatedSchemeListUseCase
import com.dhansanchay.domain.usecases.PagingSchemeListUseCase
import com.dhansanchay.domain.usecases.SchemeListCountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ViewModelSchemeList @Inject constructor(
    private val getSchemeListUseCase: SchemeListCountUseCase,
    private val paginatedSchemeListUseCase: PaginatedSchemeListUseCase,
    private val pagingSchemeListUseCase: PagingSchemeListUseCase
) : ViewModel() {

    private val tag = this.javaClass.simpleName

    private var _schemeResponse = flowOf<PagingData<SchemeModel>>()
    val schemeResponse : LiveData<PagingData<SchemeModel>> = _schemeResponse.asLiveData()

    var topRatedMovieList: MutableStateFlow<PagingData<SchemeModel>> =
        MutableStateFlow(value = PagingData.empty())
        private set

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
                        getPagedSchemeItems()
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
                      //  _schemeResponse.postValue(response.getData())
                    }
                    else -> {
                        Log.d(tag, "In $tag getSchemeList ERROR")
                    }
                }
            }
        }
    }

    fun getPagedSchemeItems() {
        val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
            // Handle the exception, e.g., log it or display an error message
            Log.e("Coroutine Exception", "Caught exception: ", throwable)
        }

        viewModelScope.launch(Dispatchers.IO+coroutineExceptionHandler) {
            val response = pagingSchemeListUseCase.executeUseCase(PagingSchemeListUseCase.Request())
            viewModelScope.launch(Dispatchers.Main) {
                when ( response.getResponseCode() ) {
                    is ResponseCode.Success -> {
                        currentPage++
                        Log.d(tag, "In $tag getSchemeList SUCCESS")
                        Log.d(tag, "In $tag Response -> ${response.getData()}")
                        _schemeResponse = response.getData()?.cachedIn(viewModelScope)!!
                    }
                    else -> {
                        Log.d(tag, "In $tag getSchemeList ERROR")
                    }
                }
            }
        }

    }


}