package com.voiceshield.ai.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.voiceshield.ai.data.local.dao.ReportDao
import com.voiceshield.ai.data.local.dao.SessionDao
import com.voiceshield.ai.data.local.dao.VoiceProfileDao
import com.voiceshield.ai.data.local.entity.ReportEntity
import com.voiceshield.ai.data.local.entity.SessionEntity
import com.voiceshield.ai.data.local.entity.VoiceProfileEntity

@Database(
    entities = [
        SessionEntity::class,
        ReportEntity::class,
        VoiceProfileEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class VoiceShieldDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun reportDao(): ReportDao
    abstract fun voiceProfileDao(): VoiceProfileDao

    companion object {
        @Volatile
        private var INSTANCE: VoiceShieldDatabase? = null

        fun getInstance(context: Context): VoiceShieldDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VoiceShieldDatabase::class.java,
                    "voiceshield_local.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
