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
        builder.append("Session Code       : ${report.sessionCode}\n")
        builder.append("Date & Time        : ${report.timestamp}\n")
        builder.append("Audio Duration     : ${report.durationSec} seconds\n")
        builder.append("Input Mode         : ${report.inputMode}\n\n")

        builder.append("--- EXECUTIVE FORENSIC SUMMARY ---\n")
        builder.append("Overall Risk Score : ${report.overallRiskScore} / 100\n")
        builder.append("Risk Classification: ${report.overallRiskLevel}\n")
        builder.append("Peak Clone Prob.   : ${String.format("%.1f", report.maxCloneProbability * 100)}%\n")
        builder.append("Speaker Similarity : ${String.format("%.1f", report.meanSpeakerSimilarity * 100)}%\n")
        builder.append("Detected Speaker   : ${report.detectedSpeaker}\n")
        builder.append("Enrolled Profile   : ${report.enrolledSpeaker}\n")
        builder.append("Model Attribution  : ${report.modelAttribution}\n")
        builder.append("Caller Intent      : ${report.intentClassification}\n")
        builder.append("Verification Result: ${report.verificationResult}\n")
        builder.append("Recommended Action : ${report.recommendedAction}\n\n")

        builder.append("--- RISK TIMELINE ---\n")
        if (report.timeline.isEmpty()) {
            builder.append("No continuous timeline points recorded.\n")
        } else {
            report.timeline.forEach { pt ->
                builder.append("  [${pt.timeDisplay}] Risk: ${pt.riskLevel.displayName} (${pt.riskScore}%) | Clone Prob: ${String.format("%.0f", pt.cloneProbability * 100)}%\n")
            }
        }
        builder.append("\n")

        builder.append("--- DETECTION EVENTS ---\n")
        val allEvents = if (report.events.isNotEmpty()) report.events else report.riskEvents
        if (allEvents.isEmpty()) {
            builder.append("No critical escalations logged during session.\n")
        } else {
            allEvents.forEach { ev ->
                builder.append("  ${String.format("%.1f", ev.timestampSec)}s - [${ev.severity}] ${ev.description}\n")
            }
        }
        builder.append("\n")

        builder.append("--- RECOMMENDED VERIFICATION GUIDANCE ---\n")
        if (report.advisoryRecommendations.isNotEmpty()) {
            report.advisoryRecommendations.forEachIndexed { idx, rec ->
                builder.append("${idx + 1}. $rec\n")
            }
        } else {
            builder.append("1. Independently call the contact using a verified, known telephone number.\n")
            builder.append("2. Ask a pre-agreed secret challenge question or shared family memory.\n")
            builder.append("3. Refrain from transferring funds or providing OTP/banking credentials over phone.\n")
            builder.append("4. Request in-person or secondary authorization for financial requests.\n")
        }
        builder.append("\n")

        builder.append("=========================================================\n")
        builder.append("PROBABILISTIC CLASSIFICATION NOTICE:\n")
        builder.append("VoiceShield AI utilizes machine learning models and signal processing\n")
        builder.append("heuristics. AI clone detection is probabilistic and must never be\n")
        builder.append("presented as 100% infallible legal proof.\n")
        builder.append("=========================================================\n")

        return builder.toString()
    }

    fun exportReportToTxt(context: Context, report: SecurityReport): File {
        val text = generateReportText(report)
        val file = File(context.cacheDir, "VoiceShield_${report.sessionCode}_Report.txt")
        file.writeText(text)
        return file
    }

    fun shareReport(context: Context, file: File) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            try {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {
                // Fallback to text reading if fileprovider isn't configured
                putExtra(Intent.EXTRA_TEXT, file.readText())
            }
            putExtra(Intent.EXTRA_SUBJECT, "VoiceShield AI Forensic Report - ${file.nameWithoutExtension}")
        }
        val chooser = Intent.createChooser(shareIntent, "Share Security Report").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun shareReportAsText(context: Context, report: SecurityReport) {
        val text = generateReportText(report)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "VoiceShield AI Report - ${report.sessionCode}")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(shareIntent, "Export Security Report").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
