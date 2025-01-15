package com.gtdvm.echopoint.utils

import android.os.CountDownTimer
import android.util.Log

class Timer(private val onTimerExpired: () -> Unit) {
    private var timer: CountDownTimer? = null

    fun startTimer(duration: Long = 10_000) {
        stopTimer()
        timer = object : CountDownTimer(duration, 1_000) {
            override fun onTick(millisUntilFinished: Long) {
                Log.d("BleTimer", "Timp rămas: ${millisUntilFinished / 1000} secunde")
            }

            override fun onFinish() {
onTimerExpired()
                Log.d("BleTimer", "Timer expirat.")
            }
        }
            .start()
    }

    fun stopTimer(){
        timer?.cancel()
        timer = null
        Log.d("BleTimer", "Timer oprit.")
    }


}