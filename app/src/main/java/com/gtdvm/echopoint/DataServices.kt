package com.gtdvm.echopoint

import android.content.Context
//import org.json.JSONArray
import  org.json.JSONException
import org.json.JSONObject
import  java.io.IOException
import  java.nio.charset.Charset


class DataServices {
    fun getDropdownCategoryName (context: Context): MutableList<String> {
        val categories = mutableListOf<String>()
        try {
            val  jsonString = loadJsonDataFromRaw(context, R.raw.data)
            val jsonObject = JSONObject(jsonString)
            val categoriesArray = jsonObject.getJSONArray("Categories")
            for (i in 0 until categoriesArray.length()) {
                val categoryObject = categoriesArray.getJSONObject(i)
                val categoryName = categoryObject.getString("Name")
                categories.add(categoryName)
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return categories
    }

    private fun loadJsonDataFromRaw(context: Context, resourceId: Int): String {
        val inputStream = context.resources.openRawResource(resourceId)
        val size = inputStream.available()
        val buffer = ByteArray(size)
        inputStream.read(buffer)
        inputStream.close()
        return buffer.toString(Charset.defaultCharset())
    }

    fun getNumbersForCategory (context: Context, categoryName: String): List<String>{
        val numbers = mutableListOf<String>()
        try {
            val  jsonString = loadJsonDataFromRaw(context, R.raw.data)
            val jsonObject = JSONObject(jsonString)
            val categoriesArray =jsonObject.getJSONArray("Categories")
            for (i in 0 until categoriesArray.length()) {
                val categoryObject = categoriesArray.getJSONObject(i)
                if (categoryObject.getString("Name") == categoryName) {
                    val numbersArray = categoryObject.getJSONArray("Numbers")
                    for (j in 0 until numbersArray.length()) {
                        val numberObject = numbersArray.getJSONObject(j)
                        val number = numberObject.getString("Number")
                        numbers.add(number)
                    }
                    break
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return numbers
    }

    fun getNameByMajor (context: Context, major: String): String {
        var name = ""
        try {
            val categoryObject = JSONObject (loadJsonDataFromRaw(context, R.raw.data))
            val categoriesArray = categoryObject.getJSONArray("Categories")
            for (i in 0 until categoriesArray.length()){
                val currentObject = categoriesArray.getJSONObject(i)
                if (currentObject.getString("Major") == major){
                    name = currentObject.getString("Name")
                    break
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        } catch (e: IOException){
            e.printStackTrace()
        }
        return name
    }

    fun getNumberByMinor (context: Context, major: String, minor: String): String {
        var numberName =""
        try {
            val currentJson = JSONObject (loadJsonDataFromRaw(context, R.raw.data))
            val categoriesArray = currentJson.getJSONArray("Categories")
            for (i in 0 until categoriesArray.length()) {
                val currentObject = categoriesArray.getJSONObject(i)
                if (currentObject.getString("Major") == major){
                    val numberObjectArray = currentObject.getJSONArray("Numbers")
                    for (j in 0 until numberObjectArray.length()){
                        val currentNumberObject = numberObjectArray.getJSONObject(j)
                        if (currentNumberObject.getString("Minor") == minor){
                            numberName = currentNumberObject.getString("Number")
                            break
                        }
                    }
                    break
                }
            }
        } catch (e: JSONException){
            e.printStackTrace()
        } catch (e: IOException){
            e.printStackTrace()
        }
        return numberName
    }

    fun getMajorByNameCategory (context: Context, nameByMajor: String): Int {
        var name = ""
        try {
            val categoryObject = JSONObject (loadJsonDataFromRaw(context, R.raw.data))
            val categoriesArray = categoryObject.getJSONArray("Categories")
            for (i in 0 until categoriesArray.length()){
                val currentObject = categoriesArray.getJSONObject(i)
                if (currentObject.getString("Name") == nameByMajor){
                    name = currentObject.getString("Major")
                    break
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        } catch (e: IOException){
            e.printStackTrace()
        }
        return Integer.parseInt(name)
    }

    fun getMinorByUnderCategoryName (context: Context, nameCategoryByMajor: String, nameUnderCategoryByMinor: String): Int {
        var numberName =""
        try {
            val currentJson = JSONObject (loadJsonDataFromRaw(context, R.raw.data))
            val categoriesArray = currentJson.getJSONArray("Categories")
            for (i in 0 until categoriesArray.length()) {
                val currentObject = categoriesArray.getJSONObject(i)
                if (currentObject.getString("Name") == nameCategoryByMajor){
                    val numberObjectArray = currentObject.getJSONArray("Numbers")
                    for (j in 0 until numberObjectArray.length()){
                        val currentNumberObject = numberObjectArray.getJSONObject(j)
                        if (currentNumberObject.getString("Number") == nameUnderCategoryByMinor){
                            numberName = currentNumberObject.getString("Minor")
                            break
                        }
                    }
                    break
                }
            }
        } catch (e: JSONException){
            e.printStackTrace()
        } catch (e: IOException){
            e.printStackTrace()
        }
        return Integer.parseInt(numberName)
    }

    fun getInformationByNumber (context: Context, categoryIdentifier: String, numberIdentifier: String): String {
        var numberInformation =""
        try {
            val currentJson = JSONObject (loadJsonDataFromRaw(context, R.raw.data))
            val categoriesArray = currentJson.getJSONArray("Categories")
            for (i in 0 until categoriesArray.length()) {
                val currentObject = categoriesArray.getJSONObject(i)
                if (currentObject.getString("Major") == categoryIdentifier || currentObject.getString("Name") == categoryIdentifier){
                    val numberObjectArray = currentObject.getJSONArray("Numbers")
                    for (j in 0 until numberObjectArray.length()){
                        val currentNumberObject = numberObjectArray.getJSONObject(j)
                        if (currentNumberObject.getString("Minor") == numberIdentifier || currentNumberObject.getString("Number") == numberIdentifier){
                            numberInformation = currentNumberObject.getString("Informations")
                            break
                        }
                    }
                    break
                }
            }
        } catch (e: JSONException){
            e.printStackTrace()
        } catch (e: IOException){
            e.printStackTrace()
        }
        return numberInformation.takeIf { it != "null" } ?: ""
    }

    /*fun getInformationForNumberByName (context: Context, categoryIdentifier: String, numberIdentifier: String): String {
        var numberInformation =""
        try {
            val currentJson = JSONObject (loadJsonDataFromRaw(context, R.raw.data))
            val categoriesArray = currentJson.getJSONArray("Categories")
            for (i in 0 until categoriesArray.length()) {
                val currentObject = categoriesArray.getJSONObject(i)
                if (currentObject.getString("Major") == categoryIdentifier || currentObject.getString("Name") == categoryIdentifier){
                    val numberObjectArray = currentObject.getJSONArray("Numbers")
                    for (j in 0 until numberObjectArray.length()){
                        val currentNumberObject = numberObjectArray.getJSONObject(j)
                        if (currentNumberObject.getString("Minor") == numberIdentifier || currentNumberObject.getString("Number") == numberIdentifier){
                            numberInformation = currentNumberObject.getString("Informations")
                            break
                        }
                    }
                    break
                }
            }
        } catch (e: JSONException){
            e.printStackTrace()
        } catch (e: IOException){
            e.printStackTrace()
        }
        return numberInformation
    }*/




}
