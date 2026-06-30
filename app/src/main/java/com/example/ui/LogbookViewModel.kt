package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LogbookViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: LogbookRepository

    val allTrips: StateFlow<List<TripLog>>
    val profile: StateFlow<LicenceProfile>

    init {
        val database = LogbookDatabase.getDatabase(application)
        val dao = database.logbookDao()
        repository = LogbookRepository(dao)

        allTrips = repository.allTrips.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Fallback to empty profile, but we filter to ensure we get updates
        profile = repository.profileFlow
            .map { it ?: LicenceProfile() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = LicenceProfile()
            )

        // Seed with delicious data on first startup to make it immediately operational
        viewModelScope.launch {
            val existing = repository.getProfileDirect()
            if (existing == null) {
                repository.insertProfile(LicenceProfile.default())
                
                // Seed 3 realistic trips to show how compliance tracking behaves:
                // Trip 1: Fully compliant work commute
                repository.insertTrip(
                    TripLog(
                        date = "2026-06-15",
                        startTime = "08:10",
                        endTime = "08:50",
                        startOdometer = 45210,
                        endOdometer = 45242,
                        vehicle = "1ABC123",
                        purpose = "Employment",
                        route = "Perth Northern Hwy to City Centre",
                        notes = "Standard Monday morning commute. Compliant."
                    )
                )
                
                // Trip 2: Overtime work trip (violates the 18:00 finish time)
                repository.insertTrip(
                    TripLog(
                        date = "2026-06-17",
                        startTime = "17:40",
                        endTime = "18:45",
                        startOdometer = 45242,
                        endOdometer = 45274,
                        vehicle = "1ABC123",
                        purpose = "Employment",
                        route = "City Centre to Home",
                        notes = "Traffic delay caused arrival after 18:00 (Allowed finish)."
                    )
                )

                // Trip 3: Personal weekend shopping trip (unauthorized day and vehicle mismatch)
                repository.insertTrip(
                    TripLog(
                        date = "2026-06-20", // Saturday (Not Mon-Fri)
                        startTime = "11:30",
                        endTime = "12:15",
                        startOdometer = 120510,
                        endOdometer = 120525,
                        vehicle = "9XYZ789", // Wrong car
                        purpose = "Shopping", // Invalid purpose
                        route = "Suburban residential streets",
                        notes = "Non-compliant family trip. Demonstration only."
                    )
                )
            }
        }
    }

    fun addTrip(trip: TripLog) {
        viewModelScope.launch {
            repository.insertTrip(trip)
        }
    }

    fun deleteTrip(trip: TripLog) {
        viewModelScope.launch {
            repository.deleteTrip(trip)
        }
    }

    fun updateProfile(newProfile: LicenceProfile) {
        viewModelScope.launch {
            repository.insertProfile(newProfile)
        }
    }
}
