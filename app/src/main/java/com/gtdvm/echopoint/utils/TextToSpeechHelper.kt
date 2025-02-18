package com.gtdvm.echopoint.utils

import android.content.Context
import android.provider.Settings
import android.speech.tts.TextToSpeech
import java.util.Locale
import android.util.Log
//import androidx.appcompat.app.AppCompatActivity

class TextToSpeechHelper(context: Context) : TextToSpeech.OnInitListener {
private var tts: TextToSpeech? = null
    private var pendingText: String? = null
    private var ttsSucces: Boolean = false

    init {
        // initialize the synthesizer
Log.d(TTSTAG, "initialize the synthesizer")
        val userTTS = Settings.Secure.getString(context.contentResolver, Settings.Secure.TTS_DEFAULT_SYNTH) ?: "com.google.android.tts"
        Log.d(TTSTAG, "The Synthesizer selected by the user is: $userTTS")
        tts = TextToSpeech(context, this, userTTS)
    }

    override fun onInit(status: Int) {
        //The reverse call after completing the initiation of TextToSpeech
        Log.d(TTSTAG, "The reverse call after completing the initiation of TextToSpeech")
        if (status == TextToSpeech.SUCCESS){
            ttsSucces = true
            Log.d(TTSTAG, "TTS initialized successfully")
            tts?.setLanguage(Locale.getDefault())
                Log.d(TTSTAG, "The language is default system set")
            val currentVoice = tts?.defaultEngine
            Log.d(TTSTAG, "The synthesizer is set: $currentVoice")
            pendingText?.let { toSpeak(it) }
            pendingText = null
        } else{
            Log.e(TTSTAG, "TTS initialization failed")
        }
    }

    // Function to speak the text received as a parameter
    fun toSpeak(text: String){
Log.d(TTSTAG, "the text is sent to the synthesizer")
        if (!ttsSucces){
            Log.d(TTSTAG, "TTS not initialized yet, storing text for later")
            pendingText = text
        } else{
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    // Release TTS resources (called in activity to avoid memory leaks)
    fun releaseOfTtsResources(){
        Log.d(TTSTAG, "Release TTS resources")
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    companion object{
        const val TTSTAG = "TextToSpeechHelper"
    }

}