package com.voiceshield.ai.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    // 10.0.2.2 is Android Emulator alias for Host loopback (127.0.0.1)
    // Supports dynamic configuration if connected to physical device or remote IP
    @Volatile
    var baseUrl: String = "http://10.0.2.2:8000/"
        set(value) {
            field = if (value.endsWith("/")) value else "$value/"
            retrofitInstance = null
            apiInstance = null
        }

    val authInterceptor = AuthInterceptor()

    val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    private var retrofitInstance: Retrofit? = null
    private var apiInstance: VoiceShieldApi? = null

    fun getApi(): VoiceShieldApi {
        return apiInstance ?: synchronized(this) {
            val retrofit = retrofitInstance ?: Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build().also { retrofitInstance = it }

            retrofit.create(VoiceShieldApi::class.java).also { apiInstance = it }
        }
    }

    fun getVoiceShieldApi(context: Any? = null): VoiceShieldApi = getApi()

    fun getWebSocketUrl(sessionId: String): String {
        val httpUrl = baseUrl
        val wsScheme = if (httpUrl.startsWith("https://")) "wss://" else "ws://"
        val hostAndPort = httpUrl.removePrefix("http://").removePrefix("https://").removeSuffix("/")
        return "${wsScheme}${hostAndPort}/ws/voice-analysis/${sessionId}"
    }
}
