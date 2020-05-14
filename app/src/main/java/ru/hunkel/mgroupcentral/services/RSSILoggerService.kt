package ru.hunkel.mgroupcentral.services

import android.app.*
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.*
import android.net.ConnectivityManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import database.entities.ControlPoints
import database.entities.Punches
import database.entities.UserInfo
import io.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import models.Beacon
import org.json.JSONArray
import org.json.JSONObject
import ru.hunkel.mgroupcentral.R
import ru.hunkel.mgroupcentral.activities.MainActivity
import ru.hunkel.mgroupcentral.database.manager.DatabaseManager
import ru.hunkel.mgroupcentral.models.IBeacon
import ru.hunkel.mgroupcentral.network.DataSender
import ru.hunkel.mgroupcentral.receivers.NetworkStateReceiver
import ru.hunkel.mgrouprssichecker.IRSSILoggerService
import ru.ogpscenter.ogpstracker.service.IGPSTrackerServiceRemote
import utils.*
import java.util.*
import kotlin.math.abs

//TAGS
private const val TAG_MAIN = "RSSIService"
private const val TAG_O_GPS_CENTER = "OGPSCenter"
private const val TAG_NETWORK = "Network"
private const val WAKE_LOCK_TAG = "rssiService:UpdateWakeLock"

//NOTIFICATIONS
private const val NOTIFICATION_FOREGROUND_SERVICE = 666
private const val NOTIFICATION_CHANNEL_ID = "ServiceNotificationChannel"
private const val NOTIFICATION_CHANNEL_NAME = "TrackingNotificationsChannel"
private const val LOCK_UPDATE_INTERVAL: Long = 120 * 1000L //120 secs

//private val MIN_TIME_BETWEEN_CONTROL_POINTS = 10 * 1000 //10 secs
private val MIN_TIME_BETWEEN_CONTROL_POINTS = 0 //0 secs
private val MAX_TIME_BETWEEN_CONTROL_POINTS = 60 * 60 * 1000 //1 hour
private val DELTA_TIME = 60 * 60 * 1000 //1 hour

class RSSILoggerService : Service(), NetworkStateReceiver.NetworkStateReceiverListener {

    private var mBluetoothManager: BluetoothManager? = null
    private var mBluetoothAdapter: BluetoothLeScanner? = null

    private lateinit var mNotificationManager: NotificationManager

    private var mLogger: Logger? = null

    //Wakelock
    private var mWakeLock: PowerManager.WakeLock? = null
    private var mLastLockUpdateMillis: Long = 0
    private var mPowerManager: PowerManager? = null

    private var mIsScanning = false

    //Timers
    private var mTimer: Timer = Timer()
    private var mScannerTimer: Timer = Timer()
    private var mCheckingTimer: Timer = Timer()

    //Database
    private lateinit var mDatabaseManager: DatabaseManager

    //Temp data
    private var mCurrentEventId = -1

    //    private var mLastScanResultTime = 0L
    private var mLastControlPointNumber = -1
    private var mPostedPunches = mutableListOf<Punches>()

    //Network
    private val mDataSender = DataSender(this)

    //Network state receiver
    private val mNetworkStateReceiver = NetworkStateReceiver()

    private var mNetworkStateReceiverRegistered = false

    //OGPSCenter service connection
    var mGpsService: IGPSTrackerServiceRemote? = null

    private val mOGPSCenterServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            mGpsService = null
            Log.i(TAG_MAIN, "ogpscenter service disconnected")
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            mGpsService = IGPSTrackerServiceRemote.Stub.asInterface(service)

