package com.example.remotewol

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET

interface Api {
    @GET("XXXXXXXXXXXXXXXXXX")
     fun getValue(): Call<String>
}
