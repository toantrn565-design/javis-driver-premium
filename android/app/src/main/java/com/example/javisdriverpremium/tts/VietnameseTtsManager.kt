package com.example.javisdriverpremium.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class VietnameseTtsManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var lastSpokenMessage: String? = null
    private var lastSpokenTimestamp: Long = 0L
    private val speechCooldownMs = 3000L // Prevent back-to-back overlaps

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val viLocale = Locale.forLanguageTag("vi-VN")
            val langResult = tts?.setLanguage(viLocale)

            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w("JAVIS_TTS", "Vietnamese TTS voice not found, falling back to default locale")
                tts?.setLanguage(Locale.getDefault())
            }

            tts?.setSpeechRate(1.05f)
            tts?.setPitch(1.0f)
            isInitialized = true
            Log.i("JAVIS_TTS", "Vietnamese TTS initialized successfully")
        } else {
            Log.e("JAVIS_TTS", "Failed to initialize TextToSpeech engine")
        }
    }

    /**
     * Speaks the given Vietnamese message if cooldown allows and not a direct duplicate within window.
     */
    fun speak(message: String, force: Boolean = false) {
        if (!isInitialized || message.isBlank()) return

        val now = System.currentTimeMillis()
        if (!force && message == lastSpokenMessage && (now - lastSpokenTimestamp) < 15_000L) {
            Log.d("JAVIS_TTS", "Duplicate voice alert suppressed: $message")
            return
        }

        if (!force && (now - lastSpokenTimestamp) < speechCooldownMs) {
            Log.d("JAVIS_TTS", "Voice alert speech cooldown active: $message")
            return
        }

        lastSpokenMessage = message
        lastSpokenTimestamp = now

        tts?.speak(
            message,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "JAVIS_ALERT_${now}"
        )
        Log.i("JAVIS_TTS", "JAVIS Voice Alert Spoken: \"$message\"")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
        } catch (e: Exception) {
            Log.e("JAVIS_TTS", "Error shutting down TTS", e)
        }
    }
}
