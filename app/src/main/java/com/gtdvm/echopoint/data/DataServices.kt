package com.gtdvm.echopoint.data

class DataServices {

    private val rootDates = DataRepository.deserializationData

    // 1. Get all category names for Dropdown
    fun getDropdownCategoryName (): List<String> {
        return rootDates.categories.map { it.name }
        }

    // Get the numbers in a given category
    fun getNumbersForCategory (categoryName: String): List<String>{
        return rootDates.categories.find { it.name == categoryName }
            ?.numbers?.map { it.number } ?: emptyList()
    }

    // Get Name by Major
    fun getNameByMajor (major: String): String {
return rootDates.categories.find { it.major == major }?.name ?: ""
    }

    // Get Number after Major and Minor
    fun getNumberByMinor (major: String, minor: String): String {
    return rootDates.categories.find { it.major == major }?.numbers?.find { it.minor == minor }?.number ?: ""
    }

    // Get Major(Int) by category name
    fun getMajorByNameCategory (nameByMajor: String): Int {
    return rootDates.categories.find { it.name == nameByMajor }?.major?.toIntOrNull() ?: 0
    }

    // Get Minor (Int) by category name and route number
    fun getMinorByUnderCategoryName (nameCategoryByMajor: String, nameUnderCategoryByMinor: String): Int {
return rootDates.categories.find { it.name == nameCategoryByMajor }?.numbers?.find { it.number == nameUnderCategoryByMinor }?.minor?.toIntOrNull() ?: 0
    }

    // Get Info by Identifiers (Major/Name and Minor/Number)
    fun getInformationByNumber (categoryIdentifier: String, numberIdentifier: String): String {
    return rootDates.categories.find { it.major == categoryIdentifier || it.name == categoryIdentifier }?.numbers
        ?.find { it.minor == numberIdentifier || it.number == numberIdentifier }?.informations?.takeIf { it != "null" } ?: ""
    }


}
