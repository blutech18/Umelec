package com.example.umelec

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Helper class for Leader-specific Firestore operations
 */
object FirestoreLeaderHelper {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private const val ELECTIONS_COLLECTION = "elections"
    private const val POSITIONS_COLLECTION = "positions"
    private const val TAG = "FirestoreLeaderHelper"

    /**
     * Create a new election for the current leader's college
     */
    fun createElection(
        title: String,
        startDate: Date,
        endDate: Date,
        isAbstainEnabled: Boolean,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        // Get current user's college information first
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            onFailure("User not authenticated")
            return
        }

        firestore.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { userDoc ->
                if (!userDoc.exists()) {
                    onFailure("User profile not found")
                    return@addOnSuccessListener
                }

                val userCollege = userDoc.getString("college") ?: ""
                val userAcronym = userDoc.getString("acronym") ?: ""

                if (userCollege.isEmpty()) {
                    onFailure("User college information not found")
                    return@addOnSuccessListener
                }

                // Check if there's already an active election for this college
                firestore.collection(ELECTIONS_COLLECTION)
                    .whereEqualTo("isActive", true)
                    .whereEqualTo("college", userCollege)
                    .limit(1)
                    .get()
                    .addOnSuccessListener { existingElections ->
                        if (!existingElections.isEmpty) {
                            onFailure("An active election already exists for $userCollege. Please end the current election first.")
                            return@addOnSuccessListener
                        }

                // Validate dates
                if (startDate.after(endDate)) {
                    onFailure("Start date must be before end date")
                    return@addOnSuccessListener
                }

                if (startDate.before(Date())) {
                    onFailure("Start date cannot be in the past")
                    return@addOnSuccessListener
                }

                        // Create election document with college information
                        val electionData = hashMapOf<String, Any>(
                            "title" to title,
                            "startDate" to Timestamp(startDate),
                            "endDate" to Timestamp(endDate),
                            "isActive" to true,
                            "isAbstainEnabled" to isAbstainEnabled,
                            "createdAt" to Timestamp.now(),
                            "status" to "pending", // pending, approved, active, ended
                            "college" to userCollege,
                            "acronym" to userAcronym,
                            "createdBy" to currentUser.uid
                        )

                        firestore.collection(ELECTIONS_COLLECTION)
                            .add(electionData)
                            .addOnSuccessListener { documentReference ->
                                Log.d(TAG, "Election created: ${documentReference.id}")
                                onSuccess(documentReference.id)
                            }
                            .addOnFailureListener { exception ->
                                Log.e(TAG, "Error creating election: ${exception.message}", exception)
                                onFailure(exception.message ?: "Failed to create election")
                            }
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "Error checking existing elections: ${exception.message}", exception)
                        onFailure(exception.message ?: "Failed to check existing elections")
                    }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting user profile: ${exception.message}", exception)
                onFailure(exception.message ?: "Failed to get user profile")
            }
    }

    /**
     * Get election by ID with full details
     */
    fun getElectionById(
        electionId: String,
        onSuccess: (Map<String, Any>?) -> Unit,
        onFailure: (String) -> Unit
    ) {
        firestore.collection(ELECTIONS_COLLECTION)
            .document(electionId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    onSuccess(document.data)
                } else {
                    onSuccess(null)
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting election: ${exception.message}", exception)
                onFailure(exception.message ?: "Failed to get election")
            }
    }

    /**
     * Add position to an election
     */
    fun addPosition(
        electionId: String,
        positionName: String,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val positionData = hashMapOf<String, Any>(
            "electionId" to electionId,
            "positionName" to positionName,
            "isActive" to true,
            "createdAt" to Timestamp.now()
        )

        firestore.collection(POSITIONS_COLLECTION)
            .add(positionData)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "Position added: ${documentReference.id}")
                onSuccess(documentReference.id)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error adding position: ${exception.message}", exception)
                onFailure(exception.message ?: "Failed to add position")
            }
    }

    /**
     * Get all positions for an election
     */
    fun getPositionsForElection(
        electionId: String,
        onSuccess: (List<Map<String, Any>>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        firestore.collection(POSITIONS_COLLECTION)
            .whereEqualTo("electionId", electionId)
            .whereEqualTo("isActive", true)
            .orderBy("positionName", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val positions = documents.documents.mapNotNull { doc ->
                    val data = doc.data?.toMutableMap() ?: return@mapNotNull null
                    data["positionId"] = doc.id
                    data
                }
                onSuccess(positions)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting positions: ${exception.message}", exception)
                onFailure(exception.message ?: "Failed to get positions")
            }
    }

    /**
     * Delete position
     */
    fun deletePosition(
        positionId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        firestore.collection(POSITIONS_COLLECTION)
            .document(positionId)
            .update("isActive", false)
            .addOnSuccessListener {
                Log.d(TAG, "Position deleted: $positionId")
                onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error deleting position: ${exception.message}", exception)
                onFailure(exception.message ?: "Failed to delete position")
            }
    }

    /**
     * Format date for display (using Philippines timezone)
     */
    fun formatDate(date: Date): String {
        val format = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        format.timeZone = TimeZone.getTimeZone("Asia/Manila")
        return format.format(date)
    }

    /**
     * Format time for display (using Philippines timezone)
     */
    fun formatTime(date: Date): String {
        val format = SimpleDateFormat("hh:mm a", Locale.getDefault())
        format.timeZone = TimeZone.getTimeZone("Asia/Manila")
        return format.format(date)
    }

    /**
     * Format date and time for display (using Philippines timezone)
     */
    fun formatDateTime(date: Date): String {
        val format = SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        format.timeZone = TimeZone.getTimeZone("Asia/Manila")
        return format.format(date)
    }
}

