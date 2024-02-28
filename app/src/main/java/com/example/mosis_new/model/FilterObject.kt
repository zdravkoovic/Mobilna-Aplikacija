package com.example.mosis_new.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.Date

class FilterObject : ViewModel() {

    var autor = MutableLiveData<String?>()
    var desc = MutableLiveData<String?>()
    var radius = MutableLiveData<String?>()
    var startDate = MutableLiveData<String?>()

    fun setFilterObject(autor: String?, desc: String?, radius: String?, start: String?){
        this.autor.value = autor
        this.desc.value = desc
        this.radius.value = radius
        this.startDate.value = start
    }
}