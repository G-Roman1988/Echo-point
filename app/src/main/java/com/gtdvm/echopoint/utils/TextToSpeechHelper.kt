package com.gtdvm.echopoint.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale
import android.util.Log

class TextToSpeechHelper(context: Context) : TextToSpeech.OnInitListener {
private var tts: TextToSpeech? = null

    init {
        // initialize the synthesizer
Log.d(TTSTAG, "initialize the synthesizer")
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        //Set preferred language default is system language
        Log.d(TTSTAG, "Set preferred language (default is system language")
        tts?.language = Locale.getDefault()
    }

    // Function to speak the text received as a parameter
    fun toSpeak(text: String){
Log.d(TTSTAG, "the text is sent to the synthesizer")
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
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