package com.example.md_project01

import android.location.Location
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

fun getStepDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val earthRadius = 6371000.0 //meters
    val dLat = Math.toRadians((lat2 - lat1))
    val dLng = Math.toRadians((lng2 - lng1))
    val a = sin(dLat/2) * sin(dLat/2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng/2) * sin(dLng/2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return (earthRadius * c)
}

fun getStepDistance(prev: Location?, new: Location): Double {
    return if (prev != null)
        getStepDistance(lat1 = prev.latitude, lat2 = new.latitude, lng1 = prev.longitude, lng2 =  new.longitude)
    else
        0.0
}

const val DAYS_1 = 1000L * 60 * 60 * 24