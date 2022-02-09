package com.app.moodtrack_android.ui.register

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.app.moodtrack_android.R
import com.app.moodtrack_android.auth.AuthResult
import com.app.moodtrack_android.databinding.FragmentRegisterBinding
import com.app.moodtrack_android.model.user.User
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterFragment : Fragment() {

    val viewModel: RegisterViewModel by viewModels()
    private var _binding: FragmentRegisterBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        binding.registerButtonRegister.setOnClickListener { attemptRegisterUser() }
        binding.registerTextviewError.visibility = View.GONE

        binding.registerButtonBack.setOnClickListener {
            findNavController().popBackStack()
        }

        viewModel.isLoading.observe(viewLifecycleOwner) {
            if (it) {
                binding.registerProgressBar.visibility = View.VISIBLE
                binding.registerButtonRegister.visibility = View.GONE
            } else {
                binding.registerProgressBar.visibility = View.GONE
                binding.registerButtonRegister.visibility = View.VISIBLE
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) {
            if (it != null) {
                binding.registerTextviewError.text = it
                binding.registerTextviewError.visibility = View.VISIBLE
            } else {
                binding.registerTextviewError.text = ""
                binding.registerTextviewError.visibility = View.GONE
            }
        }
    }

    private fun attemptRegisterUser(){
        val email = binding.registerTextinputlayoutEmail.editText?.text.toString().trim()
        val password = binding.registerTextinputlayoutPassword.editText?.text.toString().trim()
        val passwordRepeat = binding.registerTextinputlayoutPasswordRepeat.editText?.text.toString().trim()
        if(password == passwordRepeat){
            viewModel.registerUserWithPasswordAndEmail(email, passwordRepeat, ::navigateToHome)
        } else {
            viewModel.errorMessage.postValue("Passordene må være like")
        }
    }

    private fun navigateToHome(authResult: AuthResult) {
        // Since registering users may take some time, pass
        // the newly registered user to the next view.
        val bundle = Bundle()
        bundle.putSerializable("NEW_REGISTERED_USER", authResult.success as User)
        findNavController().navigate(R.id.action_registerFragment_to_homeFragment, bundle)
    }

    companion object {
        @JvmStatic
        fun newInstance() = RegisterFragment()
    }
}