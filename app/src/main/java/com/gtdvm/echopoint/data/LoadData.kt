package com.gtdvm.echopoint.data

import android.content.Context
import com.gtdvm.echopoint.R
import com.gtdvm.echopoint.models.RootCategories
import kotlinx.serialization.json.Json
import java.net.URL
import java.net.HttpURLConnection
import java.io.File
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class LoadData(private val context: Context) {
// We use applicationContext to avoid memory leaks
    private val appContext = context.applicationContext
// name of file saved in memory after download
    private val fileName = "data.json"
    // URL to remote json file to download
    private val remoteFileURL = URL("https://cloud.firesvision.com/public.php/dav/files/trTd98yqydX2Tib/?accept=zip")

    // function that reads the json file from local memory and returns as a string
    private fun loadJsonFileOfLocal(): String {
        val localFile = appContext.getFileStreamPath(fileName)
        return localFile.readText()
    }

// function that reads the json file from raw
private fun loadJsonFileOfRaw(): String{
return appContext.resources.openRawResource(R.raw.data).bufferedReader()
    .use { it.readText() }
}

    //data initialization function
    fun dataInitialization(): RootCategories{
        val jsonParser = Json {ignoreUnknownKeys = true}
        return if (isDownloadedFile()){
            jsonParser.decodeFromString<RootCategories>(loadJsonFileOfLocal())
        } else{
            jsonParser.decodeFromString<RootCategories>(loadJsonFileOfRaw())
        }
    }

    //function that downloads the file from the URL
suspend fun             downloadJsonFile(): Boolean{
        // We run on a secondary thread
return withContext(Dispatchers.IO){
try {
    val connection = remoteFileURL.openConnection() as HttpURLConnection
    connection.connect()
if (connection.responseCode == HttpURLConnection.HTTP_OK) {
    val jsonContent = connection.inputStream.bufferedReader().use { it.readText() }
    context.openFileOutput(fileName, Context.MODE_PRIVATE).use { output ->
        output.write(jsonContent.toByteArray())
    }
    val serverLastModified = connection.getHeaderField("Last-Modified")
    val serverETag = connection.getHeaderField("ETag")?.replace("\"", "")
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().apply {
        putString(LAST_MODIFIED_KEY, serverLastModified)
        putString(ETAG_KEY, serverETag)
        apply()
    }
    true
} else{
    false
}
} catch (e: Exception){
    e.printStackTrace()
    false
}
}
    }

    //function that checks the json file if it is downloaded
    fun isDownloadedFile(): Boolean{
        val file = context.getFileStreamPath(fileName)
        return file.exists()
    }

    // json file version check function
    suspend fun isNewVersionAvailable(): Boolean{
        return withContext(Dispatchers.IO){
try {
    val connection = remoteFileURL.openConnection() as HttpURLConnection
connection.requestMethod = "HEAD"
    connection.connect()
    val serverLastModified = connection.getHeaderField("Last-Modified")
    val serverETag = connection.getHeaderField("ETag")?.replace("\"", "")
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val localLastModified = prefs.getString(LAST_MODIFIED_KEY, null)
val localETag = prefs.getString(ETAG_KEY, null)
    connection.disconnect()
    if (serverLastModified != null && serverLastModified != localLastModified) return@withContext true
    if (serverETag != null && serverETag != localETag) return@withContext true
    false
} catch (e: Exception){
e.printStackTrace()
    false
}
        }
    }



    //properties for saving data about changes to json file
companion object{
    private const val         PREFS_NAME = "UpdatePrefs"
    private const val LAST_MODIFIED_KEY = "last_modified_date"
    private const val ETAG_KEY = "etag_key"

}


}