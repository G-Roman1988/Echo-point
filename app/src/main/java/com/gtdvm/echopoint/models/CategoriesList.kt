package com.gtdvm.echopoint.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class CategoriesList
    (
    @SerialName("Name")
    val name: String,
    @SerialName("Major")
            val major: String,
    @SerialName("Numbers")
            val numbers: List<Numbers>
)
