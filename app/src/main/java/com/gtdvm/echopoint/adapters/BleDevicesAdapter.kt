package com.gtdvm.echopoint.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import android.widget.Button
import androidx.recyclerview.widget.DiffUtil
import com.gtdvm.echopoint.DataServices
import com.gtdvm.echopoint.IBeacon
import com.gtdvm.echopoint.R


class BleDevicesAdapter (private val context: Context, private val devices: MutableList<IBeacon> = mutableListOf(), private val onItemClick: (IBeacon) ->Unit) : RecyclerView.Adapter<BleDevicesAdapter.ViewHolder>() {
    private  val dataServices = DataServices()
    inner class ViewHolder (itemView: View) : RecyclerView.ViewHolder (itemView) {
        private val resultScannerDevices: Button = itemView.findViewById(R.id.resultScannerDevicesButton)
        //private val resultScannerDevices: View = itemView.findViewById(R.id.resultScannerDevices)
        fun bind(resultBleDevice: IBeacon) {
            val categoryTextWidgets = dataServices.getNameByMajor(context, resultBleDevice.major.toString())
            val numberTextWidgets = dataServices.getNumberByMinor(context, resultBleDevice.major.toString(), resultBleDevice.minor.toString())
            val informationWidgets = dataServices.getInformationByNumber(context, resultBleDevice.major.toString(), resultBleDevice.minor.toString())
            resultScannerDevices.text = context.getString(R.string.DeviceWidgetName, categoryTextWidgets, numberTextWidgets, informationWidgets)

            resultScannerDevices.setOnClickListener{
                onItemClick(resultBleDevice)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = devices[position]
        holder.bind(device)
    }

    override fun getItemCount() = devices.size
    fun updateDevices (newDevices: List<IBeacon>){
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize() = devices.size
            override fun getNewListSize() = newDevices.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return devices[oldItemPosition].macAddress == newDevices[newItemPosition].macAddress
            }
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return devices[oldItemPosition] == newDevices[newItemPosition]
            }
        }

        val diffResult = DiffUtil.calculateDiff(diffCallback)
        //val previousSize = devices.size
        devices.clear()
        devices.addAll(newDevices)
        diffResult.dispatchUpdatesTo(this)
        /*if (previousSize <devices.size) {
            notifyItemRangeInserted(previousSize, devices.size - previousSize)
        } else if (previousSize > devices.size) {
            notifyItemRangeRemoved(devices.size, previousSize - devices.size)
        } else {
//            notifyDataSetChanged()
        }*/
    }

}
