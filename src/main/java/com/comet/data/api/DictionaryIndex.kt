package com.comet.data.api

data class DictionaryIndex(
    val minVersionCode: Int,
    val dictionaries: List<DictionaryItem>
)