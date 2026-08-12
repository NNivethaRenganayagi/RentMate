package com.nivetha.rentmate

import com.google.firebase.firestore.DocumentId

data class Property(
    @DocumentId val id: String = "",
    val title: String = "",
    val ownerId: String = "",
    val location: String = "",
    val rent: String = "",
    val propertyType: String = "",
    val bedrooms: Int = 0,
    val roomType: String = "",
    val amenities: String = "",
    val rules: String = "",
    val leaseDuration: String = "",
    val description: String = ""
)