            try {
                mServerUrl = mGpsService!!.punchesUploadUrl
                Log.i(TAG_O_GPS_CENTER, "ogps url: " + mGpsService!!.punchesUploadUrl)
                mServerUrlReceived = true

            } catch (ex: Exception) {
                mServerUrlReceived = false
            }
            Log.i(
                TAG_O_GPS_CENTER, ".\nogpscenter connected" +
                        "\ncomponent name: ${name.toString()}" +
                        "\nurl getted: $mServerUrlReceived"
            )
            try {
                stopOGPSCenterService()
            } catch (ex: Exception) {

            }

        }
    }

    private var mServerUrl = ""

    private var mServerUrlReceived = false

    override fun onNetworkConnectionChanged(isConnected: Boolean) {
        if (isConnected) {
            sendPunches()
            Log.i(
                TAG_NETWORK,
                "Network Broadcast Receiver: CONNECTION ESTABLISHED. TRYING TO SEND AGAIN."
            )
        }
    }

    private fun sendPunches() {
        //TODO check server url for availability
//        if (mServerUrlReceived and DataSender.isNetworkConnected(this)) { // for testing
//        if (mTimeSynchronized and mServerUrlReceived and DataSender.isNetworkConnected(this)) {
        if (mServerUrlReceived and DataSender.isNetworkConnected(this)) {
            val list =
                mDatabaseManager.actionGetPunchesByEventId(mDatabaseManager.actionGetLastEvent().id)
            val jsonString = createJsonByPunchList(list)

            CoroutineScope(Dispatchers.Default).launch {
                //                    if (mDataSender.sendPunchesAsync(jsonString, "https://postman-echo.com/get").await()){
//                    if (mDataSender.sendPunchesAsync(jsonString, "http://192.168.43.150:2023/").await()){
                if (mDataSender.sendPunchesAsync(jsonString, mServerUrl).await()) {
                    Log.i(TAG_NETWORK, "POSTED")
                    list.forEach {
                        mPostedPunches.add(it)
                    }
                } else {
                    Log.i(TAG_NETWORK, "NOT POSTED")
                    //TODO write timer task
                }

            }
        } else if (DataSender.isNetworkConnected(this).not()) {
            if (!mNetworkStateReceiverRegistered) {
                NetworkStateReceiver.networkStateReceiverListener = this
                registerReceiver(
                    mNetworkStateReceiver,
                    IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
                )
                mNetworkStateReceiverRegistered = true
                Log.i(
                    TAG_NETWORK,
                    "Network Broadcast Receiver: NO NETWORK CONNECTION. RECEIVER REGISTERED"
                )
            }
        }
//        else if (mTimeSynchronized.not()) {
//            TODO do some if time not sync yet
//        }
    }

    private fun createJsonByPunchList(
        list: List<Punches> = mDatabaseManager.actionGetPunchesByEventId(
            mCurrentEventId
        )
    ): String {
        val jsonArray = JSONArray()
        val punchList = mutableListOf<Int>()

        for (i in list) {
            if (punchList.contains(i.controlPoint)) {
                continue
            } else {
                punchList.add(i.controlPoint)
                val json = JSONObject()
                json.put("uid", "${i.controlPoint}")
                json.put("name", "cp_${i.controlPoint}")
                json.put(
                    "time",
                    convertMillisToTime(
                        i.time,
                        PATTERN_YEAR_MONTH_DAY,
                        TimeZone.getTimeZone("UTC")
                    ) + "T" +
                            convertMillisToTime(
                                i.time,
                                PATTERN_HOUR_MINUTE_SECOND,
                                TimeZone.getTimeZone("UTC")
                            ) + "Z"
                )
                json.put("score", (i.controlPoint / 10))
                json.put("priority", 400)


                val coordinates = JSONObject()

                coordinates.put("latitude", 0F)
                coordinates.put("longitude", 0F)

                json.put("coordinates", coordinates)
                json.put("agent", "")
                json.put("comment", "")
                jsonArray.put(json)
            }
        }
        Log.i(TAG_MAIN, "JSON $jsonArray")
        return jsonArray.toString()
    }

    private fun startOGPSCenterService() {
        val serviceIntent = Intent()
        serviceIntent.component = ComponentName(
            "ru.ogpscenter.ogpstracker",
            "ru.ogpscenter.ogpstracker.service.GPSTrackerService"
        )
        serviceIntent.action = "ru.ogpscenter.ogpstracker.service.GPSLoggerService"
        try {
            startService(serviceIntent)
            val res = bindService(
                serviceIntent,
                mOGPSCenterServiceConnection,
                Context.BIND_AUTO_CREATE
            )
            Log.i(TAG_O_GPS_CENTER, "Service started: $res")
        } catch (ex: Exception) {
            Log.e(TAG_O_GPS_CENTER, ex.message)
        }
    }

    private fun stopOGPSCenterService() {
        unbindService(mOGPSCenterServiceConnection)
    }

    //CLASSES
    private inner class UpdateWakeLockTimerTask : TimerTask() {
        override fun run() {
            updateWakeLock()
        }
    }

    private inner class UpdateScanningTimerTask : TimerTask() {
        override fun run() {
            restartScan()
        }
    }

    private inner class RSSILoggerServiceImplementation : IRSSILoggerService.Stub() {
        override fun start() {
            this@RSSILoggerService.startScan()
        }

        override fun startWithSettings(settings: String) {
            this@RSSILoggerService.startScan(settings)
        }

        override fun stop() {
            this@RSSILoggerService.stopScan()
        }

        override fun getFilePath(): String {
            return this@RSSILoggerService.getFilePath()
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return RSSILoggerServiceImplementation()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    private val callback = object : ScanCallback() {
        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            Log.i(TAG_MAIN, "On batch results. Size:  ${results?.size}")
        }

        override fun onScanFailed(errorCode: Int) {
            Log.i(TAG_MAIN, "Scan failed. Error code:  $errorCode")
        }

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            val time = System.currentTimeMillis()
            Log.i(TAG_MAIN, "callback called $callbackType")
            if (result != null) {
//                val record = result.scanRecord
//                Log.i(TAG, record!!.bytes.contentToString())

                val beacon = IBeacon.fromScanData(result.scanRecord?.bytes!!, result.rssi)
                Log.i(TAG_MAIN, "Record is not null")

                if (beacon?.major == 52671) {
                    val beaconUuid = UUID.fromString(beacon.proximityUuid)
                    Log.i(TAG_MAIN, "least ${beaconUuid.leastSignificantBits}")
                    Log.i(TAG_MAIN, "most ${beaconUuid.mostSignificantBits}")
                    Log.i(TAG_MAIN, "version ${beaconUuid.version()}")
                    Log.i(TAG_MAIN, "string $beaconUuid")
                    val toFile =
                        "${beacon.minor}\t" +
                                "${beacon.rssi}\t" +
                                convertMillisToTime(
                                    time,
                                    PATTERN_HOUR_MINUTE_SECOND_MILLISECOND
                                )

                    val controlPoint = ControlPoints(
                        eventId = mCurrentEventId,
                        controlPointNumber = beacon.minor,
                        rssi = beacon.rssi,
                        time = System.currentTimeMillis()
                    )

                    mDatabaseManager.actionAddControlPoint(controlPoint)

//                    mLogger?.write(toFile + "\n")

                    mLastControlPointNumber = beacon.minor
//                    mLastScanResultTime = System.currentTimeMillis()
                }
            }
        }
    }

    override fun onCreate() {

        mBluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mNotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mPowerManager = this.getSystemService(Context.POWER_SERVICE) as PowerManager
        mDatabaseManager = DatabaseManager(this)

        startOGPSCenterService()
        try {
            mBluetoothAdapter = mBluetoothManager?.adapter?.bluetoothLeScanner
        } catch (ex: Exception) {
            throw ex
        }

    }

    private fun getScanSettings(): ScanSettings {
        //Scan settings
        return ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)
