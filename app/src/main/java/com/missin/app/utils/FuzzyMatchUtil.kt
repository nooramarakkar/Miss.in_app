package com.missin.app.utils

import com.missin.app.data.model.FoundItem
import com.missin.app.data.model.LostItem
import kotlin.math.max

object FuzzyMatchUtil {
    fun calculateMatchScore(lost: LostItem, found: FoundItem): Float {
        var score = 0.0f
        
        // 1. Category (50%)
        if (lost.category.equals(found.category, ignoreCase = true)) {
            score += 0.50f
        }

        // 2. Location Haversine (30%)
        val locMatch = calculateLocationMatch(lost.location?.latitude ?: 0.0, lost.location?.longitude ?: 0.0, found.location?.latitude ?: 0.0, found.location?.longitude ?: 0.0)
        score += (0.30f * locMatch)

        // 3. Date Proximity (20%)
        val diffMillis = kotlin.math.abs(lost.timestamp - found.timestamp)
        val diffDays = diffMillis.toFloat() / (1000 * 60 * 60 * 24).toFloat()
        if (diffDays <= 2f) {
            score += 0.20f
        }

        return score
    }

    private fun calculateLocationMatch(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val r = 6371.0 // Radius of Earth in km
        val latDistance = Math.toRadians(lat2 - lat1)
        val lonDistance = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(latDistance / 2) * kotlin.math.sin(latDistance / 2) +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(lonDistance / 2) * kotlin.math.sin(lonDistance / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        val distance = r * c

        // If less than 3km, 100% spatial match.
        // Between 3km and 30km, scale down.
        // Above 30km, 0% spatial match.
        return when {
            distance <= 3.0 -> 1f
            distance >= 30.0 -> 0f
            else -> 1f - ((distance - 3.0) / 27.0).toFloat()
        }
    }
}
