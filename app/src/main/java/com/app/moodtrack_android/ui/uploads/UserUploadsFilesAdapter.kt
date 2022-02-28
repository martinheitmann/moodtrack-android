package com.app.moodtrack_android.ui.uploads

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.moodtrack_android.R
import com.app.moodtrack_android.model.DocumentFile

class UserUploadsFilesAdapter(
    private val onDeleteTapped: (Int) -> Unit,
    private val onDownloadTapped: (Int) -> Unit
) : RecyclerView.Adapter<UserUploadsFilesAdapter.ViewHolder>() {

    val TAG = "UploadListAdapter"
    private var dataset: List<DocumentFile> = emptyList()

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val deleteButton: ImageButton = view.findViewById(R.id.file_list_item_button_delete)
        private val downloadButton: ImageButton =
            view.findViewById(R.id.file_list_item_button_download)
        private val filename: TextView = view.findViewById(R.id.file_list_item_file_name)
        fun bind(position: Int) {
            val item = dataset[position]
            filename.text = item.filename
            downloadButton.setOnClickListener { onDownloadTapped(position) }
            deleteButton.setOnClickListener { onDeleteTapped(position) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.file_list_item, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = dataset.size

    @SuppressLint("NotifyDataSetChanged")
    fun setData(data: List<DocumentFile>){
        dataset = data
        notifyDataSetChanged()
    }
}