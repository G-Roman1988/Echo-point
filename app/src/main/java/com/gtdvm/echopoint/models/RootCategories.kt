package com.gtdvm.echopoint.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class RootCategories
    (
    @SerialName("Categories")
    val categories: List<CategoriesList>
)
