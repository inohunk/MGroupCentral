package ru.hunkel.mgroupcentral.database.dao

import androidx.room.*
import database.entities.*

/**
 * Interface for communicate with database
 */
@Dao
interface TrackingDao {

    //Events
    @Insert
    fun addEvent(event: Events)

    @Update
    fun updateEvent(event: Events)

    @Delete
    fun deleteEvent(event: Events)

    @Query("SELECT * FROM events WHERE id = :id")
    fun getEventById(id: Int): Events

    @Query("SELECT * FROM events ORDER BY ID DESC LIMIT 1")
    fun getLastEvent(): Events

    @Query("SELECT * FROM events ORDER BY ID DESC")
    fun getAllEvents(): List<Events>

    //ControlPoints
    @Insert
    fun addControlPoint(punch: ControlPoints)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateControlPoint(punch: ControlPoints)

    @Query("SELECT * FROM ControlPoints ORDER BY ID DESC LIMIT 1")
    fun getLastControlPoint(): ControlPoints

    @Query("SELECT * FROM ControlPoints WHERE eventId = :id")
    fun getControlPointsByEventId(id: Int): List<ControlPoints>

    @Query("SELECT * FROM ControlPoints WHERE eventId = :id ORDER BY ControlPoints.time ASC")
    fun getControlPointsByEventIdWithAscSorting(id: Int): List<ControlPoints>

    @Query("SELECT * FROM ControlPoints WHERE time > :startTime AND time < :endTime")
    fun getControlPointsByTime(startTime: Long, endTime: Long): List<ControlPoints>

    //Punches

    @Insert
    fun addPunch(punch: Punches): Long

    @Query("SELECT * FROM punches ORDER BY ID DESC LIMIT 1")
    fun getLastPunch(): Punches

    @Query("DELETE FROM PUNCHES WHERE controlPoint = :id")
    fun deletePunchByControlPointId(id: Int)

    @Query("SELECT * FROM punches WHERE controlPoint = :cp")
    fun getPunchByControlPoint(cp: Int): Punches


    @Query("SELECT * FROM controlpoints WHERE (time > :startTime AND time < :endTime) AND controlPointNumber = :controlPointNumber AND eventId = :eventId")
    fun getControlPointsByTimeAndControlPoint(
        startTime: Long,
        endTime: Long,
        controlPointNumber: Int,
        eventId: Int
    ): List<ControlPoints>

    /**
     * Use this function for getting punches before certain time stamp
     */
    @Query("SELECT * FROM punches WHERE time < :time")
    fun getPunchesBeforeCertainTime(time: Long): List<Punches>

    @Query("SELECT DISTINCT * FROM punches WHERE eventId = :id")
    fun getPunchesByEventId(id: Int): List<Punches>

    //User Info
    @Insert
    fun addUserInfo(userInfo: UserInfo)

    @Query("SELECT * FROM UserInfo WHERE eventId = :eventId")
    fun getUserInfoByEventId(eventId: Int): UserInfo

    //Device info
    @Insert
    fun addDeviceInfo(deviceInfo: DeviceInfo)

    @Update
    fun updateDeviceInfo(deviceInfo: DeviceInfo)

    @Query("SELECT * FROM deviceinfo WHERE eventId = :eventId")
    fun getDeviceInfoByEventId(eventId: Int): DeviceInfo

}