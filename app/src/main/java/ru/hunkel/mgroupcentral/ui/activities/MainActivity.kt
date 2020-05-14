package ru.hunkel.mgroupcentral.ui.activities

import android.Manifest
import android.app.ActivityManager
import android.bluetooth.BluetoothAdapter
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.util.Log.i
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import ru.hunkel.mgroupcentral.R
import ru.hunkel.mgroupcentral.managers.MGroupDatabaseManager
import ru.hunkel.mgroupcentral.services.RSSILoggerService
import ru.hunkel.mgroupcentral.utils.MODULE_TITLES
import ru.hunkel.mgrouprssichecker.IRSSILoggerService
import ru.ogpscenter.ogpstracker.service.IGPSTrackerServiceRemote


const val TAG = "MainActivity"
private const val TAG_O_GPS_CENTER = "OGPSCenter"
private const val TAG_M_GROUP_SERVICE = "MGroupService"

class MainActivity : AppCompatActivity() {
    private var mIsTracking = false

    //Service variables
    //Start status variables
    private var mIsMGroupServiceStarted = false
    private var mIsGpsTrackerServiceStarted = false

    //Bound status variables
    private var mIsMGroupServiceBounded = false

    //Need to be started variables
    private var mIsMGroupServiceNeedToBeStarted = false

    lateinit var mDatabaseManager: MGroupDatabaseManager

    //OGPSCenter service connection
    var mGpsService: IGPSTrackerServiceRemote? = null

    private val mOGPSCenterServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            mGpsService = null
            i(TAG, "ogpscenter service disconnected")
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            mGpsService = IGPSTrackerServiceRemote.Stub.asInterface(service)
            try {

                mGpsService!!.startTracking()
                mIsGpsTrackerServiceStarted = true
                updateUI()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun startOGPSTrackerService() {
        val serviceIntent = Intent()
        serviceIntent.component = ComponentName(
            "ru.ogpscenter.ogpstracker",
            "ru.ogpscenter.ogpstracker.service.GPSTrackerService"
        )
//        serviceIntent.action = "ru.ogpscenter.ogpstracker.service.GPSTrackerService"
        try {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
                != PackageManager.PERMISSION_GRANTED
                &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                != PackageManager.PERMISSION_GRANTED
            ) {
                askForLocationPermissions()
            } else {
                //do your work
            }

            startService(serviceIntent)
            val res = bindService(
                serviceIntent,
                mOGPSCenterServiceConnection,
                Context.BIND_AUTO_CREATE
            )
        } catch (ex: Exception) {
            Log.e(TAG_O_GPS_CENTER, ex.message)
        }
    }

    private fun stopOGPSTrackerService() {
        try {
            mGpsService!!.stopTracking()
            unbindService(mOGPSCenterServiceConnection)
            mIsGpsTrackerServiceStarted = false
//            val serviceIntent = Intent()
//            serviceIntent.component = ComponentName(
//                "ru.ogpscenter.ogpstracker",
//                "ru.ogpscenter.ogpstracker.service.GPSTrackerService"
//            )
//            serviceIntent.action = "ru.ogpscenter.ogpstracker.service.GPSLoggerService"
//            stopService(serviceIntent)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    /**
     * MGROUP SERVICE
     */
    var mMGroupService: IRSSILoggerService? = null

    //MGroup service connection
    private val mMGroupTrackerServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            mMGroupService = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            mMGroupService = IRSSILoggerService.Stub.asInterface(service)

            try {
                mIsMGroupServiceBounded = true
                mIsMGroupServiceStarted = true

                if (mIsMGroupServiceNeedToBeStarted) {
                    mMGroupService!!.start()
                }
                mIsMGroupServiceNeedToBeStarted = false
                updateUI()
            } catch (ex: Exception) {
                i(TAG, ex.message)
            }
        }
    }


    private fun startMGroupTrackerService() {
        mIsMGroupServiceNeedToBeStarted = true
        val serviceIntent = Intent()
        serviceIntent.component = ComponentName(
            "ru.hunkel.mgrouprssichecker",
            "services.RSSILoggerService"
        )
//        serviceIntent.action = "ru.hunkel.mgrouprssichecker.services.RSSILoggerService"
        try {
            startService(serviceIntent)
            val res = bindService(
                serviceIntent,
                mMGroupTrackerServiceConnection,
                Context.BIND_AUTO_CREATE
            )
        } catch (ex: Exception) {
            Log.e(TAG_M_GROUP_SERVICE, ex.message)
        }
    }

    private fun betaStartMGroupTrackerService() {
        mIsMGroupServiceNeedToBeStarted = true

        val serviceIntent = Intent(this, RSSILoggerService::class.java)
        startService(serviceIntent)
        bindService(
            serviceIntent,
            mMGroupTrackerServiceConnection,
            Context.BIND_WAIVE_PRIORITY
        )

//        mIsSearching = true
//        mIsServiceBounded = false
//        mIsServiceStarted = false
        updateUI()
    }

    private fun betaStopMGroupTrackerService() {
        try {
            mMGroupService?.stop()
            unbindService(mMGroupTrackerServiceConnection)
            stopService(Intent(this, RSSILoggerService::class.java))
        } catch (ex: Exception) {
            Toast.makeText(this, ex.message, Toast.LENGTH_SHORT).show()
        }

        mIsMGroupServiceStarted = false
        mIsMGroupServiceBounded = false
//        mIsSearching = false
        updateUI()
    }

