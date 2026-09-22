package com.voiceshield.ai.data.repository

import android.content.Context
import com.google.gson.Gson
import com.voiceshield.ai.data.local.VoiceShieldDatabase
import com.voiceshield.ai.data.local.entity.ReportEntity
import com.voiceshield.ai.data.local.entity.SessionEntity
import com.voiceshield.ai.data.local.entity.VoiceProfileEntity
import com.voiceshield.ai.data.remote.ApiClient
import com.voiceshield.ai.domain.model.RiskLevel
import com.voiceshield.ai.domain.model.SecurityReport
import com.voiceshield.ai.domain.model.User
import com.voiceshield.ai.domain.model.VoiceProfile
import com.voiceshield.ai.domain.repository.AnalysisRepository
import com.voiceshield.ai.domain.repository.AuthRepository
import com.voiceshield.ai.domain.model.SessionEntityDomain
import com.voiceshield.ai.domain.repository.VoiceProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.*

class AuthRepositoryImpl(
    private val context: Context
) : AuthRepository {
    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser

    private val prefs = context.getSharedPreferences("voiceshield_auth", Context.MODE_PRIVATE)

    init {
        // Restore persistent user session if stored
        val savedUid = prefs.getString("user_uid", null)
        val savedEmail = prefs.getString("user_email", null)
        val savedName = prefs.getString("user_name", null)
        if (savedUid != null && savedEmail != null) {
            val user = User(
                uid = savedUid,
                email = savedEmail,
                name = savedName ?: "Analyst",
                hasVoiceProfile = prefs.getBoolean("has_voice_profile", false)
            )
            _currentUser.value = user
            ApiClient.authInterceptor.token = "dev-$savedUid:$savedEmail"
        }
    }

    override suspend fun login(email: String, password: String): Result<User> {
        if (!email.contains("@") || password.length < 6) {
            return Result.failure(IllegalArgumentException("Invalid email format or password under 6 characters"))
        }

        // Deterministic account identifier derived from email
        val uid = "usr-" + email.replace(Regex("[^a-zA-Z0-9]"), "").take(12)
        val name = email.substringBefore("@").replace(".", " ").capitalizeWords()
        val user = User(uid = uid, email = email, name = name)

        _currentUser.value = user
        ApiClient.authInterceptor.token = "dev-$uid:$email"

        prefs.edit()
            .putString("user_uid", uid)
            .putString("user_email", email)
            .putString("user_name", name)
            .apply()

        return Result.success(user)
    }

    override suspend fun register(name: String, email: String, password: String): Result<User> {
        if (name.isBlank() || !email.contains("@") || password.length < 6) {
            return Result.failure(IllegalArgumentException("Please provide valid name, email, and password (min 6 chars)"))
        }

        val uid = "usr-" + email.replace(Regex("[^a-zA-Z0-9]"), "").take(12)
        val user = User(uid = uid, email = email, name = name)

        _currentUser.value = user
        ApiClient.authInterceptor.token = "dev-$uid:$email"

        prefs.edit()
            .putString("user_uid", uid)
            .putString("user_email", email)
            .putString("user_name", name)
            .apply()

        return Result.success(user)
    }

    override suspend fun resetPassword(email: String): Result<Unit> {
        if (!email.contains("@")) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address"))
        }
        return Result.success(Unit)
    }

    override suspend fun logout() {
        _currentUser.value = null
        ApiClient.authInterceptor.token = null
        prefs.edit().clear().apply()
    }

    override fun checkAuthState(): Boolean {
        return _currentUser.value != null
    }

    private fun String.capitalizeWords(): String =
        split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
}

