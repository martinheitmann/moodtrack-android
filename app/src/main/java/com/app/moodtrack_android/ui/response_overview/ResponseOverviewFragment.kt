package com.app.moodtrack_android.ui.response_overview

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.moodtrack_android.databinding.ResponseOverviewFragmentBinding
import dagger.hilt.android.AndroidEntryPoint
import java.time.format.TextStyle
import java.util.*

@AndroidEntryPoint
class ResponseOverviewFragment : Fragment() {

    val TAG = "ResponseOverviewFragment"

    val viewModel: ResponseOverviewViewModel by viewModels()
    private var _binding: ResponseOverviewFragmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var calendarGridAdapter: CalendarGridAdapter
    private lateinit var calendarGridLayoutManager: GridLayoutManager
    private lateinit var calendarGridRecyclerView: RecyclerView

    private lateinit var calendarListAdapter: DateListAdapter
    private lateinit var calendarListLayoutManager: LinearLayoutManager
    private lateinit var calendarListRecyclerView: RecyclerView


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ResponseOverviewFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        calendarGridAdapter = CalendarGridAdapter(viewModel::setSelectedDayIndex)
        calendarGridLayoutManager = GridLayoutManager(context, 7)
        calendarGridRecyclerView = binding.responseOverviewCalendar
        calendarGridRecyclerView.adapter = calendarGridAdapter
        calendarGridRecyclerView.layoutManager = calendarGridLayoutManager

        calendarListAdapter = DateListAdapter(requireContext())
        calendarListLayoutManager = LinearLayoutManager(context)
        calendarListRecyclerView = binding.responseOverviewEntryList
        calendarListRecyclerView.adapter = calendarListAdapter
        calendarListRecyclerView.layoutManager = calendarListLayoutManager

        viewModel.calendarCells.observe(viewLifecycleOwner) { cells ->
            Log.d(TAG, "cells size = " + cells.size.toString())
            calendarGridAdapter.data = cells
            calendarGridAdapter.selectedDayIndex = null
            calendarGridAdapter.currentDate = viewModel.currentDate
        }
        viewModel.selectedYearMonth.observe(viewLifecycleOwner) { yearMonth ->
            val year = yearMonth.year
            val month = yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
            binding.responseOverviewMonthYear.text = "$year $month"
        }
        viewModel.selectedCellIndex.observe(viewLifecycleOwner) { index ->
            calendarGridAdapter.selectedDayIndex = index
            calendarListAdapter.data = index?.let { viewModel.calendarCells.value?.get(it)?.responses }
                ?: emptyList()
            Log.d(tag, "currentCellNotificationResponses")
            if(index != null) Log.d(tag, "${viewModel.calendarCells.value?.get(index)?.responses?.size}")
        }
        binding.responseOverviewNextMonth.setOnClickListener {
            viewModel.incrementCurrentYearMonth()
        }
        binding.responseOverviewPrevMonth.setOnClickListener {
            viewModel.decrementCurrentYearMonth()
        }
        binding.responseOverviewButtonBack.setOnClickListener {
            findNavController().popBackStack();
        }
    }

}