package com.example.umelec

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import kotlin.random.Random

/**
 * Helper class for managing leader verification codes
 */
object LeaderVerificationHelper {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private const val VERIFICATION_CODES_COLLECTION = "leaderVerificationCodes"
    private const val TAG = "LeaderVerificationHelper"
    
    /**
     * Generate and send verification code to leader's email
     * @param email Leader's email address
     * @param onSuccess Callback when code is sent successfully
     * @param onFailure Callback when sending fails
     */
    fun sendVerificationCode(
        email: String,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        // Generate 6-digit verification code
        val verificationCode = generateVerificationCode()
        
        Log.d(TAG, "Generating verification code for leader: $email")
        
        // Store verification code in Firestore with expiration
        val codeData = hashMapOf<String, Any>(
            "code" to verificationCode,
            "email" to email,
            "createdAt" to com.google.firebase.Timestamp.now(),
            "expiresAt" to com.google.firebase.Timestamp(Date(System.currentTimeMillis() + 10 * 60 * 1000)), // 10 minutes
            "isUsed" to false,
            "type" to "LEADER_VERIFICATION"
        )
        
        // Use email as document ID for easy retrieval
        firestore.collection(VERIFICATION_CODES_COLLECTION)
            .document(email)
            .set(codeData)
            .addOnSuccessListener {
                Log.d(TAG, "Verification code stored in Firestore for: $email")
                
                // Send email with verification code
                TriggerEmailHelper.sendLeaderVerificationCode(
                    email = email,
                    verificationCode = verificationCode,
                    onSuccess = {
                        Log.d(TAG, "Verification code email sent successfully to: $email")
                        onSuccess(verificationCode)
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to send verification code email: $error")
                        onFailure("Failed to send verification email: $error")
                    }
                )
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to store verification code: ${exception.message}")
                onFailure("Failed to generate verification code: ${exception.message}")
            }
    }
    
    /**
     * Verify the entered code against the stored code
     * @param email Leader's email address
     * @param enteredCode Code entered by the user
     * @param onSuccess Callback when verification succeeds
     * @param onFailure Callback when verification fails
     */
    fun verifyCode(
        email: String,
        enteredCode: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        Log.d(TAG, "Verifying code for leader: $email")
        
        firestore.collection(VERIFICATION_CODES_COLLECTION)
            .document(email)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    Log.w(TAG, "No verification code found for: $email")
                    onFailure("No verification code found. Please request a new code.")
                    return@addOnSuccessListener
                }
                
                val data = document.data!!
                val storedCode = data["code"] as? String
                val isUsed = data["isUsed"] as? Boolean ?: false
                val expiresAt = data["expiresAt"] as? com.google.firebase.Timestamp
                
                when {
                    isUsed -> {
                        Log.w(TAG, "Verification code already used for: $email")
                        onFailure("This verification code has already been used. Please request a new code.")
                    }
                    expiresAt != null && expiresAt.toDate().before(Date()) -> {
                        Log.w(TAG, "Verification code expired for: $email")
                        onFailure("Verification code has expired. Please request a new code.")
                    }
                    storedCode != enteredCode -> {
                        Log.w(TAG, "Invalid verification code entered for: $email")
                        onFailure("Invalid verification code. Please check and try again.")
                    }
                    else -> {
                        Log.d(TAG, "Verification code verified successfully for: $email")
                        
                        // Mark code as used
                        markCodeAsUsed(email) {
                            // Update user verification status in Firestore
                            updateLeaderVerificationStatus(email, onSuccess, onFailure)
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error verifying code: ${exception.message}")
                onFailure("Error verifying code: ${exception.message}")
            }
    }
    
    /**
     * Mark verification code as used
     */
    private fun markCodeAsUsed(email: String, onComplete: () -> Unit) {
        firestore.collection(VERIFICATION_CODES_COLLECTION)
            .document(email)
            .update("isUsed", true)
            .addOnSuccessListener {
                Log.d(TAG, "Verification code marked as used for: $email")
                onComplete()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to mark code as used: ${exception.message}")
                onComplete() // Continue anyway
            }
    }
    
    /**
     * Update leader's verification status in users collection
     */
    private fun updateLeaderVerificationStatus(
        email: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        // Get current user UID
        val currentUser = FirebaseAuthHelper.getCurrentUser()
        if (currentUser == null) {
            onFailure("User not authenticated")
            return
        }
        
        // Update isVerified field in users collection
        firestore.collection("users")
            .document(currentUser.uid)
            .update("isVerified", true)
            .addOnSuccessListener {
                Log.d(TAG, "Leader verification status updated for: $email")
                onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to update verification status: ${exception.message}")
                onFailure("Failed to update verification status: ${exception.message}")
            }
    }
    
    /**
     * Generate a 6-digit verification code
     */
    private fun generateVerificationCode(): String {
        return Random.nextInt(100000, 999999).toString()
    }
    
    /**
     * Check if a leader needs verification
     * @param email Leader's email
     * @param onResult Callback with verification status (true if needs verification)
     */
    fun checkVerificationStatus(
        email: String,
        onResult: (Boolean) -> Unit
    ) {
        val currentUser = FirebaseAuthHelper.getCurrentUser()
        if (currentUser == null) {
            onResult(false)
            return
        }
        
        FirebaseAuthHelper.getUserDataFromFirestore(
            userId = currentUser.uid,
            onSuccess = { userData ->
                val role = userData?.get("role") as? String
                val isVerified = userData?.get("isVerified") as? Boolean ?: false
                
                // Leader needs verification if role is LEADER and not yet verified
                val needsVerification = (role == "LEADER" && !isVerified)
                onResult(needsVerification)
            },
            onFailure = {
                onResult(false)
            }
        )
    }
}
