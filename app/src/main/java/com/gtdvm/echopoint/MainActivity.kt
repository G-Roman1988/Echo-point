package com.gtdvm.echopoint


import android.content.Intent
//import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.view.ViewGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.widget.Toolbar


class MainActivity : AppCompatActivity(), ManagerDevices.BluetoothPermissionCallback {
    private lateinit var managerDevices: ManagerDevices

    override fun onLocationEnabled() {
        Toast.makeText(applicationContext, getString(R.string.Location_sucessed_enabled), Toast.LENGTH_SHORT).show()
    }

    override fun onLocationDisabled() {
        managerDevices.requestActivateLocation(this)
    }

    override fun onBluetoothEnabled() {
        Toast.makeText(applicationContext, getString(R.string.enabledBle), Toast.LENGTH_SHORT).show()
        if (!managerDevices.isLocationActive()) {
            Toast.makeText(applicationContext, getString(R.string.Location_Is_Disabled), Toast.LENGTH_SHORT).show()
            managerDevices.requestActivateLocation(this)
        }
    }

    override fun onBluetoothDisabled() {
        Toast.makeText(applicationContext, getString(R.string.dissabledBlue), Toast.LENGTH_SHORT).show()
        managerDevices.requestEnableBluetooth(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val mainAppBar: Toolbar = findViewById(R.id.MainAppBar)
        setSupportActionBar(mainAppBar)
        supportActionBar?.title = this.getString(R.string.app_name)
        managerDevices = ManagerDevices(this)
        val dataServices = DataServices()
        val spinner:Spinner = findViewById(R.id.spinner)
        val categories = dataServices.getDropdownCategoryName(this)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        //val adapter = ArrayAdapter(this, R.layout.spinner_dropdown_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        //adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinner.adapter = adapter



        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedCategory = parent.getItemAtPosition(position) as String
                Toast.makeText(applicationContext, getString(R.string.selectcategory)+selectedCategory, Toast.LENGTH_SHORT).show()
                if (selectedCategory != categories[0]) {
                    SelectedDevice.setCategory(this@MainActivity, selectedCategory)
                    val intent = Intent(applicationContext, UnderCategory::class.java)
                    intent.putExtra("selectedCategory", selectedCategory)
                    startActivity(intent)
                }
                spinner.setSelection(0)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {

            }
        }
        val startScanButton: Button = findViewById(R.id.startScaning)
        startScanButton.setOnClickListener {
            startActivity(Intent(this, ListDevices::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        if (!BeaconScanPermissionsActivity.allPermissionsGranted(this, true)) {
            val intent = Intent(this, BeaconScanPermissionsActivity::class.java)
            intent.putExtra("backgroundAccessRequested", true)
            startActivity(intent)
        }
        if (!managerDevices.isBluetoothEnabled()) {
            Toast.makeText(applicationContext, getString(R.string.dissabledBlue), Toast.LENGTH_SHORT).show()
            managerDevices.requestEnableBluetooth(this)
        }
        if (managerDevices.isBluetoothEnabled() && !managerDevices.isLocationActive()) {
            Toast.makeText(applicationContext, getString(R.string.Location_Is_Disabled), Toast.LENGTH_SHORT).show()
            managerDevices.requestActivateLocation(this)
        }
    }



}

