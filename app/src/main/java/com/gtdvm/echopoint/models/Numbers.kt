package com.gtdvm.echopoint.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Numbers
    (
    @SerialName("Number")
    val number: String,
    @SerialName("Minor")
            val minor: String,
    @SerialName("Informations")
val informations: String?
)
