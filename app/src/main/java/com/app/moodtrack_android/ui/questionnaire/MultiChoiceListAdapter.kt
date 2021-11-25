package com.app.moodtrack_android.ui.questionnaire

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.moodtrack_android.R
import com.app.moodtrack_android.model.questionnaire.MultiChoiceQuestionItem

class MultiChoiceListAdapter(private val onItemTapped: (Int) -> Unit) :
    RecyclerView.Adapter<MultiChoiceListAdapter.ViewHolder>() {
    val TAG = "MultiChoiceListAdapter"
    private var dataSet: List<MultiChoiceQuestionItem> = emptyList()
    private var selectedIndex = -1

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val selected: ImageView = view.findViewById(R.id.questionnaire_multi_choice_question_not_selected)
        private val notSelected: ImageView = view.findViewById(R.id.questionnaire_multi_choice_question_selected)
        private val questionTextView: TextView = view.findViewById(R.id.questionnaire_multi_choice_question_textview)
        private val framelayout: FrameLayout = view.findViewById(R.id.questionnaire_multi_choice_framelayout)
        fun bind(position: Int){
            val data = dataSet[position]
            Log.d(TAG, "Display: " + data.display + ", Value: " + data.value + ", Selected: " + selectedIndex)
            if(position == selectedIndex){
                selected.visibility = View.VISIBLE
                notSelected.visibility = View.GONE
            } else {
                selected.visibility = View.GONE
                notSelected.visibility = View.VISIBLE
            }
            framelayout.setOnClickListener { onItemTapped(position) }
            if(data.display != null && data.display.isNotEmpty()){
                questionTextView.text = data.display
            } else questionTextView.text = data.value
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.questionaire_multichoice_list_item, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        viewHolder.bind(position)
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size

    fun setData(choices: List<MultiChoiceQuestionItem>){
        dataSet = choices
        notifyDataSetChanged()
    }

    fun setSelected(selected: Int){
        selectedIndex = selected
        notifyDataSetChanged()
    }

    fun clearSelected(){
        selectedIndex = -1
        notifyDataSetChanged()
    }
}