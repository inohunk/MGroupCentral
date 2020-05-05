package ru.hunkel.mgroupcentral.activities

import android.Manifest
import android.content.*
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
import kotlinx.android.synthetic.main.activity_main.*
import ru.hunkel.mgroupcentral.R
import ru.hunkel.mgrouprssichecker.IRSSILoggerService
import ru.ogpscenter.ogpstracker.service.IGPSTrackerServiceRemote


const val TAG = "MainActivity"
private const val TAG_O_GPS_CENTER = "OGPSCenter"
private const val TAG_M_GROUP_SERVICE = "MGroupService"

class MainActivity : AppCompatActivity() {
    private var mIsTracking = false

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

        setButtonListeners()
    }

    private fun setButtonListeners() {
        start_stop_button.setOnClickListener {
            changeTrackingState()
            updateUI()
        }
    }

    private fun showMessage(string: String) {
        Toast.makeText(this, string, Toast.LENGTH_SHORT).show()
    }

    private fun changeTrackingState() {
        if (mIsTracking) {
            if (use_bluetooth_tracking_checkbox.isChecked && use_gps_tracking_checkbox.isChecked) {
                stopMGroupTrackerService()
                stopOGPSTrackerService()
            }
            if (use_gps_tracking_checkbox.isChecked && use_bluetooth_tracking_checkbox.isChecked.not()) {
                stopOGPSTrackerService()
            }
            if (use_gps_tracking_checkbox.isChecked.not() && use_bluetooth_tracking_checkbox.isChecked) {
                stopMGroupTrackerService()
            }
            showMessage("Stopped")
        } else {
            if (use_bluetooth_tracking_checkbox.isChecked && use_gps_tracking_checkbox.isChecked) {
                startOGPSTrackerService()
                startMGroupTrackerService()
            }
            if (use_bluetooth_tracking_checkbox.isChecked.not() && use_gps_tracking_checkbox.isChecked) {
                startOGPSTrackerService()
            }
            if (use_bluetooth_tracking_checkbox.isChecked && use_gps_tracking_checkbox.isChecked.not()) {
                startMGroupTrackerService()
            }
        }
        mIsTracking = !mIsTracking
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

    //OGPSCenter service connection
    var mGpsService: IGPSTrackerServiceRemote? = null
    var mOGPSTrackingServiceStarted: Boolean = false

    private val mOGPSCenterServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            mGpsService = null
            i(TAG, "ogpscenter service disconnected")
            mOGPSTrackingServiceStarted = false
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            mGpsService = IGPSTrackerServiceRemote.Stub.asInterface(service)
            try {
                mOGPSTrackingServiceStarted = true
                mGpsService!!.startTracking()
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
        serviceIntent.action = "ru.ogpscenter.ogpstracker.service.GPSLoggerService"
        try {
//            startService(serviceIntent)
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
//            val serviceIntent = Intent()
//            serviceIntent.component = ComponentName(
//                "ru.ogpscenter.ogpstracker",
//                "ru.ogpscenter.ogpstracker.service.GPSTrackerService"
//            )
//            serviceIntent.action = "ru.ogpscenter.ogpstracker.service.GPSLoggerService"
//            stopService(serviceIntent)
        } catch (ex: Exception) {
            i(TAG, ex.message)
        }
    }

    /**
     * MGROUP SERVICE
     */
    var mMGroupService: IRSSILoggerService? = null
    var mMGroupServiceStarted: Boolean = false

    //MGroup service connection
    private val mMGroupTrackerServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            mMGroupService = null
            mMGroupServiceStarted = false
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            mMGroupService = IRSSILoggerService.Stub.asInterface(service)
            try {
                mMGroupServiceStarted = true
                mMGroupService!!.start()
            } catch (ex: Exception) {
                i(TAG, ex.message)
            }
        }
    }

    private fun startMGroupTrackerService() {
        val serviceIntent = Intent()
        serviceIntent.component = ComponentName(
            "ru.hunkel.mgrouprssichecker",
            "services.RSSILoggerService"
        )
//        serviceIntent.action = "ru.hunkel.mgrouprssichecker.services.RSSILoggerService"
//        try {
//            startService(serviceIntent)
            val res = bindService(
                serviceIntent,
                mMGroupTrackerServiceConnection,
                Context.BIND_AUTO_CREATE
            )
//
//        } catch (ex: Exception) {
//            Log.e(TAG_O_GPS_CENTER, ex.message)
//        }
    }

    private fun stopMGroupTrackerService() {
        try {
            mMGroupService!!.stop()
            unbindService(mMGroupTrackerServiceConnection)
            mMGroupServiceStarted = false
//            val serviceIntent = Intent()
//
//            serviceIntent.component = ComponentName(
//                "ru.hunkel.mgrouprssichecker",
//                "services.RSSILoggerService"
//            )
//            serviceIntent.action = "ru.hunkel.mgrouprssichecker.services.RSSILoggerService"
//
//            stopService(serviceIntent)
        } catch (ex: Exception) {
            i(TAG, ex.message)
        }
    }

    private fun updateUI() {
        if (mIsTracking) {
            start_stop_button.text = "Stop"
            use_bluetooth_tracking_checkbox.isEnabled = false
            use_gps_tracking_checkbox.isEnabled = false
        } else {
            start_stop_button.text = "Start"
            use_bluetooth_tracking_checkbox.isEnabled = true
            use_gps_tracking_checkbox.isEnabled = true
        }
    }
}
