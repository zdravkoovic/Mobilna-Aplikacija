package com.example.mosis_new

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.mosis_new.databinding.FragmentLoginBinding
import com.example.mosis_new.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val user: User by activityViewModels()
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val signUp = binding.loginSignUp
        var login = binding.loginButton
        val email: EditText = requireView().findViewById<EditText>(R.id.login_email_text)
        val pass: EditText = requireView().findViewById<EditText>(R.id.login_password_text)


        login.setOnClickListener{
            loginUser(email.text.toString(), pass.text.toString())
//            findNavController().navigate(R.id.action_LoginFragment_To_MapFragment)
        }

        signUp.setOnClickListener{
            this.findNavController().navigate(R.id.action_LoginFragment_to_RegisterFragment)
        }

//        val navView : NavigationView = requireActivity().findViewById<NavigationView>(R.id.nav_view)
//        navView.visibility = View.GONE

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if(currentUser != null){
            user.setFirebaseUser(auth.currentUser)
            findNavController().navigate(R.id.action_LoginFragment_To_MapFragment)
        }
    }

    // My functions

    private fun loginUser(email: String, password: String){
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if(task.isSuccessful){
                    user.setFirebaseUser(auth.currentUser)
                    findNavController().navigate(R.id.action_LoginFragment_To_MapFragment)
                }else{
                    Toast.makeText(
                        activity,
                        "Authentication failed.",
                        Toast.LENGTH_SHORT,
                    ).show()

                }
            }
    }
}