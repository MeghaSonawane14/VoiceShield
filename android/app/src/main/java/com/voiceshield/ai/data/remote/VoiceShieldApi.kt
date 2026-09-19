package com.voiceshield.ai.data.remote

import com.voiceshield.ai.data.remote.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface VoiceShieldApi {

    @POST("auth/verify")
    suspend fun verifyToken(): Response<VerifyAuthResponse>

    @GET("users/me")
    suspend fun getCurrentUser(): Response<UserDto>

    @Multipart
    @POST("voice-profile/create")
    suspend fun createVoiceProfile(
        @Part("name") name: RequestBody,
        @Part("notes") notes: RequestBody,
        @Part audio: MultipartBody.Part?
    ): Response<VoiceProfileDto>

    @GET("voice-profile")
    suspend fun getVoiceProfile(): Response<VoiceProfileDto>

    @DELETE("voice-profile")
    suspend fun deleteVoiceProfile(): Response<Map<String, Any>>

    @FormUrlEncoded
    @POST("analysis/start")
    suspend fun startAnalysis(
        @Field("mode") mode: String
    ): Response<StartAnalysisResponse>

    @Multipart
    @POST("analysis/upload")
    suspend fun uploadAudio(
        @Part file: MultipartBody.Part,
        @Part("custom_pretext") customPretext: RequestBody?
    ): Response<Map<String, Any>>

    @FormUrlEncoded
    @POST("analysis/stop")
    suspend fun stopAnalysis(
        @Field("session_id") sessionId: String
    ): Response<StopAnalysisResponse>

    @GET("analysis/history")
    suspend fun getAnalysisHistory(): Response<List<SessionHistoryDto>>

    @GET("analysis/{session_id}")
    suspend fun getAnalysisDetails(
        @Path("session_id") sessionId: String
    ): Response<Map<String, Any>>

    @GET("report/{report_id}")
    suspend fun getReport(
        @Path("report_id") reportId: String
    ): Response<Map<String, Any>>

    @DELETE("analysis/{session_id}")
    suspend fun deleteAnalysis(
        @Path("session_id") sessionId: String
    ): Response<Map<String, Any>>

    @GET("api/dataset/stats")
    suspend fun getDatasetStats(): Response<DatasetStatsDto>
}
