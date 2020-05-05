package ru.hunkel.mgroupcentral.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
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
        call_button.setOnClickListener {
            try {
                mMGroupService!!.start()
                mGpsService!!.startTracking()
            } catch (ex: Exception) {

            }
        }
    }

    private fun showMessage(string: String) {
        Toast.makeText(this, string, Toast.LENGTH_SHORT).show()
    }

    private fun changeTrackingState() {
        if (mIsTracking) {
            stopMGroupTrackerService()
            stopOGPSTrackerService()
            showMessage("Stopped")
        } else {
//            if (use_bluetooth_tracking_checkbox.isChecked && use_gps_tracking_checkbox.isChecked) {
//                startOGPSTrackerService()
//                startMGroupTrackerService()
//            }
//            if (use_gps_tracking_checkbox.isChecked) {
//                startOGPSTrackerService()
//            }
//            if (use_bluetooth_tracking_checkbox.isChecked) {
//                startMGroupTrackerService()
//            }
            startMGroupTrackerService()
            startOGPSTrackerService()
        }
        mIsTracking = !mIsTracking
    }

    //OGPSCenter service connection
    var mGpsService: IGPSTrackerServiceRemote? = null
    var mOGPSTrackingServiceStarted: Boolean = false

    private val mOGPSCenterServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            mGpsService = null
            i(TAG, "ogpscenter service disconnected")
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            mGpsService = IGPSTrackerServiceRemote.Stub.asInterface(service)
            try {
                mOGPSTrackingServiceStarted = true
                mGpsService!!.startTracking()
                i(
                    TAG_O_GPS_CENTER, ".\nogpscenter connected" +
                            "\ncomponent name: ${name.toString()}\n"
                )
                i(TAG_O_GPS_CENTER, "Service started")

            } catch (ex: Exception) {

            }
        }
    }

    @SuppressLint("LogNotTimber")
    private fun startOGPSTrackerService() {
        val serviceIntent = Intent()
        serviceIntent.component = ComponentName(
            "ru.ogpscenter.ogpstracker",
            "ru.ogpscenter.ogpstracker.service.GPSTrackerService"
        )
        serviceIntent.action = "ru.ogpscenter.ogpstracker.service.GPSLoggerService"
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

            // Show an expanation to the user *asynchronously* -- don't block
            // this thread waiting for the user's response! After the user
            // sees the explanation, try again to request the permission.
        } else {

            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )

            // MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION is an
            // app-defined int constant. The callback method gets the
            // result of the request.
        }
    }

    private fun stopOGPSTrackerService() {
        try {
            unbindService(mOGPSCenterServiceConnection)
            val serviceIntent = Intent()
            serviceIntent.component = ComponentName(
                "ru.ogpscenter.ogpstracker",
                "ru.ogpscenter.ogpstracker.service.GPSTrackerService"
            )
            stopService(serviceIntent)
        } catch (ex: Exception) {
            i(TAG, ex.message)
//            ex.printStackTrace()
        }
    }

    //MGroup service connection
    var mMGroupService: IRSSILoggerService? = null
    var mMGroupServiceStarted: Boolean = false

    private val mMGroupTrackerServiceConnection = object : ServiceConnection {
        @SuppressLint("LogNotTimber")
        override fun onServiceDisconnected(name: ComponentName?) {
            mGpsService = null
            i(TAG_M_GROUP_SERVICE, "MGroup service disconnected")
        }

        @SuppressLint("LogNotTimber")
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            mMGroupService = IRSSILoggerService.Stub.asInterface(service)

            try {
                mMGroupServiceStarted = true
                mMGroupService!!.start()
            } catch (ex: Exception) {
                i(TAG, ex.message)
            }
            i(
                TAG_O_GPS_CENTER, ".\nMGroup service connected" +
                        "\ncomponent name: ${name.toString()}\n"
            )
            i(TAG_M_GROUP_SERVICE, "Service started: true")
        }
    }

    @SuppressLint("LogNotTimber")
    private fun startMGroupTrackerService() {

        val serviceIntent = Intent()

        serviceIntent.component = ComponentName(
            "ru.hunkel.mgrouprssichecker",
            "services.RSSILoggerService"
        )
        serviceIntent.action = "ru.hunkel.mgrouprssichecker.services.RSSILoggerService"
        try {
            startService(serviceIntent)
            val res = bindService(
                serviceIntent,
                mMGroupTrackerServiceConnection,
                Context.BIND_AUTO_CREATE
            )

        } catch (ex: Exception) {
            Log.e(TAG_O_GPS_CENTER, ex.message)
        }

//        startService(serviceIntent)

//        bindService(
//            serviceIntent,
//            mMgroupTrackerServiceConnection,
//            Context.BIND_WAIVE_PRIORITY
//        )
//        bindService(
//            intent,
//            mMGroupTrackerServiceConnection,
//            Context.BIND_AUTO_CREATE
//        )
    }

    private fun stopMGroupTrackerService() {
        try {
            val serviceIntent = Intent()

            serviceIntent.component = ComponentName(
                "ru.hunkel.mgrouprssichecker",
                ".services.RSSILoggerService"
            )

            unbindService(mMGroupTrackerServiceConnection)
            stopService(serviceIntent)
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
