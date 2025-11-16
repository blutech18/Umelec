package com.example.umelec

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import de.hdodenhof.circleimageview.CircleImageView
import java.util.concurrent.TimeUnit
import java.util.Calendar



class Leader_monitor : AppCompatActivity() {

    // Notification Manager (Assumed external class, not included here)
    private lateinit var notificationManager: NotificationManager

    // Header views (only icons)
    private lateinit var profileIcon: ImageView
    private lateinit var notificationIcon: ImageView

    private var countDownTimer: CountDownTimer? = null
    private var candidateItemWidth = 0

    // ⭐️ Simulated data for leading candidates
    private val leadingCandidates = listOf(
        LeadingCandidate(
            position = "Chairperson",
            name = "Mark Tan",
            votes = 2540,
            profileResId = R.drawable.ic_profile
        ),
        LeadingCandidate(
            position = "Treasurer",
            name = "Sarah Lee",
            votes = 1800,
            profileResId = R.drawable.ic_profile
        ),
        LeadingCandidate(
            position = "PRO",
            name = "Alex Stone",
            votes = 1500,
            profileResId = R.drawable.ic_profile
        )
    )

    // ⭐️ Fake Future Dates for Countdown demonstration
    private val UPCOMING_START_TIME_MS: Long
    private val ONGOING_END_TIME_MS: Long

    // ⭐️ Set to ONGOING for testing voter turnout card
    private var resultCardState = ResultCardState.ENDED

    init {
        // Calculate demonstration dates immediately
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 5) // 5 days for UPCOMING phase
        UPCOMING_START_TIME_MS = calendar.timeInMillis

        val calendar2 = Calendar.getInstance()
        calendar2.add(Calendar.MINUTE, 10) // 10 minutes for ONGOING phase
        ONGOING_END_TIME_MS = calendar2.timeInMillis
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ensure this points to the correct XML file where the cards are defined
        setContentView(R.layout.activity_leader_monitor)

        // Init notification manager
        notificationManager = NotificationManager(this)


        // Init header + footer
        initializeViews()
        setupUIListeners()
        setupFooterNavigation()

