package com.comet.data.api

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Streaming
import retrofit2.http.Url

interface DictionaryService {
    @GET("index.json")
    fun getDictionaries(): Call<DictionaryIndex>

    @Streaming
    @GET("{language}.dic")
    fun downloadDictionaryFile(@Path("language") language: String): Call<ResponseBody>

    companion object {
        fun create(): DictionaryService = Retrofit.Builder()
            .baseUrl("https://barfru.dreamhosters.com/dictionaries/")
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(DictionaryService::class.java)

    }
}