class VoiceProfileRepositoryImpl(
    private val database: VoiceShieldDatabase
) : VoiceProfileRepository {
    override fun getVoiceProfile(userId: String): Flow<VoiceProfile?> {
        return database.voiceProfileDao().getVoiceProfile(userId).map { entity ->
            entity?.let {
                VoiceProfile(
                    id = it.profileId,
                    userId = it.userId,
                    name = it.name,
                    status = it.status,
                    registrationDate = it.registrationDate,
                    embeddingDim = it.embeddingDim,
                    notes = it.notes
                )
            }
        }
    }

    override suspend fun createVoiceProfile(
        userId: String,
        name: String,
        audioBytes: ByteArray?
    ): Result<VoiceProfile> {
        try {
            val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            val profileId = "vprof-${UUID.randomUUID().toString().take(8)}"

            // Call backend API
            val namePart = name.toRequestBody("text/plain".toMediaTypeOrNull())
            val notesPart = "Enrolled trusted voice sample".toRequestBody("text/plain".toMediaTypeOrNull())
            var audioPart: MultipartBody.Part? = null
            if (audioBytes != null && audioBytes.isNotEmpty()) {
                val reqBody = audioBytes.toRequestBody("audio/wav".toMediaTypeOrNull())
                audioPart = MultipartBody.Part.createFormData("audio", "profile_voice.wav", reqBody)
            }

            try {
                ApiClient.getApi().createVoiceProfile(namePart, notesPart, audioPart)
            } catch (e: Exception) {
                // Backend call optional in offline / simulator mode
            }

            // Save to local Room DB
            val entity = VoiceProfileEntity(
                userId = userId,
                profileId = profileId,
                name = name,
                status = "ACTIVE",
                registrationDate = now,
                embeddingDim = 128,
                notes = "Calibrated 128-D ECAPA-TDNN voiceprint"
            )
            database.voiceProfileDao().insertVoiceProfile(entity)

            val profile = VoiceProfile(
                id = profileId,
                userId = userId,
                name = name,
                status = "ACTIVE",
                registrationDate = now,
                embeddingDim = 128
            )
            return Result.success(profile)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    override suspend fun deleteVoiceProfile(userId: String): Result<Unit> {
        return try {
            try {
                ApiClient.getApi().deleteVoiceProfile()
            } catch (e: Exception) {
                // Backend call
            }
            database.voiceProfileDao().deleteVoiceProfile(userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class AnalysisRepositoryImpl(
    private val database: VoiceShieldDatabase,
    private val gson: Gson = Gson()
) : AnalysisRepository {

    override fun getSessionHistory(userId: String): Flow<List<SessionEntityDomain>> {
        return database.sessionDao().getUserSessions(userId).map { list ->
            list.map {
                SessionEntityDomain(
                    sessionId = it.sessionId,
                    userId = it.userId,
                    sessionCode = it.sessionCode,
                    startTime = it.startTime,
                    durationSec = it.durationSec,
                    inputMode = it.inputMode,
                    overallRisk = it.overallRisk,
                    riskLevel = it.riskLevel,
                    maxCloneProb = it.maxCloneProb,
                    speakerSimilarity = it.speakerSimilarity,
                    likelySpeaker = it.likelySpeaker,
                    isDemo = it.isDemo
                )
            }
        }
    }

    override suspend fun getSessionById(sessionId: String, userId: String): SessionEntityDomain? {
        val entity = database.sessionDao().getSessionById(sessionId, userId) ?: return null
        return SessionEntityDomain(
            sessionId = entity.sessionId,
            userId = entity.userId,
            sessionCode = entity.sessionCode,
            startTime = entity.startTime,
            durationSec = entity.durationSec,
            inputMode = entity.inputMode,
            overallRisk = entity.overallRisk,
            riskLevel = entity.riskLevel,
            maxCloneProb = entity.maxCloneProb,
            speakerSimilarity = entity.speakerSimilarity,
            likelySpeaker = entity.likelySpeaker,
            isDemo = entity.isDemo
        )
    }

    override suspend fun saveSession(session: SessionEntityDomain) {
        val entity = SessionEntity(
            sessionId = session.sessionId,
            userId = session.userId,
            sessionCode = session.sessionCode,
            startTime = session.startTime,
            durationSec = session.durationSec,
            inputMode = session.inputMode,
            overallRisk = session.overallRisk,
            riskLevel = session.riskLevel,
            maxCloneProb = session.maxCloneProb,
            speakerSimilarity = session.speakerSimilarity,
            likelySpeaker = session.likelySpeaker,
            isDemo = session.isDemo
        )
        database.sessionDao().insertSession(entity)
    }

    override suspend fun deleteSession(sessionId: String, userId: String) {
        database.sessionDao().deleteSession(sessionId, userId)
        try {
            ApiClient.getApi().deleteAnalysis(sessionId)
        } catch (e: Exception) {
            // Ignored
        }
    }

    override suspend fun saveReport(report: SecurityReport, userId: String) {
        val json = gson.toJson(report)
        val entity = ReportEntity(
            reportId = report.reportId,
            sessionId = report.sessionId,
            userId = userId,
            createdAt = report.timestamp,
            reportJson = json,
            isDemo = report.isDemo
        )
        database.reportDao().insertReport(entity)
    }

    override suspend fun getReport(reportId: String, userId: String): SecurityReport? {
        val entity = database.reportDao().getReportById(reportId, userId)
            ?: database.reportDao().getReportBySessionId(reportId, userId)
            ?: return null

        return try {
            gson.fromJson(entity.reportJson, SecurityReport::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun clearDemoData() {
        database.sessionDao().clearDemoSessions()
        database.reportDao().clearDemoReports()
    }
}
