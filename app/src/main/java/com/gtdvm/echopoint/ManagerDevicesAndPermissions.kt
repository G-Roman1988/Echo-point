package com.gtdvm.echopoint

import android.Manifest
import android.annotation.SuppressLint
//import android.app.AlertDialog
import androidx.appcompat.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.graphics.toColorInt

open class ManagerDevicesAndPermissions : AppCompatActivity() {

    val requestPermissionsLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            // Handle Permission granted/rejected
            permissions.entries.forEach {
                val permissionName = it.key
                val isGranted = it.value
                if (isGranted) {
                    Toast.makeText(this, "$permissionName aceptată", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "$permissionName permission granted")
                    // Permission is granted. Continue the action or workflow in your
                    // app.
                } else {
                    Toast.makeText(this, "$permissionName refuzată", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "$permissionName permission denied")
                    // Explain to the user that the feature is unavailable because the
                    // features requires a permission that the user has denied.
                }
            }
        }

    companion object {
        const val TAG = "ManagerDevicesAndPermissions"
    }
}

class PermissionsHelper(val context: Context) {
    // Manifest.permission.ACCESS_BACKGROUND_LOCATION
    // Manifest.permission.ACCESS_FINE_LOCATION
    // Manifest.permission.BLUETOOTH_CONNECT
    // Manifest.permission.BLUETOOTH_SCAN
    fun isPermissionGranted(permissionString: String): Boolean {
        return (ContextCompat.checkSelfPermission(context, permissionString) == PackageManager.PERMISSION_GRANTED)
    }
    fun setFirstTimeAskingPermission(permissionString: String, isFirstTime: Boolean) {
        //val sharedPreference =
            context.getSharedPreferences("org.altbeacon.permisisons", AppCompatActivity.MODE_PRIVATE)
                .edit {putBoolean(permissionString, isFirstTime)}
    }

    fun isFirstTimeAskingPermission(permissionString: String): Boolean {
        val sharedPreference = context.getSharedPreferences("org.altbeacon.permisisons", AppCompatActivity.MODE_PRIVATE)
        return sharedPreference.getBoolean(permissionString, true)
    }

