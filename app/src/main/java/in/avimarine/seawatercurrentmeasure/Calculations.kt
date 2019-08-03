package `in`.avimarine.seawatercurrentmeasure

import android.hardware.GeomagneticField
import android.location.Location
import android.util.Log
import net.sf.geographiclib.Geodesic
import net.sf.geographiclib.GeodesicMask
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * This file is part of an
 * Avi Marine Innovations project: SeaWaterCurrentMeasure
 * first created by aayaffe on 03/08/2019.
 */

private val TAG = "Calculations"

fun getDirString(dir: Double, magnetic: Boolean, fromNotation: Boolean, location: Location, time: Long): String {
    var calcDir = dir
    if (magnetic) {
        val geomagneticField = GeomagneticField(
            location.latitude.toFloat(),
            location.longitude.toFloat(),
            location.altitude.toFloat(),
            time
        )
        Log.d(TAG, "Declination is: " + geomagneticField.declination)
        calcDir += geomagneticField.declination
    }
    if (fromNotation) calcDir = calcDir - 180
    if (calcDir < 0) calcDir = 360 + calcDir
    return String.format("%03d", calcDir.toInt()) + if (magnetic) " M" else ""
}

fun getSpeedString(firstTime: Long, secondTime: Long, dist: Double, units: String = "m_per_min"): String {
    val speed = getSpeed(dist, firstTime, secondTime)
    if (speed > 3000) { //The current is over 95 kts
        return "Error"
    }
    if (units == "m_per_sec") {
        return (if (speed < 10) String.format("%.1f", toMPerSec(speed)) else String.format(
            "%.0f",
            toMPerSec(speed)
        )) + " m/sec"
    } else if (units == "knots") {
        return (if (speed < 10) String.format("%.1f", toKnots(speed)) else String.format(
            "%.0f",
            toKnots(speed)
        )) + " kts"
    } else {
        return (if (speed < 10) String.format("%.1f", speed) else String.format("%.0f", speed)) + " m/min"
    }
}

fun toKnots(speed: Double): Double {
    return speed * 0.0323974
}

fun toMPerSec(speed: Double): Double {
    return speed / 60
}

/**
 * Returns the speed in metres per minute
 * @param dist Distance in metres
 * @param firstTime start time in milliseconds
 * @param secondTime end time in millisecond
 * @return Speed in metres per minute
 */
fun getSpeed(dist: Double, firstTime: Long, secondTime: Long): Double {
    val duration = (secondTime - firstTime).toDouble() / (1000 * 60)
    return dist / duration
}

private val geod = Geodesic.WGS84// This matches EPSG4326, which is the coordinate system used by Geolake

/**
 * Get the distance between two points in meters.
 * @param lat1 First point'getDirString latitude
 * @param lon1 First point'getDirString longitude
 * @param lat2 Second point'getDirString latitude
 * @param lon2 Second point'getDirString longitude
 * @return Distance between the first and the second point in meters
 */
fun getDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val line = geod.InverseLine(
        lat1,
        lon1,
        lat2,
        lon2,
        GeodesicMask.DISTANCE_IN or GeodesicMask.LATITUDE or GeodesicMask.LONGITUDE
    )
    return line.Distance()
}

/**
 * Get the azimuth between two points in degrees.
 * @param lat1 First point'getDirString latitude
 * @param lon1 First point'getDirString longitude
 * @param lat2 Second point'getDirString latitude
 * @param lon2 Second point'getDirString longitude
 * @return Azimuth between the first and the second point in degrees true
 */
fun getDirection(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val line = geod.InverseLine(
        lat1,
        lon1,
        lat2,
        lon2,
        GeodesicMask.DISTANCE_IN or GeodesicMask.LATITUDE or GeodesicMask.LONGITUDE
    )
    return line.Azimuth()
}

fun timeStamptoDateString(timestamp: Long): String {
    val date = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
    val formatter = DateTimeFormatter.ofPattern("(dd)HH:mm:ss")
    return date.format(formatter)
}