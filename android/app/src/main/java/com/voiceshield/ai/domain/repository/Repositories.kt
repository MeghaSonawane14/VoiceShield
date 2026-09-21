package com.voiceshield.ai.domain.repository

import com.voiceshield.ai.domain.model.SecurityReport
import com.voiceshield.ai.domain.model.SessionEntityDomain
import com.voiceshield.ai.domain.model.User
import com.voiceshield.ai.domain.model.VoiceProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow


interface AuthRepository {
    val currentUser: StateFlow<User?>
    suspend fun login(email: String, password: String):Result<User>
    suspend fun register(name: String, email: String, password: String): Result<User>
    suspend fun resetPassword(email: String): Result<Unit>
    suspend fun logout()
    fun checkAuthState(): Boolean
}

interface VoiceProfileRepository {
    fun getVoiceProfile(userId: String): Flow<VoiceProfile?>
    suspend fun createVoiceProfile(userId: String, name: String, audioBytes: ByteArray?): Result<VoiceProfile>
    suspend fun deleteVoiceProfile(userId: String): Result<Unit>
}

interface AnalysisRepository {
    fun getSessionHistory(userId: String): Flow<List<SessionEntityDomain>>
    suspend fun getSessionById(sessionId: String, userId: String): SessionEntityDomain?
    suspend fun saveSession(session: SessionEntityDomain)
    suspend fun deleteSession(sessionId: String, userId: String)
    suspend fun saveReport(report: SecurityReport, userId: String)
    suspend fun getReport(reportId: String, userId: String): SecurityReport?
    suspend fun clearDemoData()
}
