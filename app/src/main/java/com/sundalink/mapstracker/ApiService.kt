package com.sundalink.mapstracker

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface ApiService {

    @GET("api/v1/umroh-schedules/{scheduleId}/daily-programs")
    fun getDays(
        @Path("scheduleId") scheduleId: String,
        @Header("Authorization") token: String
    ): Call<List<Day>>

    @GET("api/v1/umroh-schedules/{scheduleId}/daily-programs/{dayId}/activities")
    fun getActivities(
        @Path("scheduleId") scheduleId: String,
        @Path("dayId") dayId: String,
        @Header("Authorization") token: String
    ): Call<List<Activity>>
}