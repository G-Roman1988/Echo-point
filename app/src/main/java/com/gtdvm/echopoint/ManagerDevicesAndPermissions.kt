package com.gtdvm.echopoint

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
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
import androidx.core.view.setPadding
import android.widget.Toast

open class ManagerDevicesAndPermissions : AppCompatActivity() {

    val requestPermissionsLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            // Handle Permission granted/rejected
            permissions.entries.forEach {
                val permissionName = it.key
                val isGranted = it.value
                if (isGranted) {
                    Toast.makeText(this, "$permissionName aceptată", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "$permissionName permission granted: $isGranted")
                    // Permission is granted. Continue the action or workflow in your
                    // app.
                } else {
                    Toast.makeText(this, "$permissionName refuzată", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "$permissionName permission granted: $isGranted")
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
        val sharedPreference = context.getSharedPreferences("org.altbeacon.permisisons",
            AppCompatActivity.MODE_PRIVATE
        )
        sharedPreference.edit().putBoolean(permissionString,
            isFirstTime).apply()
    }

    fun isFirstTimeAskingPermission(permissionString: String): Boolean {
        val sharedPreference = context.getSharedPreferences("org.altbeacon.permisisons", AppCompatActivity.MODE_PRIVATE)
        return sharedPreference.getBoolean(permissionString, true)
    }

    fun beaconScanPermissionGroupsNeeded(backgroundAccessRequested: Boolean = false): List<Array<String>> {
        val permissions = ArrayList<Array<String>>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // As of version M (6) we need FINE_LOCATION (or COARSE_LOCATION, but we ask for FINE)
            permissions.add(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
        }
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
    private var scale: Float = 1.0f
        get() {
            return this.getResources().getDisplayMetrics().density
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        layout = LinearLayout(this)
        layout.setPadding(dp(20))
        layout.gravity = Gravity.CENTER
        layout.setBackgroundColor(Color.BLACK)
        layout.orientation = LinearLayout.VERTICAL
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


        val titleView = TextView(this)
        titleView.setGravity(Gravity.CENTER)
        titleView.textSize = dp(10).toFloat()
        titleView.text = title
        titleView.layoutParams = params

        layout.addView(titleView)
        val messageView = TextView(this)
        messageView.text = message
        messageView.setGravity(Gravity.CENTER)
        messageView.textSize = dp(5).toFloat()
        messageView.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
        messageView.layoutParams = params
        layout.addView(messageView)

        var index = 0
        for (permissionGroup in permissionGroups) {
            val checkBox = CheckBox(this).apply {
                id = index
                text = permissionButtonTitles.getString(permissionGroup.first())
                layoutParams = params
                setOnClickListener(checkBoxClickListener)
            }
            layout.addView(checkBox)
            index += 1
        }

        continueButton = Button(this)
        continueButton.text = continueButtonTitle
        continueButton.isEnabled = false
        continueButton.setOnClickListener {
            this.finish()
        }
        continueButton.layoutParams = params
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
        var index = 0
        for (permissionsGroup in this.permissionGroups) {
            val checkBox = findViewById<CheckBox>(index)
            if (allPermissionsGranted(permissionsGroup)) {
                checkBox.setBackgroundColor(Color.parseColor("#448844"))
                checkBox.isChecked = true
            }
            else {
                checkBox.setBackgroundColor(Color.RED)
                checkBox.isChecked = false
            }
            index ++
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

            var showRationale = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                showRationale = shouldShowRequestPermissionRationale(firstPermission)
            }
            if (showRationale ||  PermissionsHelper(this).isFirstTimeAskingPermission(firstPermission)) {
                PermissionsHelper(this).setFirstTimeAskingPermission(firstPermission, false)
                requestPermissionsLauncher.launch(permissionsGroup)
            }
            else {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Nu pot solicita permisiunea")
                builder.setMessage("Această permisiune a fost refuzată anterior acestei aplicații.  Pentru a o acorda acum, trebuie să accesați Setările Android pentru a activa această permisiune.")
                builder.setPositiveButton("OK", null)
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