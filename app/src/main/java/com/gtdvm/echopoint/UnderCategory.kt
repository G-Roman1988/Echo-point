package com.gtdvm.echopoint

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
//import android.view.LayoutInflater
//import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
//import com.gtdvm.echopoint.databinding.ActivityUnderCategoryBinding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
//import android.widget.Button
//import android.widget.Toast
//import android.view.View
//import android.view.ViewGroup
import com.gtdvm.echopoint.adapters.NumberAdapter


class UnderCategory : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var numberAdapter: NumberAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_under_category)
        val dropdownSelectedCategory = intent.getStringExtra("selectedCategory")
        val toolbar: Toolbar = findViewById(R.id.underCategoryListToolBar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = dropdownSelectedCategory

        recyclerView = findViewById(R.id.underCategoryList)
        recyclerView.layoutManager = LinearLayoutManager(this)

        if (dropdownSelectedCategory != null) {
            val dataServices = DataServices()
            val numbers = dataServices.getNumbersForCategory(this, dropdownSelectedCategory)
            numberAdapter = NumberAdapter(this, numbers, dropdownSelectedCategory) {
                    number -> onNumberClicked(number)
            }
            recyclerView.adapter = numberAdapter
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                SelectedDevice.resetSelection()
                finish()
            }
        })
            }

    private fun onNumberClicked (number: String) {
SelectedDevice.setUnderCategory(this, number)
        if (SelectedDevice.isAllItems(this, number)){
            val listDevices = Intent(applicationContext, ListDevices::class.java)
            startActivity(listDevices)
        } else{
            val intentAutoConnect = Intent(applicationContext, ScanAndCommunicationSelectedDevice::class.java)
            startActivity(intentAutoConnect)
        }
    }
}

