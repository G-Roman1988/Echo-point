package com.gtdvm.echopoint

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView

class NumberAdapter(private val context: Context, private val numbers: List<String>, private val numeCategorie: String, private val onItemClick: (String) ->Unit) : RecyclerView.Adapter<NumberAdapter.NumberViewHolder>() {
    inner class NumberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        //private val underCategory: View = itemView.findViewById(R.id.underCategoryList)
        private val underCategory: Button = itemView.findViewById(R.id.resultScannerDevicesButton)
        fun bind(number: String) {
            underCategory.text =context.getString(R.string.DeviceWidgetList, numeCategorie, number)

            underCategory.setOnClickListener {
                onItemClick(number)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NumberViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false)
        return NumberViewHolder(view)
    }

    override fun onBindViewHolder(holder: NumberViewHolder, position: Int) {
        val number = numbers[position]
        holder.bind(number)
    }

    override fun getItemCount(): Int {
        return numbers.size
    }

}