        // ⭐️ Start the UI update logic
        updateUIForPhase(resultCardState)
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }

    private fun initializeViews() {
        profileIcon = findViewById(R.id.profileIcon)
        notificationIcon = findViewById(R.id.notificationIcon)
    }

    private fun setupUIListeners() {
        // Profile → go to profile page
        profileIcon.setOnClickListener {
            val intent = Intent(this, Leader_profile::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        // Notification → toggle dropdown
        notificationIcon.setOnClickListener {
            notificationManager.toggleNotificationDropdown(it as ImageView)
        }
    }

    private fun setupFooterNavigation() {
        val navHome: LinearLayout = findViewById(R.id.nav_home)
        val navSetup: LinearLayout = findViewById(R.id.nav_setup)
        val navManage: LinearLayout = findViewById(R.id.nav_manage)
        val navMonitor: LinearLayout = findViewById(R.id.nav_monitor)
        val navFaq: LinearLayout = findViewById(R.id.nav_faq)

        val navigateTo = { activityClass: Class<*> ->
            if (activityClass != this::class.java) {
                val intent = Intent(this, activityClass)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(intent)
                overridePendingTransition(0, 0)
            }
        }

        navHome.setOnClickListener { navigateTo(Leader_homepage::class.java) }
        navSetup.setOnClickListener { navigateTo(Leader_Setup::class.java) }
        navManage.setOnClickListener { navigateTo(Leader_manage_voters::class.java) }
        navMonitor.setOnClickListener { /* Already here */ }
        navFaq.setOnClickListener { navigateTo(Leader_faqs::class.java) }
    }

    // ----------------------------------------------------------------------
    // --- UTILITY ---
    // ----------------------------------------------------------------------

    private fun Int.toPx(): Int = (this * resources.displayMetrics.density).toInt()

    // ----------------------------------------------------------------------
    // --- MAIN PHASE LOGIC ---
    // ----------------------------------------------------------------------

    private fun updateUIForPhase(state: ResultCardState) {
        val timeCard: ConstraintLayout = findViewById(R.id.TimeCard)
        val candidatesPreviewCard: LinearLayout = findViewById(R.id.CandidatesPreviewCard)
        val talliesCard: LinearLayout = findViewById(R.id.TalliesCard)
        val resultsCard: LinearLayout = findViewById(R.id.ResultsCard)
        val reportCard: LinearLayout = findViewById(R.id.ReportCard)
        // ⭐️ Voter Turnout Card reference
        val voteTurnoutCard: LinearLayout = findViewById(R.id.VoteTurnoutCard)

        // Reset visibility
        timeCard.visibility = View.GONE
        candidatesPreviewCard.visibility = View.GONE
        talliesCard.visibility = View.GONE
        resultsCard.visibility = View.GONE
        reportCard.visibility = View.GONE
        voteTurnoutCard.visibility = View.GONE // ⭐️ Hidden by default

        countDownTimer?.cancel()

        when (state) {
            ResultCardState.UPCOMING -> {
                timeCard.visibility = View.VISIBLE
                findViewById<TextView>(R.id.TimeTitle).text = "Next election starts in"
                setupCountdown(UPCOMING_START_TIME_MS)
            }
            ResultCardState.ONGOING -> {
                timeCard.visibility = View.VISIBLE
                candidatesPreviewCard.visibility = View.VISIBLE
                talliesCard.visibility = View.VISIBLE
                // ⭐️ Make the Vote Turnout Card visible only in ONGOING phase
                voteTurnoutCard.visibility = View.VISIBLE

                findViewById<TextView>(R.id.TimeTitle).text = "Remaining time for the election"
                setupCountdown(ONGOING_END_TIME_MS)
                setupCandidatesPreviewCard()
                setupTalliesCard(isFinal = false)

                // ⭐️ Call the function to fetch and display the live voter turnout data
                setupVoterTurnoutCard()
            }
            ResultCardState.ENDED -> {
                timeCard.visibility = View.VISIBLE
                talliesCard.visibility = View.VISIBLE
                resultsCard.visibility = View.VISIBLE
                reportCard.visibility = View.VISIBLE

                // You can choose to show or hide the Vote Turnout Card in ENDED phase:
                // voteTurnoutCard.visibility = View.VISIBLE

                findViewById<TextView>(R.id.TimeTitle).text = "Election has ended"
                updateTimerDisplay(0)

                setupTalliesCard(isFinal = true)
                setupResultsCard()
                setupReportCard()

                // If shown, call the function to display the final voter turnout data
                // setupVoterTurnoutCard()
            }
            ResultCardState.NO_ELECTION -> {
                timeCard.visibility = View.VISIBLE
                findViewById<TextView>(R.id.TimeTitle).text = "No election data available."
                updateTimerDisplay(0)
            }
        }
    }

    // ----------------------------------------------------------------------
    // --- COUNTDOWN TIMER LOGIC ---
    // ----------------------------------------------------------------------

    private fun setupCountdown(futureTimeMs: Long) {
        countDownTimer?.cancel()
        val timeRemaining = futureTimeMs - System.currentTimeMillis()

        if (timeRemaining <= 0) {
            updateTimerDisplay(0)
            return
        }

        countDownTimer = object : CountDownTimer(timeRemaining, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                updateTimerDisplay(millisUntilFinished)
            }

            override fun onFinish() {
                updateTimerDisplay(0)
                if (resultCardState == ResultCardState.UPCOMING) {
                    resultCardState = ResultCardState.ONGOING
                    updateUIForPhase(resultCardState)
                } else if (resultCardState == ResultCardState.ONGOING) {
                    resultCardState = ResultCardState.ENDED
                    updateUIForPhase(resultCardState)
                }
            }
        }.start()
    }

    private fun updateTimerDisplay(millis: Long) {
        val days = TimeUnit.MILLISECONDS.toDays(millis)
        val hours = TimeUnit.MILLISECONDS.toHours(millis) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60

        findViewById<TextView>(R.id.tvDaysValue).text = String.format("%02d", days)
        findViewById<TextView>(R.id.tvHoursValue).text = String.format("%02d", hours)
        findViewById<TextView>(R.id.tvMinutesValue).text = String.format("%02d", minutes)
        findViewById<TextView>(R.id.tvSecondsValue).text = String.format("%02d", seconds)
    }

    // ----------------------------------------------------------------------
    // --- ONGOING PHASE CARD LOGIC (CandidatesPreviewCard) ---
    // ----------------------------------------------------------------------

    private fun setupCandidatesPreviewCard() {
        val container: LinearLayout = findViewById(R.id.candidateListContainer)
        val cardLayout: LinearLayout = findViewById(R.id.CandidatesPreviewCard)

        cardLayout.post {
            val viewWidth = cardLayout.width
            val btnPrev: ImageButton = findViewById(R.id.btnPrevCandidate)
            val btnNext: ImageButton = findViewById(R.id.btnNextCandidate)

            // ⭐️ CHANGE 1: Calculate item width to fit exactly 2 candidates
            // We use the full card width divided by 2. This ensures no cutoff.
            candidateItemWidth = (viewWidth / 2).coerceAtLeast(180.toPx())

            container.removeAllViews()

            leadingCandidates.forEach { candidate ->
                container.addView(createLeadingCandidateView(candidate))
            }

            setupCandidateScrollControls()
        }
    }

    private fun createLeadingCandidateView(candidate: LeadingCandidate): View {

        val cardView = LayoutInflater.from(this).inflate(
            R.layout.leading_candidate_card,
            null,
            false
        ) as LinearLayout

        // 2. Apply Layout Parameters
        cardView.layoutParams = LinearLayout.LayoutParams(
            candidateItemWidth, // Use the calculated width
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            // ⭐️ CHANGE 2: Remove margins to ensure the calculated width is precise
            rightMargin = 0.toPx()
            leftMargin = 0.toPx()
        }

        // 3. Find and set data for all views from the XML
        val profilePic: CircleImageView = cardView.findViewById(R.id.candidatePhoto)
        val nameText: TextView = cardView.findViewById(R.id.candidateName)
        val positionText: TextView = cardView.findViewById(R.id.candidatePosition)
        val voteCountText: TextView = cardView.findViewById(R.id.candidateVotecount)

        // Set the actual data
        profilePic.setImageResource(candidate.profileResId)
        nameText.text = candidate.name
        positionText.text = candidate.position

        // Format the vote count for display
        val voteCountString = String.format("%,d Votes", candidate.votes)
        voteCountText.text = voteCountString

        return cardView
    }

    private fun setupCandidateScrollControls() {
        val scrollView: HorizontalScrollView = findViewById(R.id.candidateScrollView)
        val btnPrev: ImageButton = findViewById(R.id.btnPrevCandidate)
        val btnNext: ImageButton = findViewById(R.id.btnNextCandidate)

        val shouldShowArrows = leadingCandidates.size > 1
        btnPrev.visibility = if (shouldShowArrows) View.VISIBLE else View.INVISIBLE
        btnNext.visibility = if (shouldShowArrows) View.VISIBLE else View.INVISIBLE

        btnPrev.setOnClickListener {
            if (candidateItemWidth > 0) {
                scrollView.smoothScrollBy(-candidateItemWidth, 0)
            }
        }

        btnNext.setOnClickListener {
            if (candidateItemWidth > 0) {
                scrollView.smoothScrollBy(candidateItemWidth, 0)
            }
        }
    }

    // ----------------------------------------------------------------------
    // --- TALLIES & RESULTS CARD LOGIC ---
    // ----------------------------------------------------------------------

    private fun setupTalliesCard(isFinal: Boolean) {
        val talliesTitle = findViewById<TextView>(R.id.talliesCardTitle)
        val talliesMessage = findViewById<TextView>(R.id.talliesCardMessage)
        val btnTally = findViewById<AppCompatButton>(R.id.btnTally)

        if (isFinal) {
            talliesTitle.text = "Final Tallies"
            talliesMessage.text = "Live tallies have been finalized."
            btnTally.text = "View Final Tallies"
        } else {
            talliesTitle.text = "Live Tallies"
            talliesMessage.text = "Live tallies are available."
            btnTally.text = "View Live Tally"
        }

        btnTally.setOnClickListener {
            val intent = Intent(this, Tallies::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
    }

    private fun setupResultsCard() {
        val btnResult = findViewById<AppCompatButton>(R.id.btnResult)

        btnResult.setOnClickListener {
            val intent = Intent(this, OfficialResults::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
    }

    private fun setupReportCard() {
        val btnViewReport = findViewById<AppCompatButton>(R.id.btnViewReport)

        btnViewReport.setOnClickListener {
            val intent = Intent(this, AutomatedReports::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
    }

    // ----------------------------------------------------------------------
    // --- VOTER TURNOUT CARD LOGIC ---
    // ----------------------------------------------------------------------

    /**
     * Updates the data displayed in the Voter Turnout card, including the chart and percentages.
     */
    private fun setupVoterTurnoutCard() {
        // ⭐️ Reference to the custom chart view
        val donutView: DoughnutChartView = findViewById(R.id.voterTurnoutChart)
        val tvVotedPercent: TextView = findViewById(R.id.tvVotedPercentage)
        val tvNotVotedPercent: TextView = findViewById(R.id.tvNotVotedPercentage)

        // ----------------------------------------------------------------------
        // ⭐️ BACKEND/DATABASE INTEGRATION POINT ⭐️
        // ----------------------------------------------------------------------

        // 1. Database Variables (Replace these with actual async data fetching)
        val totalVoters = 10
        val votedCount = 5

        // 2. Calculate percentages
        val votedPercentageFloat = if (totalVoters > 0) (votedCount.toFloat() / totalVoters) * 100 else 0f

        // Use an Int for display and chart drawing
        val votedPercentage = votedPercentageFloat.toInt().coerceIn(0, 100)
        val notVotedPercentage = 100 - votedPercentage

        // ----------------------------------------------------------------------
        // ⭐️ END OF DATABASE INTEGRATION POINT ⭐️
        // ----------------------------------------------------------------------

        // 3. Update the custom Doughnut Chart View
        donutView.votedPercentage = votedPercentage

        // 4. Update the TextViews
        tvVotedPercent.text = "$votedPercentage%"
        tvNotVotedPercent.text = "$notVotedPercentage%"
    }
}