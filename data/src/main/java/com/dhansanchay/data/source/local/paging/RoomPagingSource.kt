package com.dhansanchay.data.source.local.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.dhansanchay.data.source.local.dao.SchemeDao
import com.dhansanchay.data.source.local.entity.SchemeEntity

class RoomPagingSource(private val itemDao: SchemeDao, private val onItemsLoaded: (List<SchemeEntity>) -> Unit) : PagingSource<Int, SchemeEntity>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SchemeEntity> {
        return try {
            val page = params.key ?: 0
            val pageSize = params.loadSize
            val offset = page * pageSize

            val items = itemDao.getItems(pageSize, offset)

            // Manually notify the UI about the loaded items
            onItemsLoaded(items)

            LoadResult.Page(
                data = items,
                prevKey = if (page == 0) null else page - 1,
                nextKey = if (items.isEmpty()) null else page + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, SchemeEntity>): Int? {
        return null
    }
}