//            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
//            .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
            .build()
    }

    private fun getScanFilters(): ArrayList<ScanFilter> {
        val beacon = Beacon(
            UUID.fromString("74278bda-b644-4520-8f0c-720eaf059935"),
            52671,
            0
        )

        val filter1 = getScanFilter(beacon)
        return arrayListOf(filter1)
    }

    private fun startScan(settings: String = "") {
        //Start scanning
        try {
            mBluetoothAdapter?.startScan(getScanFilters(), getScanSettings(), callback)
            mDatabaseManager.actionStartEvent(System.currentTimeMillis())
            mCurrentEventId = mDatabaseManager.actionGetLastEvent().id
            val sharedPreferences = getDefaultSharedPreferences(this)
            val userNumber = sharedPreferences.getString("key_participant_number", "")
            Log.i(TAG_MAIN, "USERNUM$userNumber")
            val userInfo = UserInfo(
                eventId = mCurrentEventId,
                number = userNumber!!
            )
            mDatabaseManager.actionAddUserInfo(userInfo)

        } catch (ex: Exception) {
            Log.e(TAG_MAIN, ex.message)
        }
//            if (settings != "") {
//            val unpacked = SettingsJSONConverter.unpackSettings(settings)
//            val fileName = unpacked["key_participant_number"].toString()
//            Logger(this.filesDir.absolutePath, fileName)
//        } else {
//            Logger(this.filesDir.absolutePath)
//        }
//        mLogger?.write(
//            "${convertMillisToTime(
//                System.currentTimeMillis(),
//                PATTERN_FULL_DATE_WITH_MILLISECONDS
//            )}\n"
//        )
//        mLogger?.write(
//            "Model: ${Build.MODEL}" +
//                    "\nAPI: ${Build.VERSION.SDK_INT}" +
//                    "\nBattery level: ${getBatteryLevel(this)}\n"
//        )
        startForeground(NOTIFICATION_FOREGROUND_SERVICE, getMainNotification())
        Log.i(TAG_MAIN, "started")
        mIsScanning = true
        try {

            mTimer.purge()
            mTimer.schedule(UpdateWakeLockTimerTask(), 0, 250 * 1000)
            mScannerTimer.schedule(UpdateScanningTimerTask(), 600_000, 600_000)

            mCheckingTimer.purge()
            mCheckingTimer.schedule(CheckLastPunchTimerTask(), 30 * 1000, 30 * 1000L)
        } catch (ex: Exception) {
        }
    }

    private fun restartScan() {
        //Stop last scanning
        try {
            mBluetoothAdapter?.stopScan(callback)
        } catch (ex: Exception) {
            Log.e(TAG_MAIN, ex.message)
            mLogger?.write("${ex.message}\n")
        }
        //Start scanning
        try {
            mBluetoothAdapter?.startScan(getScanFilters(), getScanSettings(), callback)
            mLogger?.write("restarted\n")
        } catch (ex: Exception) {
            Log.e(TAG_MAIN, ex.message)
            mLogger?.write("${ex.message}\n")
        }
    }

    private fun stopScan() {
        try {

            mBluetoothAdapter?.stopScan(callback)
            mLogger?.write("Battery level: ${getBatteryLevel(this)}")
            mLogger?.close()
            mTimer.cancel()
            mTimer.purge()
            mScannerTimer.cancel()
            mScannerTimer.purge()
            mCheckingTimer.cancel()
            mCheckingTimer.purge()
            mWakeLock?.release()
            mDatabaseManager.actionStopEvent(System.currentTimeMillis(), getBatteryLevel(this))
            Log.i(TAG_MAIN, "stopped")
            stopForeground(true)
            mIsScanning = false
        } catch (ex: Exception) {
        }
    }

    private fun getFilePath(): String {
        val list = Logger.getFilePaths(this.filesDir.absolutePath)
        for (i in list) {
            Log.i(TAG_MAIN, i)
        }
        return list[list.size - 1]
    }

    private fun getMainNotification(): Notification {
        val mBuilder = NotificationCompat.Builder(
            this,
            NOTIFICATION_CHANNEL_ID
        )
        mBuilder.setSmallIcon(R.drawable.ic_directions_run_24px)
        mBuilder.setLargeIcon(
            getBitmap(
                this,
                R.drawable.ic_directions_run_24px
            )
        )
        mBuilder.setContentTitle("Соревнование идет!\n")
        mBuilder.setContentText(
            "Участник №${mDatabaseManager.actionGetUserInfoByEventId(
                mCurrentEventId
            ).number}"
        )
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 0
        )

        mBuilder.setContentIntent(pendingIntent)
        mBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        mBuilder.priority = NotificationCompat.PRIORITY_MAX
        mBuilder.setAutoCancel(false)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
            )
            mNotificationManager.createNotificationChannel(channel)
            mBuilder.setChannelId(channel.id)
        }

        val notification = mBuilder.build()
        notification!!.flags = Notification.FLAG_NO_CLEAR

        return notification
    }

    private fun updateWakeLock() {
        if (mIsScanning) {
            val currentMillis = System.currentTimeMillis()
            if (abs(currentMillis - mLastLockUpdateMillis) > LOCK_UPDATE_INTERVAL) {
                mLastLockUpdateMillis = currentMillis

                if (mWakeLock != null) {
                    mWakeLock!!.release()
                    mWakeLock = null
                }
                mWakeLock =
                    mPowerManager?.newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK,
                        WAKE_LOCK_TAG
                    )
                mWakeLock!!.apply {
                    acquire(60 * 5 * 1000) //Timeout 5 min's
                }
                Log.i(TAG_MAIN, "WAKELOCK ACQUIRED")
            }
        } else {
            mLastLockUpdateMillis = 0
            if (this.mWakeLock != null) {
                try {
                    if (mWakeLock!!.isHeld) {
                        this.mWakeLock!!.release()
//                        mWakeLock!!.release()
                    }
                } catch (ex: Exception) {

                }
                this.mWakeLock = null
            }
            Log.i(TAG_MAIN, "WAKELOCK RELEASED")
        }
    }

    private inner class CheckLastPunchTimerTask : TimerTask() {
        override fun run() {
            checkPunches()
        }
    }

    private fun checkPunches() {
        val currentTime = System.currentTimeMillis()
//        val deltaTime = currentTime - mLastScanResultTime
        Log.i(
            TAG_MAIN,
            "PUNCH \n" +
                    "currentTime: ${convertMillisToTime(
                        System.currentTimeMillis(),
                        PATTERN_HOUR_MINUTE_SECOND_MILLISECOND
                    )}\n"
