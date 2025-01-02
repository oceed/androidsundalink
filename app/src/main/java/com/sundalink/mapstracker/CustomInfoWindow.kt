package com.sundalink.mapstracker

import android.widget.ImageView
import android.widget.TextView
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.InfoWindow

class CustomInfoWindow(mapView: MapView?) : InfoWindow(R.layout.layout_tooltip, mapView) {

    override fun onOpen(item: Any) {
        val marker = item as Marker
        val infoWindowData = marker.relatedObject as ModelMain

        val tvNamaLokasi = mView.findViewById<TextView>(R.id.tvNamaLokasi)
        val tvAlamat = mView.findViewById<TextView>(R.id.tvAlamat)
        val imageClose = mView.findViewById<ImageView>(R.id.imageClose)

        tvNamaLokasi.text = infoWindowData.strName
        tvAlamat.text = infoWindowData.strVicinity
        imageClose.setOnClickListener {
            marker.closeInfoWindow()
        }
    }

    override fun onClose() {
        // Tidak ada tindakan tambahan
    }
}
