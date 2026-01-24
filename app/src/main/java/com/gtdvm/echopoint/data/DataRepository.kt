package com.gtdvm.echopoint.data

import android.content.Context
import com.gtdvm.echopoint.models.RootCategories

object DataRepository {

    // Save the object with the deserialized data
    lateinit var deserializationData: RootCategories
        private set

    // initialize the data saving the class instance and deserialized data
    fun dataPreparation(context: Context){
        val loadDataInstance = LoadData(context)
        deserializationData = loadDataInstance.dataInitialization()
    }

    fun isInitialized() = ::deserializationData.isInitialized



}