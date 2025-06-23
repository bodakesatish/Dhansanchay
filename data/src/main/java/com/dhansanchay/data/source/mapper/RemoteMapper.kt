package com.dhansanchay.data.source.mapper

interface RemoteMapper<Remote, Data, Domain> {
    fun Remote.mapToData(): Data
    fun Data.mapToDomain(): Domain
}