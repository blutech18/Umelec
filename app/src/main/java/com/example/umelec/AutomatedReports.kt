package com.example.umelec

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// Assuming DoughnutChartView is a custom class defined in a separate file (as implied by the source)
// We only need the import or the class itself to be accessible.

class AutomatedReports : AppCompatActivity() {

    // 1. Define the data structure for the bar chart
    data class YearVoteData(val yearLabel: String, val voteCount: Int, val barItemViewId: Int)

    // 2. Sample Data (Replace with your actual data source)
    private val voteData = mutableListOf<YearVoteData>()
    private var userCollege: String = ""
    private var currentElectionId: String? = null

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

        // Get user's college first, then load all data
        loadUserCollegeAndData()
    }

    /**
     * Get user's college and then load all report data filtered by college
     */
    private fun loadUserCollegeAndData() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            android.util.Log.e("AutomatedReports", "User not authenticated")
            return
        }

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { userDoc ->
                if (!userDoc.exists()) {
                    android.util.Log.e("AutomatedReports", "User profile not found")
                    return@addOnSuccessListener
                }

                userCollege = userDoc.getString("college") ?: ""
                if (userCollege.isEmpty()) {
                    android.util.Log.e("AutomatedReports", "User college information not found")
                    return@addOnSuccessListener
                }

                android.util.Log.d("AutomatedReports", "Loading data for college: $userCollege")

                // Get current election ID for this college
                FirestoreElectionHelper.getCurrentElectionId(
                    onSuccess = { electionId ->
                        currentElectionId = electionId
                        // Load all data filtered by college
                        setupYearBarChart()
                        setupVoterTurnoutCard()
                        setupReportHeader()
                        setupDemographicCard()
                        setupDeclaredWinnersCard()
                        setupPositionRankCards()
                    },
                    onFailure = { error ->
                        android.util.Log.e("AutomatedReports", "Error getting election ID: $error")
                        // Still try to load data without election ID
                        setupYearBarChart()
                        setupVoterTurnoutCard()
                        setupReportHeader()
                        setupDemographicCard()
                        setupDeclaredWinnersCard()
                        setupPositionRankCards()
                    }
                )
            }
            .addOnFailureListener { exception ->
                android.util.Log.e("AutomatedReports", "Error getting user profile: ${exception.message}")
            }
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

        if (userCollege.isEmpty() || currentElectionId == null) {
            android.util.Log.w("AutomatedReports", "Cannot load position ranks: college or election ID missing")
            return
        }

        // Get vote tallies for this election (already filtered by college via election)
        FirestoreVoteHelper.getVoteTallies(
            electionId = currentElectionId!!,
            onSuccess = { tallies ->
                // Group tallies by position
                val positionGroups = tallies.groupBy { it.positionName }
                
                // Convert to PositionRank format
                val allPositionData = positionGroups.map { (positionName, positionTallies) ->
                    PositionRank(
                        positionTitle = positionName,
                        candidates = positionTallies.map { tally ->
                            CandidateVote(tally.candidateName, tally.voteCount)
                        }.sortedByDescending { it.votes },
                        abstentionCount = 0 // Abstentions would need to be calculated separately if needed
                    )
                }.sortedBy { it.positionTitle }
                
                renderPositionRankCards(mainContainer, allPositionData)
            },
            onFailure = { error ->
                android.util.Log.e("AutomatedReports", "Error getting vote tallies: $error")
                renderPositionRankCards(mainContainer, emptyList())
            }
        )
    }

    private fun renderPositionRankCards(mainContainer: LinearLayout, allPositionData: List<PositionRank>) {
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

        if (userCollege.isEmpty() || currentElectionId == null) {
            android.util.Log.w("AutomatedReports", "Cannot load winners: college or election ID missing")
            return
        }

        // Get vote tallies and determine winners (already filtered by college via election)
        FirestoreVoteHelper.getVoteTallies(
            electionId = currentElectionId!!,
            onSuccess = { tallies ->
                // Group by position and get top candidate per position
                val positionGroups = tallies.groupBy { it.positionName }
                val declaredWinnersData = positionGroups.mapNotNull { (positionName, positionTallies) ->
                    val topCandidate = positionTallies.maxByOrNull { it.voteCount }
                    topCandidate?.let {
                        Winner(it.candidateName, positionName)
                    }
                }.sortedBy { it.position }

                val inflater = layoutInflater

                declaredWinnersData.forEach { winner ->
            val rowView = inflater.inflate(R.layout.winner_data_row, winnersContainer, false)

            val tvName: TextView = rowView.findViewById(R.id.tvWinnerCandidateName)
            val tvPosition: TextView = rowView.findViewById(R.id.tvWinnerPosition)

            tvName.text = winner.candidateName
            tvPosition.text = winner.position

            winnersContainer.addView(rowView)
        }
            },
            onFailure = { error ->
                android.util.Log.e("AutomatedReports", "Error getting vote tallies for winners: $error")
            }
        )
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

        if (userCollege.isEmpty()) {
            tvFemaleEligible.text = "0"
            tvMaleEligible.text = "0"
            tvFemaleTurnout.text = "0%"
            tvFemaleSummaryRate.text = "0%"
            tvMaleTurnout.text = "0%"
            tvMaleSummaryRate.text = "0%"
            return
        }

        // Get voters for this college
        FirestoreVoterHelper.getVotersByCollege(
            college = userCollege,
            onSuccess = { voters ->
                val femaleVoters = voters.filter { (it["gender"] as? String ?: "").equals("Female", ignoreCase = true) }
                val maleVoters = voters.filter { (it["gender"] as? String ?: "").equals("Male", ignoreCase = true) }
                
                val femaleEligibleCount = femaleVoters.size
                val maleEligibleCount = maleVoters.size

                if (currentElectionId != null) {
                    // Get votes for this election
                    FirestoreVoterHelper.getVotersWhoVoted(
                        electionId = currentElectionId!!,
                        onSuccess = { votedVoters ->
                            val votedIds = votedVoters.mapNotNull { it["userId"] as? String }.toSet()
                            
                            val femaleVotedCount = femaleVoters.count { it["userId"] as? String in votedIds }
                            val maleVotedCount = maleVoters.count { it["userId"] as? String in votedIds }

                            val femaleTurnoutPercent = calculateTurnout(femaleVotedCount, femaleEligibleCount)
                            val maleTurnoutPercent = calculateTurnout(maleVotedCount, maleEligibleCount)

                            tvFemaleEligible.text = femaleEligibleCount.toString()
                            tvMaleEligible.text = maleEligibleCount.toString()

                            tvFemaleTurnout.text = "$femaleTurnoutPercent%"
                            tvFemaleSummaryRate.text = "$femaleTurnoutPercent%"

                            tvMaleTurnout.text = "$maleTurnoutPercent%"
                            tvMaleSummaryRate.text = "$maleTurnoutPercent%"
                        },
                        onFailure = { error ->
                            android.util.Log.e("AutomatedReports", "Error getting voted voters: $error")
                            tvFemaleEligible.text = femaleEligibleCount.toString()
                            tvMaleEligible.text = maleEligibleCount.toString()
                            tvFemaleTurnout.text = "0%"
                            tvFemaleSummaryRate.text = "0%"
                            tvMaleTurnout.text = "0%"
                            tvMaleSummaryRate.text = "0%"
                        }
                    )
                } else {
                    tvFemaleEligible.text = femaleEligibleCount.toString()
                    tvMaleEligible.text = maleEligibleCount.toString()
                    tvFemaleTurnout.text = "0%"
                    tvFemaleSummaryRate.text = "0%"
                    tvMaleTurnout.text = "0%"
                    tvMaleSummaryRate.text = "0%"
                }
            },
            onFailure = { error ->
                android.util.Log.e("AutomatedReports", "Error getting voters: $error")
                tvFemaleEligible.text = "0"
                tvMaleEligible.text = "0"
                tvFemaleTurnout.text = "0%"
                tvFemaleSummaryRate.text = "0%"
                tvMaleTurnout.text = "0%"
                tvMaleSummaryRate.text = "0%"
            }
        )
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

        if (userCollege.isEmpty()) {
            tvVotedPercent.text = "0%"
            tvNotVotedPercent.text = "100%"
            return
        }

        // Get voters for this college
        FirestoreVoterHelper.getVotersByCollege(
            college = userCollege,
            onSuccess = { voters ->
                val totalVoters = voters.size
                
                if (currentElectionId != null) {
                    // Get votes for this election
                    FirestoreVoterHelper.getVotersWhoVoted(
                        electionId = currentElectionId!!,
                        onSuccess = { votedVoters ->
                            // Filter voted voters by college
                            val votedIds = votedVoters.mapNotNull { it["userId"] as? String }.toSet()
                            val collegeVotedCount = voters.count { it["userId"] as? String in votedIds }
                            
                            val votedPercentageFloat = if (totalVoters > 0) (collegeVotedCount.toFloat() / totalVoters) * 100 else 0f
                            val votedPercentage = votedPercentageFloat.toInt().coerceIn(0, 100)
                            val notVotedPercentage = 100 - votedPercentage

                            tvVotedPercent.text = "$votedPercentage%"
                            tvNotVotedPercent.text = "$notVotedPercentage%"
                        },
                        onFailure = { error ->
                            android.util.Log.e("AutomatedReports", "Error getting voted voters: $error")
                            tvVotedPercent.text = "0%"
                            tvNotVotedPercent.text = "100%"
                        }
                    )
                } else {
                    tvVotedPercent.text = "0%"
                    tvNotVotedPercent.text = "100%"
                }
            },
            onFailure = { error ->
                android.util.Log.e("AutomatedReports", "Error getting voters: $error")
                tvVotedPercent.text = "0%"
                tvNotVotedPercent.text = "100%"
            }
        )
    }


    // ----------------------------------------------------------------------
    // --- BAR CHART LOGIC (Existing) ---
    // ----------------------------------------------------------------------
    private fun setupYearBarChart() {
        val barAreaContainer: LinearLayout = findViewById(R.id.BarArea)
        val tvTotalVotedCount: TextView = findViewById(R.id.tvTotalVotedCount)

        if (userCollege.isEmpty() || currentElectionId == null) {
            tvTotalVotedCount.text = "0"
            return
        }

        // Get voter statistics by year for this college's election
        FirestoreVoterHelper.getVoterStatisticsByYear(
            electionId = currentElectionId!!,
            onSuccess = { yearCounts ->
                // Filter to only include voters from this college
                FirestoreVoterHelper.getVotersByCollege(
                    college = userCollege,
                    onSuccess = { collegeVoters ->
                        val collegeVoterIds = collegeVoters.mapNotNull { it["userId"] as? String }.toSet()
                        
                        // Get votes and filter by college voters
                        FirebaseFirestore.getInstance()
                            .collection("votes")
                            .whereEqualTo("electionId", currentElectionId)
                            .get()
                            .addOnSuccessListener { voteDocuments ->
                                val collegeVoteUserIds = voteDocuments.documents
                                    .mapNotNull { it.getString("userId") }
                                    .filter { it in collegeVoterIds }
                                    .distinct()

                                // Get year distribution for college voters who voted
                                val collegeYearCounts = mutableMapOf<String, Int>()
                                var completed = 0
                                val total = collegeVoteUserIds.size

                                if (total == 0) {
                                    voteData.clear()
                                    voteData.add(YearVoteData("1st", 0, R.id.barItem1st))
                                    voteData.add(YearVoteData("2nd", 0, R.id.barItem2nd))
                                    voteData.add(YearVoteData("3rd", 0, R.id.barItem3rd))
                                    voteData.add(YearVoteData("4th", 0, R.id.barItem4th))
                                    updateBarChart(barAreaContainer, tvTotalVotedCount)
                                    return@addOnSuccessListener
                                }

                                collegeVoteUserIds.forEach { userId ->
                                    FirebaseFirestore.getInstance()
                                        .collection("users")
                                        .document(userId)
                                        .get()
                                        .addOnSuccessListener { userDoc ->
                                            val yearRaw = userDoc.getString("year") ?: "Unknown"
                                            val year = yearRaw.replace(" Year", "").trim()
                                            collegeYearCounts[year] = (collegeYearCounts[year] ?: 0) + 1

                                            completed++
                                            if (completed == total) {
                                                voteData.clear()
                                                voteData.add(YearVoteData("1st", collegeYearCounts["1st"] ?: 0, R.id.barItem1st))
                                                voteData.add(YearVoteData("2nd", collegeYearCounts["2nd"] ?: 0, R.id.barItem2nd))
                                                voteData.add(YearVoteData("3rd", collegeYearCounts["3rd"] ?: 0, R.id.barItem3rd))
                                                voteData.add(YearVoteData("4th", collegeYearCounts["4th"] ?: 0, R.id.barItem4th))
                                                updateBarChart(barAreaContainer, tvTotalVotedCount)
                                            }
                                        }
                                        .addOnFailureListener { exception ->
                                            android.util.Log.e("AutomatedReports", "Error getting user data: ${exception.message}")
                                            completed++
                                            if (completed == total) {
                                                voteData.clear()
                                                voteData.add(YearVoteData("1st", collegeYearCounts["1st"] ?: 0, R.id.barItem1st))
                                                voteData.add(YearVoteData("2nd", collegeYearCounts["2nd"] ?: 0, R.id.barItem2nd))
                                                voteData.add(YearVoteData("3rd", collegeYearCounts["3rd"] ?: 0, R.id.barItem3rd))
                                                voteData.add(YearVoteData("4th", collegeYearCounts["4th"] ?: 0, R.id.barItem4th))
                                                updateBarChart(barAreaContainer, tvTotalVotedCount)
                                            }
                                        }
                                }
                            }
                            .addOnFailureListener { exception ->
                                android.util.Log.e("AutomatedReports", "Error getting votes: ${exception.message}")
                                tvTotalVotedCount.text = "0"
                            }
                    },
                    onFailure = { error ->
                        android.util.Log.e("AutomatedReports", "Error getting college voters: $error")
                        tvTotalVotedCount.text = "0"
                    }
                )
            },
            onFailure = { error ->
                android.util.Log.e("AutomatedReports", "Error getting voter statistics: $error")
                tvTotalVotedCount.text = "0"
            }
        )
    }

    private fun updateBarChart(barAreaContainer: LinearLayout, tvTotalVotedCount: TextView) {
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