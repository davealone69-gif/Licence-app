package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Entity(tableName = "licence_profile")
data class LicenceProfile(
    @PrimaryKey val id: Int = 1,
    val holderName: String = "",
    val licenceNumber: String = "",
    val allowedDays: String = "Mon,Tue,Wed,Thu,Fri", // Comma-separated shorthand
    val allowedStartTime: String = "07:00", // 24-hour style HH:mm
    val allowedEndTime: String = "17:00", // 24-hour style HH:mm
    val allowedVehicle: String = "", // e.g. 1ABC123
    val allowedPurposes: String = "Employment", // e.g., Employment, Education, Medical
    val targetRequiredHours: Int = 0 // Some courts mandate logging specific hours, e.g., 50. 0 means optional.
) {
    // Companion helper to generate default/empty profile
    companion object {
        fun default() = LicenceProfile(
            holderName = "Jane Doe",
            licenceNumber = "EDL-9843-02",
            allowedDays = "Mon,Tue,Wed,Thu,Fri",
            allowedStartTime = "07:00",
            allowedEndTime = "18:00",
            allowedVehicle = "1ABC123",
            allowedPurposes = "Employment, Medical",
            targetRequiredHours = 50
        )
    }
}

@Entity(tableName = "trip_logs")
data class TripLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // YYYY-MM-DD
    val startTime: String, // HH:mm
    val endTime: String, // HH:mm
    val startOdometer: Int,
    val endOdometer: Int,
    val vehicle: String,
    val purpose: String,
    val route: String,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    val distanceKm: Int
        get() = (endOdometer - startOdometer).coerceAtLeast(0)

    val durationMinutes: Int
        get() {
            return try {
                val startParts = startTime.split(":").map { it.toInt() }
                val endParts = endTime.split(":").map { it.toInt() }
                if (startParts.size == 2 && endParts.size == 2) {
                    val startMin = startParts[0] * 60 + startParts[1]
                    val endMin = endParts[0] * 60 + endParts[1]
                    if (endMin >= startMin) {
                        endMin - startMin
                    } else {
                        // Overnight trip (uncommon but handles 24hr wrap-around)
                        (1440 - startMin) + endMin
                    }
                } else 0
            } catch (e: Exception) {
                0
            }
        }

    /**
     * Inspects if this trip conforms to the active Extraordinary Licence conditions.
     * Returns a Pair: Boolean (isCompliant), and String (reason/description if non-compliant)
     */
    fun checkCompliance(profile: LicenceProfile): Pair<Boolean, String> {
        val infractions = mutableListOf<String>()

        // 1. Vehicle Match
        if (profile.allowedVehicle.isNotBlank() && !vehicle.equals(profile.allowedVehicle, ignoreCase = true)) {
            infractions.add("Vehicle mismatch: entered '$vehicle', allowed '${profile.allowedVehicle}'")
        }

        // 2. Day of Week Match
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dateObj = sdf.parse(date)
            if (dateObj != null) {
                val cal = Calendar.getInstance()
                cal.time = dateObj
                // Convert Calendar.DAY_OF_WEEK to short string Mon, Tue, etc.
                val dayStr = when (cal.get(Calendar.DAY_OF_WEEK)) {
                    Calendar.MONDAY -> "Mon"
                    Calendar.TUESDAY -> "Tue"
                    Calendar.WEDNESDAY -> "Wed"
                    Calendar.THURSDAY -> "Thu"
                    Calendar.FRIDAY -> "Fri"
                    Calendar.SATURDAY -> "Sat"
                    Calendar.SUNDAY -> "Sun"
                    else -> ""
                }
                val allowedDaysList = profile.allowedDays.split(",").map { it.trim() }
                if (!allowedDaysList.contains(dayStr)) {
                    infractions.add("Driving day not permitted: '$dayStr'")
                }
            }
        } catch (e: Exception) {
            // parsing error, ignore
        }

        // 3. Time Match (within allowedStartTime to allowedEndTime)
        try {
            val tripStartMinutes = timeToMinutes(startTime)
            val tripEndMinutes = timeToMinutes(endTime)

            val allowedStartMinutes = timeToMinutes(profile.allowedStartTime)
            val allowedEndMinutes = timeToMinutes(profile.allowedEndTime)

            if (tripStartMinutes < allowedStartMinutes || tripStartMinutes > allowedEndMinutes) {
                infractions.add("Trip start time ($startTime) outside allowed hours (${profile.allowedStartTime} - ${profile.allowedEndTime})")
            }
            if (tripEndMinutes < allowedStartMinutes || tripEndMinutes > allowedEndMinutes) {
                infractions.add("Trip end time ($endTime) outside allowed hours (${profile.allowedStartTime} - ${profile.allowedEndTime})")
            }
        } catch (e: Exception) {
            // parsing error, ignore
        }

        // 4. Purpose check
        val allowedPurposesList = profile.allowedPurposes.split(",").map { it.trim().lowercase() }
        if (allowedPurposesList.isNotEmpty() && purpose.isNotBlank()) {
            val tripPurposeLower = purpose.lowercase()
            val containsAllowed = allowedPurposesList.any { allowed ->
                tripPurposeLower.contains(allowed) || allowed.contains(tripPurposeLower)
            }
            if (!containsAllowed) {
                infractions.add("Trip purpose '$purpose' may not match allowed conditions: '${profile.allowedPurposes}'")
            }
        }

        return if (infractions.isEmpty()) {
            true to "In full compliance with licence conditions."
        } else {
            false to infractions.joinToString("\n")
        }
    }

    private fun timeToMinutes(timeStr: String): Int {
        val parts = timeStr.trim().split(":")
        if (parts.size >= 2) {
            val hours = parts[0].toIntOrNull() ?: 0
            val minutes = parts[1].toIntOrNull() ?: 0
            return hours * 60 + minutes
        }
        return 0
    }
}
