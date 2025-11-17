package com.example.umelec

import android.content.Context
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableResult
import org.json.JSONObject

/**
 * EmailService - Reusable SMTP email service for sending various types of emails
 * 
 * This service communicates with Firebase Cloud Functions to send emails via SMTP.
 * It supports multiple email types: password reset, verification, notifications, etc.
 */
object EmailService {
    // Use us-central1 region to match Cloud Functions deployment
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("us-central1")
    
    // Email types supported by the service
    enum class EmailType {
        PASSWORD_RESET,
        EMAIL_VERIFICATION,
        WELCOME,
        NOTIFICATION,
        VOTE_CONFIRMATION,
        ELECTION_REMINDER,
        CUSTOM
    }
    
    /**
     * Email request data class
     */
    data class EmailRequest(
        val to: String,
        val emailType: EmailType,
        val subject: String? = null, // Optional: custom subject, otherwise uses template default
        val templateData: Map<String, Any> = emptyMap(), // Data to populate template variables
        val customHtml: String? = null, // Optional: custom HTML body (for CUSTOM type)
        val customText: String? = null // Optional: custom text body (for CUSTOM type)
    )
    
    /**
     * Send email using Firebase Cloud Functions
     * 
     * @param request EmailRequest containing email details
     * @param onSuccess Callback when email is sent successfully
     * @param onFailure Callback when email sending fails
     */
    fun sendEmail(
        request: EmailRequest,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        try {
            // Validate request before building payload
            if (request.to.isBlank()) {
                onFailure("Email address is required")
                return
            }
            
            // Build the request payload
            val payload = buildEmailPayload(request)
            
            // Validate payload has required fields (double check before sending)
            val toValue = payload["to"] as? String ?: ""
            val emailTypeValue = payload["emailType"] as? String ?: ""
            
            if (toValue.isBlank()) {
                onFailure("Email address is missing or empty in payload")
                return
            }
            
            if (emailTypeValue.isBlank()) {
                onFailure("Email type is missing or empty in payload")
                return
            }
            
            // Call the Firebase Cloud Function
            val sendEmailFunction = functions.getHttpsCallable("sendEmail")
            
            // Send the payload - Firebase Functions will serialize HashMap to JSON
            // The payload must be a Map<String, Any> for proper serialization
            sendEmailFunction.call(payload as Map<String, Any>)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val result = task.result?.getData() as? Map<*, *>
                        val message = result?.get("message") as? String 
                            ?: "Email sent successfully"
                        onSuccess(message)
                    } else {
                        val exception = task.exception
                        
                        // Extract error message with more detail
                        val errorMessage = when {
                            exception is com.google.firebase.functions.FirebaseFunctionsException -> {
                                // Get the detailed error message from the Cloud Function
                                val detailMessage = exception.message ?: "Unknown error"
                                
                                when (exception.code) {
                                    com.google.firebase.functions.FirebaseFunctionsException.Code.NOT_FOUND -> 
                                        "Email service not found. Please contact support."
                                    com.google.firebase.functions.FirebaseFunctionsException.Code.UNAVAILABLE -> 
                                        "Email service unavailable. Please check your internet connection and try again."
                                    com.google.firebase.functions.FirebaseFunctionsException.Code.PERMISSION_DENIED -> 
                                        "Permission denied. Please contact support."
                                    com.google.firebase.functions.FirebaseFunctionsException.Code.INVALID_ARGUMENT -> 
                                        // Use the detailed message from the Cloud Function for invalid arguments
                                        detailMessage
                                    com.google.firebase.functions.FirebaseFunctionsException.Code.INTERNAL -> 
                                        "Internal server error. Please try again later."
                                    else -> detailMessage
                                }
                            }
                            exception?.message != null -> exception.message!!
                            else -> "Failed to send email. Please try again."
                        }
                        onFailure(errorMessage)
                    }
                }
        } catch (e: Exception) {
            onFailure("Error preparing email request: ${e.message}")
        }
    }
    
    /**
     * Send password reset email
     * Convenience method for password reset functionality
     * 
     * Note: If resetLink is empty, the Cloud Function will generate it automatically
     * using Firebase Admin SDK. Otherwise, the provided resetLink will be used.
     * 
     * @param email Recipient email address
     * @param resetLink Password reset link (optional - will be generated by Cloud Function if empty)
     * @param userName Optional: User's name for personalization
     * @param onSuccess Callback when email is sent successfully
     * @param onFailure Callback when email sending fails
     */
    fun sendPasswordResetEmail(
        email: String,
        resetLink: String = "",
        userName: String? = null,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val templateData = mutableMapOf<String, Any>()
        
        // Only add resetLink if provided (otherwise Cloud Function will generate it)
        if (resetLink.isNotEmpty()) {
            templateData["resetLink"] = resetLink
        }
        
        userName?.let {
            templateData["userName"] = it
        }
        
        val request = EmailRequest(
            to = email,
            emailType = EmailType.PASSWORD_RESET,
            templateData = templateData
        )
        
        sendEmail(
            request = request,
            onSuccess = { onSuccess() },
            onFailure = onFailure
        )
    }
    
    /**
     * Send email verification email
     * 
     * @param email Recipient email address
     * @param verificationLink Email verification link
     * @param userName Optional: User's name for personalization
     * @param onSuccess Callback when email is sent successfully
     * @param onFailure Callback when email sending fails
     */
    fun sendEmailVerification(
        email: String,
        verificationLink: String,
        userName: String? = null,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val templateData = mutableMapOf<String, Any>(
            "verificationLink" to verificationLink
        )
        
        userName?.let {
            templateData["userName"] = it
        }
        
        val request = EmailRequest(
            to = email,
            emailType = EmailType.EMAIL_VERIFICATION,
            templateData = templateData
        )
        
        sendEmail(
            request = request,
            onSuccess = { onSuccess() },
            onFailure = onFailure
        )
    }
    
    /**
     * Send welcome email
     * 
     * @param email Recipient email address
     * @param userName User's name
     * @param onSuccess Callback when email is sent successfully
     * @param onFailure Callback when email sending fails
     */
    fun sendWelcomeEmail(
        email: String,
        userName: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val request = EmailRequest(
            to = email,
            emailType = EmailType.WELCOME,
            templateData = mapOf("userName" to userName)
        )
        
        sendEmail(
            request = request,
            onSuccess = { onSuccess() },
            onFailure = onFailure
        )
    }
    
    /**
     * Send notification email
     * 
     * @param email Recipient email address
     * @param title Notification title
     * @param message Notification message
     * @param userName Optional: User's name for personalization
     * @param onSuccess Callback when email is sent successfully
     * @param onFailure Callback when email sending fails
     */
    fun sendNotificationEmail(
        email: String,
        title: String,
        message: String,
        userName: String? = null,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val templateData = mutableMapOf<String, Any>(
            "title" to title,
            "message" to message
        )
        
        userName?.let {
            templateData["userName"] = it
        }
        
        val request = EmailRequest(
            to = email,
            emailType = EmailType.NOTIFICATION,
            subject = title,
            templateData = templateData
        )
        
        sendEmail(
            request = request,
            onSuccess = { onSuccess() },
            onFailure = onFailure
        )
    }
    
    /**
     * Send vote confirmation email
     * 
     * @param email Recipient email address
     * @param userName User's name
     * @param voteDetails Details about the vote cast
     * @param onSuccess Callback when email is sent successfully
     * @param onFailure Callback when email sending fails
     */
    fun sendVoteConfirmationEmail(
        email: String,
        userName: String,
        voteDetails: Map<String, Any>,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val templateData = mutableMapOf<String, Any>(
            "userName" to userName
        )
        templateData.putAll(voteDetails)
        
        val request = EmailRequest(
            to = email,
            emailType = EmailType.VOTE_CONFIRMATION,
            templateData = templateData
        )
        
        sendEmail(
            request = request,
            onSuccess = { onSuccess() },
            onFailure = onFailure
        )
    }
    
    /**
     * Send password reset verification code
     * 
     * @param email Recipient email address
     * @param verificationCode 6-digit verification code
     * @param userName Optional: User's name for personalization
     * @param onSuccess Callback when email is sent successfully
     * @param onFailure Callback when email sending fails
     */
    fun sendPasswordResetCode(
        email: String,
        verificationCode: String,
        userName: String? = null,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val htmlBody = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Password Reset Verification Code</title>
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;">
                <div style="background-color: #00537A; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0;">
                    <h1 style="margin: 0;">UMelec</h1>
                </div>
                <div style="background-color: #f9f9f9; padding: 30px; border-radius: 0 0 5px 5px;">
                    <h2 style="color: #00537A;">Password Reset Verification Code</h2>
                    ${if (userName != null) "<p>Hello $userName,</p>" else "<p>Hello,</p>"}
                    <p>You have requested to reset your password for your UMelec account.</p>
                    <p>Please use the following verification code to proceed:</p>
                    <div style="background-color: white; padding: 20px; border: 2px solid #00537A; border-radius: 5px; text-align: center; margin: 30px 0;">
                        <h1 style="color: #00537A; font-size: 36px; letter-spacing: 8px; margin: 0;">$verificationCode</h1>
                    </div>
                    <p style="color: #666; font-size: 12px;">
                        <strong>Note:</strong> This code will expire in 10 minutes. If you didn't request this, 
                        please ignore this email or contact support.
                    </p>
                    <hr style="border: none; border-top: 1px solid #ddd; margin: 20px 0;">
                    <p style="color: #666; font-size: 12px; text-align: center;">
                        This is an automated message from UMelec. Please do not reply to this email.
                    </p>
                </div>
            </body>
            </html>
        """.trimIndent()
        
        val textBody = """
            Password Reset Verification Code - UMelec
            
            ${if (userName != null) "Hello $userName," else "Hello,"}
            
            You have requested to reset your password for your UMelec account.
            
            Please use the following verification code to proceed:
            $verificationCode
            
            Note: This code will expire in 10 minutes. If you didn't request this, 
            please ignore this email or contact support.
            
            This is an automated message from UMelec. Please do not reply to this email.
        """.trimIndent()
        
        sendCustomEmail(
            email = email,
            subject = "Password Reset Verification Code - UMelec",
            htmlBody = htmlBody,
            textBody = textBody,
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }
    
    /**
     * Send custom email
     * 
     * @param email Recipient email address
     * @param subject Email subject
     * @param htmlBody HTML email body
     * @param textBody Optional: Plain text email body
     * @param onSuccess Callback when email is sent successfully
     * @param onFailure Callback when email sending fails
     */
    fun sendCustomEmail(
        email: String,
        subject: String,
        htmlBody: String,
        textBody: String? = null,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val request = EmailRequest(
            to = email,
            emailType = EmailType.CUSTOM,
            subject = subject,
            customHtml = htmlBody,
            customText = textBody
        )
        
        sendEmail(
            request = request,
            onSuccess = { onSuccess() },
            onFailure = onFailure
        )
    }
    
    /**
     * Build email payload for Firebase Cloud Function
     * Returns a HashMap that Firebase Functions can properly serialize
     */
    private fun buildEmailPayload(request: EmailRequest): HashMap<String, Any> {
        // Always include required fields first - ensure they are non-null strings
        val emailTo = request.to.trim()
        val emailTypeStr = request.emailType.name
        
        if (emailTo.isEmpty()) {
            throw IllegalArgumentException("Email address cannot be empty")
        }
        
        if (emailTypeStr.isEmpty()) {
            throw IllegalArgumentException("Email type cannot be empty")
        }
        
        // Build payload as HashMap - Firebase Functions can serialize this properly
        val payload = HashMap<String, Any>()
        payload["to"] = emailTo
        payload["emailType"] = emailTypeStr
        
        // Add optional fields only if they have values
        request.subject?.takeIf { it.isNotBlank() }?.let {
            payload["subject"] = it
        }
        
        if (request.templateData.isNotEmpty()) {
            payload["templateData"] = request.templateData
        }
        
        request.customHtml?.takeIf { it.isNotBlank() }?.let {
            payload["customHtml"] = it
        }
        
        request.customText?.takeIf { it.isNotBlank() }?.let {
            payload["customText"] = it
        }
        
        return payload
    }
}