//                    "lastTime: ${convertMillisToTime(
//                        mLastScanResultTime,
//                        PATTERN_HOUR_MINUTE_SECOND_MILLISECOND
//                    )}\n" +
//                    "deltaTime: \n${convertMillisToTime(
//                        deltaTime,
//                        PATTERN_HOUR_MINUTE_SECOND_MILLISECOND,
//                        timeZone = TimeZone.getTimeZone("UTC")
//                    )}"
        )

        Log.i(
            TAG_MAIN, "PUNCH1 \n" +
                    "currentTime: ${convertMillisToTime(
                        System.currentTimeMillis(),
                        PATTERN_HOUR_MINUTE_SECOND_MILLISECOND
                    )}\n"
//                    "lastTime: ${convertMillisToTime(
//                        mLastScanResultTime,
//                        PATTERN_HOUR_MINUTE_SECOND_MILLISECOND,
//                        TimeZone.getTimeZone("UTC")
//                    )}\n" +
//                    "deltaTime: \n${convertMillisToTime(
//                        deltaTime,
//                        PATTERN_HOUR_MINUTE_SECOND_MILLISECOND,
//                        timeZone = TimeZone.getTimeZone("UTC")
//                    )}"
        )
        val controlPoints = mDatabaseManager.actionGetControlPointsByTime(
            currentTime - DELTA_TIME,
            currentTime,
            mLastControlPointNumber,
            mCurrentEventId
        )
        Log.i(
            TAG_MAIN,
            "\n" +
                    "size: ${controlPoints.size}" +
                    ""
        )
        val controlPointWithMinRSSI = analyzeControlPoints(controlPoints)
        if (controlPointWithMinRSSI != null) {

            mDatabaseManager.actionAddPunch(controlPointWithMinRSSI)
            Log.i(
                TAG_MAIN, "PUNCH3 \n" +
                        "\nAdded punch:\n" +
                        "event id: ${controlPointWithMinRSSI.eventId}\n" +
                        "control point: ${controlPointWithMinRSSI.controlPoint}\n" +
                        "time: ${convertMillisToTime(
                            controlPointWithMinRSSI.time,
                            PATTERN_HOUR_MINUTE_SECOND_MILLISECOND
                        )}\n"
            )
            if (checkInList(controlPointWithMinRSSI.controlPoint)) {

            } else {
                mPunchesIdentifiers.add(controlPointWithMinRSSI.controlPoint)
                sendPunches()
            }
            /*try {
                if (sendedPunches.isNotEmpty()) {
                    sendedPunches.forEach {
                        if (controlPointWithMinRSSI.time == it.time
                            &&
                            controlPointWithMinRSSI.controlPoint == it.controlPoint
                        ) {
                        } else {
                            sendPunches()
                        }
                    }
                } else {
                    sendPunches()
                }*/
            //TODO CHECK IF ALREADY SEND
            /* } catch (ex: Exception) {

             }*/
        }
    }

    private fun analyzeControlPoints(controlPoints: List<ControlPoints>): Punches? {
        if (controlPoints.isEmpty()) return null
        var minRssi: Int = -200
        var maxRssiIndex = 0
        Log.i(TAG_MAIN, "PUNCH ANL")
        try {
            controlPoints.forEachIndexed { index, controlPoint ->
                if (controlPoint.rssi > minRssi) {
                    minRssi = controlPoint.rssi
                    Log.i(TAG_MAIN, "PUNCH ANL1")
                    maxRssiIndex = index
                }
            }
        } catch (ex: Exception) {

        }
        val controlPoint = controlPoints[maxRssiIndex]
        val punches = Punches(
            eventId = mCurrentEventId,
            controlPoint = controlPoint.controlPointNumber,
            time = controlPoint.time
        )
        Log.i(TAG_MAIN, "PUNCH ANL2")
        return punches
    }

    private val mPunchesIdentifiers = LinkedList<Int>()

    private fun checkInList(cp: Int): Boolean {
        return if (mPunchesIdentifiers.contains(cp)) {
            Log.i(TAG_MAIN, "Beacon $cp already exists in list")
            true
        } else {
            Log.i(TAG_MAIN, "else")
            false
        }
    }
}