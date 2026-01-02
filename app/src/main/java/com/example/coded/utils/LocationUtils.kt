package com.herdmat.coded.utils

import com.google.android.gms.maps.model.LatLng
import android.location.Location
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

fun getDistanceToProvider(jobLocation: LatLng, providerLocation: LatLng): Double {
    val earthRadius = 6371.0 // km
    val dLat = Math.toRadians(providerLocation.latitude - jobLocation.latitude)
    val dLng = Math.toRadians(providerLocation.longitude - jobLocation.longitude)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(jobLocation.latitude)) * cos(Math.toRadians(providerLocation.latitude)) *
            sin(dLng / 2) * sin(dLng / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return earthRadius * c
}