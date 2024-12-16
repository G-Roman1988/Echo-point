package com.gtdvm.echopoint

import android.content.Intent
import android.os.Bundle
//import android.view.LayoutInflater
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import com.gtdvm.echopoint.databinding.ActivityUnderCategoryBinding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
//import android.widget.Button
//import android.widget.Toast
//import android.view.View
//import android.view.ViewGroup
import com.gtdvm.echopoint.adapters.NumberAdapter


class UnderCategory : AppCompatActivity() {

    private lateinit var binding: ActivityUnderCategoryBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var numberAdapter: NumberAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityUnderCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(findViewById(R.id.toolbar))
        binding.toolbarLayout.title = title
        binding.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
        recyclerView = findViewById(R.id.underCategoryList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val dropdownSelectedCategory = intent.getStringExtra("selectedCategory")
        if (dropdownSelectedCategory != null) {
            val dataServices = DataServices()
            val numbers = dataServices.getNumbersForCategory(this, dropdownSelectedCategory)
            numberAdapter = NumberAdapter(this, numbers, dropdownSelectedCategory) {
                    number -> onNumberClicked(number)
            }
            recyclerView.adapter = numberAdapter
        }
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
        //Toast.makeText (this, getString(R.string.startBle)+number, Toast.LENGTH_SHORT).show()
    }
}

