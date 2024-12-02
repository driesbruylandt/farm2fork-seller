package com.example.farm2fork_sellers.utils.Data

import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.google.gson.Gson
import java.io.InputStream
import java.io.InputStreamReader

class ApiResponseDeserializer : ResponseDeserializable<ApiResponse> {
    override fun deserialize(content: String): ApiResponse? {
        return Gson().fromJson(content, ApiResponse::class.java)
    }

    override fun deserialize(inputStream: InputStream): ApiResponse? {
        return Gson().fromJson(InputStreamReader(inputStream), ApiResponse::class.java)
    }
}