    fun beaconScanPermissionGroupsNeeded(backgroundAccessRequested: Boolean = false): List<Array<String>> {
        val permissions = ArrayList<Array<String>>()
         // As of version M (6) we need FINE_LOCATION (or COARSE_LOCATION, but we ask for FINE)
            permissions.add(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // As of version Q (10) we need FINE_LOCATION and BACKGROUND_LOCATION
            if (backgroundAccessRequested) {
                permissions.add(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // As of version S (12) we need FINE_LOCATION, BLUETOOTH_SCAN and BACKGROUND_LOCATION
            // Manifest.permission.BLUETOOTH_CONNECT is not absolutely required to do just scanning,
            // but it is required if you want to access some info from the scans like the device name
            // and the aditional cost of requsting this access is minimal, so we just request it
            permissions.add(arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // As of version T (13) we POST_NOTIFICATIONS permissions if using a foreground service
            permissions.add(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
        }
        return permissions
    }

}



open class BeaconScanPermissionsActivity: ManagerDevicesAndPermissions()  {
    lateinit var layout: LinearLayout
    private lateinit var permissionGroups: List<Array<String>>
    private lateinit var continueButton: Button
    private val scale: Float get() = resources.displayMetrics.density

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        layout = LinearLayout(this)
        //layout.setPadding(dp(20))
        layout.gravity = Gravity.CENTER
        layout.setBackgroundColor(Color.BLACK)
        layout.orientation = LinearLayout.VERTICAL
        ViewCompat.setOnApplyWindowInsetsListener(layout) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                top = systemBars.top + dp(20),
                bottom = systemBars.bottom + dp(20),
                left = dp(20),
                right = dp(20)
            )
            insets
        }

        val backgroundAccessRequested = intent.getBooleanExtra("backgroundAccessRequested", true)
        val title = intent.getStringExtra("title") ?: "Permisiuni necesare"
        val message = intent.getStringExtra("message") ?: "Pentru a scana dispozitive BLE, această aplicație necesită următoarele permisiuni de la sistemul de operare.  Vă rugăm să atingeți fiecare buton pentru a acorda  fiecare permisiunea necesară."
        val continueButtonTitle = intent.getStringExtra("continueButtonTitle") ?: "Continue"
        val permissionButtonTitles = intent.getBundleExtra("permissionBundleTitles") ?: getDefaultPermissionTitlesBundle()

        permissionGroups = PermissionsHelper(this).beaconScanPermissionGroupsNeeded(backgroundAccessRequested)

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(dp(0), dp(10), dp(0), dp(10))
        val titleView = TextView(this).apply {
            //titleView.setGravity(Gravity.CENTER)
            gravity = Gravity.CENTER
            textSize = dp(10).toFloat()
            text = title
            layoutParams = params
        }
        layout.addView(titleView)
        val messageView = TextView(this).apply {
            text = message
            gravity = Gravity.CENTER
            textSize = dp(5).toFloat()
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            layoutParams = params
        }
        layout.addView(messageView)

        for ((index, permissionGroup) in permissionGroups.withIndex()) {
            val checkBox = CheckBox(this).apply {
                id = index
                text = permissionButtonTitles.getString(permissionGroup.first())
                layoutParams = params
                setOnClickListener(checkBoxClickListener)
            }
            layout.addView(checkBox)
        }

        continueButton = Button(this).apply {
            text = continueButtonTitle
            isEnabled = false
            setOnClickListener { finish() }
            layoutParams = params
        }
        layout.addView(continueButton)

        setContentView(layout)
    }

    private fun dp(value: Int): Int {
        return (value * scale + 0.5f).toInt()
    }

    private val checkBoxClickListener = View.OnClickListener { view ->
        val checkBox = view as CheckBox
        val permissionsGroup = permissionGroups[checkBox.id]
        if (allPermissionsGranted(permissionsGroup)){
            checkBox.isChecked = true
        } else{
            promptForPermissions(permissionsGroup)
        }
    }

    @SuppressLint("InlinedApi")
    fun getDefaultPermissionTitlesBundle(): Bundle {
        val bundle = Bundle()
        bundle.putString(Manifest.permission.ACCESS_FINE_LOCATION, "Location")
        bundle.putString(Manifest.permission.ACCESS_BACKGROUND_LOCATION, "Background Location")
        bundle.putString(Manifest.permission.BLUETOOTH_SCAN, "Bluetooth")
        bundle.putString(Manifest.permission.POST_NOTIFICATIONS, "Notifications")
        return bundle
    }


    private fun allPermissionGroupsGranted(): Boolean {
        for (permissionsGroup in permissionGroups) {
            if (!allPermissionsGranted(permissionsGroup)) {
                return false
            }
        }
        return true
    }

    private fun setCheckBoxColors() {
        for ((index, permissionsGroup) in permissionGroups.withIndex()) {
            val checkBox = findViewById<CheckBox>(index)
            if (allPermissionsGranted(permissionsGroup)) {
                checkBox.setBackgroundColor("#448844".toColorInt())
                checkBox.isChecked = true
            }
            else {
                checkBox.setBackgroundColor("#FF6666".toColorInt())
                checkBox.isChecked = false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setCheckBoxColors()
        if (allPermissionGroupsGranted()) {
            continueButton.isEnabled = true
        }
    }

    private fun promptForPermissions(permissionsGroup: Array<String>) {
        if (!allPermissionsGranted(permissionsGroup)) {
            val firstPermission = permissionsGroup.first()
            val isFirstTime = PermissionsHelper(this).isFirstTimeAskingPermission(firstPermission)
            val showRationale = shouldShowRequestPermissionRationale(firstPermission)
            Log.d(TAG, "promptForPermissions: permission=$firstPermission showRationale=$showRationale isFirstTime=$isFirstTime")
            if (isFirstTime) {
// First request — we ask for permission directly
                PermissionsHelper(this).setFirstTimeAskingPermission(firstPermission, false)
                requestPermissionsLauncher.launch(permissionsGroup)
            } else if (showRationale) {
                // Second request — Android shows rationale (user refused once) We ask again, Android will display the explanation
                requestPermissionsLauncher.launch(permissionsGroup)
            }
            else {
                Log.d(TAG, "Permissions have been denied and the settings button is displayed.")
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Permisiune necesară")
                builder.setMessage("Această permisiune a fost refuzată anterior. " + "Pentru a o acorda, apăsați 'Deschide Setările', navigați la " + "'Permisiuni' și acordați permisiunea necesară.")
                builder.setPositiveButton("Deschide Setările") { _, _ ->
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                    }
                    startActivity(intent)
                }
                builder.setNegativeButton("Anulează", null)
                builder.show()
            }
        }
    }
    private fun allPermissionsGranted(permissionsGroup: Array<String>): Boolean {
        val permissionsHelper = PermissionsHelper(this)
        for (permission in permissionsGroup) {
            if (!permissionsHelper.isPermissionGranted(permission)) {
                return false
            }
        }
        return true
    }

    companion object {
        const val TAG = "BeaconScanPermissionActivity"
        fun allPermissionsGranted(context: Context, backgroundAccessRequested: Boolean): Boolean {
            val permissionsHelper = PermissionsHelper(context)
            val permissionsGroups = permissionsHelper.beaconScanPermissionGroupsNeeded(backgroundAccessRequested)
            for (permissionsGroup in permissionsGroups) {
                for (permission in permissionsGroup) {
                    if (!permissionsHelper.isPermissionGranted(permission)) {
                        return false
                    }
                }
            }
            return true
        }
    }

    //override fun onCreate(savedInstanceState: Bundle?) {
        //super.onCreate(savedInstanceState)
    //}
}