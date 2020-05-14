package ru.hunkel.mgroupcentral.database.manager

import android.content.Context
import android.util.Log
import database.entities.*
import kotlinx.coroutines.runBlocking
import ru.hunkel.mgroupcentral.database.MGroupDatabase
import utils.PATTERN_HOUR_MINUTE_SECOND_MILLISECOND
import utils.convertMillisToTime

class DatabaseManager(context: Context) {

    private val TAG = "DatabaseManager"

    private var mDb = MGroupDatabase.getInstance(context)

    private var mCurrentEvent: Events = Events()

    //Events
    fun actionStartEvent(time: Long) {
        val event = Events(
            startTime = time
        )
        runBlocking {
            mDb.trackingModel().addEvent(event)
            mCurrentEvent = actionGetLastEvent()
        }
        Log.i(TAG, "event ${mCurrentEvent.id} started")
    }

    fun actionStartEvent(time: Long, deviceInfo: DeviceInfo) {
        val event = Events(
            startTime = time
        )
        runBlocking {
            mDb.trackingModel().addEvent(event)
            mCurrentEvent = actionGetLastEvent()
        }
//        actionAddDeviceInfo(deviceInfo)
        Log.i(TAG, "event ${mCurrentEvent.id} started")
    }

    fun actionUpdateEvent(event: Events) {
        mDb.trackingModel().updateEvent(event)
        Log.i(TAG, "Event ${event.id} updated")
    }

    fun actionStopEvent(time: Long, batteryLevel: Int) {
        val event = actionGetLastEvent()
        event.endTime = time
        mDb.trackingModel().updateEvent(event)
//        val deviceInfo = getDeviceInfoByEventId(event.id)
//        deviceInfo.endBatteryLevel = batteryLevel
//        updateDeviceInfo(deviceInfo)
        Log.i(TAG, "event ${event.id} stopped")
    }

    fun actionGetLastEvent(): Events {
        val event = mDb.trackingModel().getLastEvent()
        Log.i(
            TAG,
            "Get last event:\n" +
                    "EVENT INFO:\n\t" +
                    "id: ${event.id}\n\t" +
                    "start time: ${convertMillisToTime(event.startTime)}\n\t" +
                    "end time: ${convertMillisToTime(event.endTime)}\n"
        )
        return event
    }

    fun actionGetAllEvents(): List<Events> {
        val events = mDb.trackingModel().getAllEvents()
        Log.i(
            TAG,
            "EVENTS INFO:\n\t" +
                    "size: ${events.size}\n\t"
        )
        return events
    }

    fun actionDeleteEvent(event: Events) {
        mDb.trackingModel().deleteEvent(event)
        Log.i(
            TAG,
            "REMOVED EVENT INFO:\n\t" +
                    "id: ${event.id}\n\t" +
                    "start time: ${convertMillisToTime(event.startTime)}\n\t" +
                    "end time: ${convertMillisToTime(event.endTime)}\n"
        )
    }

    fun actionGetEventById(id: Int): Events {
        return mDb.trackingModel().getEventById(id)
    }

