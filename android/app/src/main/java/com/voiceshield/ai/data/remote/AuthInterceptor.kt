package com.voiceshield.ai.data.remote

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    @Volatile
    var token: String? = null

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val builder = original.newBuilder()

        val currentToken = token
        if (!currentToken.isNullOrEmpty()) {
            builder.header("Authorization", "Bearer $currentToken")
        }

        return chain.proceed(builder.build())
    }
}
