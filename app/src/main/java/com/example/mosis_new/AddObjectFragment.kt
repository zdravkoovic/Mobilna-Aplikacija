package com.example.mosis_new

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.text.DateFormat
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import com.example.mosis_new.databinding.FragmentAddObjectBinding
import com.example.mosis_new.databinding.FragmentMapBinding
import com.example.mosis_new.databinding.FragmentRegisterBinding
import com.example.mosis_new.model.Object
import com.example.mosis_new.model.User
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.app
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File
import java.util.Calendar
import java.util.Date

class AddObjectFragment : Fragment() {

    private val user: User by activityViewModels()
    private val obj: Object by activityViewModels()
    private var _binding: FragmentAddObjectBinding? = null
    private val binding get() = _binding!!

    private var location: MutableLiveData<Location?> = MutableLiveData(null)
    lateinit var db: FirebaseFirestore
    lateinit var auth: FirebaseAuth
    lateinit var pickMedia: ActivityResultLauncher<Intent>
    private var imageUri: Uri? = null
    private var imageUriTemp: Uri? = null
    private lateinit var storage: StorageReference
    private var locEnabled: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth
        db = Firebase.firestore
        storage = FirebaseStorage.getInstance(Firebase.app).reference
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentAddObjectBinding.inflate(inflater, container, false)

        pickMedia = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
            when(result.resultCode){
                Activity.RESULT_OK -> {
                    if(result.data != null && result.data!!.data != null ){
                        imageUri = result.data!!.data as Uri
                        binding.addObjectPicture.setImageURI(imageUri)
                    }else{
                        imageUri = imageUriTemp
                        binding.addObjectPicture.setImageURI(imageUriTemp)
                    }
                    Log.d("PhotoPicker", "Selected URI: $imageUri")
                }
                else -> {
                    Log.d("PhotoPicker", "Otkazano")
                }
            }
        }

        binding.addObjectButton.setOnClickListener{
            storeDataInFirebase(binding.addObjectSubjectPlain.text.toString(), binding.addObjectDescPlain.text.toString())
        }

        location.observe(viewLifecycleOwner){
            locEnabled = location.value != null
        }

        if(ActivityCompat.checkSelfPermission(requireActivity(),android.Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireActivity(),android.Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()){
                    isGranted:Boolean->
                if(isGranted){
                    LocationServices.getFusedLocationProviderClient(requireContext()).lastLocation.addOnSuccessListener {
                        location.value = it
                    }
                }
            }.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }else{
            LocationServices.getFusedLocationProviderClient(requireContext()).lastLocation.addOnSuccessListener {
                location.value = it
            }
        }

        return binding.root
    }

    override fun onStart() {
        super.onStart()

        val image = binding.addObjectPicture
        image.setOnClickListener{
            val galleryintent = Intent(Intent.ACTION_GET_CONTENT, null)
            galleryintent.type = "image/*"
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val path = File(requireActivity().filesDir, "Pictures")

            if(!path.exists()) path.mkdir()
            val image2 = File(path, "Slika_${
                DateFormat.getDateTimeInstance().format(Date())
            }.jpg")

            imageUriTemp = FileProvider.getUriForFile(requireActivity(), "com.example.MOSIS_new.fileprovider", image2)

            val chooser = Intent(Intent.ACTION_CHOOSER)
            chooser.putExtra(Intent.EXTRA_TITLE, "Select from: ")
            chooser.putExtra(Intent.EXTRA_INTENT, galleryintent)
            val intentArray = arrayOf(cameraIntent)
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
            pickMedia.launch(chooser)


        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // My functions

    private fun storeDataInFirebase(sub: String, desc: String) {
        val firebaseImage = storage.child("images/destinations/${user.email.value}")
        Log.i("Email", "Email is ${user.email}")
        firebaseImage.putFile(imageUri!!).continueWithTask{task ->
            if(task.isSuccessful){
                firebaseImage.downloadUrl
            }
            else{
                task.exception?.let {
                    throw it
                }
            }
        }.addOnCompleteListener{task->
            if(task.isSuccessful){
                addObjectToFirebase(sub, desc, task.result)
            }else{
                task.exception?.let {
                    throw it
                }
            }
        }
    }

    private fun addObjectToFirebase(sub: String, desc: String, uri: Uri){
//        obj.setObject(sub, desc, uri, user.email.value.toString(), "Klisura")

        val obj = hashMapOf(
            "destination" to sub,
            "description" to desc,
            "imageUri" to uri,
            "kindOf" to 0,
            "location" to GeoPoint(location.value!!.latitude, location.value!!.longitude),
            "date" to Calendar.getInstance().time,
            "user" to db.collection("users").document(user.email.value.toString()),
            "like" to ArrayList<String>()
        )

        var destId = db.collection("destinations").document().id
        db.collection("destinations").document(destId)
            .set(obj)
            .addOnSuccessListener {
                Toast.makeText(activity, "Dodata je lokacija", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_AddObjectFragment_To_MapFragment)
            }
            .addOnFailureListener{
                Toast.makeText(activity, "Nije dodata lokacija", Toast.LENGTH_SHORT).show()
            }
    }

}