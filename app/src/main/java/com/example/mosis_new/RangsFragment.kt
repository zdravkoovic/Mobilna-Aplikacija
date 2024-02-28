package com.example.mosis_new

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import coil.load

class RangsFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private var users: List<DocumentSnapshot>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        db = Firebase.firestore
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_rangs, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db.collection("users").get().addOnSuccessListener {
            users = it.documents
            val adapter = object : ArrayAdapter<DocumentSnapshot>(requireContext(), R.layout.items, users!!){
                var items: List<DocumentSnapshot>
                init{
                    items = users!!.sortedBy {
                        it.data!!["score"] as Long
                    }.reversed()
                }
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    var listItem = convertView
                    if(listItem == null)
                        listItem = LayoutInflater.from(context).inflate(R.layout.items, parent, false)
                    listItem?.findViewById<ImageView>(R.id.icon)?.load(Uri.parse(items[position].data!!["photoUri"] as String))
                    listItem?.findViewById<TextView>(R.id.firstLine)?.text = items[position].data!!["displayName"] as String
                    listItem?.findViewById<TextView>(R.id.secondLine)?.text = (items[position].data!!["score"] as Long).toString()
                    listItem?.findViewById<TextView>(R.id.position)?.text = (position + 1).toString()
                    return listItem!!
                }
            }
            requireView().findViewById<ListView>(R.id.rangs_listView).adapter = adapter
        }
    }
}