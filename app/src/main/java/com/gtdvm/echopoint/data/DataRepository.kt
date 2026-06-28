package com.gtdvm.echopoint.data

import android.content.Context
import com.gtdvm.echopoint.models.RootCategories
import com.gtdvm.echopoint.utils.NetworkTools
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import android.util.Log

object DataRepository {

    private val _isDataReady = MutableStateFlow(false)
    val isDataReady: StateFlow<Boolean> = _isDataReady
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var activeJob: Job? = null
    private enum class SyncResult {NO_NEW_VERSION, DOWNLOAD_SUCCESS, DOWNLOAD_FAILED}

    // Save the object with the deserialized data
    lateinit var deserializationData: RootCategories
        private set

    // initialize the data saving the class instance and deserialized data
    fun dataPreparation(context: Context){
        val loadDataInstance = LoadData(context)
        if (isInitialized()) return
        activeJob?.cancel()
        activeJob = repositoryScope.launch {
            if (NetworkTools.isInternetAvailable(context)){
                startUpdateAndLoadChain(loadDataInstance)
                Log.d("DataRepository", "este internet se face conectarea la server")
            } else {
                loadDataOnly(loadDataInstance)
                Log.d("DataRepository", "nu este internet se incarca datele locale")
            }
        }
    }

// The function that checks and downloads, returning the status
    private suspend fun syncRemoteData (loadDataInstance: LoadData) : SyncResult {
return  try {
    if (loadDataInstance.isNewVersionAvailable()) {
        val success = loadDataInstance.downloadJsonFile()
        if (success) {
            SyncResult.DOWNLOAD_SUCCESS
        } else {
            SyncResult.DOWNLOAD_FAILED
        }
    } else {
        SyncResult.NO_NEW_VERSION
    }
} catch (e: Exception) {
e.printStackTrace()
    SyncResult.DOWNLOAD_FAILED
}
    }

    // load function
private suspend fun     loadDataOnly(loadDataInstance: LoadData) {
     deserializationData = withContext(Dispatchers.IO) {
                loadDataInstance.dataInitialization()
            }
        _isDataReady.value = true
        Log.d("DataRepository", "functia de load a datelor")
        }


    // The function that handles the update "chain" (only for the internet case)
    private suspend fun startUpdateAndLoadChain(loadData: LoadData) {
        val resultOfDownload = withContext(Dispatchers.IO) {
syncRemoteData(loadData)
    }
    when(resultOfDownload) {
        SyncResult.DOWNLOAD_SUCCESS -> {
            deserializationData = withContext(Dispatchers.IO) {loadData.dataInitialization() }
            Log.d("DataRepository", "functia de descarcare a reusit")
        }
        SyncResult.NO_NEW_VERSION -> {
            deserializationData = withContext(Dispatchers.IO) {loadData.dataInitialization()}
            Log.d("DataRepository", "nu este nici o versiune noua")
        }
        SyncResult.DOWNLOAD_FAILED -> {
            println("eror of download ")
            Log.d("DataRepository", "sa produs o eroare")
            deserializationData = withContext(Dispatchers.IO) {loadData.dataInitialization()}
        }
    }
        _isDataReady.value = true
}


    fun isInitialized() = ::deserializationData.isInitialized



}