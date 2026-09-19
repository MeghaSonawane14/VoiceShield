package com.voiceshield.ai.data.local.dao

import androidx.room.*
import com.voiceshield.ai.data.local.entity.ReportEntity
import com.voiceshield.ai.data.local.entity.SessionEntity
import com.voiceshield.ai.data.local.entity.VoiceProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM local_sessions WHERE userId = :userId AND isDemo = 0 ORDER BY startTime DESC")
    fun getUserSessions(userId: String): Flow<List<SessionEntity>>

    @Query("SELECT * FROM local_sessions WHERE sessionId = :sessionId AND userId = :userId LIMIT 1")
    suspend fun getSessionById(sessionId: String, userId: String): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)

    @Query("DELETE FROM local_sessions WHERE sessionId = :sessionId AND userId = :userId")
    suspend fun deleteSession(sessionId: String, userId: String)

    @Query("DELETE FROM local_sessions WHERE isDemo = 1")
    suspend fun clearDemoSessions()
}

@Dao
interface ReportDao {
    @Query("SELECT * FROM local_reports WHERE userId = :userId AND isDemo = 0 ORDER BY createdAt DESC")
    fun getUserReports(userId: String): Flow<List<ReportEntity>>

    @Query("SELECT * FROM local_reports WHERE reportId = :reportId AND userId = :userId LIMIT 1")
    suspend fun getReportById(reportId: String, userId: String): ReportEntity?

    @Query("SELECT * FROM local_reports WHERE sessionId = :sessionId AND userId = :userId LIMIT 1")
    suspend fun getReportBySessionId(sessionId: String, userId: String): ReportEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportEntity)

    @Query("DELETE FROM local_reports WHERE reportId = :reportId AND userId = :userId")
    suspend fun deleteReport(reportId: String, userId: String)

    @Query("DELETE FROM local_reports WHERE isDemo = 1")
    suspend fun clearDemoReports()
}

@Dao
interface VoiceProfileDao {
    @Query("SELECT * FROM local_voice_profile WHERE userId = :userId LIMIT 1")
    fun getVoiceProfile(userId: String): Flow<VoiceProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoiceProfile(profile: VoiceProfileEntity)

    @Query("DELETE FROM local_voice_profile WHERE userId = :userId")
    suspend fun deleteVoiceProfile(userId: String)
}
