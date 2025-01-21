package com.sundalink.mapstracker

import java.time.LocalTime

data class Activity(
    val time: String,
    val description: String,
    val date: String,
    val summaryPlaces: String = ""
)