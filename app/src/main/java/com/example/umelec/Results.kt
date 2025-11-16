package com.example.umelec

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import java.util.concurrent.TimeUnit
import java.util.Calendar


class Results : AppCompatActivity() {

    private lateinit var notificationManager: NotificationManager
    private var countDownTimer: CountDownTimer? = null
    private var candidateItemWidth = 0
    private var currentCandidateIndex = 0

    // Real data from Firestore
    private var leadingCandidates = emptyList<LeadingCandidate>()
    private var currentElectionId: String? = null
    private var resultCardState = ResultCardState.NO_ELECTION

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_results)

        notificationManager = NotificationManager(this)

        setupHeaderIcons()
        setupFooterNavigation()

        // Determine election state from Firestore
        determineElectionState()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }

    // ----------------------------------------------------------------------
    // --- UTILITY ---
    // ----------------------------------------------------------------------

    private fun Int.toPx(): Int = (this * resources.displayMetrics.density).toInt()

    // ----------------------------------------------------------------------
    // --- MAIN PHASE LOGIC ---
    // ----------------------------------------------------------------------

    /**
     * Determine election state from Firestore
     */
    private fun determineElectionState() {
        FirestoreElectionHelper.getCurrentElectionId(
            onSuccess = { electionId ->
                currentElectionId = electionId
                if (electionId != null) {
                    FirestoreElectionHelper.determineElectionState(
                        onSuccess = { state ->
                            resultCardState = when (state) {
                                ElectionState.UPCOMING -> ResultCardState.UPCOMING
                                ElectionState.ONGOING -> ResultCardState.ONGOING
                                ElectionState.ENDED -> ResultCardState.ENDED
                                ElectionState.NO_ELECTION -> ResultCardState.NO_ELECTION
                            }
                            updateUIForPhase(resultCardState)
                        },
                        onFailure = { error ->
                            android.util.Log.e("Results", "Error determining state: $error")
                            resultCardState = ResultCardState.NO_ELECTION
                            updateUIForPhase(resultCardState)
                        }
                    )
                } else {
                    resultCardState = ResultCardState.NO_ELECTION
                    updateUIForPhase(resultCardState)
                }
            },
            onFailure = { error ->
                android.util.Log.e("Results", "Error getting election ID: $error")
                resultCardState = ResultCardState.NO_ELECTION
                updateUIForPhase(resultCardState)
            }
        )
    }

    private fun updateUIForPhase(state: ResultCardState) {
        val timeCard: ConstraintLayout = findViewById(R.id.TimeCard)
        val candidatesPreviewCard: LinearLayout = findViewById(R.id.CandidatesPreviewCard)
        val talliesCard: LinearLayout = findViewById(R.id.TalliesCard)
        val resultsCard: LinearLayout = findViewById(R.id.ResultsCard)
        val receiptCard: LinearLayout = findViewById(R.id.ReceiptCard)

        timeCard.visibility = View.GONE
        candidatesPreviewCard.visibility = View.GONE
        talliesCard.visibility = View.GONE
        resultsCard.visibility = View.GONE
        receiptCard.visibility = View.GONE

        countDownTimer?.cancel()


        when (state) {
            ResultCardState.UPCOMING -> {
                timeCard.visibility = View.VISIBLE
                findViewById<TextView>(R.id.TimeTitle).text = "Next election starts in"
                setupCountdownForElection(isStartDate = true)
            }
            ResultCardState.ONGOING -> {
                timeCard.visibility = View.VISIBLE
                candidatesPreviewCard.visibility = View.VISIBLE
                talliesCard.visibility = View.VISIBLE

                findViewById<TextView>(R.id.TimeTitle).text = "Remaining time for the election"
                setupCountdownForElection(isStartDate = false)
                loadLeadingCandidates()
                setupTalliesCard(isFinal = false)
            }
            ResultCardState.ENDED -> {
                timeCard.visibility = View.VISIBLE
                talliesCard.visibility = View.VISIBLE
                resultsCard.visibility = View.VISIBLE
                receiptCard.visibility = View.VISIBLE

                findViewById<TextView>(R.id.TimeTitle).text = "Election has ended"
                updateTimerDisplay(0)

                setupTalliesCard(isFinal = true)
                setupResultsCard()
                setupReceiptCard()
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

    /**
     * Setup countdown based on election dates from Firestore
     */
    private fun setupCountdownForElection(isStartDate: Boolean) {
        FirestoreElectionHelper.getCurrentElection(
            onSuccess = { electionData ->
                if (electionData != null) {
                    // Get dates from Firestore
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("elections")
                        .document(currentElectionId ?: return@getCurrentElection)
                        .get()
                        .addOnSuccessListener { document ->
                            if (document.exists()) {
                                // Use DocumentSnapshot getTimestamp method instead of casting
                                val startDateTimestamp = document.getTimestamp("startDate")
                                val endDateTimestamp = document.getTimestamp("endDate")
                                
                                val startDate = startDateTimestamp?.toDate()
                                val endDate = endDateTimestamp?.toDate()
                                
                                val targetDate = if (isStartDate) startDate else endDate
                                if (targetDate != null) {
                                    setupCountdown(targetDate.time)
                                } else {
                                    updateTimerDisplay(0)
                                }
                            }
                        }
                        .addOnFailureListener { error ->
                            android.util.Log.e("Results", "Error getting election dates: $error")
                            updateTimerDisplay(0)
                        }
                } else {
                    updateTimerDisplay(0)
                }
            },
            onFailure = { error ->
                android.util.Log.e("Results", "Error fetching election: $error")
                updateTimerDisplay(0)
            }
        )
    }

    private fun setupCountdown(futureTimeMs: Long) {
        countDownTimer?.cancel()
        val timeRemaining = futureTimeMs - System.currentTimeMillis()

        if (timeRemaining <= 0) {
            updateTimerDisplay(0)
            return
        }
        // ... (CountDownTimer implementation remains the same) ...
        countDownTimer = object : CountDownTimer(timeRemaining, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                updateTimerDisplay(millisUntilFinished)
            }

            override fun onFinish() {
                updateTimerDisplay(0)
                if (resultCardState == ResultCardState.UPCOMING) {
                    resultCardState = ResultCardState.ONGOING
                    updateUIForPhase(resultCardState)
                    Toast.makeText(this@Results, "The election is now ONGOING!", Toast.LENGTH_LONG).show()
                } else if (resultCardState == ResultCardState.ONGOING) {
                    resultCardState = ResultCardState.ENDED
                    updateUIForPhase(resultCardState)
                    Toast.makeText(this@Results, "The election has ENDED!", Toast.LENGTH_LONG).show()
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
    // --- ONGOING PHASE CARD LOGIC ---
    // ----------------------------------------------------------------------

    /**
     * Load leading candidates from Firestore
     */
    private fun loadLeadingCandidates() {
        currentElectionId?.let { electionId ->
            FirestoreVoteHelper.getLeadingCandidates(
                electionId = electionId,
                limit = 3,
                onSuccess = { candidates ->
                    leadingCandidates = candidates
                    setupCandidatesPreviewCard()
                },
                onFailure = { error ->
                    android.util.Log.e("Results", "Error loading leading candidates: $error")
                    leadingCandidates = emptyList()
                    setupCandidatesPreviewCard()
                }
            )
        } ?: run {
            leadingCandidates = emptyList()
            setupCandidatesPreviewCard()
        }
    }

    private fun setupCandidatesPreviewCard() {
        val container: LinearLayout = findViewById(R.id.candidateListContainer)
        val cardLayout: LinearLayout = findViewById(R.id.CandidatesPreviewCard)

        cardLayout.post {
            val viewWidth = cardLayout.width
            val btnPrev: ImageButton = findViewById(R.id.btnPrevCandidate)
            val btnNext: ImageButton = findViewById(R.id.btnNextCandidate)

            val arrowWidth = btnPrev.width + btnNext.width + (8.toPx() * 2)

            candidateItemWidth = ((viewWidth - arrowWidth) / 2).coerceAtLeast(180.toPx())

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
            findViewById<ViewGroup>(R.id.candidateListContainer),
            false
        ) as LinearLayout

        cardView.layoutParams = LinearLayout.LayoutParams(
            candidateItemWidth.coerceAtLeast(180.toPx()),
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            rightMargin = 4.toPx()
            leftMargin = 4.toPx()
        }

        val profilePic = cardView.findViewById<ImageView>(R.id.candidatePhoto)
        val nameText = cardView.findViewById<TextView>(R.id.candidateName)
        val positionText = cardView.findViewById<TextView>(R.id.candidatePosition)
        val voteCountText = cardView.findViewById<TextView>(R.id.candidateVotecount)

        profilePic.setImageResource(candidate.profileResId)
        nameText.text = candidate.name
        positionText.text = candidate.position

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

    // ----------------------------------------------------------------------
    // --- RECEIPT CARD LOGIC (ENDED) ---
    // ----------------------------------------------------------------------

    /**
     * Helper to clear the receipt input fields.
     */
    private fun clearReceiptFields() {
        findViewById<EditText>(R.id.inputEmail).text.clear()
        findViewById<EditText>(R.id.inputSignatureSnippet).text.clear()
    }

    /**
     * Custom dialog for successful verification.
     */
    private fun showReceiptSuccessDialog() {
        val layoutInflater = LayoutInflater.from(this)
        // ⭐️ Assumes you have R.layout.custom_toast_success
        val dialogView = layoutInflater.inflate(R.layout.custom_toast_success, null)

        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
        val dialog = builder.create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setGravity(Gravity.CENTER)
        dialog.setCanceledOnTouchOutside(false)

        // ⭐️ Updated content for verification success
        dialogView.findViewById<TextView>(R.id.toast_title).text = "Verified!"
        dialogView.findViewById<TextView>(R.id.toast_value).text = "Ballot included in final count."

        val btnAction = dialogView.findViewById<Button>(R.id.btn_action)
        btnAction.text = "Ok"
        btnAction.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    /**
     * Custom dialog for verification failure (no match).
     */
    private fun showReceiptFailureDialog() {
        val layoutInflater = LayoutInflater.from(this)
        // ⭐️ Assumes you have R.layout.custom_toast_error
        val dialogView = layoutInflater.inflate(R.layout.custom_toast_error, null)

        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
        val dialog = builder.create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setGravity(Gravity.CENTER)
        dialog.setCanceledOnTouchOutside(false)

        // ⭐️ Updated content for no match failure
        dialogView.findViewById<TextView>(R.id.toast_title).text = "No Match Found."
        dialogView.findViewById<TextView>(R.id.toast_value).text = "Check codes or contact COSEL."

        // Use the close button to dismiss and clear fields
        dialogView.findViewById<ImageButton>(R.id.btn_close).setOnClickListener {
            dialog.dismiss()
            clearReceiptFields()
        }

        // NOTE: References to otpLayouts and red border logic are removed as they are specific
        // to an OTP input layout not available here.

        dialog.show()
    }

    /**
     * Sets up the receipt card logic, now using custom AlertDialogs.
     */
    private fun setupReceiptCard() {
        val inputEmail: EditText = findViewById(R.id.inputEmail)
        val inputSignatureSnippet: EditText = findViewById(R.id.inputSignatureSnippet)
        val btnSearch: AppCompatButton = findViewById(R.id.btnSearch)
        val codeContainer: LinearLayout = findViewById(R.id.Code)
        val signatureSnippetContainer: LinearLayout = findViewById(R.id.SignatureSnippet)

        codeContainer.visibility = View.GONE
        signatureSnippetContainer.visibility = View.GONE

        fun areInputsEmpty(): Boolean {
            return inputEmail.text.isNullOrBlank() && inputSignatureSnippet.text.isNullOrBlank()
        }

        val textWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (!areInputsEmpty()) {
                    codeContainer.visibility = View.GONE
                    signatureSnippetContainer.visibility = View.GONE
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        inputEmail.addTextChangedListener(textWatcher)
        inputSignatureSnippet.addTextChangedListener(textWatcher)

        // Search Button Logic (Updated to use Dialogs)
        btnSearch.setOnClickListener {
            val email = inputEmail.text.toString().trim()
            val signatureSnippet = inputSignatureSnippet.text.toString().trim()

            if (email.isEmpty() && signatureSnippet.isEmpty()) {
                // Keep as Toast for quick input prompt
                codeContainer.visibility = View.VISIBLE
                signatureSnippetContainer.visibility = View.VISIBLE
                Toast.makeText(this, "Please enter verification details.", Toast.LENGTH_SHORT).show()

            } else if (email.isNotEmpty() && signatureSnippet.isNotEmpty()) {
                // Verify receipt from Firestore
                verifyReceipt(email, signatureSnippet)
            } else {
                // Keep as Toast for quick input prompt
                Toast.makeText(this, "Please fill both Email and Signature fields for verification.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Verify vote receipt from Firestore
     */
    private fun verifyReceipt(email: String, signatureSnippet: String) {
        // Find user by email
        FirebaseAuthHelper.getCurrentUser()?.let { currentUser ->
            if (currentUser.email?.equals(email, ignoreCase = true) == true) {
                // Get user's vote receipt
                currentElectionId?.let { electionId ->
                    FirestoreVoteHelper.getVoteReceipt(
                        userId = currentUser.uid,
                        electionId = electionId,
                        onSuccess = { receipt ->
                            if (receipt != null) {
                                // Check if signature snippet matches
                                if (receipt.signaturePreview.contains(signatureSnippet, ignoreCase = true)) {
                                    showReceiptSuccessDialog()
                                } else {
                                    showReceiptFailureDialog()
                                }
                            } else {
                                showReceiptFailureDialog()
                            }
                        },
                        onFailure = { error ->
                            android.util.Log.e("Results", "Error verifying receipt: $error")
                            showReceiptFailureDialog()
                        }
                    )
                } ?: showReceiptFailureDialog()
            } else {
                // Email doesn't match current user - search by email in Firestore
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .whereEqualTo("email", email)
                    .limit(1)
                    .get()
                    .addOnSuccessListener { documents ->
                        if (documents.isEmpty) {
                            showReceiptFailureDialog()
                            return@addOnSuccessListener
                        }
                        val userId = documents.documents[0].id
                        currentElectionId?.let { electionId ->
                            FirestoreVoteHelper.getVoteReceipt(
                                userId = userId,
                                electionId = electionId,
                                onSuccess = { receipt ->
                                    if (receipt != null && receipt.signaturePreview.contains(signatureSnippet, ignoreCase = true)) {
                                        showReceiptSuccessDialog()
                                    } else {
                                        showReceiptFailureDialog()
                                    }
                                },
                                onFailure = { error ->
                                    showReceiptFailureDialog()
                                }
                            )
                        } ?: showReceiptFailureDialog()
                    }
                    .addOnFailureListener {
                        showReceiptFailureDialog()
                    }
            }
        } ?: showReceiptFailureDialog()
    }

    // ----------------------------------------------------------------------
    // --- EXISTING LOGIC (HEADER, FOOTER) ---
    // ----------------------------------------------------------------------

    private fun setupHeaderIcons() {
        val profileIcon: ImageView? = findViewById(R.id.profileIcon)
        val notificationIcon: ImageView? = findViewById(R.id.notificationIcon)

        profileIcon?.setOnClickListener {
            val intent = Intent(this, Profile::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        notificationIcon?.setOnClickListener {
            if (it is ImageView) {
                if (::notificationManager.isInitialized) {
                    notificationManager.toggleNotificationDropdown(it)
                }
            }
        }
    }

    private fun setupFooterNavigation() {
        val navHome: LinearLayout? = findViewById(R.id.nav_home)
        val navVote: LinearLayout? = findViewById(R.id.nav_vote)
        val navCandidates: LinearLayout? = findViewById(R.id.nav_candidates)
        val navResults: LinearLayout? = findViewById(R.id.nav_results)
        val navFaq: LinearLayout? = findViewById(R.id.nav_faq)

        val navigateTo = { activityClass: Class<*> ->
            if (activityClass != this::class.java) {
                val intent = Intent(this, activityClass)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(intent)
                overridePendingTransition(0, 0)
            }
        }

        navHome?.setOnClickListener { navigateTo(Homepage::class.java) }
        navVote?.setOnClickListener { navigateTo(Vote::class.java) }
        navCandidates?.setOnClickListener { navigateTo(Candidates::class.java) }
        navResults?.setOnClickListener { /* Already here, do nothing */ }
        navFaq?.setOnClickListener { navigateTo(Faq::class.java) }
    }
}
