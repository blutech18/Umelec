package com.example.umelec

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.textfield.TextInputEditText

// 💡 NEW IMPORTS for Keyboard and Focus Management and TextWatcher
import android.content.Context
import android.view.inputmethod.InputMethodManager
import android.view.MotionEvent
import android.graphics.Rect
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import com.google.android.material.textfield.TextInputLayout // Need this for the boxStrokeColor change
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// 1. Data Class to represent a Voter
data class Voter(
    val id: String,
    val name: String,
    val year: String,
    val hasVoted: Boolean
)

class Leader_manage_voters_list : AppCompatActivity() {

    // Define color constant (AS IS from Forgotpassword.kt)
    private val COLOR_PRIMARY_BLUE = Color.parseColor("#00537A")

    // 2. References to Views (using lateinit for simplicity)
    private lateinit var btnBack: ImageButton
    private lateinit var tvVoted: TextView
    private lateinit var tvNotVoted: TextView
    private lateinit var inputVoter: TextInputEditText
    private lateinit var btnSearch: AppCompatButton
    private lateinit var voterItemLayout: LinearLayout
    private lateinit var voterLoadingGroup: LinearLayout
    // Added reference to the TextInputLayout to change its outline color
    private lateinit var layoutSearchVoters: TextInputLayout


    // Voter data
    private var allVoters = emptyList<Voter>()
    private var currentElectionId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leader_manage_voters_list)

        // Initialize Views
        btnBack = findViewById(R.id.btnBack)
        tvVoted = findViewById(R.id.tvVoted)
        tvNotVoted = findViewById(R.id.tvNotVoted)
        inputVoter = findViewById(R.id.inputVoter)
        btnSearch = findViewById(R.id.btnSearch)
        voterItemLayout = findViewById(R.id.VoterItemLayout)
        voterLoadingGroup = findViewById(R.id.voterLoadingGroup)
        // Initialize the TextInputLayout reference
        layoutSearchVoters = findViewById(R.id.SearchVoters)

        // Set up initial UI and listeners
        setupListeners()
        // Load voters from Firestore
        loadVoters()
    }

    // =========================================================================
    // KEYBOARD AND FOCUS HELPER (AS IS from Forgotpassword.kt)
    // =========================================================================
    private fun hideKeyboardAndClearFocus() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        currentFocus?.clearFocus()
    }

    private fun setupListeners() {
        // Function 1: Handle Back Button Click
        btnBack.setOnClickListener {
            finish()
        }

        // Function 4: Handle Search Button Click
        btnSearch.setOnClickListener {
            val query = inputVoter.text.toString().trim()
            if (query.isNotEmpty()) {
                searchVoters(query)
                hideKeyboardAndClearFocus() // Hide keyboard after search
            } else {
                // If search box is empty, show the full list again
                inflateVoterList(allVoters)
                updateVoterCounts(allVoters)
                hideKeyboardAndClearFocus() // Hide keyboard if user clicks search on empty field
            }
        }

        // ---------------------------------------------------------------------
        // 💡 NEW LOGIC: TextWatcher for Auto-Reset
        // ---------------------------------------------------------------------
        inputVoter.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // When text is cleared, reset the voter list to default/all voters
                if (s.isNullOrEmpty()) {
                    inflateVoterList(allVoters)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // ---------------------------------------------------------------------
        // 💡 NEW LOGIC: Focus Change Listener for Outline Color
        // ---------------------------------------------------------------------
        inputVoter.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                // Set outline color to COLOR_PRIMARY_BLUE when focused
                layoutSearchVoters.boxStrokeColor = COLOR_PRIMARY_BLUE
            } else {
                // Reset outline color when unfocused (using your default gray/hint color)
                // You must know the default color of your boxStrokeColor here.
                // Assuming your default color is the one used for startIconTint (#8C8CA1)
                layoutSearchVoters.boxStrokeColor = Color.parseColor("#8C8CA1")
            }
        }
    }

    /**
     * Load voters from Firestore (filtered by college)
     */
    private fun loadVoters() {
        android.util.Log.d("Leader_manage_voters_list", "Loading voters...")
        
        // Get current user's college first
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            android.util.Log.e("Leader_manage_voters_list", "User not authenticated")
            voterLoadingGroup.visibility = View.GONE
            voterItemLayout.visibility = View.VISIBLE
            allVoters = emptyList()
            updateVoterCounts(allVoters)
            inflateVoterList(allVoters)
            return
        }

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { userDoc ->
                if (!userDoc.exists()) {
                    android.util.Log.e("Leader_manage_voters_list", "User profile not found")
                    voterLoadingGroup.visibility = View.GONE
                    voterItemLayout.visibility = View.VISIBLE
                    allVoters = emptyList()
                    updateVoterCounts(allVoters)
                    inflateVoterList(allVoters)
                    return@addOnSuccessListener
                }

                val userCollege = userDoc.getString("college") ?: ""
                if (userCollege.isEmpty()) {
                    android.util.Log.e("Leader_manage_voters_list", "User college information not found")
                    voterLoadingGroup.visibility = View.GONE
                    voterItemLayout.visibility = View.VISIBLE
                    allVoters = emptyList()
                    updateVoterCounts(allVoters)
                    inflateVoterList(allVoters)
                    return@addOnSuccessListener
                }

                android.util.Log.d("Leader_manage_voters_list", "Loading voters for college: $userCollege")
                
                // Get current election ID
                FirestoreElectionHelper.getCurrentElectionId(
                    onSuccess = { electionId ->
                        android.util.Log.d("Leader_manage_voters_list", "Current election ID: $electionId")
                        currentElectionId = electionId
                        if (electionId != null) {
                            // Load voters filtered by college
                            FirestoreVoterHelper.getVotersByCollege(
                                college = userCollege,
                                onSuccess = { votersData ->
                            android.util.Log.d("Leader_manage_voters_list", "Loaded ${votersData.size} voters from Firestore")
                            // Load voters who have voted
                            FirestoreVoterHelper.getVotersWhoVoted(
                                electionId = electionId,
                                onSuccess = { votedVoters ->
                                    android.util.Log.d("Leader_manage_voters_list", "Found ${votedVoters.size} voters who voted")
                                    val votedIds = votedVoters.mapNotNull { it["userId"] as? String }.toSet()
                                    android.util.Log.d("Leader_manage_voters_list", "Voted user IDs: $votedIds")
                                    
                                    // Convert to Voter data class
                                    allVoters = votersData.map { voterData ->
                                        val userId = voterData["userId"] as? String ?: ""
                                        val firstName = voterData["firstname"] as? String ?: ""
                                        val lastName = voterData["lastname"] as? String ?: ""
                                        val name = "$firstName $lastName".trim()
                                        val yearRaw = voterData["year"] as? String ?: "Unknown"
                                        // Normalize year format: "2nd Year" -> "2nd"
                                        val year = yearRaw.replace(" Year", "").trim()
                                        val hasVoted = userId in votedIds
                                        
                                        android.util.Log.d("Leader_manage_voters_list", "Voter: $name (ID: $userId) - Voted: $hasVoted")
                                        Voter(userId, name, year, hasVoted)
                                    }
                                    
                                    android.util.Log.d("Leader_manage_voters_list", "Total voters: ${allVoters.size}, Voted: ${allVoters.count { it.hasVoted }}")
                                    voterLoadingGroup.visibility = View.GONE
                                    voterItemLayout.visibility = View.VISIBLE
                                    updateVoterCounts(allVoters)
                                    inflateVoterList(allVoters)
                                },
                                onFailure = { error ->
                                    android.util.Log.e("Leader_manage_voters_list", "Error getting voted voters: $error")
                                    // Still show all voters even if voted check fails
                                    allVoters = votersData.map { voterData ->
                                        val userId = voterData["userId"] as? String ?: ""
                                        val firstName = voterData["firstname"] as? String ?: ""
                                        val lastName = voterData["lastname"] as? String ?: ""
                                        val name = "$firstName $lastName".trim()
                                        val yearRaw = voterData["year"] as? String ?: "Unknown"
                                        val year = yearRaw.replace(" Year", "").trim()
                                        Voter(userId, name, year, false)
                                    }
                                    voterLoadingGroup.visibility = View.GONE
                                    voterItemLayout.visibility = View.VISIBLE
                                    updateVoterCounts(allVoters)
                                    inflateVoterList(allVoters)
                                }
                            )
                        },
                        onFailure = { error ->
                            android.util.Log.e("Leader_manage_voters_list", "Error loading voters: $error")
                            voterLoadingGroup.visibility = View.GONE
                            voterItemLayout.visibility = View.VISIBLE
                            allVoters = emptyList()
                            updateVoterCounts(allVoters)
                            inflateVoterList(allVoters)
                        }
                    )
                } else {
                    // No active election
                    android.util.Log.w("Leader_manage_voters_list", "No active election found")
                    voterLoadingGroup.visibility = View.GONE
                    voterItemLayout.visibility = View.VISIBLE
                    allVoters = emptyList()
                    updateVoterCounts(allVoters)
                    inflateVoterList(allVoters)
                }
            },
            onFailure = { error ->
                android.util.Log.e("Leader_manage_voters_list", "Error getting election ID: $error")
                voterLoadingGroup.visibility = View.GONE
                voterItemLayout.visibility = View.VISIBLE
                allVoters = emptyList()
                updateVoterCounts(allVoters)
                inflateVoterList(allVoters)
            }
        )
            }
            .addOnFailureListener { exception ->
                android.util.Log.e("Leader_manage_voters_list", "Error getting user profile: ${exception.message}")
                voterLoadingGroup.visibility = View.GONE
                voterItemLayout.visibility = View.VISIBLE
                allVoters = emptyList()
                updateVoterCounts(allVoters)
                inflateVoterList(allVoters)
            }
    }

    // Function 2 & 3: Update Voted/Unvoted Counts
    private fun updateVoterCounts(voters: List<Voter>) {
        val votedCount = voters.count { it.hasVoted }
        val unvotedCount = voters.size - votedCount

        android.util.Log.d("Leader_manage_voters_list", "Updating counts: Total=${voters.size}, Voted=$votedCount, Unvoted=$unvotedCount")
        tvVoted.text = votedCount.toString()
        tvNotVoted.text = unvotedCount.toString()
    }

    // Function 5: Inflate the Voter List
    private fun inflateVoterList(votersToDisplay: List<Voter>) {
        android.util.Log.d("Leader_manage_voters_list", "Inflating voter list: ${votersToDisplay.size} voters")
        // Clear existing views before adding new ones
        voterItemLayout.removeAllViews()

        if (votersToDisplay.isEmpty()) {
            android.util.Log.w("Leader_manage_voters_list", "No voters to display")
            // Show empty state message
            val emptyTextView = TextView(this).apply {
                text = "No voters found."
                textSize = 14f
                setTextColor(Color.parseColor("#666666"))
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                val paddingPx = (40 * resources.displayMetrics.density).toInt()
                setPadding(0, paddingPx, 0, paddingPx)
                typeface = ResourcesCompat.getFont(this@Leader_manage_voters_list, R.font.poppins_regular)
            }
            voterItemLayout.addView(emptyTextView)
            return
        }

        val inflater = LayoutInflater.from(this)

        votersToDisplay.forEach { voter ->
            // Inflate the item_voter.xml layout
            val itemView = inflater.inflate(R.layout.item_voter, voterItemLayout, false)

            // Get references to the TextViews and ImageView in the inflated layout
            val tvVoterName = itemView.findViewById<TextView>(R.id.tvVoterName)
            val tvVoterID = itemView.findViewById<TextView>(R.id.tvVoterID)
            val tvVoterYear = itemView.findViewById<TextView>(R.id.tvVoterYear)
            val tvVotedStatus = itemView.findViewById<TextView>(R.id.tvVotedStatus)
            val ivVotedCheck = itemView.findViewById<ImageView>(R.id.ivVotedCheck)

            // Set the data
            tvVoterName.text = voter.name
            tvVoterID.text = voter.id
            tvVoterYear.text = voter.year

            // Handle Voted Status Logic
            if (voter.hasVoted) {
                tvVotedStatus.text = "Voted"
                ivVotedCheck.setImageResource(R.drawable.ic_toast_check)
            } else {
                tvVotedStatus.text = "Unvoted"
                ivVotedCheck.setImageResource(R.drawable.ic_toast_error)
            }

            // Add the fully configured view to the container
            voterItemLayout.addView(itemView)
        }
        android.util.Log.d("Leader_manage_voters_list", "Voter list inflation complete")
    }

    // Function 6: Search Logic (Client-side filtering simulation)
    private fun searchVoters(query: String) {
        val normalizedQuery = query.trim().lowercase()

        // Filter the list based on name or ID containing the query
        val filteredList = allVoters.filter { voter ->
            voter.name.lowercase().contains(normalizedQuery) ||
                    voter.id.lowercase().contains(normalizedQuery)
        }

        // Re-inflate the list with the filtered results
        inflateVoterList(filteredList)
    }

    // =========================================================================
    // DISPATCH TOUCH EVENT (CLICK OUTSIDE TO UNFOCUS/HIDE KEYBOARD) (AS IS from Forgotpassword.kt)
    // =========================================================================
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            // Only proceed if the current focus is a TextInputEditText
            if (v is TextInputEditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)

                // Check if the click coordinates are outside the TextInputEditText bounds
                if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    hideKeyboardAndClearFocus()
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}