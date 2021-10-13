package me.dgmieth.kungfubbq

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import androidx.fragment.app.Fragment
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.applandeo.materialcalendarview.EventDay
import com.applandeo.materialcalendarview.listeners.OnDayClickListener
import kotlinx.android.synthetic.main.fragment_calendar.*
import kotlinx.android.synthetic.main.fragment_preorder.*
import java.text.SimpleDateFormat
import java.util.*

class CalendarFragment : Fragment(R.layout.fragment_calendar) {

    private val events : MutableList<EventDay> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        for(i in 1..4){
            val cal = Calendar.getInstance()
            cal.set(2021,9,(i*7))
            events.add(EventDay(cal,R.drawable.icon_calendar))
        }
        for(i in 1..4){
            val cal = Calendar.getInstance()
            cal.set(2021,10,(i*7))
            events.add(EventDay(cal,R.drawable.icon_calendar))
        }
        calendarCalendar.setEvents(events)
        val today = Date()
        val dateFormatter = SimpleDateFormat()
        dateFormatter.applyPattern("y")
        val year = (dateFormatter.format(today).toString()).toInt()
        dateFormatter.applyPattern("M")
        var month = (dateFormatter.format(today).toString()).toInt()-1
        dateFormatter.applyPattern("d")
        val date = (dateFormatter.format(today).toString()).toInt()
        val min = Calendar.getInstance()
        val max = Calendar.getInstance()
        Log.d("CalendarFragment", "Year $year, Month $month, Date $date")
        min.set(year,month,date)
        calendarCalendar.setDate(min)
        min.set(Calendar.DATE,1)
        Log.d("CalendarFragment", "$min")
        //setting min date in calendar
        calendarCalendar.setMinimumDate(min)
        month += 2
        if(month > 11){
            max.set(Calendar.YEAR, year + 1)
            max.set(Calendar.MONTH, 0)
        }else{
            max.set(Calendar.YEAR, year)
            max.set(Calendar.MONTH, month)
        }
        //setting max date
        max.set(Calendar.DATE, min.getActualMaximum(Calendar.DATE))
        Log.d("CalendarFragment", "Year $year, Month $month, Date $date")
        Log.d("CalendarFragment", "$max")
        calendarCalendar.setMaximumDate(max)
        //setting onClickListener
        calendarCalendar.setOnDayClickListener { eventDay ->
            println(eventDay.calendar.time.toString())
        }
        calendarPreOrder.setOnClickListener {
            val action = CalendarFragmentDirections.callPreOrder()
            findNavController().navigate(action)
        }
        calendarUpdateOrder.setOnClickListener {
            val action = CalendarFragmentDirections.callUpdateOrder()
            findNavController().navigate(action)
        }
        calendarPayOrder.setOnClickListener {
            val action = CalendarFragmentDirections.callPayOrder()
            findNavController().navigate(action)
        }
        calendarPaidOrder.setOnClickListener {
            val action = CalendarFragmentDirections.callPaidOrder()
            findNavController().navigate(action)
        }
    }
      override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
    }
}