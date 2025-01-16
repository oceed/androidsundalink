package com.sundalink.mapstracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ActivitiesAdapter(private val activities: List<Activity>) :
    RecyclerView.Adapter<ActivitiesAdapter.ActivityViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_activity, parent, false)
        return ActivityViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        holder.bind(activities[position])
    }

    override fun getItemCount() = activities.size

    inner class ActivityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(activity: Activity) {
            itemView.findViewById<TextView>(R.id.textViewTime).text = activity.time
            itemView.findViewById<TextView>(R.id.textViewDescription).text = activity.description
        }
    }
}
