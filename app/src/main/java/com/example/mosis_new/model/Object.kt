package com.example.mosis_new.model

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.GeoPoint

class Object : ViewModel() {

    var displayName = MutableLiveData<String?>()
    var description = MutableLiveData<String?>()
    var photoUri =  MutableLiveData<Uri?>()
    var userId = MutableLiveData<String?>()
    var kindOf = MutableLiveData<Int?>()
    var location = MutableLiveData<GeoPoint?>()

    fun setObject(name: String, desc: String, uri: Uri?, id: String, kindOf: Int, loc: GeoPoint?){
        displayName.value = name
        description.value = desc
        photoUri.value = uri
        userId.value = id
        this.kindOf.value = kindOf
        location.value = loc

    }
}