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
import android.util.Log

class LoadData(private val context: Context) {
// We use applicationContext to avoid memory leaks
    private val appContext = context.applicationContext
// name of file saved in memory after download
    private val fileName = "data.json"
    // the full path to the local json file (data.json) in the application's internal storage.
    private val pathLocalJson: File get() = context.getFileStreamPath(fileName)
    // the full path to the temporary file, used as an intermediate destination
    private val tempFile: File get() = context.getFileStreamPath("$fileName.tmp")
    // URL to remote json file to download
    private val remoteFileURL = URL("https://cloud.firesvision.com/index.php/s/trTd98yqydX2Tib/download")

    // function that reads the json file from local memory and returns as a string
    private fun loadJsonFileOfLocal(): String {
        //val localFile = appContext.getFileStreamPath(fileName)
        return pathLocalJson.readText()
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

    // Normalize ETag: remove quotes, "-gzip" suffix (added by Apache
    private fun extractEtagFormZip(rawETag: String?): String? {
if (rawETag == null) return null
        return rawETag.replace("\"", "")
            .removeSuffix("-gzip")
            .removePrefix("W/")
    }

    //function that downloads the file from the URL
suspend fun downloadJsonFile(): Boolean{
        // We run on a secondary thread
return withContext(Dispatchers.IO){
    val connection = remoteFileURL.openConnection() as HttpURLConnection
try {
    connection.instanceFollowRedirects = true
    connection.connect()
    Log.d("LoadData", "downloadJsonFile() - response code: ${connection.responseCode}")
    Log.d("LoadData", "downloadJsonFile() Last-Modified RAW din raspuns: ${connection.getHeaderField("Last-Modified")}")
    Log.d("LoadData", "downloadJsonFile() ETag RAW din raspuns: ${connection.getHeaderField("ETag")}")
    Log.d("LoadData", "downloadJsonFile() URL final (dupa redirect): ${connection.url}")
if (connection.responseCode == HttpURLConnection.HTTP_OK) {
    try {
        connection.inputStream.use { input ->
tempFile.outputStream().use { output ->
    input.copyTo(output)
}
        }
    } catch (e: Exception) {
        Log.d("LoadData", "downloadJsonFile() descarcare intrerupta, sterg fisierul temporar: ${e.message}", e)
        tempFile.delete()
        return@withContext false
    }
val isDataValidation = try {
val jsonParser = Json { ignoreUnknownKeys = true }
    jsonParser.decodeFromString<RootCategories>(tempFile.readText())
    true
} catch (e: Exception) {
    Log.d("LoadData", "downloadJsonFile() fisierul descarcat nu e JSON valid, il ignoram: ${e.message}", e)
    false
}
if (!isDataValidation) {
    tempFile.delete()
        return@withContext false
}
    val renamed = tempFile.renameTo(pathLocalJson)
    if (!renamed) {
        Log.e("LoadData", "downloadJsonFile() rename-ul fisierului temporar a esuat")
        tempFile.delete()
        return@withContext false
    }

    //val jsonContent = connection.inputStream.bufferedReader().use { it.readText() }
    //context.openFileOutput(fileName, Context.MODE_PRIVATE).use { output ->
        //output.write(jsonContent.toByteArray())
    //}
    val serverLastModified = connection.getHeaderField("Last-Modified")
    val serverETag = extractEtagFormZip(connection.getHeaderField("ETag"))
    Log.d("LoadData", "downloadJsonFile() SALVAM in prefs -> Last-Modified: $serverLastModified | ETag normalizat: $serverETag")
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().apply {
        putString(LAST_MODIFIED_KEY, serverLastModified)
        putString(ETAG_KEY, serverETag)
        apply()
    }
    true
} else{
    Log.d("LoadData", "downloadJsonFile() FAILED - response code nu este 200: ${connection.responseCode}")
    false
}
} catch (e: Exception){
    Log.e("LoadData", "downloadJsonFile() EXCEPTION: ${e.message}", e)
    e.printStackTrace()
    false
} finally {
    connection.disconnect()
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
connection.instanceFollowRedirects = true
    //connection.setRequestProperty("Accept-Encoding", "identity")
connection.requestMethod = "HEAD"
    connection.connect()
    val serverLastModified = connection.getHeaderField("Last-Modified")
    val serverETag = extractEtagFormZip(connection.getHeaderField("ETag"))
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val localLastModified = prefs.getString(LAST_MODIFIED_KEY, null)
val localETag = prefs.getString(ETAG_KEY, null)
    Log.d("LoadData", "===== isNewVersionAvailable() - HEAD request =====")
    Log.d("LoadData", "isNewVersionAvailable() response code: ${connection.responseCode}")
    Log.d("LoadData", "isNewVersionAvailable() URL final (dupa redirect): ${connection.url}")
    Log.d("LoadData", "isNewVersionAvailable() SERVER Last-Modified: '$serverLastModified'  |  LOCAL Last-Modified: '$localLastModified'")
    Log.d("LoadData", "isNewVersionAvailable() SERVER ETag: '$serverETag'  |  LOCAL ETag: '$localETag'")
    Log.d("LoadData", "isNewVersionAvailable() TOATE HEADERELE: ${connection.headerFields}")
    connection.disconnect()
    if (serverLastModified != null && serverLastModified != localLastModified){
        Log.d("LoadData", "isNewVersionAvailable() -> TRUE (Last-Modified diferit)")
        return@withContext true
    }
    if (serverETag != null && serverETag != localETag){
        Log.d("LoadData", "isNewVersionAvailable() -> TRUE (ETag diferit)")
        return@withContext true
    }
    Log.d("LoadData", "isNewVersionAvailable() -> FALSE (nicio schimbare detectata)")
    false
} catch (e: Exception){
    Log.d("LoadData", "isNewVersionAvailable() EXCEPTION: ${e.message}", e)
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