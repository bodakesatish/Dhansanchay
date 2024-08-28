package com.dhansanchay.data.source.local.paging

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.dhansanchay.data.source.base.BaseOutput
import com.dhansanchay.data.source.local.dao.SchemeDao
import com.dhansanchay.data.source.local.entity.SchemeEntity
import com.dhansanchay.data.source.local.source.SchemeDataSourceLocal
import com.dhansanchay.domain.model.response.SchemeModel

class RoomPagingSource(private val localDataSource: SchemeDataSourceLocal) : PagingSource<Int, SchemeModel>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SchemeModel> {
        return try {
            val page = params.key ?: 1
            val pageSize = params.loadSize
            val offset = (page - 1) * pageSize


            return try {
                val localOutput = localDataSource.getPagingSchemeList(pageSize, offset) // Map todomain models

                val items = if (localOutput is BaseOutput.Success) {
                    localOutput.output!!
                } else {
                    ArrayList()
                }
               val prevKey = if (page == 0) null else page - 1
                val nextKey = if (items.isEmpty()) null else page + 1
                Log.i("PagingSource","Items->"+items)
                LoadResult.Page(items, prevKey = prevKey, nextKey = nextKey)
            } catch (e: Exception) {
                LoadResult.Error(e)
            }

//            val items = itemDao.getItems(pageSize, offset)
//
//            // Manually notify the UI about the loaded items
//            onItemsLoaded(items)
//
//            LoadResult.Page(
//                data = items,
//                prevKey = if (page == 0) null else page - 1,
//                nextKey = if (items.isEmpty()) null else page + 1
//            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, SchemeModel>): Int? {
        return null
    }
}