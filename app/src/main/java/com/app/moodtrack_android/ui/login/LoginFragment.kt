package com.app.moodtrack_android.ui.login

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.app.moodtrack_android.R
import com.app.moodtrack_android.auth.AuthResult
import com.app.moodtrack_android.databinding.FragmentLoginBinding
import com.app.moodtrack_android.model.user.User
import com.app.moodtrack_android.repository.LogEntryRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : Fragment() {

    val TAG = "LoginFragment"

    private val viewModel: LoginViewModel by viewModels()
    private lateinit var logEntryRepository: LogEntryRepository
    private var _binding: FragmentLoginBinding? = null

    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val callback = requireActivity().onBackPressedDispatcher.addCallback(this) {
            activity?.finishAffinity()
        }
        callback.isEnabled = true
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if(FirebaseAuth.getInstance().currentUser != null){
            navigateToHome(null)
        }

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        viewModel.errorMessage.observe(viewLifecycleOwner, { error ->
            if(error != null){
                binding.loginTextviewErrorMessage.text = error
                binding.loginTextviewErrorMessage.visibility = View.VISIBLE
            } else {
                binding.loginTextviewErrorMessage.visibility = View.GONE
            }
        })

        viewModel.isLoading.observe(viewLifecycleOwner){
            setLoginButtonLoading(it)
        }

        binding.buttonLogin.setOnClickListener { attemptLogin() }
        binding.buttonRegister.setOnClickListener { navigateToRegister() }
        binding.loginTextviewErrorMessage.visibility = View.GONE
    }

    private fun attemptLogin(){
        val email = binding.loginTextInputLayoutEmail.editText?.text.toString().trim()
        val password = binding.loginTextInputLayoutPassword.editText?.text.toString().trim()
        if(email.isNotEmpty() && password.isNotEmpty()) {
            viewModel.authenticateWithEmailAndPassword(email, password, ::navigateToHome)
        } else {
            if(email.isEmpty()){
                binding.loginTextInputLayoutEmail.error = "Dette feltet kan ikke være tomt"
            }
            if(password.isEmpty()){
                binding.loginTextInputLayoutPassword.error = "Dette feltet kan ikke være tomt"
            }
        }
    }

    private fun navigateToRegister(){
        findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
    }

    private fun navigateToHome(user: User?){
        val bundle = Bundle()
        bundle.putSerializable("SIGNED_IN_USER", user)
        findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
    }

    private fun setLoginButtonLoading(isLoading: Boolean){
        if(isLoading){
            Log.d(TAG, "Setting loading state -> loading")
            binding.buttonLogin.visibility = View.GONE
            binding.progressBarLoading.visibility = View.VISIBLE
        } else {
            Log.d(TAG, "Setting loading state -> not loading")
            binding.buttonLogin.visibility = View.VISIBLE
            binding.progressBarLoading.visibility = View.GONE
        }
    }

    private fun setErrorMessage(authResult: AuthResult){
        var errorMessage = "En ukjent feil forekom."
        when(authResult.error){
            is FirebaseNetworkException -> {
                errorMessage = "En nettverksfeil forekom."
            }
            is FirebaseAuthInvalidCredentialsException -> {
                errorMessage = "Ugyldig passord eller epost."
            }
        }
        val duration = Toast.LENGTH_SHORT
        val toast = Toast.makeText(context, errorMessage, duration)
        toast.show()
    }
}