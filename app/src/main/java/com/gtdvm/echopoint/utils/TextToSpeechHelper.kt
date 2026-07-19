package com.gtdvm.echopoint.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.runtime.traceEventStart
import androidx.lifecycle.Observer
import com.gtdvm.echopoint.viewmodel.BeaconViewModel
import java.util.Locale


class TextToSpeechHelper(context: Context) : TextToSpeech.OnInitListener {
    private var viewModelObserver: Observer<List<BeaconViewModel.DeviceStatusEvent>>? = null
    private var observedViewModel: BeaconViewModel? = null

    private var tts: TextToSpeech? = null
    private var ttsInitialized: Boolean = false
    private val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    // We use STREAM_MUSIC: it works with the screen off and in the background,
    private val ttsParams = Bundle().apply {
        putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
    }

    // Queue of pending texts: to not miss incoming messages while TTS is not ready or when another sound (TalkBack, etc.) has audio focus.
    private val speakQueue = ArrayDeque<String>()
    // The text that was spoken when I lost audio focus
    private var interruptedText: String? = null
    // AudioFocusRequest used on Android 8+ (API 26+)
    private var audioFocusRequest: AudioFocusRequest? = null

    // Listener that receives notifications when audio focus changes. For example: TalkBack starts talking → we get LOSS_TRANSIENT →
    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                // Returned to focus (TalkBack/other finished talking), Picking up where we left off.
                Log.d(TTSTAG, "AudioFocus: GAIN - reluam vorbirea")
                interruptedText?.let { text ->
                    interruptedText = null
                    speakInternal(text, addToQueue = false)
                }
// If there are still texts in the queue (they came while we were interrupted), we will also talk to them after TTS finishes the resumed one.
                drainQueue()
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Someone else has temporary focus (TalkBack is talking, calling, etc.) We stop TTS and will resume it when we get GAIN back.
                Log.d(TTSTAG, "AudioFocus: LOSS_TRANSIENT - oprim TTS, vom relua la GAIN")
                if (tts?.isSpeaking == true) {
                    tts?.stop()
                }
            }

            AudioManager.AUDIOFOCUS_LOSS -> {
                // We lost the permanent focus (the user opened YouTube, etc.) We stop TTS and empty the queue - it makes no sense to resume after a permanent loss.
                Log.d(TTSTAG, "AudioFocus: LOSS permanent - oprim TTS si golim coada")
                tts?.stop()
                speakQueue.clear()
                interruptedText = null
                abandonAudioFocus()
            }
        }
    }

    init {
        Log.d(TTSTAG, "initialize the synthesizer")
        val userTTS = Settings.Secure.getString(context.contentResolver, Settings.Secure.TTS_DEFAULT_SYNTH) ?: "com.google.android.tts"
        Log.d(TTSTAG, "The Synthesizer selected by the user is: $userTTS")
        tts = TextToSpeech(context, this, userTTS).apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
        }
    }

    override fun onInit(status: Int) {
        Log.d(TTSTAG, "The reverse call after completing the initiation of TextToSpeech")
        if (status == TextToSpeech.SUCCESS) {
            ttsInitialized = true
            Log.d(TTSTAG, "TTS initialized successfully")
            //tts?.setLanguage(Locale.getDefault())
            tts?.language = Locale.getDefault()
            Log.d(TTSTAG, "The language is default system set: ${tts?.defaultEngine}")
            // Empty the queue of accumulated texts while TTS initializes
            drainQueue()
        } else {
            Log.e(TTSTAG, "TTS initialization failed")
        }
    }

    // Public speak function - adds the text to the queue and tries to speak
    fun toSpeak(text: String) {
        Log.d(TTSTAG, "toSpeak() called: \"$text\"")
        if (!ttsInitialized) {
            // TTS is still initializing - we put it in the queue, drainQueue() will talk to them after init
            Log.d(TTSTAG, "TTS not initialized yet, adding to queue")
            speakQueue.addLast(text)
            return
        }
        speakInternal(text, addToQueue = true)
    }

    // The internal function that requires focus and actually speaks, addToQueue = true: the text is new, if TTS is busy we put it in the TTS queue addToQueue = false: the text is resumed after the interruption, we speak it directly
    private fun speakInternal(text: String, addToQueue: Boolean) {
        val focusGranted = requestAudioFocus()
        if (!focusGranted) {
            // We didn't get focus - we put it in the queue to talk when we get it
            Log.d(TTSTAG, "Audio focus refused, we add to the queue: \"$text\"")
            speakQueue.addLast(text)
            return
        }
        if (tts?.isSpeaking == true && addToQueue) {
            // TTS is already speaking - add after (QUEUE_ADD)
            Log.d(TTSTAG, "TTS occupied, QUEUE_ADD: \"$text\"")
            interruptedText = text
            tts?.speak(text, TextToSpeech.QUEUE_ADD, ttsParams, "tts_utterance")
        } else {
// Free TTS or resume after interruption - we speak directly (QUEUE_FLUSH)
            Log.d(TTSTAG, "TTS liber, QUEUE_FLUSH: \"$text\"")
            interruptedText = text
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, ttsParams, "tts_utterance")
        }
    }

    // Empty the queue of waiting texts, speaking them in turn
    private fun drainQueue() {
        if (!ttsInitialized) return
        while (speakQueue.isNotEmpty()) {
            val text = speakQueue.removeFirst()
            Log.d(TTSTAG, "drainQueue: TALK \"$text\"")
            speakInternal(text, addToQueue = false)
        }
    }

    // Request audio focus before speaking, GAIN_TRANSIENT_MAY_DUCK: we ask for temporary focus and allow other sources audio to reduce the volume (duck) instead of stopping completely.
    private fun requestAudioFocus(): Boolean {
        val focusRequest = AudioFocusRequest
            .Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setOnAudioFocusChangeListener(audioFocusListener)
            .setAcceptsDelayedFocusGain(true)
            .build()
        audioFocusRequest = focusRequest
        val result = audioManager.requestAudioFocus(focusRequest)
        val granted = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED ||
                result == AudioManager.AUDIOFOCUS_REQUEST_DELAYED
        Log.d(TTSTAG, "requestAudioFocus: ${if (granted) "PROVIDED" else "DENIED"} (result=$result)")
        return granted
    }

    // Release the audio focus when we no longer need it
    private fun abandonAudioFocus() {
        audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        Log.d(TTSTAG, "abandonAudioFocus: focus released")
    }

    // Release TTS resources (called in activity to avoid memory leaks)
    fun releaseOfTtsResources() {
        Log.d(TTSTAG, "Release TTS resources")
        stopObservingViewModel()
        tts?.stop()
        tts?.shutdown()
        tts = null
        speakQueue.clear()
        interruptedText = null
        abandonAudioFocus()
    }

    // Called from onPause() of the activity: TTS subscribes to the ViewModel
    fun startObservingViewModel(viewModel: BeaconViewModel) {
        Log.d(TTSTAG, "startObservingViewModel - TTS takes over the announcements in the background")
        observedViewModel = viewModel
        val observer = Observer<List<BeaconViewModel.DeviceStatusEvent>> { events ->
            events.forEach { event ->
                val text = when (event.status) {
                    BeaconViewModel.DeviceStatus.FOUND ->
                                                 AuxiliaryFunctions.getDeviceAnnouncementText(event.device)
                    BeaconViewModel.DeviceStatus.LOST ->
                        AuxiliaryFunctions.getDeviceAnnouncementText(event.device)
                }
                toSpeak(text)
            }
        }
        viewModelObserver = observer
        viewModel.deviceStatus.observeForever(observer)
    }

// Called from onResume() of the activity: TTS passes the announcements back to the activity and unsubscribes from the ViewModel.
fun stopObservingViewModel() {
    Log.d(TTSTAG, "stopObservingViewModel - the activity retrieves the announcements")
    viewModelObserver?.let { observer ->
        observedViewModel?.deviceStatus?.removeObserver(observer)
    }
    viewModelObserver = null
    observedViewModel = null
}

    companion object {
        const val TTSTAG = "TextToSpeechHelper"
    }


}

