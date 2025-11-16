package com.example.umelec

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.NumberPicker
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale

class CustomDatePickerDialog(
    context: Context,
    private val onDateSelected: (String) -> Unit // Callback function
) : BottomSheetDialog(context) {

    private val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_custom_date_picker)

        val monthPicker: NumberPicker = findViewById<NumberPicker>(R.id.monthPicker)!!
        val dayPicker: NumberPicker = findViewById<NumberPicker>(R.id.dayPicker)!!
        val yearPicker: NumberPicker = findViewById<NumberPicker>(R.id.yearPicker)!!
        val btnApply: Button = findViewById<Button>(R.id.btnApplyDate)!!

        val currentCalendar = Calendar.getInstance()
        val currentYear = currentCalendar.get(Calendar.YEAR)
        val currentMonth = currentCalendar.get(Calendar.MONTH)
        val currentDay = currentCalendar.get(Calendar.DAY_OF_MONTH)

        // --- Month Picker Setup (0-11)
        monthPicker.apply {
            minValue = 0
            maxValue = months.size - 1
            displayedValues = months
            value = currentMonth // Set to current month
        }

        // --- Day Picker Setup (1-31)
        dayPicker.apply {
            minValue = 1
            maxValue = 31
            value = currentDay // Set to current day
            wrapSelectorWheel = true
        }

        // --- Year Picker Setup (Current Year to Current Year + 10)
        yearPicker.apply {
            minValue = currentYear
            maxValue = currentYear + 10
            value = currentYear // Set to current year
        }

        // --- Apply Button Listener
        btnApply.setOnClickListener {
            val selectedMonthIndex = monthPicker.value
            val selectedDay = dayPicker.value
            val selectedYear = yearPicker.value

            // 1. Create a Calendar object for the selected date
            val selectedCalendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, selectedYear)
                set(Calendar.MONTH, selectedMonthIndex)
                set(Calendar.DAY_OF_MONTH, selectedDay)
                set(Calendar.HOUR_OF_DAY, 0) // Clear time components for pure date comparison
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // 2. Clear time components for the current date for pure date comparison
            val todayCalendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // 3. Validation Check: Must be today or a future date
            // Note: We use isBefore/isAfter for clear comparison logic, but directly comparing timestamps works too.
            if (selectedCalendar.before(todayCalendar)) {
                Toast.makeText(context, "Please select today or a future date.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener // Stop processing, do not apply or dismiss
            }

            // 4. If valid, format and apply the date
            val selectedDate = dateFormatter.format(selectedCalendar.time)
            onDateSelected(selectedDate)
            dismiss()
        }
    }
}