package com.gtdvm.echopoint

import android.app.assist.AssistStructure.ViewNode
import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class NotificationViewModel: ViewModel() {
    private val _notificationData = MutableLiveData<String>()
    val notificationData: LiveData<String> get() = _notificationData

    fun showNotificationData(text: String){
        _notificationData.postValue(text)
    }

}