package com.app.moodtrack_android.ui.home

import android.graphics.Color
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.app.moodtrack_android.R
import com.app.moodtrack_android.databinding.FragmentHomeBinding
import com.app.moodtrack_android.model.Status.*
import com.app.moodtrack_android.model.user.User
import com.google.firebase.iid.FirebaseInstanceId
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class HomeFragment : Fragment() {
    private val TAG = "HomeFragment"
    private val viewModel: HomeViewModel by viewModels()
    private var _binding: FragmentHomeBinding? = null

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
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "getInstanceId failed", task.exception)
                }
                val token = task.result?.token
                Log.d(TAG, "FCM token: $token")
            }

        val html = Html.fromHtml(getString(R.string.app_text_info))
        binding.ftv.text = html
        binding.ftv.textColor = Color.parseColor("#808080")
        binding.ftv.setTextSize(48f)

        binding.homeSettingsButton.setOnClickListener {
            val bundle = Bundle()
            findNavController().navigate(R.id.action_homeFragment_to_settingsFragment2, bundle)
        }

        binding.homeImagebuttonResponseCalendar.setOnClickListener {
            val bundle = Bundle()
            findNavController().navigate(R.id.action_homeFragment_to_responseOverviewFragment, bundle)
        }

        binding.homeImagebuttonUploadView.setOnClickListener {
            val bundle = Bundle()
            findNavController().navigate(R.id.action_homeFragment_to_userUploadsFragment, bundle)
        }

        binding.homeButtonErrorRetry.setOnClickListener {
            viewModel.fetchUser();
        }

        viewModel.user.observe(viewLifecycleOwner) { user ->
            when (user.status) {
                SUCCESS -> showSuccessUi()
                ERROR -> showErrorUi()
                LOADING -> showLoadingUi()
            }
        }

        viewModel.user.observe(viewLifecycleOwner) { user ->
            if (user.status == SUCCESS && user.data is User)
                viewModel.updateRegistrationToken(user.data)
        }
    }

    override fun onStart() {
        super.onStart()
        if(!viewModel.isLoggedIn()){
            findNavController().navigate(R.id.action_global_loginFragment)
        }
    }

    fun showSuccessUi(){
        binding.homeLinearlayoutInformation.visibility = View.VISIBLE
        binding.homeProgressBar.visibility = View.GONE
        binding.homeLinearlayoutError.visibility = View.GONE
    }

    fun showLoadingUi(){
        binding.homeProgressBar.visibility = View.VISIBLE
        binding.homeLinearlayoutInformation.visibility = View.GONE
        binding.homeLinearlayoutError.visibility = View.GONE
    }

    fun showErrorUi(){
        binding.homeLinearlayoutInformation.visibility = View.GONE
        binding.homeProgressBar.visibility = View.GONE
        binding.homeLinearlayoutError.visibility = View.VISIBLE
    }
}