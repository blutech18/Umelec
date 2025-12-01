package com.example.umelec

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException

/**
 * Helper class for Firebase Authentication operations
 */
object FirebaseAuthHelper {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private const val PREFS_NAME = "UMelecPrefs"
    private const val KEY_EMAIL = "user_email"
    private const val KEY_PASSWORD = "user_password"

    /**
     * Get current Firebase user
     */
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    /**
     * Check if user is logged in
     */
    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    /**
     * Sign in with email and password
     */
    fun signIn(
        email: String,
        password: String,
        onSuccess: (FirebaseUser) -> Unit,
        onFailure: (String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    if (user != null) {
                        onSuccess(user)
                    } else {
                        onFailure("Sign in failed: User is null")
                    }
                } else {
                    val errorMessage = task.exception?.message ?: "Sign in failed"
                    onFailure(errorMessage)
                }
            }
    }

    /**
     * Create a new user account with email and password
     */
    fun createUser(
        email: String,
        password: String,
        onSuccess: (FirebaseUser) -> Unit,
        onFailure: (String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    if (user != null) {
                        onSuccess(user)
                    } else {
                        onFailure("Registration failed: User is null")
                    }
                } else {
                    val errorMessage = task.exception?.message ?: "Registration failed"
                    onFailure(errorMessage)
                }
            }
    }

    /**
     * Send password reset email using Firebase's built-in email service
     * Note: Firebase will send an email even if the email doesn't exist (for security reasons)
     */
    fun sendPasswordResetEmail(
        email: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Success - email sent (even if email doesn't exist in Firebase for security)
                    onSuccess()
                } else {
                    val errorMessage = task.exception?.message ?: "Failed to send password reset email"
                    onFailure(errorMessage)
                }
            }
    }

    /**
     * Send password reset email using custom SMTP service via Firebase Cloud Functions
     * This method generates a password reset link and sends it via the custom email service
     * 
     * @param email User's email address
     * @param userName Optional: User's name for personalization
     * @param onSuccess Callback when email is sent successfully
     * @param onFailure Callback when email sending fails
     */
    fun sendPasswordResetEmailViaSMTP(
        email: String,
        userName: String? = null,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        // First, generate a password reset link using Firebase Auth
        // We'll use Firebase's sendPasswordResetEmail to generate the action code,
        // but we'll intercept it via Cloud Function to send via SMTP instead
        
        // Alternative: Use Cloud Function to generate reset link and send via SMTP
        // For now, we'll use the EmailService which calls a Cloud Function
        // that handles both link generation and SMTP sending
        
        // Get user data from Firestore to get userName if not provided
        if (userName == null) {
            // Try to find user by email in Firestore
            firestore.collection("users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener { documents ->
                    val userData = documents.documents.firstOrNull()?.data
                    val userNameFromDb = userData?.get("firstname") as? String
                    
                    // Generate reset link - we'll use a Cloud Function for this
                    // The Cloud Function will generate the reset link and send via SMTP
                    sendPasswordResetViaCloudFunction(email, userNameFromDb, onSuccess, onFailure)
                }
                .addOnFailureListener {
                    // If we can't get user data, proceed without userName
                    sendPasswordResetViaCloudFunction(email, null, onSuccess, onFailure)
                }
        } else {
            sendPasswordResetViaCloudFunction(email, userName, onSuccess, onFailure)
        }
    }
    
    /**
     * Helper method to send password reset via Cloud Function
     * The Cloud Function will generate the reset link using Firebase Admin SDK
     * and send it via SMTP
     */
    private fun sendPasswordResetViaCloudFunction(
        email: String,
        userName: String?,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        // Use EmailService which calls Cloud Function
        // The Cloud Function will handle generating the reset link using Firebase Admin SDK
        // and sending it via SMTP
        // Note: resetLink will be generated by the Cloud Function, so we pass empty string
        EmailService.sendPasswordResetEmail(
            email = email,
            resetLink = "", // Will be generated by Cloud Function
            userName = userName,
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }

    /**
     * Reset password using action code from email link
     * This is used when user clicks the reset link in their email
     */
    fun confirmPasswordReset(
        actionCode: String,
        newPassword: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        auth.confirmPasswordReset(actionCode, newPassword)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onSuccess()
                } else {
                    val errorMessage = task.exception?.message ?: "Failed to reset password"
                    onFailure(errorMessage)
                }
            }
    }

    /**
     * Verify password reset code (from email link)
     */
    fun verifyPasswordResetCode(
        actionCode: String,
        onSuccess: (String) -> Unit, // Returns the email associated with the code
        onFailure: (String) -> Unit
    ) {
        auth.verifyPasswordResetCode(actionCode)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val email = task.result
                    onSuccess(email)
                } else {
                    val errorMessage = task.exception?.message ?: "Invalid or expired reset code"
                    onFailure(errorMessage)
                }
            }
    }

    /**
     * Generate password reset action code via Cloud Function
     * This is used after verification code is verified to get a Firebase Auth action code
     * 
     * @param email User's email address
     * @param onSuccess Callback with the action code
     * @param onFailure Callback when generation fails
     */
    fun generatePasswordResetActionCode(
        email: String,
        onSuccess: (String) -> Unit, // Returns the action code
        onFailure: (String) -> Unit
    ) {
        // Call Cloud Function to generate password reset action code
        val functions = FirebaseFunctions.getInstance("us-central1")
        val generateCodeFunction = functions.getHttpsCallable("generatePasswordResetCode")
        
        val data = hashMapOf("email" to email)
        
        generateCodeFunction.call(data)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val result = task.result?.getData() as? Map<*, *>
                    val actionCode = result?.get("actionCode") as? String
                    
                    if (actionCode != null && actionCode.isNotEmpty()) {
                        onSuccess(actionCode)
                    } else {
                        onFailure("Failed to generate password reset code: Invalid response")
                    }
                } else {
                    val exception = task.exception
                    val errorMessage = when {
                        exception is FirebaseFunctionsException -> {
                            when (exception.code) {
                                FirebaseFunctionsException.Code.NOT_FOUND -> 
                                    "User not found. Please check your email address."
                                FirebaseFunctionsException.Code.INVALID_ARGUMENT -> 
                                    exception.message ?: "Invalid email address"
                                else -> exception.message ?: "Failed to generate password reset code"
                            }
                        }
                        exception?.message != null -> exception.message!!
                        else -> "Failed to generate password reset code. Please try again."
                    }
                    onFailure(errorMessage)
                }
            }
    }

    /**
     * Sign out current user
     */
    fun signOut() {
        auth.signOut()
    }

    /**
     * Change password for the current user
     * Requires re-authentication with the current password before updating
     */
    fun changePassword(
        currentPassword: String,
        newPassword: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val user = auth.currentUser
        if (user == null) {
            onFailure("No user is currently signed in")
            return
        }

        val email = user.email
        if (email == null) {
            onFailure("User email not found")
            return
        }

        // Step 1: Re-authenticate with current password
        val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, currentPassword)
        user.reauthenticate(credential)
            .addOnCompleteListener { reauthTask ->
                if (reauthTask.isSuccessful) {
                    // Step 2: Update password after successful re-authentication
                    user.updatePassword(newPassword)
                        .addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                android.util.Log.d("FirebaseAuthHelper", "Password updated successfully")
                                onSuccess()
                            } else {
                                val errorMessage = updateTask.exception?.message ?: "Failed to update password"
                                android.util.Log.e("FirebaseAuthHelper", "Error updating password: $errorMessage")
                                onFailure(errorMessage)
                            }
                        }
                } else {
                    val errorMessage = reauthTask.exception?.message ?: "Failed to verify current password"
                    android.util.Log.e("FirebaseAuthHelper", "Error re-authenticating: $errorMessage")
                    
                    // Provide user-friendly error messages
                    val friendlyMessage = when {
                        errorMessage.contains("wrong-password", ignoreCase = true) ||
                        errorMessage.contains("invalid-credential", ignoreCase = true) ||
                        errorMessage.contains("invalid password", ignoreCase = true) ->
                            "Current password is incorrect"
                        errorMessage.contains("network", ignoreCase = true) ->
                            "Network error. Please check your internet connection and try again."
                        else -> "Failed to verify current password: $errorMessage"
                    }
                    onFailure(friendlyMessage)
                }
            }
    }

    /**
     * Delete the current user's Firebase Auth account
     * This is used to clean up incomplete registrations
     */
    fun deleteCurrentUser(
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val user = auth.currentUser
        if (user == null) {
            onFailure("No user is currently signed in")
            return
        }

        user.delete()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    android.util.Log.d("FirebaseAuthHelper", "User account deleted successfully")
                    onSuccess()
                } else {
                    val errorMessage = task.exception?.message ?: "Failed to delete user account"
                    android.util.Log.e("FirebaseAuthHelper", "Error deleting user: $errorMessage")
                    onFailure(errorMessage)
                }
            }
    }

    /**
     * Check if user registration is complete by checking Firestore
     */
    fun isRegistrationComplete(
        userId: String,
        onComplete: (Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        firestore.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val registrationCompleted = document.getBoolean("registrationCompleted") ?: false
                    onComplete(registrationCompleted)
                } else {
                    // No user document exists, registration is not complete
                    onComplete(false)
                }
            }
            .addOnFailureListener { exception ->
                android.util.Log.e("FirebaseAuthHelper", "Error checking registration status: ${exception.message}")
                onError(exception.message ?: "Failed to check registration status")
            }
    }

    /**
     * Save user data to Firestore
     * Uses merge option to avoid overwriting existing data
     */
    fun saveUserDataToFirestore(
        userId: String,
        userData: Map<String, Any>,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        firestore.collection("users")
            .document(userId)
            .set(userData, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                // Log the full error for debugging
                android.util.Log.e("FirebaseAuthHelper", "Error saving user data: ${exception.message}", exception)
                onFailure(exception.message ?: "Failed to save user data")
            }
    }

    /**
     * Update user data in Firestore
     */
    fun updateUserDataInFirestore(
        userId: String,
        userData: Map<String, Any>,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        firestore.collection("users")
            .document(userId)
            .update(userData)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onFailure(exception.message ?: "Failed to update user data")
            }
    }

    /**
     * Get user data from Firestore
     */
    fun getUserDataFromFirestore(
        userId: String,
        onSuccess: (Map<String, Any>?) -> Unit,
        onFailure: (String) -> Unit
    ) {
        firestore.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    onSuccess(document.data)
                } else {
                    onSuccess(null)
                }
            }
            .addOnFailureListener { exception ->
                onFailure(exception.message ?: "Failed to get user data")
            }
    }

    /**
     * Save email and password temporarily in SharedPreferences (for multi-step registration)
     */
    fun saveTemporaryCredentials(context: Context, email: String, password: String) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString(KEY_EMAIL, email)
        editor.putString(KEY_PASSWORD, password)
        editor.apply()
    }

    /**
     * Get temporary email from SharedPreferences
     */
    fun getTemporaryEmail(context: Context): String? {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_EMAIL, null)
    }

    /**
     * Get temporary password from SharedPreferences
     */
    fun getTemporaryPassword(context: Context): String? {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PASSWORD, null)
    }

    /**
     * Clear temporary credentials from SharedPreferences
     */
    fun clearTemporaryCredentials(context: Context) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.remove(KEY_EMAIL)
        editor.remove(KEY_PASSWORD)
        editor.apply()
    }

    /**
     * Check if email already exists (by checking if user can sign in)
     * Note: This is a workaround. In production, you might want to use Cloud Functions
     */
    fun checkEmailExists(
        email: String,
        onExists: () -> Unit,
        onNotExists: () -> Unit,
        onError: (String) -> Unit
    ) {
        // We can't directly check if email exists without trying to sign in
        // This is a limitation of Firebase Auth
        // For now, we'll just proceed and let Firebase handle the error during registration
        onNotExists()
    }
}

