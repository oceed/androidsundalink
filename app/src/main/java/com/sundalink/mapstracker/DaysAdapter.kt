package com.sundalink.mapstracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DayAdapter(
    private val days: List<Day>,
    private val onClick: (Day) -> Unit
) : RecyclerView.Adapter<DayAdapter.DayViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val day = days[position]
        holder.dayTextView.text = "Day ${day.day}: ${day.summaryPlaces}"
        holder.itemView.setOnClickListener { onClick(day) }
    }

    override fun getItemCount(): Int = days.size

    inner class DayViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dayTextView: TextView = view.findViewById(android.R.id.text1)
    }
}