package com.example.umelec

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.*

// Data structure for a single winner's result
data class OfficialResultCandidate(
    val name: String,
    val position: String,
    val votes: Int,
    val photoResId: Int // Resource ID for the drawable/image (e.g., R.drawable.profile_placeholder)
)

class OfficialResults : AppCompatActivity() {

    // ----------------------------------------------------------------------
    // --- BACKEND/DATABASE INTEGRATION POINTS ---
    // ----------------------------------------------------------------------

    /** * DB/BACKEND GUIDE:
     * 1. Fetch the timestamp (in milliseconds) of when the results were officially certified.
     * This time is used for the "As of [Date], [Time]" status text.
     */
    private val officialUpdateTimeMillis = System.currentTimeMillis()

    /** * DB/BACKEND GUIDE:
     * 2. This List should be populated by querying your database for the final, certified winners.
     * Each entry requires the candidate's name, their winning position, their final vote count,
     * and the Android resource ID for their profile photo.
     */
    private val winnerData = listOf(
        OfficialResultCandidate("Jane Doe", "Chairperson", 1580, R.drawable.ic_launcher_background),
        OfficialResultCandidate("Mark Tan", "Vice-Chairperson", 1710, R.drawable.ic_launcher_background),
        OfficialResultCandidate("David Chan", "Secretary", 1900, R.drawable.ic_launcher_background),
        OfficialResultCandidate("Maria Dela Cruz", "Treasurer", 2200, R.drawable.ic_launcher_background),
        OfficialResultCandidate("Kenji Sato", "Auditor", 1770, R.drawable.ic_launcher_background),
        // Additional candidate for better scroll testing
        OfficialResultCandidate("Sarah Lee", "Board Member 1", 1650, R.drawable.ic_launcher_background)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_official_results)

        // Setup UI elements
        setupHeaderBehavior()
        setupResultsCards()
        // setupFooterNavigation() // REMOVED
    }

    // ----------------------------------------------------------------------
    // --- HEADER AND STATUS TEXT BEHAVIOR ---
    // ----------------------------------------------------------------------

    private fun setupHeaderBehavior() {
        // 1. Back Button
        val btnBack: ImageButton? = findViewById(R.id.btnBack)
        btnBack?.setOnClickListener {
            finish() // Goes back to the previous activity
            overridePendingTransition(0, 0)
        }

        // 2. Results Status Text
        val resultsText: TextView? = findViewById(R.id.ResultsText)

        // Generate the dynamic time stamp
        val formattedDateTime = formatDateTime(officialUpdateTimeMillis)

        // Set the required certified status text
        resultsText?.text = "Here are the officially declared winners and the final vote tallies per position. Results have been verified and certified by the election Adviser.\nAs of $formattedDateTime."
    }

    // ----------------------------------------------------------------------
    // --- CARD GENERATION LOGIC (FIXED) ---
    // ----------------------------------------------------------------------

    /**
     * Finds the parent container and dynamically generates a card for each winner.
     * 🔥 FIX: Sets LayoutParams with bottom AND horizontal margins for each card.
     */
    private fun setupResultsCards() {
        val outerContainer: LinearLayout? = findViewById(R.id.registerContainer)
        outerContainer?.removeAllViews() // Clear any static placeholder card in the XML

        // Define the margin in DP (20dp) and convert it to pixels
        val cardMarginBottomPx = 20.toPx()
        val cardMarginHorizontalPx = 20.toPx() // 🔥 NEW: Horizontal margin in pixels

        winnerData.forEach { winner ->
            val candidateCardView = createCandidateCardView(winner)

            // 1. Create LayoutParams for the card
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                // 2. Apply the bottom margin
                bottomMargin = cardMarginBottomPx
                // 🔥 3. Apply the horizontal margins programmatically
                marginStart = cardMarginHorizontalPx
                marginEnd = cardMarginHorizontalPx
            }

            // 4. Apply the parameters to the view
            candidateCardView.layoutParams = params

            outerContainer?.addView(candidateCardView)
        }
    }

    /**
     * Inflates the list_item_winner_card template and populates it with a single winner's data.
     */
    private fun createCandidateCardView(winner: OfficialResultCandidate): View {
        val inflater = LayoutInflater.from(this)

        // Inflate the reusable XML layout file (list_item_winner_card.xml)
        // NOTE: The third argument 'false' ensures the layout parameters are not attached yet.
        val cardView = inflater.inflate(R.layout.list_item_winner_card, null, false) as LinearLayout

        // Populate data into the card views
        cardView.findViewById<CircleImageView>(R.id.candidate_profile_photo).setImageResource(winner.photoResId)
        cardView.findViewById<TextView>(R.id.candidate_name).text = winner.name
        cardView.findViewById<TextView>(R.id.candidate_position).text = winner.position

        // Format votes with thousand separator (e.g., 1,250 Votes)
        val votesString = String.format("%,d Votes", winner.votes)
        cardView.findViewById<TextView>(R.id.candidate_total_votes).text = votesString

        return cardView
    }

    // ----------------------------------------------------------------------
    // --- HELPER FUNCTIONS ---
    // ----------------------------------------------------------------------

    /**
     * Formats a time in milliseconds into a standard date and time string.
     */
    private fun formatDateTime(timeMillis: Long): String {
        val formatter = SimpleDateFormat("MMMM dd, yyyy, hh:mm a", Locale.getDefault())
        return formatter.format(Date(timeMillis))
    }

    /**
     * Extension function to convert DP units to screen Pixels.
     * This is required for setting margins programmatically.
     */
    private fun Int.toPx(): Int = (this * resources.displayMetrics.density).toInt()

    // ----------------------------------------------------------------------
    // --- FOOTER NAVIGATION LOGIC (REMOVED) ---
    // ----------------------------------------------------------------------

    // The setupFooterNavigation function was removed as requested.
}