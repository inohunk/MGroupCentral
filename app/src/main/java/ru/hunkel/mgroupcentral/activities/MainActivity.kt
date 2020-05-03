package ru.hunkel.mgroupcentral.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import ru.hunkel.mgroupcentral.R

const val TAG = "MainActivity"

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

    private fun changeTrackingState() {
        if (mIsTracking) {
            if (use_bluetooth_tracking_checkbox.isChecked) {

            }
        } else {

        }
        mIsTracking = !mIsTracking
    }

    private fun startOGPSTrackerService() {

    }

    private fun stopOGPSTrackerService() {

    }

    private fun startMGroupTrackerService() {

    }

    private fun stopMGroupTrackerService() {

    }

    private fun updateUI() {
        if (mIsTracking) {
            start_stop_button.text = "Stop"
        } else {
            start_stop_button.text = "Start"
        }
    }
}
