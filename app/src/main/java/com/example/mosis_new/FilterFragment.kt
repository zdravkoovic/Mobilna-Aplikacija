package com.example.mosis_new

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CalendarView
import android.widget.Filter
import android.widget.TextView
import android.widget.Toast
import androidx.core.util.Pair
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.mosis_new.databinding.FragmentFilterBinding
import com.example.mosis_new.databinding.FragmentMapBinding
import com.example.mosis_new.model.FilterObject
import com.google.android.material.datepicker.MaterialDatePicker
import java.util.Date

class FilterFragment : Fragment() {

    private var _binding: FragmentFilterBinding? = null
    private val binding get() = _binding!!
    private val filterObject: FilterObject by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFilterBinding.inflate(inflater, container, false)

        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val autor : TextView = requireView().findViewById<TextView>(R.id.filter_autor)
        val desc : TextView = requireView().findViewById<TextView>(R.id.filter_atribut)
        val radius : TextView = requireView().findViewById<TextView>(R.id.filter_radius)
        val kalendar: CalendarView = requireView().findViewById<CalendarView>(R.id.filter_kalendar)
        val btn: Button = requireView().findViewById<Button>(R.id.filter_button)
        var startDate: String = ""
        kalendar.setOnDateChangeListener { view, year, month, dayOfMonth ->
            val date = (dayOfMonth.toString() + "-"
                    + (month + 1) + "-" + year)
            startDate = date
        }

        btn.setOnClickListener{
            filterObject.setFilterObject(autor.text.toString(), desc.text.toString(), radius.text.toString(), startDate)
            findNavController().navigate(R.id.action_FilterFragment_To_MapFragment)
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val item = menu.findItem(R.id.action_filters)
        item.isVisible = false
    }
}