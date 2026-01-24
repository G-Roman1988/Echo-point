package com.gtdvm.echopoint

import com.gtdvm.echopoint.data.DataServices

@Suppress("MayBeConstant")
object SelectedDevice {
    private lateinit var dataServices: DataServices
    private val defaultCategory: Int = 0
    private val defaultUnderCategory: Int = 0
    private var selectedCategory: Int = defaultCategory
    private var selectedUnderCategory: Int = defaultUnderCategory
    private var tempCategoryValue: String = ""

    fun setCategory(categoryName: String){
        this.tempCategoryValue = categoryName
        dataServices = DataServices()
        this.selectedCategory = dataServices.getMajorByNameCategory(categoryName)
    }

    fun setUnderCategory(underCategoryName: String){
        dataServices = DataServices()
        this.selectedUnderCategory = dataServices.getMinorByUnderCategoryName(tempCategoryValue, underCategoryName)
    }

    @Suppress("KotlinConstantConditions")
    fun isSelectedDevice(categorySelected: Int, underCategorySelected: Int): Boolean {
return if (selectedCategory > defaultCategory && categorySelected == selectedCategory){
when{
categorySelected == selectedCategory && underCategorySelected == selectedUnderCategory && selectedUnderCategory != defaultUnderCategory -> true
categorySelected == selectedCategory && underCategorySelected > selectedUnderCategory && selectedUnderCategory == defaultUnderCategory -> true
    else -> false
}
} else if (categorySelected > selectedCategory && selectedCategory == defaultCategory){
when{
    categorySelected > selectedCategory && underCategorySelected > selectedUnderCategory && selectedUnderCategory == defaultUnderCategory -> true
    else -> false
}
}
else{
    false
}
    }

    fun isAllItems(items: String): Boolean {
        dataServices = DataServices()
        val underItems = dataServices.getMinorByUnderCategoryName(tempCategoryValue, items)
       return (selectedCategory > defaultCategory && underItems == selectedUnderCategory && selectedUnderCategory == defaultUnderCategory)
    }

    fun resetSelection(){
        selectedCategory = defaultCategory
        selectedUnderCategory = defaultUnderCategory
    }


}