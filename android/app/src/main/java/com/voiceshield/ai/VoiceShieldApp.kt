package com.voiceshield.ai

import android.app.Application
import com.voiceshield.ai.data.local.VoiceShieldDatabase
import com.voiceshield.ai.data.remote.ApiClient
import com.voiceshield.ai.data.remote.VoiceAnalysisWebSocketClient
import com.voiceshield.ai.data.repository.AnalysisRepositoryImpl
import com.voiceshield.ai.data.repository.AuthRepositoryImpl
import com.voiceshield.ai.data.repository.VoiceProfileRepositoryImpl
import com.voiceshield.ai.domain.repository.AnalysisRepository
import com.voiceshield.ai.domain.repository.AuthRepository
import com.voiceshield.ai.domain.repository.VoiceProfileRepository
import com.voiceshield.ai.services.AudioRecordManager
import com.voiceshield.ai.services.DemoScenarioProvider

class VoiceShieldApp : Application() {

    lateinit var database: VoiceShieldDatabase
        private set

    lateinit var authRepository: AuthRepository
        private set

    lateinit var voiceProfileRepository: VoiceProfileRepository
        private set

    lateinit var analysisRepository: AnalysisRepository
        private set

    lateinit var audioRecordManager: AudioRecordManager
        private set

    lateinit var webSocketClient: VoiceAnalysisWebSocketClient
        private set

    lateinit var demoScenarioProvider: DemoScenarioProvider
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = VoiceShieldDatabase.getInstance(this)
        val api = ApiClient.getVoiceShieldApi(this)

        authRepository = AuthRepositoryImpl(this, api)
        voiceProfileRepository = VoiceProfileRepositoryImpl(database.voiceProfileDao(), api)
        analysisRepository = AnalysisRepositoryImpl(database.sessionDao(), database.reportDao(), api)
        audioRecordManager = AudioRecordManager(this)
        webSocketClient = VoiceAnalysisWebSocketClient()
        demoScenarioProvider = DemoScenarioProvider()
    }

    companion object {
        lateinit var instance: VoiceShieldApp
            private set
    }
}
