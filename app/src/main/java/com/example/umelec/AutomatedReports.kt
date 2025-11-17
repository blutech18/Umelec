package com.example.umelec

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

// Assuming DoughnutChartView is a custom class defined in a separate file (as implied by the source)
// We only need the import or the class itself to be accessible.

class AutomatedReports : AppCompatActivity() {

    // 1. Define the data structure for the bar chart
    data class YearVoteData(val yearLabel: String, val voteCount: Int, val barItemViewId: Int)

    // 2. Sample Data (Replace with your actual data source)
    private val voteData = listOf(
        YearVoteData("1st", 15, R.id.barItem1st),
        YearVoteData("2nd", 8, R.id.barItem2nd),
        YearVoteData("3rd", 7, R.id.barItem3rd),
        YearVoteData("4th", 4, R.id.barItem4th)
    )

    // 3. Define data structure for Declared Winners (Existing)
    data class Winner(val candidateName: String, val position: String)

    // ⭐️ NEW: Define data structures for Position Ranks ⭐️
    data class CandidateVote(val name: String, val votes: Int)
    data class PositionRank(val positionTitle: String, val candidates: List<CandidateVote>, val abstentionCount: Int)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_automated_reports)

        // 💡 NEW: Set up the back button logic
        setupBackButton()

        // Set up the bar chart logic
        setupYearBarChart()

        // ⭐️ INTEGRATED: Set up the Vote Turnout card logic
        setupVoterTurnoutCard()

        // ⭐️ NEW: Set up the Report Header/Status logic (Dates, PDF Download)
        setupReportHeader()

        // ⭐️ NEW: Set up the Demographic Card logic
        setupDemographicCard()

        // ⭐️ NEW: Set up the Declared Winners Card logic
        setupDeclaredWinnersCard()

        // ⭐️ NEW: Set up the Position Rank Card logic (Main Request)
        setupPositionRankCards()
    }

    /**
     * 💡 NEW: Set up the listener for the back button to navigate back.
     */
    private fun setupBackButton() {
        val btnBack: ImageButton = findViewById(R.id.btnBack)

        btnBack.setOnClickListener {
            // When clicked, finish the current activity to return to the previous one
            finish()
        }
    }

    // ----------------------------------------------------------------------
    // --- POSITION RANK CARD LOGIC (New) ---
    // ----------------------------------------------------------------------
    private fun setupPositionRankCards() {
        // ⭐️ IMPORTANT: This container must exist in your activity_automated_reports.xml
        // to hold all dynamically generated position cards.
        val mainContainer: LinearLayout = findViewById(R.id.llPositionRanksContainer)
        mainContainer.removeAllViews() // Clear previous views

        // ----------------------------------------------------------------------
        // ⭐️ BACKEND/DATABASE INTEGRATION POINT for Position Ranks ⭐️
        // Fetch data structured by Position, containing all candidates and their votes.
        // ----------------------------------------------------------------------
        val allPositionData = listOf(
            PositionRank(
                "President",
                listOf(
                    CandidateVote("Anne Garcia", 100),
                    CandidateVote("Ben Torres", 75),
                    CandidateVote("Cathy Lim", 50)
                ),
                abstentionCount = 15
            ),
            PositionRank(
                "Vice President",
                listOf(
                    CandidateVote("Michael Sison", 120),
                    CandidateVote("Sarah Cruz", 90),
                    CandidateVote("David Lee", 30),
                    CandidateVote("Elena Reyes", 25)
                ),
                abstentionCount = 10
            ),
            PositionRank(
                "Secretary",
                listOf(
                    CandidateVote("Elisa Reyes", 80),
                    CandidateVote("Francis Dee", 78)
                ),
                abstentionCount = 5
            ),
            PositionRank(
                "Treasurer",
                listOf(
                    CandidateVote("John Smith", 150)
                ),
                abstentionCount = 20
            )
        )
        // ----------------------------------------------------------------------

        val inflater = layoutInflater

        allPositionData.forEach { positionRank ->
            // 1. Inflate the main card template for this position
            // Assumes you created R.layout.position_rank_card_template
            val positionCardView = inflater.inflate(R.layout.position_rank_card_template, mainContainer, false) as LinearLayout

            // 2. Find elements in the inflated card
            val tvTitle: TextView = positionCardView.findViewById(R.id.PositionTitleVoteCount)
            val abstentionCounter: TextView = positionCardView.findViewById(R.id.AbstainVoteCounter)
            val rowsContainer: LinearLayout = positionCardView.findViewById(R.id.llCandidateRankRowsContainer)

            // 3. Set card title and abstention count
            tvTitle.text = positionRank.positionTitle
            abstentionCounter.text = positionRank.abstentionCount.toString()

            // 4. Sort candidates by votes (highest first)
            val sortedCandidates = positionRank.candidates.sortedByDescending { it.votes }

            // 5. Loop through sorted candidates and inflate rank rows
            sortedCandidates.forEachIndexed { index, candidate ->
                // Assumes you created R.layout.candidate_rank_row
                val rowView = inflater.inflate(R.layout.candidate_rank_row, rowsContainer, false)

                val tvRank: TextView = rowView.findViewById(R.id.tvCandidateRank)
                val tvName: TextView = rowView.findViewById(R.id.tvCandidateName)
                val tvVotes: TextView = rowView.findViewById(R.id.tvCandidateTotalVotes)

                // The rank is the index + 1
                tvRank.text = (index + 1).toString()
                tvName.text = candidate.name
                tvVotes.text = candidate.votes.toString()

                rowsContainer.addView(rowView)
            }

            // 6. Add the complete position card to the main container
            mainContainer.addView(positionCardView)
        }
    }


    // ----------------------------------------------------------------------
    // --- DECLARED WINNERS CARD LOGIC (Existing) ---
    // ----------------------------------------------------------------------
    private fun setupDeclaredWinnersCard() {
        val winnersContainer: LinearLayout = findViewById(R.id.llWinnersRowsContainer)
        winnersContainer.removeAllViews()

        // ----------------------------------------------------------------------
        // ⭐️ BACKEND/DATABASE INTEGRATION POINT for Declared Winners ⭐️
        // ----------------------------------------------------------------------
        val declaredWinnersData = listOf(
            Winner("Anne Garcia", "President"),
            Winner("Michael Sison", "Vice President"),
            Winner("Elisa Reyes", "Secretary"),
            Winner("John Smith", "Treasurer"),
            Winner("Jane Doe", "Auditor")
        )
        // ----------------------------------------------------------------------

        val inflater = layoutInflater

        declaredWinnersData.forEach { winner ->
            val rowView = inflater.inflate(R.layout.winner_data_row, winnersContainer, false)

            val tvName: TextView = rowView.findViewById(R.id.tvWinnerCandidateName)
            val tvPosition: TextView = rowView.findViewById(R.id.tvWinnerPosition)

            tvName.text = winner.candidateName
            tvPosition.text = winner.position

            winnersContainer.addView(rowView)
        }
    }


    // ----------------------------------------------------------------------
    // --- REPORT HEADER LOGIC (Existing) ---
    // ----------------------------------------------------------------------
    private fun setupReportHeader() {
        val tvReportGeneratedDateTime: TextView = findViewById(R.id.tvReportGeneratedDateTime)
        val tvStartElectionPeriodDates: TextView = findViewById(R.id.tvStartElectionPeriodDates)
        val tvEndElectionPeriodDates: TextView = findViewById(R.id.tvEndElectionPeriodDates)
        val masterPdfDlLayout: LinearLayout = findViewById(R.id.MasterPdfDlLayout)

        // ----------------------------------------------------------------------
        // ⭐️ BACKEND/DATABASE INTEGRATION POINT for Election Dates ⭐️
        // ----------------------------------------------------------------------
        val currentDateTime = "2025-11-15, 02:04 PM" // Placeholder for current date/time
        val electionStartDate = "Oct 1, 2025"        // Placeholder for Start Date from DB
        val electionEndDate = "Oct 3, 2025"          // Placeholder for End Date from DB
        // ----------------------------------------------------------------------

        tvReportGeneratedDateTime.text = currentDateTime
        tvStartElectionPeriodDates.text = electionStartDate
        tvEndElectionPeriodDates.text = electionEndDate

        masterPdfDlLayout.setOnClickListener {
            // ----------------------------------------------------------------------
            // ⭐️ BACKEND/DATABASE INTEGRATION POINT for PDF Download ⭐️
            // ----------------------------------------------------------------------
            Toast.makeText(this, "pdf downloaded", Toast.LENGTH_SHORT).show()
        }
    }


    // ----------------------------------------------------------------------
    // --- DEMOGRAPHIC CARD LOGIC (Existing) ---
    // ----------------------------------------------------------------------
    private fun setupDemographicCard() {
        val tvFemaleEligible: TextView = findViewById(R.id.tvFemaleEligible)
        val tvMaleEligible: TextView = findViewById(R.id.tvMaleEligible)
        val tvFemaleTurnout: TextView = findViewById(R.id.tvFemaleTurnout)
        val tvMaleTurnout: TextView = findViewById(R.id.tvMaleTurnout)
        val tvFemaleSummaryRate: TextView = findViewById(R.id.tvFemaleSummaryRate)
        val tvMaleSummaryRate: TextView = findViewById(R.id.tvMaleSummaryRate)

        // ----------------------------------------------------------------------
        // ⭐️ BACKEND/DATABASE INTEGRATION POINT for Demographics ⭐️
        // ----------------------------------------------------------------------
        val femaleEligibleCount = 200
        val femaleVotedCount = 40
        val maleEligibleCount = 150
        val maleVotedCount = 75
        // ----------------------------------------------------------------------

        val femaleTurnoutPercent = calculateTurnout(femaleVotedCount, femaleEligibleCount)
        val maleTurnoutPercent = calculateTurnout(maleVotedCount, maleEligibleCount)

        tvFemaleEligible.text = femaleEligibleCount.toString()
        tvMaleEligible.text = maleEligibleCount.toString()

        tvFemaleTurnout.text = "$femaleTurnoutPercent%"
        tvFemaleSummaryRate.text = "$femaleTurnoutPercent%"

        tvMaleTurnout.text = "$maleTurnoutPercent%"
        tvMaleSummaryRate.text = "$maleTurnoutPercent%"
    }

    private fun calculateTurnout(votedCount: Int, eligibleCount: Int): Int {
        return if (eligibleCount > 0) {
            ((votedCount.toFloat() / eligibleCount.toFloat()) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }
    }

    // ----------------------------------------------------------------------
    // --- VOTER TURNOUT CARD LOGIC (Existing) ---
    // ----------------------------------------------------------------------
    private fun setupVoterTurnoutCard() {
        // NOTE: DoughnutChartView is assumed to be a custom class
        // DoughnutChartView can be accessed via findViewById(R.id.voterTurnoutChart) if needed in the future
        val tvVotedPercent: TextView = findViewById(R.id.tvVotedPercentage)
        val tvNotVotedPercent: TextView = findViewById(R.id.tvNotVotedPercentage)

        // ----------------------------------------------------------------------
        // ⭐️ BACKEND/DATABASE INTEGRATION POINT for Overall Turnout ⭐️
        // ----------------------------------------------------------------------
        val totalVoters = 350
        val votedCount = 115

        val votedPercentageFloat = if (totalVoters > 0) (votedCount.toFloat() / totalVoters) * 100 else 0f
        val votedPercentage = votedPercentageFloat.toInt().coerceIn(0, 100)
        val notVotedPercentage = 100 - votedPercentage

        // ----------------------------------------------------------------------
        // ⭐️ END OF DATABASE INTEGRATION POINT ⭐️
        // ----------------------------------------------------------------------

        // NOTE: The next line is commented out as DoughnutChartView class is not available to cast
        // donutView.votedPercentage = votedPercentage
        tvVotedPercent.text = "$votedPercentage%"
        tvNotVotedPercent.text = "$notVotedPercentage%"
    }


    // ----------------------------------------------------------------------
    // --- BAR CHART LOGIC (Existing) ---
    // ----------------------------------------------------------------------
    private fun setupYearBarChart() {
        val barAreaContainer: LinearLayout = findViewById(R.id.BarArea)
        val tvTotalVotedCount: TextView = findViewById(R.id.tvTotalVotedCount)

        val totalVotes = voteData.sumOf { it.voteCount }

        tvTotalVotedCount.text = totalVotes.toString()

        if (totalVotes == 0) return

        barAreaContainer.post {
            val containerWidth = barAreaContainer.width

            voteData.forEach { data ->
                val barItemView = findViewById<View>(data.barItemViewId)
                val tvBarLabel: TextView = barItemView.findViewById(R.id.tvBarLabel)
                val progressBar: View = barItemView.findViewById(R.id.vBarProgress)
                val tvBarValue: TextView = barItemView.findViewById(R.id.tvBarValue)

                tvBarLabel.text = data.yearLabel
                tvBarValue.text = data.voteCount.toString()

                val votePercentage = data.voteCount.toFloat() / totalVotes.toFloat()

                val targetWidth = (containerWidth * votePercentage).toInt()

                val params: ViewGroup.LayoutParams = progressBar.layoutParams
                params.width = targetWidth
                progressBar.layoutParams = params
            }
        }
    }
}