package com.app.moodtrack_android.ui.uploads

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.moodtrack_android.databinding.UserUploadsFragmentBinding
import dagger.hilt.android.AndroidEntryPoint
import java.io.FileDescriptor

@AndroidEntryPoint
class UserUploadsFragment : Fragment() {
    val _tag = "UserUploadsFragment"

    val viewModel: UserUploadsViewModel by viewModels()
    private var _binding: UserUploadsFragmentBinding? = null
    private val binding get() = _binding!!

    lateinit var linearLayoutManager: LinearLayoutManager
    lateinit var adapter: UserUploadsFilesAdapter
    lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = UserUploadsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = binding.userUploadFilesRecyclerview
        linearLayoutManager = LinearLayoutManager(requireContext())
        adapter = UserUploadsFilesAdapter(
            { pos -> viewModel.deleteDocument(pos) },
            { pos -> viewModel.downloadDocument(pos, ::showToast, ::showToast) },
        )

        recyclerView.layoutManager = linearLayoutManager
        recyclerView.adapter = adapter

        binding.userUploadUploadFilesButton.setOnClickListener {
            openActivityForResult()
        }

        binding.userUploadRemoveCurrentFileButton.setOnClickListener {
            viewModel.removeSelectedUri()
        }

        binding.userUploadConfirmUploadButton.setOnClickListener {
            viewModel.uploadDocument()
        }

        viewModel.selectedUri.observe(viewLifecycleOwner){ uri ->
            var displayText = "Ingen valget filer"
            if(uri != null) {
                val fSize = viewModel.getUriFileSize(uri)
                val fName = viewModel.getUriFilename(uri)
                displayText = "$fName(${viewModel.createFileSizeString(fSize.toDouble())})"
                binding.userUploadRemoveCurrentFileButton.visibility = View.VISIBLE
                binding.userUploadUploadFilesButton.visibility = View.GONE
                binding.userUploadConfirmUploadButton.visibility = View.VISIBLE
            } else {
                binding.userUploadRemoveCurrentFileButton.visibility = View.GONE
                binding.userUploadUploadFilesButton.visibility = View.VISIBLE
                binding.userUploadConfirmUploadButton.visibility = View.GONE
            }
            binding.userUploadFileName.text = displayText
        }

        viewModel.documents.observe(viewLifecycleOwner){ docs ->
            adapter.setData(docs)
            if(docs.isNullOrEmpty())
                binding.userUploadFilesEmpty.visibility = View.VISIBLE
            else binding.userUploadFilesEmpty.visibility = View.GONE
        }
    }

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val resultData: Intent? = result.data
            resultData?.data.also { uri ->
                Log.d(_tag, "resultLauncher received uri: ${uri.toString()}")
                uri?.let { mUri ->
                    viewModel.setUri(mUri)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun openActivityForResult(pickerInitialUri: Uri? = null) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"

            // Optionally, specify a URI for the file that should appear in the
            // system file picker when it loads.
            if(pickerInitialUri != null)
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
        }
        resultLauncher.launch(intent)
    }

    private fun showToast(text: String){
        activity?.runOnUiThread {
            Toast
                .makeText(requireContext(), text, Toast.LENGTH_SHORT)
                .show()
        }
    }
}