    //ControlPoints
    fun actionAddControlPoint(punch: ControlPoints) {
        mDb.trackingModel().addControlPoint(punch)
        punch.time = punch.time
        val lastPunch = mDb.trackingModel().getLastControlPoint()

        Log.i(
            TAG,
            "\nAdded control point:\n" +
                    "id: ${lastPunch.id}\n" +
                    "event id: ${lastPunch.eventId}\n" +
                    "control point: ${lastPunch.controlPointNumber}\n" +
                    "time: ${convertMillisToTime(
                        lastPunch.time,
                        PATTERN_HOUR_MINUTE_SECOND_MILLISECOND
                    )}\n"
        )
    }

    fun actionReplaceControlPoint(punch: ControlPoints) {
        mDb.trackingModel().deletePunchByControlPointId(punch.controlPointNumber)
        mDb.trackingModel().addControlPoint(punch)
        val lastControlPoint = mDb.trackingModel().getLastControlPoint()
        Log.i(
            TAG,
            "\nReplaced control point:\n" +
                    "id: ${lastControlPoint.id}\n" +
                    "event id: ${lastControlPoint.eventId}\n" +
                    "control point: ${lastControlPoint.controlPointNumber}\n" +
                    "time: ${convertMillisToTime(
                        lastControlPoint.time,
                        PATTERN_HOUR_MINUTE_SECOND_MILLISECOND
                    )}\n"
        )
    }

    fun actionGetControlPointsByTime(timeStart: Long, timeEnd: Long): List<ControlPoints> {
        val controlPoints = mDb.trackingModel().getControlPointsByTime(timeStart, timeEnd)
        Log.i(TAG, "actionGetControlPointsByTime: size: ${controlPoints.size}")
        return controlPoints
    }

    fun actionGetControlPointsByTime(
        timeStart: Long,
        timeEnd: Long,
        controlPointId: Int,
        currentEventId: Int
    ): List<ControlPoints> {
        val controlPoints =
            mDb.trackingModel().getControlPointsByTimeAndControlPoint(
                timeStart,
                timeEnd,
                controlPointId,
                eventId = currentEventId
            )
        Log.i(TAG, "actionGetControlPointsByTime: size: ${controlPoints.size}")
        return controlPoints
    }

    //Punches

    fun actionAddPunch(punch: Punches) {
        mDb.trackingModel().addPunch(punch)
        val lastPunch = mDb.trackingModel().getLastPunch()
        Log.i(
            TAG,
            "\nAdded punch:\n" +
                    "id: ${lastPunch.id}\n" +
                    "event id: ${lastPunch.eventId}\n" +
                    "control point: ${lastPunch.controlPoint}\n" +
                    "time: ${convertMillisToTime(
                        lastPunch.time,
                        PATTERN_HOUR_MINUTE_SECOND_MILLISECOND
                    )}\n"
        )
    }

    fun actionGetControlPointsByEventIdAsString(id: Int): String {
        var punchesString = ""
        val controlPoints = mDb.trackingModel().getControlPointsByEventId(id)
        Log.i(TAG, "PUNCHES LIST:\n")
        for (controlPoint in controlPoints) {
            val s =
                "${controlPoint.controlPointNumber}\t" +
                        "${controlPoint.rssi}\t" +
                        "${convertMillisToTime(
                            controlPoint.time,
                            PATTERN_HOUR_MINUTE_SECOND_MILLISECOND
                        )}\n"
            punchesString += s
            Log.i(TAG, "\t${controlPoint.controlPointNumber}\n")
        }
        return punchesString
    }

    fun actionReplacePunchSimple(punch: ControlPoints) {
        punch.time = punch.time
        mDb.trackingModel().updateControlPoint(punch)
    }

    fun actionGetLastPunch(): ControlPoints {
        return mDb.trackingModel().getLastControlPoint()
    }

    fun actionGetControlPointsByEventId(id: Int): List<ControlPoints> {
        val punches = mDb.trackingModel().getControlPointsByEventId(id)
        Log.i(TAG, "PUNCHES LIST:\n")
        for (punch in punches) {
            punch.time = (punch.time)
            Log.i(TAG, "\t${punch.controlPointNumber}\n")
        }
        return punches
    }

    fun actionGetControlPointsByEventIdWithAscSorting(id: Int): List<ControlPoints> {
        val punches = mDb.trackingModel().getControlPointsByEventIdWithAscSorting(id)
        Log.i(TAG, "PUNCHES LIST:\n")
        for (punch in punches) {
            punch.time = (punch.time)
            Log.i(TAG, "\t${punch.controlPointNumber}\n")
        }
        return punches
    }

    fun actionGetPunchByControlPoint(cp: Int): Punches {
        return mDb.trackingModel().getPunchByControlPoint(cp)
    }

    fun actionGetPunchesBeforeCertainTime(time: Long): List<Punches> {
        return mDb.trackingModel().getPunchesBeforeCertainTime(time)
    }

    fun actionGetPunchesByEventId(id: Int): List<Punches> {
        return mDb.trackingModel().getPunchesByEventId(id)
    }

    fun actionGetPunchesByEventIdAsString(id: Int): String {
        var punchesString = ""
        val punches = mDb.trackingModel().getPunchesByEventId(id)
        Log.i(TAG, "PUNCHES LIST:\n")
        for (punch in punches) {
            val s =
                "${punch.controlPoint}\t" +
                        "${convertMillisToTime(
                            punch.time,
                            PATTERN_HOUR_MINUTE_SECOND_MILLISECOND
                        )}\n"
            punchesString += s
        }
        return punchesString
    }

    //UserInfo
    fun actionAddUserInfo(userInfo: UserInfo) {
        mDb.trackingModel().addUserInfo(userInfo)
    }

    fun actionGetUserInfoByEventId(eventId: Int): UserInfo {
        return mDb.trackingModel().getUserInfoByEventId(eventId)
    }

    fun actionAddDeviceInfo(deviceInfo: DeviceInfo) {
        mDb.trackingModel().addDeviceInfo(deviceInfo)
    }

    fun updateDeviceInfo(deviceInfo: DeviceInfo) {
        mDb.trackingModel().updateDeviceInfo(deviceInfo)
    }

    private fun getDeviceInfoByEventId(eventId: Int): DeviceInfo {
        return mDb.trackingModel().getDeviceInfoByEventId(eventId)
    }

}