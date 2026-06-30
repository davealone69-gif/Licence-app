package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LogbookDao {

    @Query("SELECT * FROM trip_logs ORDER BY date DESC, startTime DESC")
    fun getAllTrips(): Flow<List<TripLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripLog)

    @Delete
    suspend fun deleteTrip(trip: TripLog)

    @Query("SELECT * FROM licence_profile WHERE id = 1 LIMIT 1")
    fun getProfileFlow(): Flow<LicenceProfile?>

    @Query("SELECT * FROM licence_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfileDirect(): LicenceProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: LicenceProfile)
}
