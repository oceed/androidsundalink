package com.sundalink.mapstracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ActivityAdapter(private val activities: List<Activity>) : RecyclerView.Adapter<ActivityAdapter.ActivityViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_activity, parent, false)
        return ActivityViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        val activity = activities[position]
        holder.timeTextView.text = activity.time
        holder.descriptionTextView.text = activity.description
        holder.summaryPlacesTextView.text = activity.summaryPlaces
    }

    override fun getItemCount(): Int = activities.size

    inner class ActivityViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timeTextView: TextView = view.findViewById(R.id.textViewTime)
        val descriptionTextView: TextView = view.findViewById(R.id.textViewDescription)
        val summaryPlacesTextView: TextView = view.findViewById(R.id.text_summary_places)
    }
}
