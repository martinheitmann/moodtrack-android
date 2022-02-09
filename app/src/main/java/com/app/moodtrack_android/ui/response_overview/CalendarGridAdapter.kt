package com.app.moodtrack_android.ui.response_overview

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.app.moodtrack_android.R
import java.time.LocalDateTime

class CalendarGridAdapter(
    private val onCellClicked: (Int) -> Unit
) : RecyclerView.Adapter<CalendarGridAdapter.ViewHolder>() {
    val tag = "CalendarGridAdapter"

    var data: List<CalendarCell> = mutableListOf()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    var currentDate: LocalDateTime? = null
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    var selectedDayIndex: Int? = null
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            if (value != null && data.elementAtOrNull(value) != null) {
                field = value
                notifyDataSetChanged()
            }
        }

    private fun isCurrentDay(cell: CalendarCell): Boolean {
        val mCurrentDate = currentDate
        if (mCurrentDate != null && cell.dayOfMonth == mCurrentDate.dayOfMonth
            && cell.yearMonth.monthValue == mCurrentDate.monthValue
            && cell.yearMonth.year == mCurrentDate.year
        ) {
            return true
        }
        return false
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val dateTextView: TextView = view.findViewById(R.id.calendar_grid_date)
        private val cardView: CardView = view.findViewById(R.id.calendar_grid_cardview)
        private val countTextView: TextView = view.findViewById(R.id.calendar_grid_count)
        private val iconImageView: ImageView = view.findViewById(R.id.calendar_grid_icon)

        fun setCellClickedListener(position: Int, onCellClicked: (Int) -> Unit) {
            cardView.setOnClickListener { onCellClicked(position) }
        }

        fun setDayOfMonth(dayOfMonth: Int) {
            dateTextView.text = dayOfMonth.toString()
        }

        fun setAsCurrentDay() {
            cardView.setCardBackgroundColor(Color.parseColor("#4169e1"))
            dateTextView.setTextColor(Color.WHITE)
        }

        fun setSelected() {
            cardView.setCardBackgroundColor(Color.parseColor("#87ceeb"))
            dateTextView.setTextColor(Color.WHITE)
        }

        fun setIsDifferentMonth() {
            cardView.setCardBackgroundColor(Color.parseColor("#708090"))
            dateTextView.setTextColor(Color.WHITE)
        }

        fun setResponseCount(count: Int) {
            if (count == 0) countTextView.text = ""
            else countTextView.text = count.toString()
        }

        fun setIcon(bitmap: Bitmap?) {
            if (bitmap != null) {
                iconImageView.setImageBitmap(bitmap)
                iconImageView.visibility = View.VISIBLE
            } else {
                Log.d("CalendarGridAdapter", "icon bitmap was null")
                iconImageView.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.calendar_grid_element, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.setIsRecyclable(false)
        val cell = data[position]
        holder.setDayOfMonth(cell.dayOfMonth)
        if (isCurrentDay(cell)) holder.setAsCurrentDay()
        else if (isCurrentDay(cell) && position == selectedDayIndex) holder.setAsCurrentDay()
        else if (position == selectedDayIndex) holder.setSelected()
        if (!cell.isCurrentMonth) holder.setIsDifferentMonth()
        holder.setResponseCount(getResponseCount(cell))
        if (cell.responses.isNotEmpty()){
            val iconCell = getHighestResponseBitmap(cell)
            holder.setIcon(iconCell)
        }
        else holder.setIcon(null)
        holder.setCellClickedListener(position, onCellClicked)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    private fun getHighestResponseBitmap(cell: CalendarCell): Bitmap? {
        val r = chooseHighestResponse(cell)
        if (r?.choiceIcon != null) Log.d(
            tag,
            "getHighestResponseBitmap: icon returned for day ${cell.dayOfMonth}."
        )
        return r?.choiceIcon
    }

    private fun getResponseCount(cell: CalendarCell): Int {
        return cell.responses.size
    }

    private fun chooseHighestResponse(cell: CalendarCell): CalendarNotificationResponse? {
        val responses = cell.responses
        return responses
            .filter { r -> r.choiceIcon != null }
            .filter { r -> r.showIcon }
            .filter { r -> r.choiceValue.toIntOrNull() != null }
            .maxByOrNull { r -> r.choiceValue.toInt() }
    }
}