package com.app.moodtrack_android.ui.response_overview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import androidx.recyclerview.widget.RecyclerView
import com.app.moodtrack_android.R
import com.app.moodtrack_android.util.AppDateUtils
import java.io.IOException
import java.time.ZoneId
import java.util.*

class DateListAdapter(val context: Context): RecyclerView.Adapter<DateListAdapter.ViewHolder>() {

    var data: List<CalendarNotificationResponse> = emptyList()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    class ViewHolder(view: View, val context: Context) : RecyclerView.ViewHolder(view) {
        private val timeView: TextView = view.findViewById(R.id.calendar_list_element_time)
        private val dateView: TextView = view.findViewById(R.id.calendar_list_element_date)
        private val img: ImageView = view.findViewById(R.id.calendar_list_icon)
        private val qtw: TextView = view.findViewById(R.id.calendar_list_question_text)

        fun setTime(text: String){
            timeView.text = text
        }

        fun setDate(text: String){
            dateView.text = text
        }

        fun setIcon(bitmap: Bitmap?){
            if(bitmap != null)
                img.setImageBitmap(bitmap)
            else
                img.setImageBitmap(AppCompatResources.getDrawable(context, R.drawable.ic_baseline_help_24)?.toBitmap())
        }

        fun setQuestionText(text: String){
            qtw.text = text
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.calendar_list_element, parent, false)

        return ViewHolder(view, context)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]
        holder.setDate(formatDate(item.timestamp))
        holder.setTime(formatTime(item.timestamp))
        holder.setIcon(item.choiceIcon)
        holder.setQuestionText(item.question)
    }

    override fun getItemCount() = data.size

    private fun formatTime(d: Date): String {
        val ldt = AppDateUtils.convertDateToLocalDateTime(d, ZoneId.systemDefault())
        return "${ldt.hour.toString().padStart(2,'0')}:${ldt.minute.toString().padStart(2,'0')}"
    }

    private fun formatDate(d: Date): String {
        val ldt = AppDateUtils.convertDateToLocalDateTime(d, ZoneId.systemDefault())
        return "${ldt.dayOfMonth} ${ldt.month}"
    }
}