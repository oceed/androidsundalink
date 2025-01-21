package com.sundalink.mapstracker

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate
import java.time.LocalTime

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

        val currentDate = LocalDate.now()
        val activityDate = LocalDate.parse(activity.date)

        val startTime = LocalTime.parse(activity.time)
        val endTime = if (position < activities.size - 1) {
            LocalTime.parse(activities[position + 1].time) // Waktu aktivitas berikutnya
        } else {
            LocalTime.of(23, 59) // Akhir hari untuk aktivitas terakhir
        }

        val currentTime = LocalTime.now()

        // Menentukan status
        when {
            // Status "proses" jika tanggalnya sama dan waktu saat ini di rentang waktu aktivitas
            currentDate == activityDate && currentTime.isAfter(startTime) && currentTime.isBefore(endTime) -> {
                holder.statusTextView.text = "Proses"
                holder.statusTextView.setTextColor(holder.itemView.context.getColor(R.color.bluetext))
                holder.statusBg.setCardBackgroundColor(holder.itemView.context.getColor(R.color.bluebg))
                holder.activityBg.setCardBackgroundColor(holder.itemView.context.getColor(R.color.myprimary))
                holder.timeTextView.setTextColor(holder.itemView.context.getColor(android.R.color.white))
                holder.descriptionTextView.setTextColor(holder.itemView.context.getColor(android.R.color.white))
                holder.summaryPlacesTextView.setTextColor(holder.itemView.context.getColor(android.R.color.white))
            }

            // Status "selesai" jika aktivitas sudah lewat
            currentDate > activityDate || (currentDate == activityDate && currentTime.isAfter(endTime)) -> {
                holder.statusTextView.text = "Selesai"
                holder.statusTextView.setTextColor(holder.itemView.context.getColor(R.color.myprimary))
                holder.statusBg.setCardBackgroundColor(holder.itemView.context.getColor(R.color.greenbg))
                holder.activityBg.setCardBackgroundColor(holder.itemView.context.getColor(android.R.color.white))
                holder.timeTextView.setTextColor(holder.itemView.context.getColor(android.R.color.black))
                holder.descriptionTextView.setTextColor(holder.itemView.context.getColor(android.R.color.black))
                holder.summaryPlacesTextView.setTextColor(holder.itemView.context.getColor(android.R.color.black))
            }

            // Status "segera" jika aktivitas ada di masa depan
            currentDate < activityDate || (currentDate == activityDate && currentTime.isBefore(startTime)) -> {
                holder.statusTextView.text = "Segera"
                holder.statusTextView.setTextColor(holder.itemView.context.getColor(R.color.orangetext))
                holder.statusBg.setCardBackgroundColor(holder.itemView.context.getColor(R.color.orangebg))
                holder.activityBg.setCardBackgroundColor(holder.itemView.context.getColor(android.R.color.white))
                holder.timeTextView.setTextColor(holder.itemView.context.getColor(android.R.color.black))
                holder.descriptionTextView.setTextColor(holder.itemView.context.getColor(android.R.color.black))
                holder.summaryPlacesTextView.setTextColor(holder.itemView.context.getColor(android.R.color.black))
            }
        }
    }

    override fun getItemCount(): Int = activities.size

    inner class ActivityViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timeTextView: TextView = view.findViewById(R.id.textViewTime)
        val descriptionTextView: TextView = view.findViewById(R.id.textViewDescription)
        val summaryPlacesTextView: TextView = view.findViewById(R.id.text_summary_places)
        val statusTextView: TextView = view.findViewById(R.id.statustext)
        val activityBg: CardView = view.findViewById(R.id.activityBg)
        val statusBg: CardView = view.findViewById(R.id.statusbg)
    }
}
