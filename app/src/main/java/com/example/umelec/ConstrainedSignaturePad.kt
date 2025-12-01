package com.example.umelec

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.Toast
import com.github.gcacace.signaturepad.views.SignaturePad

class ConstrainedSignaturePad @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    // The defStyle argument is correctly used here but MUST NOT be passed to the super constructor.
    // We call the two-argument constructor exposed by the SignaturePad class.
    defStyle: Int = 0
// FIX: Removed 'defStyle' from the superclass constructor call
) : SignaturePad(context, attrs) {

    // Define the boundary margin (5% on all sides)
    private val marginPercentage = 0.05f

    // Internal state to track if the boundary has been crossed
    private var isBoundaryCrossed = false

    // A listener/callback to notify the Castvote3 activity when the state changes
    var onBoundaryCrossedListener: ((Boolean) -> Unit)? = null

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Calculate the constraint boundaries based on the current size of the pad
        val padWidth = width.toFloat()
        val padHeight = height.toFloat()

        val minX = padWidth * marginPercentage
        val maxX = padWidth * (1 - marginPercentage)
        val minY = padHeight * marginPercentage
        val maxY = padHeight * (1 - marginPercentage)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // 1. When starting a new signature, reset the state
                isBoundaryCrossed = false
                onBoundaryCrossedListener?.invoke(isBoundaryCrossed)
            }

            MotionEvent.ACTION_MOVE -> {
                val x = event.x
                val y = event.y

                // 2. Check if the current point is outside the constraint
                if (x < minX || x > maxX || y < minY || y > maxY) {
                    if (!isBoundaryCrossed) {
                        // Mark as crossed only once
                        isBoundaryCrossed = true

                        // Give immediate feedback to the user
                        Toast.makeText(context, "Please keep your signature within the dashed area.", Toast.LENGTH_SHORT).show()

                        // Notify the activity to disable the Submit button and show the error text
                        onBoundaryCrossedListener?.invoke(isBoundaryCrossed)
                    }

                    // CRUCIAL: Return true to consume the event, preventing the superclass
                    // from drawing the line segment that crossed the boundary.
                    return true
                }
            }
        }

        // 3. For all other actions (like ACTION_UP or valid ACTION_MOVE),
        // let the original SignaturePad class handle the drawing.
        return super.onTouchEvent(event)
    }

    /**
     * Override clear() to ensure we reset the internal state and notify the activity.
     */
    override fun clear() {
        super.clear()
        isBoundaryCrossed = false
        onBoundaryCrossedListener?.invoke(isBoundaryCrossed)
    }
}