package com.example.mosis_new

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doAfterTextChanged
import androidx.navigation.fragment.findNavController
import com.example.mosis_new.databinding.FragmentRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.io.File
import android.icu.text.DateFormat.getDateTimeInstance
import androidx.core.content.FileProvider
import androidx.fragment.app.activityViewModels
import com.example.mosis_new.model.User
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.ktx.app
import com.google.firebase.storage.FirebaseStorage
import java.util.Date
import com.google.firebase.storage.StorageReference

class RegisterFragment : Fragment(){

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    lateinit var email: EditText
    lateinit var pass: EditText
    lateinit var doublePass: EditText
    lateinit var name: EditText
    lateinit var lastName: EditText
    lateinit var phone: EditText
    lateinit var btn: Button
    lateinit var db: FirebaseFirestore
    lateinit var auth: FirebaseAuth
    lateinit var pickMedia: ActivityResultLauncher<Intent>
    private var imageUri: Uri? = null
    private var imageUriTemp: Uri? = null
    private lateinit var storage: StorageReference
    private val user: User by activityViewModels()

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
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)

        pickMedia = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result ->
            when(result.resultCode){
                Activity.RESULT_OK -> {
                    if(result.data != null && result.data!!.data != null ){
                        imageUri = result.data!!.data as Uri
                        binding.registerImage.setImageURI(imageUri)
                    }else{
                        imageUri = imageUriTemp
                        binding.registerImage.setImageURI(imageUriTemp)
                    }
                    Log.d("PhotoPicker", "Selected URI: $imageUri")
                }
                else -> {
                    Log.d("PhotoPicker", "Otkazano")
                }
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val signIn: TextView = view.findViewById<TextView>(R.id.register_sign_in)
        signIn.setOnClickListener{
            findNavController().navigate(R.id.action_RegisterFragment_To_LoginFragment)
        }

        email = requireView().findViewById<EditText>(R.id.register_email)
        pass = requireView().findViewById<EditText>(R.id.register_password)
        doublePass = requireView().findViewById<EditText>(R.id.register_password_repeat)
        name = requireView().findViewById<EditText>(R.id.register_name)
        lastName = requireView().findViewById<EditText>(R.id.register_lastName)
        phone = requireView().findViewById<EditText>(R.id.register_phone)

        btn = requireView().findViewById<Button>(R.id.register_button)
        btn.isEnabled = false

        checkButtonIsEnable(email, pass, name, lastName, phone)

        btn.setOnClickListener{
            val Email = email.text.toString()
            val Pass = pass.text.toString()
            val Name = name.text.toString()
            val LastName = lastName.text.toString()
            val Phone = phone.text.toString()
            val DoublePass = doublePass.text.toString()

            if(Pass == DoublePass)
                loginUser(Email, Pass, Name, LastName, Phone)
            else{
                Toast.makeText(activity, "Lozinke se ne poklapaju!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()

        val image = binding.registerImage
        image.setOnClickListener{
            val galleryintent = Intent(Intent.ACTION_GET_CONTENT, null)
            galleryintent.type = "image/*"
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val path = File(requireActivity().filesDir, "Pictures")

            if(!path.exists()) path.mkdir()
            val image2 = File(path, "Slika_${
                getDateTimeInstance().format(Date())
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

    private fun checkButtonIsEnable(email: EditText, pass: EditText, name: EditText, lastName: EditText, phone: EditText){

        var validEmail: Boolean = false
        var validPass: Boolean = false
        var validName: Boolean = false
        var validLastName: Boolean = false
        var validPhone: Boolean = false

        email.doAfterTextChanged {
            validEmail = (email.text.isNotEmpty())
            btn.isEnabled = validName && validLastName && validPass && validPhone && validEmail
        }
        pass.doAfterTextChanged {
            validPass = pass.text.isNotEmpty()
            btn.isEnabled = validName && validLastName && validPass && validPhone && validEmail
        }
        name.doAfterTextChanged {
            validName = name.text.isNotEmpty()
            btn.isEnabled = validName && validLastName && validPass && validPhone && validEmail
        }
        lastName.doAfterTextChanged {
            validLastName = lastName.text.isNotEmpty()
            btn.isEnabled = validName && validLastName && validPass && validPhone && validEmail
        }
        phone.doAfterTextChanged {
            validPhone = phone.text.isNotEmpty()
            btn.isEnabled = validName && validLastName && validPass && validPhone && validEmail
        }
    }
    private fun loginUser(email: String, pass: String, name: String, lastName: String, phone: String){
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener(requireActivity()){ task ->
                if(task.isSuccessful){
                    addDataToFireStore(email, pass, name, lastName, phone)
                    Toast.makeText(activity, "Kreiran je korisnik!", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_RegisterFragment_To_MapFragment)
                }else{
                    Toast.makeText(
                        activity,
                        "Authentication failed in registration.",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
    }
    private fun addDataToFireStore(email: String, pass: String, name: String, lastName: String, phone: String){

        user.setFirebaseUser(auth.currentUser)
        user.displayName.value = name
        if(imageUri!=null){
            val firebaseImages = storage.child("images/profiles/$email")
            firebaseImages.putFile(imageUri!!).continueWithTask { task ->
                if (task.isSuccessful) {
                    firebaseImages.downloadUrl
                }else{
                    task.exception?.let {
                        throw it
                    }
                }
            }.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    auth.currentUser?.updateProfile(userProfileChangeRequest {
                        photoUri = task.result
                        displayName = name
                    })?.addOnCompleteListener {
                        user.setFirebaseUser(auth.currentUser)
                    }
                    user.photoUri.value = task.result
                    db.collection("users").document(email).update("photoUri", task.result)
                }
            }
        }else{
            auth.currentUser?.updateProfile(userProfileChangeRequest {
                displayName = name
            })?.addOnCompleteListener {
                user.setFirebaseUser(auth.currentUser)
            }
        }

        val score: Int = 0

        val user = hashMapOf(
            "email" to email,
            "password" to pass,
            "name" to name,
            "lastName" to lastName,
            "phone" to phone,
            "score" to score,
            "posts" to ArrayList<String>(),
            "displayName" to user.displayName.value,
            "photoUri" to ""
        )
        db.collection("users").document(email)
            .set(user)
            .addOnSuccessListener {
                Log.i("Firebase", "Document SnapShot successfuly writen!")
            }
            .addOnFailureListener { Log.w("Firebase", "Error writing document!")}
    }
}