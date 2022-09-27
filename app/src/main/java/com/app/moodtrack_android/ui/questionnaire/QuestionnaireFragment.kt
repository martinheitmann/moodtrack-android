package com.app.moodtrack_android.ui.questionnaire

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.annotation.RequiresApi
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.moodtrack_android.R
import com.app.moodtrack_android.databinding.FragmentQuestionnaireBinding
import com.app.moodtrack_android.model.questionnaire.QuestionProperties
import com.app.moodtrack_android.model.questionnaire.QuestionnaireFreeTextQuestion
import com.app.moodtrack_android.model.questionnaire.QuestionnaireMultiChoiceQuestion
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class QuestionnaireFragment : Fragment() {

    private val TAG = "QuestionnaireFragment"
    private val viewModel: QuestionnaireViewModel by viewModels()
    private var _binding: FragmentQuestionnaireBinding? = null

    private val binding get() = _binding!!

    private lateinit var choiceRecyclerView: RecyclerView
    private lateinit var choiceAdapter: MultiChoiceListAdapter
    private lateinit var layoutManager: LinearLayoutManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuestionnaireBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // May use to later track which node triggered this questionnaire.
        val receivedNodeId = arguments?.get(getString(R.string.notification_node_id)) as String
        val receivedQuestionnaireId =
            arguments?.get(getString(R.string.in_app_questionnaire_id)) as String
        // Message ID allows us to trace the "path" which users have taken.
        val receivedMessageId =
            arguments?.get(getString(R.string.notification_message_id)) as String?
        Log.d(TAG, "Received message id: $receivedMessageId")
        // If true, don't send the response.
        val isDryRun = arguments?.get(getString(R.string.is_dry_run)) as Boolean
        viewModel.getQuestionnaire(receivedQuestionnaireId)
        viewModel.isDryRun = isDryRun
        viewModel.messageId = receivedMessageId

        // Register and compose view components
        choiceAdapter = MultiChoiceListAdapter(::setSelectedListItem)
        choiceRecyclerView = binding.questionnaireMultichoiceRecyclerview
        layoutManager = LinearLayoutManager(context)
        choiceRecyclerView.adapter = choiceAdapter
        choiceRecyclerView.layoutManager = layoutManager

        // This observer is triggered whenever the user navigates to a new question.
        viewModel.currentQuestion.observe(viewLifecycleOwner) { currentQuestion ->
            setTopRowButtons(currentQuestion.index, viewModel.questionnaireList.size)
            // Question count tracker
            binding.questionnaireTextviewCount.text =
                "${currentQuestion.index + 1} av ${viewModel.questionnaireList.size}"
            // Decide whether or not this question is the last in the questionnaire
            val isFinal =
                viewModel.isFinalQuestion(currentQuestion.index, viewModel.questionnaireList.size)
            // Enable/disable UI components depending on the question type
            if (currentQuestion is QuestionnaireMultiChoiceQuestion) {
                binding.questionnaireMultiquestionContainer.visibility = View.VISIBLE
                binding.questionnaireFreetextContainer.visibility = View.GONE
                binding.qustionnaireQuestionTextview.text = currentQuestion.question
                checkProperties(currentQuestion.additionalProperties)

                // Set the data shown in the multi choice recyclerview
                choiceAdapter.setData(currentQuestion.choices)
                val currentChoice = viewModel.registeredChoices[currentQuestion.index]
                if (currentChoice != null) {
                    // Show the "next" button only if an answer has been selected
                    choiceAdapter.setSelected(currentChoice as Int)
                    setNextButton(true, isFinal)
                } else {
                    setNextButton(false, isFinal)
                }
            }
            if (currentQuestion is QuestionnaireFreeTextQuestion) {
                binding.questionnaireMultiquestionContainer.visibility = View.GONE
                binding.questionnaireFreetextContainer.visibility = View.VISIBLE
                binding.qustionnaireQuestionTextview.text = currentQuestion.question
                checkProperties(currentQuestion.additionalProperties)

                val currentInput = viewModel.registeredChoices[currentQuestion.index]
                if (currentInput != null) {
                    binding.questionnaireFreeetextTextinput.setText(currentInput as String)
                } else {
                    binding.questionnaireFreeetextTextinput.setText("")
                }
            }
        }
        // Observer for submission loading status
        viewModel.isSubmitting.observe(viewLifecycleOwner) { isSubmitting ->
            if (isSubmitting) {
                binding.questionnaireButtonSubmit.visibility = View.GONE
                binding.questionnaireProgressbarSubmitting.visibility = View.VISIBLE
            } else {
                binding.questionnaireButtonSubmit.visibility = View.VISIBLE
                binding.questionnaireProgressbarSubmitting.visibility = View.GONE
            }
        }
        // Listener for navigating to the next question.
        binding.questionnaireButtonNext.setOnClickListener {
            choiceAdapter.clearSelected()
            viewModel.setNewCurrentInputText(binding.questionnaireFreeetextTextinput.text.toString())
            viewModel.getNextQuestion()
        }
        // Listener for navigating to the previous question.
        binding.questionnaireButtonPrev.setOnClickListener {
            choiceAdapter.clearSelected()
            viewModel.setNewCurrentInputText(binding.questionnaireFreeetextTextinput.text.toString())
            viewModel.getPreviousQuestion()
        }
        // Listener for answer submission button in the top row.
        binding.questionnaireButtonSubmit.setOnClickListener {
            viewModel.submitQuestionnaireResponse(::popFragment)
        }
        // Listener for move backwards button in the top row.
        binding.questionnaireButtonCancel.setOnClickListener {
            findNavController().popBackStack()
        }
        // Listener for "next" button in free text view.
        binding.questionnaireFreetextButtonNext.setOnClickListener {
            choiceAdapter.clearSelected()
            viewModel.setNewCurrentInputText(binding.questionnaireFreeetextTextinput.text.toString())
            viewModel.getNextQuestion()
        }
        // Listener for "next" button in multi choice view.
        binding.questionnaireMultiquestionButtonNext.setOnClickListener {
            choiceAdapter.clearSelected()
            viewModel.setNewCurrentInputText(binding.questionnaireFreeetextTextinput.text.toString())
            viewModel.getNextQuestion()
        }
        // Listener for answer submission button in multi choice view.
        binding.questionnaireMultiquestionButtonFinish.setOnClickListener {
            viewModel.submitQuestionnaireResponse(::popFragment)
        }
        // Listener for answer submission button in free text view.
        binding.questionnaireFreetextButtonFinish.setOnClickListener {
            viewModel.submitQuestionnaireResponse(::popFragment)
        }
        // Listener for deciding if the free text field is empty or not.
        // "Next" should only be shown if not empty.
        binding.questionnaireFreeetextTextinput.doOnTextChanged { text, _, _, _ ->
            val isFinal = viewModel.isFinalQuestion(
                viewModel.currentQuestion.value?.index ?: -1,
                viewModel.questionnaireList.size)
            if (text != null) {
                viewModel.setNewCurrentInputText(text.toString())
                if(text.isEmpty()){
                    setNextButton(false, isFinal)
                } else {
                    setNextButton(true, isFinal)
                }
            } else {
                setNextButton(false, isFinal)
            }
        }
    }

    /**
     * Sets the selected choice for the current multi-choice
     * question.
     */
    fun setSelectedListItem(value: Int) {
        val currentQuestionIndex = viewModel.currentQuestion.value?.index
        val isFinal = viewModel.isFinalQuestion(
            viewModel.currentQuestion.value?.index ?: -1,
            viewModel.questionnaireList.size
        )
        setNextButton(true, isFinal)
        currentQuestionIndex?.let { _ ->
            viewModel.currentChoiceIndex = value
            choiceAdapter.setSelected(viewModel.currentChoiceIndex)
        }
    }

    /***
     * Sets the visibility for the "next" navigation buttons
     * for the respective question views.
     */
    private fun setNextButton(show: Boolean, isLast: Boolean) {
        // If false, show neither finish nor next.
        // Should be the case for no input.
        if (show) {
            // If last question, show submission button only and not next.
            if (isLast) {
                binding.questionnaireMultiquestionButtonNext.visibility = View.GONE
                binding.questionnaireFreetextButtonNext.visibility = View.GONE
                binding.questionnaireFreetextButtonFinish.visibility = View.VISIBLE
                binding.questionnaireMultiquestionButtonFinish.visibility = View.VISIBLE
            } else {
                binding.questionnaireMultiquestionButtonNext.visibility = View.VISIBLE
                binding.questionnaireFreetextButtonNext.visibility = View.VISIBLE
                binding.questionnaireFreetextButtonFinish.visibility = View.GONE
                binding.questionnaireMultiquestionButtonFinish.visibility = View.GONE
            }
        } else {
            binding.questionnaireMultiquestionButtonNext.visibility = View.GONE
            binding.questionnaireFreetextButtonNext.visibility = View.GONE
            binding.questionnaireFreetextButtonFinish.visibility = View.GONE
            binding.questionnaireMultiquestionButtonFinish.visibility = View.GONE
        }
    }

    /**
     * Sets the prev/next/submit button visibility for the view
     * depending on the index.
     */
    private fun setTopRowButtons(index: Int, listSize: Int) {
        when (index) {
            0 -> {
                if (listSize == 1) {
                    binding.questionnaireButtonPrev.visibility = View.GONE
                    binding.questionnaireButtonSubmit.visibility = View.VISIBLE

                    binding.questionnaireButtonCancel.visibility = View.VISIBLE
                    binding.questionnaireButtonNext.visibility = View.GONE
                } else {
                    binding.questionnaireButtonPrev.visibility = View.GONE
                    binding.questionnaireButtonSubmit.visibility = View.GONE

                    binding.questionnaireButtonCancel.visibility = View.VISIBLE
                    binding.questionnaireButtonNext.visibility = View.VISIBLE
                }
            }
            listSize - 1 -> {
                binding.questionnaireButtonNext.visibility = View.GONE
                binding.questionnaireButtonCancel.visibility = View.GONE

                binding.questionnaireButtonPrev.visibility = View.VISIBLE
                binding.questionnaireButtonSubmit.visibility = View.VISIBLE
            }
            else -> {
                binding.questionnaireButtonNext.visibility = View.VISIBLE
                binding.questionnaireButtonPrev.visibility = View.VISIBLE
                binding.questionnaireButtonCancel.visibility = View.GONE
                binding.questionnaireButtonSubmit.visibility = View.GONE
            }
        }
    }

    /**
     * Pops the current fragment.
     */
    private fun popFragment() {
        // We have to call the NavController on the UI thread since
        // the caller is running in a coroutine.
        activity?.runOnUiThread {
            findNavController().popBackStack()
        }
    }

    private fun checkProperties(props: List<QuestionProperties>){
        props.forEach { prop ->
            when(prop.key){
                "android.widget.breathingExercise" -> {
                    if(prop.value == "enabled"){
                        loadBreathingExerciseAsset()
                    }
                }
                else -> {
                    binding.qustionnaireQuestionWebview.visibility = View.GONE
                }
            }
        }
    }

    private fun loadBreathingExerciseAsset(){
        binding.qustionnaireQuestionWebview.settings.javaScriptEnabled = true
        binding.qustionnaireQuestionWebview.settings.allowFileAccess = true
        binding.qustionnaireQuestionWebview.setInitialScale(1)
        binding.qustionnaireQuestionWebview.settings.loadWithOverviewMode = true
        binding.qustionnaireQuestionWebview.settings.useWideViewPort = true
        binding.qustionnaireQuestionWebview.loadUrl("file:///android_asset/html/pulsing_animation.html")
        binding.qustionnaireQuestionWebview.visibility = View.VISIBLE
    }
}