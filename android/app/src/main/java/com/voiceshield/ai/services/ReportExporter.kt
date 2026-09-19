package com.voiceshield.ai.services

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.voiceshield.ai.domain.model.SecurityReport
import java.io.File

object ReportExporter {

    fun generateReportText(report: SecurityReport): String {
        val builder = StringBuilder()
        builder.append("=========================================================\n")
        builder.append("                 VOICE SHIELD AI                         \n")
        builder.append("            SECURITY ANALYSIS REPORT                     \n")
        builder.append("=========================================================\n\n")

        if (report.isDemo) {
            builder.append("*********************************************************\n")
            builder.append("               DEMO MODE — SAMPLE DATA                   \n")
            builder.append("  (This report was generated using simulated demo data)  \n")
            builder.append("*********************************************************\n\n")
        }

        builder.append("Analysis ID        : ${report.reportId}\n")
        builder.append("Session Code       : ${report.sessionId}\n")
        builder.append("Date & Time        : ${report.timestamp}\n")
        builder.append("Audio Duration     : ${report.durationSec} seconds\n\n")

        builder.append("--- EXECUTIVE FORENSIC SUMMARY ---\n")
        builder.append("Overall Risk Score : ${report.overallRiskScore} / 100\n")
        builder.append("Risk Classification: ${report.riskLevel.name}\n")
        builder.append("AI Clone Prob.     : ${String.format("%.1f", report.cloneProbability * 100)}%\n")
        builder.append("Speaker Similarity : ${String.format("%.1f", report.speakerSimilarity * 100)}%\n")
        builder.append("Likely Speaker     : ${report.likelySpeaker}\n")
        builder.append("Verification Result: ${report.verificationResult}\n")
        builder.append("Recommended Action : ${report.recommendedAction}\n\n")

        builder.append("--- RISK TIMELINE ---\n")
        if (report.timeline.isEmpty()) {
            builder.append("No continuous timeline points recorded.\n")
        } else {
            report.timeline.forEach { pt ->
                builder.append("  [${pt.timeDisplay}] Risk: ${pt.riskLevel.name} (${String.format("%.0f", pt.riskScore * 100)}%) | Clone Prob: ${String.format("%.0f", pt.cloneProb * 100)}%\n")
            }
        }
        builder.append("\n")

        builder.append("--- DETECTION EVENTS ---\n")
        if (report.events.isEmpty()) {
            builder.append("No critical escalations logged during session.\n")
        } else {
            report.events.forEach { ev ->
                builder.append("  ${String.format("%.1f", ev.timestampSec)}s - [${ev.severity}] ${ev.description}\n")
            }
        }
        builder.append("\n")

        builder.append("--- RECOMMENDED VERIFICATION GUIDANCE ---\n")
        builder.append("1. Independently call the contact using a verified, known telephone number.\n")
        builder.append("2. Ask a pre-agreed secret challenge question or shared family memory.\n")
        builder.append("3. Refrain from transferring funds or providing OTP/banking credentials over phone.\n")
        builder.append("4. Request in-person or secondary authorization for financial requests.\n\n")

        builder.append("=========================================================\n")
        builder.append("ACADEMIC DISCLAIMER:\n")
        builder.append("VoiceShield AI provides probabilistic security analysis and is\n")
        builder.append("intended as an academic prototype. AI predictions are probabilistic\n")
        builder.append("and should not be treated as 100% conclusive proof.\n")
        builder.append("=========================================================\n")

        return builder.toString()
    }

    fun shareReportAsText(context: Context, report: SecurityReport) {
        val text = generateReportText(report)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "VoiceShield AI Report - ${report.reportId}")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Export Security Report"))
    }
}
