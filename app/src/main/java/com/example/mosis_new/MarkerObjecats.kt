package com.example.mosis_new

import com.google.firebase.firestore.DocumentSnapshot
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MarkerObjecats(map: MapView, document: DocumentSnapshot): Marker(map) {
    var doc:DocumentSnapshot? = null
    init {
        doc = document
    }
}