    private fun stopMGroupTrackerService() {
        try {
            mMGroupService!!.stop()
            unbindService(mMGroupTrackerServiceConnection)
            val serviceIntent = Intent()

            serviceIntent.component = ComponentName(
                "ru.hunkel.mgrouprssichecker",
                "services.RSSILoggerService"
            )
            serviceIntent.action = "ru.hunkel.mgrouprssichecker.services.RSSILoggerService"

            stopService(serviceIntent)
            mIsMGroupServiceStarted = false
            mIsMGroupServiceBounded = false
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_overflow_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settings_menu_button -> {
                val intent = Intent(
                    this,
                    GeneralSettingsActivity::class.java
                )
                startActivity(intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mDatabaseManager = MGroupDatabaseManager(this)
        checkForServiceInstalled()
        setButtonListeners()
    }

    override fun onResume() {
        super.onResume()

        checkForServiceAvailability()
        updateUI()
    }

    private fun checkForServiceInstalled() {
        val modulesList = mDatabaseManager.actionGetAllModules()
        modulesList.forEach { module ->
            MODULE_TITLES.forEach { moduleTitles ->
                if (module.appPackage == moduleTitles.key) {
                    if (module.appPackage == "ru.ogpscenter.ogpstracker") {
                        use_gps_tracking_checkbox.isEnabled = true
                        use_gps_tracking_checkbox.setTextColor(resources.getColor(android.R.color.black))
                    }
                    if (module.appPackage == "ru.hunkel.mgrouprssicecker") {
                        use_bluetooth_tracking_checkbox.isEnabled = true
                        use_bluetooth_tracking_checkbox.setTextColor(resources.getColor(android.R.color.black))
                    }
                }
            }
        }
        super.onDestroy()
    }

    private fun setButtonListeners() {
        start_stop_button.setOnClickListener {
            changeTrackingState()
            updateUI()
        }
        module_settings_button.setOnClickListener {
            val intent = Intent(
                this,
                GeneralSettingsActivity::class.java
            )
            startActivity(intent)
        }
    }

    private fun showMessage(string: String) {
        Toast.makeText(this, string, Toast.LENGTH_SHORT).show()
    }

    private fun changeTrackingState() {
        if (mIsTracking) {
            if (
                use_bluetooth_tracking_checkbox.isChecked &&
                use_gps_tracking_checkbox.isChecked
            ) {
//                stopMGroupTrackerService()
                betaStopMGroupTrackerService()
                stopOGPSTrackerService()
            }
            if (
                use_gps_tracking_checkbox.isChecked &&
                use_bluetooth_tracking_checkbox.isChecked.not()
            ) {
                stopOGPSTrackerService()
            }
            if (
                use_gps_tracking_checkbox.isChecked.not() &&
                use_bluetooth_tracking_checkbox.isChecked
            ) {
//                stopMGroupTrackerService()
                betaStopMGroupTrackerService()
            }
        } else {
            if (
                use_bluetooth_tracking_checkbox.isChecked &&
                use_gps_tracking_checkbox.isChecked
            ) {
                startOGPSTrackerService()
                betaStartMGroupTrackerService()
//                startMGroupTrackerService()
            }
            if (
                use_bluetooth_tracking_checkbox.isChecked.not() &&
                use_gps_tracking_checkbox.isChecked
            ) {
                startOGPSTrackerService()
            }
            if (
                use_bluetooth_tracking_checkbox.isChecked &&
                use_gps_tracking_checkbox.isChecked.not()
            ) {
//                startMGroupTrackerService()
                betaStartMGroupTrackerService()
            }
        }
        mIsTracking = !mIsTracking
    }

    private fun checkForServiceAvailability() {
        if (isServiceRunning(RSSILoggerService::class.java)) {
            mIsMGroupServiceStarted = true
            bindService(
                Intent(this, RSSILoggerService::class.java),
                mMGroupTrackerServiceConnection,
                Context.BIND_AUTO_CREATE
            )
        }
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val activityManager =
            getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in activityManager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private fun askForLocationPermissions() {

        // Should we show an explanation?
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            AlertDialog.Builder(this)
                .setTitle("Location permessions needed")
                .setMessage("you need to allow this permission!")
                .setPositiveButton(
                    "Sure",
                    DialogInterface.OnClickListener { dialog, which ->
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                            1
                        )
                    })
                .setNegativeButton(
                    "Not now",
                    DialogInterface.OnClickListener { dialog, which ->
                        //                                        //Do nothing
                    })
                .show()
        } else {
        }
    }

    private fun updateUI() {
        if (mIsMGroupServiceStarted && mIsGpsTrackerServiceStarted.not()) {
            start_stop_button.text = "Stop"
            use_bluetooth_tracking_checkbox.isEnabled = false
            use_gps_tracking_checkbox.isEnabled = false
            use_bluetooth_tracking_checkbox.isChecked = true
        } else if (mIsGpsTrackerServiceStarted && mIsMGroupServiceStarted.not()) {
            start_stop_button.text = "Stop"
            use_bluetooth_tracking_checkbox.isEnabled = false
            use_gps_tracking_checkbox.isEnabled = false
            use_gps_tracking_checkbox.isChecked = true

        } else if (mIsGpsTrackerServiceStarted && mIsMGroupServiceStarted) {
            start_stop_button.text = "Stop"
            use_bluetooth_tracking_checkbox.isEnabled = false
            use_gps_tracking_checkbox.isEnabled = false

            use_gps_tracking_checkbox.isChecked = true
            use_bluetooth_tracking_checkbox.isChecked = true

        } else {
            start_stop_button.text = "Start"
            checkForServiceInstalled()
//            use_bluetooth_tracking_checkbox.isEnabled = true
//            use_gps_tracking_checkbox.isEnabled = true
        }
    }
}
