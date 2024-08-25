package com.dhansanchay.data.source.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dhansanchay.data.source.base.BaseEntity

@Entity(tableName = SchemeEntity.TABLE_NAME)
data class SchemeEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = Columns.ID)
    val id: Int,
    @ColumnInfo(name = Columns.SCHEME_CODE)
    val schemeCode: String,
    @ColumnInfo(name = Columns.SCHEME_NAME)
    val schemeName: String
) : BaseEntity() {

    companion object {
        const val TABLE_NAME = "scheme"
    }

    internal object Columns {
        internal const val ID = "id"
        internal const val SCHEME_CODE = "schemeCode"
        internal const val SCHEME_NAME = "schemeName"
    }

}