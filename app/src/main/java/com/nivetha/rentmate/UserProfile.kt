package com.nivetha.rentmate

data class UserProfile(
    val fullName: String = "",
    val age: String = "",
    val role: String = "",
    val location: String = "",
    val budget: String = "",
    val preferredLocation: String = "",
    val roomType: String = "",
    val cleanliness: Int = 0,
    val sleepSchedule: String = "",
    val foodPreference: String = "",
    val smoking: Boolean = false,
    val pets: Boolean = false,
    val leaseDuration: String = "",
    val amenities: String = ""
)
