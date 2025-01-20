package com.sundalink.mapstracker

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class DayAdapter(
    private val days: List<Day>,
    private val onClick: (Day) -> Unit
) : RecyclerView.Adapter<DayAdapter.DayViewHolder>() {

    private var selectedDayId: String? = null // Untuk melacak day yang dipilih

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_day, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val day = days[position]

        // Atur teks hari
        holder.dayTextView.text = "${day.day}"

        // Logika untuk warna latar belakang dan teks
        val isSelected = day.id == selectedDayId
        val context = holder.itemView.context

        if (isSelected) {
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.myprimary)) // Hijau untuk yang dipilih
            holder.dayTextView.setTextColor(ContextCompat.getColor(context, android.R.color.white))
            holder.titleTextView.setTextColor(ContextCompat.getColor(context, android.R.color.white))// Teks putih
        } else {
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, android.R.color.white)) // Putih untuk lainnya
            holder.dayTextView.setTextColor(ContextCompat.getColor(context, R.color.black))
            holder.titleTextView.setTextColor(ContextCompat.getColor(context, R.color.black))// Teks hitam
        }

        // Set OnClickListener untuk memilih hari
        holder.itemView.setOnClickListener {
            selectedDayId = day.id // Simpan ID hari yang dipilih
            notifyDataSetChanged() // Refresh tampilan
            onClick(day) // Panggil callback
        }
    }

    override fun getItemCount(): Int = days.size

    inner class DayViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: CardView = view as CardView // Ambil CardView root
        val dayTextView: TextView = view.findViewById(R.id.dayTextView)
        val titleTextView: TextView = view.findViewById(R.id.titleTextView)
    }

    // Fungsi untuk mengatur hari yang dipilih pertama kali
    fun setInitialSelectedDay(dayId: String?) {
        selectedDayId = dayId
        notifyDataSetChanged()
    }
}
