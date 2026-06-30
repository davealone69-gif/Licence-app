package com.example.data

import kotlinx.coroutines.flow.Flow

class LogbookRepository(private val logbookDao: LogbookDao) {
    val allTrips: Flow<List<TripLog>> = logbookDao.getAllTrips()
    val profileFlow: Flow<LicenceProfile?> = logbookDao.getProfileFlow()

    suspend fun getProfileDirect(): LicenceProfile? {
        return logbookDao.getProfileDirect()
    }

    suspend fun insertTrip(trip: TripLog) {
        logbookDao.insertTrip(trip)
    }

    suspend fun deleteTrip(trip: TripLog) {
        logbookDao.deleteTrip(trip)
    }

    suspend fun insertProfile(profile: LicenceProfile) {
        logbookDao.insertProfile(profile)
    }
}
