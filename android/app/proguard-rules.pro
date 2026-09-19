# VoiceShield AI Proguard Rules
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.voiceshield.ai.data.remote.dto.** { *; }
-keep class com.voiceshield.ai.domain.model.** { *; }
