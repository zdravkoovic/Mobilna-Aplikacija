package com.example.mosis_new.data

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class User(var email: String? = null, var pass: String? = null, var name: String? = null, var lastName: String? = null, var phone: String? = null){

}
