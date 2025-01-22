package com.gtdvm.echopoint

//import android.icu.number.IntegerWidth
//import androidx.annotation.IntegerRes
import android.content.Context

@Suppress("MayBeConstant")
object SelectedDevice {
    private lateinit var dataServices: DataServices
    private val defaultCategory: Int = 0
    private val defaultUnderCategory: Int = 0
    private var selectedCategory: Int = defaultCategory
    private var selectedUnderCategory: Int = defaultUnderCategory
    private var tempCategoryValue: String = ""

    fun setCategory(context: Context, categoryName: String){
        this.tempCategoryValue = categoryName
        dataServices = DataServices()
        this.selectedCategory = dataServices.getMajorByNameCategory(context, categoryName)
    }

    fun setUnderCategory(context: Context, underCategoryName: String){
        dataServices = DataServices()
        this.selectedUnderCategory = dataServices.getMinorByUnderCategoryName(context, tempCategoryValue, underCategoryName)
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

    fun isAllItems(context: Context, items: String): Boolean {
        dataServices = DataServices()
        val underItems = dataServices.getMinorByUnderCategoryName(context, tempCategoryValue, items)
       return (selectedCategory > defaultCategory && underItems == selectedUnderCategory && selectedUnderCategory == defaultUnderCategory)
    }

    fun resetSelection(){
        selectedCategory = defaultCategory
        selectedUnderCategory = defaultUnderCategory
    }


}