package com.voiceshield.ai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.voiceshield.ai.navigation.BottomNavigationBar
import com.voiceshield.ai.navigation.NavGraph
import com.voiceshield.ai.ui.theme.CyberDarkBackground
import com.voiceshield.ai.ui.theme.VoiceShieldTheme

class MainActivity : ComponentActivity() {

    private val requestAudioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission result handled; AudioRecordManager checks permission gracefully
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check & request RECORD_AUDIO permission at launch
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        val app = application as VoiceShieldApp

        setContent {
            VoiceShieldTheme {
                val navController = rememberNavController()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CyberDarkBackground
                ) {
                    Scaffold(
                        bottomBar = {
                            BottomNavigationBar(navController = navController)
                        },
                        containerColor = CyberDarkBackground
                    ) { innerPadding ->
                        NavGraph(
                            navController = navController,
                            authRepository = app.authRepository,
                            voiceProfileRepository = app.voiceProfileRepository,
                            analysisRepository = app.analysisRepository,
                            audioRecordManager = app.audioRecordManager,
                            webSocketClient = app.webSocketClient,
                            demoScenarioProvider = app.demoScenarioProvider,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}
