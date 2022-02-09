package com.app.moodtrack_android.ui.settings

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.app.moodtrack_android.R
import com.app.moodtrack_android.databinding.FragmentSettingsBinding
import com.app.moodtrack_android.model.Status
import com.app.moodtrack_android.model.user.User
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private val viewModel: SettingsViewModel by viewModels()
    private var _binding: FragmentSettingsBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        binding.settingsButtonBack.setOnClickListener { findNavController().popBackStack() }
        binding.settingsButtonSignout.setOnClickListener { signOut() }

        binding.settingsNotificationsActivate.setOnClickListener {
            binding.settingsProgressbarNotif.visibility = View.VISIBLE
            binding.settingsNotificationsActivate.visibility = View.GONE
            viewModel.updateNotificationPrefs()
        }
        binding.settingsNotificationsDeactivate.setOnClickListener {
            binding.settingsProgressbarNotif.visibility = View.VISIBLE
            binding.settingsNotificationsDeactivate.visibility = View.GONE
            viewModel.updateNotificationPrefs()
        }

        viewModel.user.observe(viewLifecycleOwner) { user ->
            when (user.status) {
                Status.SUCCESS -> setSuccessUi()
                Status.ERROR -> setErrorUi()
                Status.LOADING -> setLoadingUi()
            }
        }

        viewModel.user.observe(viewLifecycleOwner) { user ->
            val mUser = viewModel.user.value?.data as User?
            if (mUser != null && user.status == Status.SUCCESS) {
                mUser.notificationsEnabled?.let { value ->
                    if (value) setNotificationsEnabledUi()
                    else setNotificationsDisabledUi()
                }
            } else if (user.status == Status.LOADING) setNotificationsLoadingUi()
        }

        viewModel.signOutPending.observe(viewLifecycleOwner) { signOutPending ->
            if (signOutPending) {
                setSignOutPendingUi()
            } else {
                resetSignOutUi()
            }
        }
    }

    private fun setErrorUi(){
        binding.settingsLinearlayoutInfo.visibility = View.GONE
        binding.settingsProgressBar.visibility = View.GONE
        binding.settingsLinearlayoutError.visibility = View.VISIBLE
    }

    private fun setSuccessUi(){
        binding.settingsLinearlayoutInfo.visibility = View.VISIBLE
        binding.settingsProgressBar.visibility = View.GONE
        binding.settingsLinearlayoutError.visibility = View.GONE
        binding.settingsTextviewUserId.text = (viewModel.user.value?.data as User)._id
        binding.settingsTextviewUserEmail.text = (viewModel.user.value?.data as User).email
    }

    private fun setLoadingUi(){
        binding.settingsLinearlayoutInfo.visibility = View.GONE
        binding.settingsProgressBar.visibility = View.VISIBLE
        binding.settingsLinearlayoutError.visibility = View.GONE
    }

    private fun setNotificationsEnabledUi(){
        binding.settingsNotificationsActivate.visibility = View.GONE
        binding.settingsNotificationsDeactivate.visibility = View.VISIBLE
        binding.settingsProgressbarNotif.visibility = View.GONE
        binding.settingsTextviewNotificationsStatusText.text = "Varsler er aktivert"
        binding.settingsTextviewNotificationsStatusText.visibility = View.VISIBLE
    }

    private fun setNotificationsLoadingUi(){
        binding.settingsNotificationsActivate.visibility = View.GONE
        binding.settingsNotificationsDeactivate.visibility = View.GONE
        binding.settingsProgressbarNotif.visibility = View.VISIBLE
        binding.settingsTextviewNotificationsStatusText.visibility = View.GONE
    }

    private fun setNotificationsDisabledUi(){
        binding.settingsNotificationsActivate.visibility = View.VISIBLE
        binding.settingsNotificationsDeactivate.visibility = View.GONE
        binding.settingsProgressbarNotif.visibility = View.GONE
        binding.settingsTextviewNotificationsStatusText.text = "Varsler er ikke aktivert"
        binding.settingsTextviewNotificationsStatusText.visibility = View.VISIBLE
    }

    private fun setSignOutPendingUi(){
        binding.settingsProgressbarLogout.visibility = View.VISIBLE
        binding.settingsButtonSignout.visibility = View.GONE
    }

    private fun resetSignOutUi(){
        binding.settingsProgressbarLogout.visibility = View.GONE
        binding.settingsButtonSignout.visibility = View.VISIBLE
    }

    private fun signOut(){
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if(user != null){
            viewModel.removeUserFcmToken(::exitApp)
        }
    }

    private fun exitApp(){
        val auth = FirebaseAuth.getInstance()
        auth.signOut()
        findNavController().navigate(R.id.action_global_loginFragment)
    }

}