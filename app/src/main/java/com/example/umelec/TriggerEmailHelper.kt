package com.example.umelec

import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

/**
 * Helper class for sending emails using Firebase Trigger Email extension
 * 
 * The Trigger Email extension watches the 'mail' collection in Firestore.
 * When a document is added, it automatically sends an email via SMTP.
 */
object TriggerEmailHelper {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private const val MAIL_COLLECTION = "mail" // Collection watched by Trigger Email extension

    /**
     * Send password reset email using Trigger Email extension
     * 
     * @param email Recipient email address
     * @param resetLink Password reset link
     * @param userName Optional: User's name for personalization
     * @param onSuccess Callback when email document is created successfully
     * @param onFailure Callback when email document creation fails
     */
    fun sendPasswordResetEmail(
        email: String,
        resetLink: String,
        userName: String? = null,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val emailData = hashMapOf<String, Any>(
            "to" to email,
            "message" to hashMapOf<String, Any>(
                "subject" to "Password Reset Request - UMelec",
                "html" to buildPasswordResetHtml(resetLink, userName),
                "text" to buildPasswordResetText(resetLink, userName)
            )
        )

        // Add document to 'mail' collection - Trigger Email extension will send it
        firestore.collection(MAIL_COLLECTION)
            .add(emailData)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onFailure(exception.message ?: "Failed to send email")
            }
    }

    /**
     * Send welcome email using Trigger Email extension
     * 
     * @param email Recipient email address
     * @param userName User's name
     * @param onSuccess Callback when email document is created successfully
     * @param onFailure Callback when email document creation fails
     */
    fun sendWelcomeEmail(
        email: String,
        userName: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val emailData = hashMapOf<String, Any>(
            "to" to email,
            "message" to hashMapOf<String, Any>(
                "subject" to "Welcome to UMelec!",
                "html" to buildWelcomeHtml(userName),
                "text" to buildWelcomeText(userName)
            )
        )

        firestore.collection(MAIL_COLLECTION)
            .add(emailData)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onFailure(exception.message ?: "Failed to send email")
            }
    }

    /**
     * Send notification email using Trigger Email extension
     * 
     * @param email Recipient email address
     * @param title Email title/subject
     * @param message Email message content
     * @param userName Optional: User's name for personalization
     * @param onSuccess Callback when email document is created successfully
     * @param onFailure Callback when email document creation fails
     */
    fun sendNotificationEmail(
        email: String,
        title: String,
        message: String,
        userName: String? = null,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val emailData = hashMapOf<String, Any>(
            "to" to email,
            "message" to hashMapOf<String, Any>(
                "subject" to title,
                "html" to buildNotificationHtml(title, message, userName),
                "text" to buildNotificationText(title, message, userName)
            )
        )

        firestore.collection(MAIL_COLLECTION)
            .add(emailData)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onFailure(exception.message ?: "Failed to send email")
            }
    }

    /**
     * Send password reset verification code email using Trigger Email extension
     * 
     * @param email Recipient email address
     * @param verificationCode 6-digit verification code
     * @param userName Optional: User's name for personalization
     * @param onSuccess Callback when email document is created successfully
     * @param onFailure Callback when email document creation fails
     */
    fun sendPasswordResetCode(
        email: String,
        verificationCode: String,
        userName: String? = null,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val emailData = hashMapOf<String, Any>(
            "to" to email,
            "message" to hashMapOf<String, Any>(
                "subject" to "Password Reset Verification Code - UMelec",
                "html" to buildPasswordResetCodeHtml(verificationCode, userName),
                "text" to buildPasswordResetCodeText(verificationCode, userName)
            )
        )

        firestore.collection(MAIL_COLLECTION)
            .add(emailData)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onFailure(exception.message ?: "Failed to send email")
            }
    }

    /**
     * Send custom email using Trigger Email extension
     * 
     * @param email Recipient email address
     * @param subject Email subject
     * @param htmlBody HTML email body
     * @param textBody Optional: Plain text email body
     * @param onSuccess Callback when email document is created successfully
     * @param onFailure Callback when email document creation fails
     */
    fun sendCustomEmail(
        email: String,
        subject: String,
        htmlBody: String,
        textBody: String? = null,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val messageData = hashMapOf<String, Any>(
            "subject" to subject,
            "html" to htmlBody
        )

        textBody?.let {
            messageData["text"] = it
        }

        val emailData = hashMapOf<String, Any>(
            "to" to email,
            "message" to messageData
        )

        firestore.collection(MAIL_COLLECTION)
            .add(emailData)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onFailure(exception.message ?: "Failed to send email")
            }
    }

    // Email template builders

    private fun buildPasswordResetHtml(resetLink: String, userName: String?): String {
        val greeting = if (userName != null) "Dear $userName," else "Dear User,"
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Password Reset - UMelec</title>
                <style>
                    /* Fallback fonts for better email client compatibility */
                    body, table, td, p, a, h1, h2 {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                    }
                </style>
            </head>
            <body style="margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; background-color: #f5f5f5;">
                <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%" style="background: linear-gradient(180deg, #0098E0 0%, #00537A 100%); padding: 20px 10px;">
                    <tr>
                        <td align="center">
                            <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="500" style="max-width: 500px; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1);">
                                <!-- Header with Gradient -->
                                <tr>
                                    <td style="background: linear-gradient(180deg, #0098E0 0%, #00537A 100%); padding: 20px 20px; text-align: center;">
                                        <h1 style="margin: 0; color: #ffffff; font-size: 24px; font-weight: 700; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; letter-spacing: 0.5px;">UMelec</h1>
                                    </td>
                                </tr>
                                <!-- Content -->
                                <tr>
                                    <td style="padding: 30px 25px; background-color: #ffffff;">
                                        <h2 style="margin: 0 0 15px 0; color: #0E0E2C; font-size: 20px; font-weight: 600; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;">Password Reset Request</h2>
                                        <p style="margin: 0 0 15px 0; color: #5C5C77; font-size: 15px; line-height: 1.5; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;">$greeting</p>
                                        <p style="margin: 0 0 25px 0; color: #5C5C77; font-size: 15px; line-height: 1.5; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;">We received a request to reset the password for your UMelec account. To proceed, please click the button below:</p>
                                        
                                        <!-- Reset Button with Gradient -->
                                        <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%">
                                            <tr>
                                                <td align="center" style="padding: 15px 0 25px 0;">
                                                    <a href="$resetLink" style="display: inline-block; background: linear-gradient(180deg, #0098E0 0%, #00537A 100%); color: #ffffff; text-decoration: none; padding: 12px 30px; border-radius: 12px; font-size: 15px; font-weight: 600; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; box-shadow: 0 3px 6px rgba(0, 83, 122, 0.2);">Reset Password</a>
                                                </td>
                                            </tr>
                                        </table>
                                        
                                        <p style="margin: 0 0 10px 0; color: #8C8CA1; font-size: 13px; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;">If the button doesn't work, copy and paste this link into your browser:</p>
                                        <div style="background-color: #f9f9f9; border: 1px solid #e0e0e0; border-radius: 6px; padding: 12px; margin: 0 0 25px 0; word-break: break-all;">
                                            <p style="margin: 0; color: #00537A; font-size: 11px; font-family: 'Courier New', monospace; line-height: 1.4;">$resetLink</p>
                                        </div>
                                        
                                        <p style="margin: 0 0 15px 0; color: #5C5C77; font-size: 13px; line-height: 1.5; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;">
                                            This link is valid for 1 hour. If you did not request a password reset, please disregard this email.
                                        </p>
                                        
                                        <p style="margin: 25px 0 0 0; color: #5C5C77; font-size: 14px; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;">Sincerely,<br>The UMelec Team</p>
                                    </td>
                                </tr>
                                <!-- Footer -->
                                <tr>
                                    <td style="background-color: #f9f9f9; padding: 15px 25px; text-align: center; border-top: 1px solid #e0e0e0;">
                                        <p style="margin: 0; color: #8C8CA1; font-size: 11px; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;">
                                            © $currentYear UMelec. All rights reserved.
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
        """.trimIndent()
    }

    private fun buildPasswordResetText(resetLink: String, userName: String?): String {
        val greeting = if (userName != null) "Dear $userName," else "Dear User,"
        
        return """
Password Reset Request - UMelec

$greeting

We received a request to reset the password for your UMelec account. To proceed, please click the button below:

$resetLink

This link is valid for 1 hour. If you did not request a password reset, please disregard this email.

Sincerely,
The UMelec Team
        """.trimIndent()
    }

    private fun buildPasswordResetCodeHtml(verificationCode: String, userName: String?): String {
        val greeting = if (userName != null) "Dear $userName," else "Dear User,"
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Password Reset Verification Code - UMelec</title>
                <style>
                    /* Fallback fonts for better email client compatibility */
                    body, table, td, p, a, h1, h2 {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                    }
                </style>
            </head>
            <body style="margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; background-color: #f5f5f5;">
                <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%" style="background: linear-gradient(180deg, #0098E0 0%, #00537A 100%); padding: 20px 10px;">
                    <tr>
                        <td align="center">
                            <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="500" style="max-width: 500px; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1);">
                                <!-- Header with Gradient -->
                                <tr>
                                    <td style="background: linear-gradient(180deg, #0098E0 0%, #00537A 100%); padding: 20px 20px; text-align: center;">
                                        <h1 style="margin: 0; color: #ffffff; font-size: 24px; font-weight: 700; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; letter-spacing: 0.5px;">UMelec</h1>
                                    </td>
                                </tr>
                                <!-- Content -->
                                <tr>
                                    <td style="padding: 30px 25px; background-color: #ffffff;">
                                        <h2 style="margin: 0 0 15px 0; color: #0E0E2C; font-size: 20px; font-weight: 600; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;">Password Reset Verification Code</h2>
                                        <p style="margin: 0 0 15px 0; color: #5C5C77; font-size: 15px; line-height: 1.5; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;">$greeting</p>
                                        <p style="margin: 0 0 25px 0; color: #5C5C77; font-size: 15px; line-height: 1.5; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;">We received a request to reset the password for your UMelec account. Please use the following verification code to proceed:</p>
                                        
                                        <!-- Verification Code Display -->
                                        <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%">
                                            <tr>
                                                <td align="center" style="padding: 15px 0 25px 0;">
                                                    <div style="background: linear-gradient(180deg, #0098E0 0%, #00537A 100%); color: #ffffff; padding: 20px 30px; border-radius: 12px; display: inline-block; box-shadow: 0 3px 6px rgba(0, 83, 122, 0.2);">
                                                        <h1 style="margin: 0; color: #ffffff; font-size: 32px; font-weight: 700; font-family: 'Courier New', monospace; letter-spacing: 8px;">$verificationCode</h1>
                                                    </div>
                                                </td>
                                            </tr>
                                        </table>
                                        
                                        <p style="margin: 0 0 15px 0; color: #5C5C77; font-size: 13px; line-height: 1.5; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;">
                                            This code is valid for 10 minutes. If you did not request a password reset, please disregard this email.
                                        </p>
                                        
                                        <p style="margin: 25px 0 0 0; color: #5C5C77; font-size: 14px; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;">Sincerely,<br>The UMelec Team</p>
                                    </td>
                                </tr>
                                <!-- Footer -->
                                <tr>
                                    <td style="background-color: #f9f9f9; padding: 15px 25px; text-align: center; border-top: 1px solid #e0e0e0;">
                                        <p style="margin: 0; color: #8C8CA1; font-size: 11px; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;">
                                            © $currentYear UMelec. All rights reserved.
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
        """.trimIndent()
    }

    private fun buildPasswordResetCodeText(verificationCode: String, userName: String?): String {
        val greeting = if (userName != null) "Dear $userName," else "Dear User,"
        
        return """
Password Reset Verification Code - UMelec

$greeting

We received a request to reset the password for your UMelec account. Please use the following verification code to proceed:

$verificationCode

This code is valid for 10 minutes. If you did not request a password reset, please disregard this email.

Sincerely,
The UMelec Team
        """.trimIndent()
    }

    private fun buildWelcomeHtml(userName: String): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Welcome</title>
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;">
                <div style="background-color: #00537A; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0;">
                    <h1 style="margin: 0;">UMelec</h1>
                </div>
                <div style="background-color: #f9f9f9; padding: 30px; border-radius: 0 0 5px 5px;">
                    <h2 style="color: #00537A;">Welcome to UMelec!</h2>
                    <p>Hello $userName,</p>
                    <p>Thank you for joining UMelec! We're excited to have you on board.</p>
                    <p>Your account has been successfully created. You can now:</p>
                    <ul>
                        <li>Participate in elections</li>
                        <li>View election results</li>
                        <li>Receive important notifications</li>
                        <li>Manage your profile</li>
                    </ul>
                    <p>If you have any questions, please don't hesitate to contact our support team.</p>
                    <p>Best regards,<br>The UMelec Team</p>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun buildWelcomeText(userName: String): String {
        return """
            Welcome to UMelec!
            
            Hello $userName,
            
            Thank you for joining UMelec! We're excited to have you on board.
            
            Your account has been successfully created. You can now:
            - Participate in elections
            - View election results
            - Receive important notifications
            - Manage your profile
            
            If you have any questions, please don't hesitate to contact our support team.
            
            Best regards,
            The UMelec Team
        """.trimIndent()
    }

    private fun buildNotificationHtml(title: String, message: String, userName: String?): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Notification</title>
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;">
                <div style="background-color: #00537A; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0;">
                    <h1 style="margin: 0;">UMelec</h1>
                </div>
                <div style="background-color: #f9f9f9; padding: 30px; border-radius: 0 0 5px 5px;">
                    <h2 style="color: #00537A;">$title</h2>
                    ${if (userName != null) "<p>Hello $userName,</p>" else "<p>Hello,</p>"}
                    <div style="background-color: white; padding: 20px; border-left: 4px solid #00537A; margin: 20px 0;">
                        $message
                    </div>
                    <p style="color: #666; font-size: 12px;">
                        This is an automated notification from UMelec.
                    </p>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun buildNotificationText(title: String, message: String, userName: String?): String {
        return """
            $title - UMelec
            
            ${if (userName != null) "Hello $userName," else "Hello,"}
            
            $message
            
            This is an automated notification from UMelec.
        """.trimIndent()
    }

    /**
     * Send leader verification code email using Trigger Email extension
     * 
     * @param email Recipient email address
     * @param verificationCode 6-digit verification code
     * @param userName Optional: User's name for personalization
     * @param onSuccess Callback when email document is created successfully
     * @param onFailure Callback when email document creation fails
     */
    fun sendLeaderVerificationCode(
        email: String,
        verificationCode: String,
        userName: String? = null,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val emailData = hashMapOf<String, Any>(
            "to" to email,
            "message" to hashMapOf<String, Any>(
                "subject" to "Leader Verification Code - UMelec",
                "html" to buildLeaderVerificationHtml(verificationCode, userName),
                "text" to buildLeaderVerificationText(verificationCode, userName)
            )
        )

        // Add document to 'mail' collection - Trigger Email extension will send it
        firestore.collection(MAIL_COLLECTION)
            .add(emailData)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onFailure(exception.message ?: "Failed to send verification email")
            }
    }

    private fun buildLeaderVerificationHtml(verificationCode: String, userName: String?): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Leader Verification - UMelec</title>
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto;">
                <div style="background-color: #00537A; padding: 20px; text-align: center;">
                    <h1 style="color: white; margin: 0;">UMelec</h1>
                    <p style="color: #E8F4FD; margin: 5px 0 0 0;">University of Makati Electronic Election</p>
                </div>
                <div style="background-color: #f9f9f9; padding: 30px; border-radius: 0 0 5px 5px;">
                    <h2 style="color: #00537A;">Leader Account Verification</h2>
                    ${if (userName != null) "<p>Hello $userName,</p>" else "<p>Hello,</p>"}
                    <p>You have successfully logged in to your leader account. To complete the verification process, please enter the following code in the app:</p>
                    
                    <div style="background-color: white; padding: 25px; border: 2px solid #00537A; border-radius: 8px; text-align: center; margin: 25px 0;">
                        <h1 style="color: #00537A; font-size: 36px; margin: 0; letter-spacing: 8px; font-family: 'Courier New', monospace;">$verificationCode</h1>
                    </div>
                    
                    <p><strong>Important:</strong></p>
                    <ul>
                        <li>This code will expire in <strong>10 minutes</strong></li>
                        <li>Do not share this code with anyone</li>
                        <li>If you didn't request this verification, please contact support</li>
                    </ul>
                    
                    <p style="color: #666; font-size: 12px; margin-top: 30px;">
                        This is an automated verification email from UMelec. Please do not reply to this email.
                    </p>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun buildLeaderVerificationText(verificationCode: String, userName: String?): String {
        return """
            Leader Account Verification - UMelec
            
            ${if (userName != null) "Hello $userName," else "Hello,"}
            
            You have successfully logged in to your leader account. To complete the verification process, please enter the following code in the app:
            
            Verification Code: $verificationCode
            
            Important:
            - This code will expire in 10 minutes
            - Do not share this code with anyone
            - If you didn't request this verification, please contact support
            
            This is an automated verification email from UMelec. Please do not reply to this email.
        """.trimIndent()
    }
}

