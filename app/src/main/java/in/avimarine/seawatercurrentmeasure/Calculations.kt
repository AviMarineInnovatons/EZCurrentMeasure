package `in`.avimarine.seawatercurrentmeasure

import android.hardware.GeomagneticField
import android.location.Location
import android.util.Log
import com.mapbox.geojson.Point
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
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
    if (fromNotation) {
        calcDir -= 180
    }
    if (calcDir < 0) {
        calcDir += 360
    }
    return String.format("%03d", Math.round(calcDir)) + if (magnetic) " M" else ""
}

fun getDirErrorString(dir:Double):String{
    return String.format("%d", Math.round(dir))
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

fun getDistance(firstLocation : Location, secondLocation: Location): Double {
    return getDistance(firstLocation.latitude, firstLocation.longitude, secondLocation.latitude, secondLocation.longitude)
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
fun getDirection(firstLocation: Location, secondLocation: Location): Double {
    return getDirection(firstLocation.latitude,firstLocation.longitude,secondLocation.latitude,secondLocation.longitude)
}

fun timeStamptoDateString(timestamp: Long): String {
    val date = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
    val formatter = DateTimeFormatter.ofPattern("(dd)HH:mm:ss")
    return date.format(formatter)
}

fun Location.toPoint() : Point {
    return Point.fromLngLat(this.longitude,this.latitude)
}

/**
 * Returns Error in degrees (total error equals +/- ret)
 * err1, err2 in meters
 */
fun getDirError(firstLocation: Location, secondLocation: Location): Double {
    val ber = getDirection(firstLocation,secondLocation)
    val tmpLocA = TurfMeasurement.destination(firstLocation.toPoint(),
        firstLocation.accuracy.toDouble(),(ber-90)%360,TurfConstants.UNIT_METERS)
    val tmpLocB = TurfMeasurement.destination(secondLocation.toPoint(),
        secondLocation.accuracy.toDouble(),(ber+90)%360,TurfConstants.UNIT_METERS)
    var res = TurfMeasurement.bearing(tmpLocA,tmpLocB)
    res = (res-ber)
    return res
}

/**
 * @return Speed in metres per minute
 */
fun getSpdError(firstLocation: Location, secondLocation: Location): Double {
    val dist = firstLocation.accuracy + secondLocation.accuracy;
    val duration = (secondLocation.time - firstLocation.time).toDouble() / (1000 * 60)
    return dist / duration
}