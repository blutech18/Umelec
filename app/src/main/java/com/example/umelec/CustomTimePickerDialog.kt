package com.example.umelec

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.NumberPicker
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.DecimalFormat

class CustomTimePickerDialog(
    context: Context,
    private val onTimeSelected: (String) -> Unit // Callback function
) : BottomSheetDialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_custom_time_picker)

        // FIX: Use '!!' to assert non-nullability, resolving the 'NumberPicker?' to 'NumberPicker' mismatch.
        val hrsPicker: NumberPicker = findViewById<NumberPicker>(R.id.hrsPicker)!!
        val minPicker: NumberPicker = findViewById<NumberPicker>(R.id.minPicker)!!
        val amPmPicker: NumberPicker = findViewById<NumberPicker>(R.id.amPmPicker)!!
        val btnApply: Button = findViewById<Button>(R.id.btnApplyTime)!!

        val amPm = arrayOf("AM", "PM")
        val df = DecimalFormat("00") // Helper for two digits

        // --- Hour Picker Setup (1-12)
        hrsPicker.apply {
            minValue = 1
            maxValue = 12
            wrapSelectorWheel = true
            setFormatter { df.format(it) }
        }

        // --- Minute Picker Setup (0-59)
        minPicker.apply {
            minValue = 0
            maxValue = 59
            wrapSelectorWheel = true
            setFormatter { df.format(it) }
        }

        // --- AM/PM Picker Setup
        amPmPicker.apply {
            displayedValues = amPm
            minValue = 0
            maxValue = 1
            wrapSelectorWheel = false
        }

        // --- Apply Button Listener
        btnApply.setOnClickListener {
            val hour = hrsPicker.value
            val minute = minPicker.value
            val amPmValue = amPm[amPmPicker.value]

            val formattedHour = df.format(hour)
            val formattedMinute = df.format(minute)

            val selectedTime = "$formattedHour:$formattedMinute $amPmValue"
            onTimeSelected(selectedTime)
            dismiss()
        }
    }
}