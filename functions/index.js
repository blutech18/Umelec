const {onCall, HttpsError} = require("firebase-functions/v2/https");
const {setGlobalOptions} = require("firebase-functions/v2");
const admin = require("firebase-admin");
const nodemailer = require("nodemailer");

// Initialize Firebase Admin
admin.initializeApp();

// Set global options for v2 functions
setGlobalOptions({
  region: "us-central1",
});

// Email configuration - Replace with your SMTP settings
// Supports both Firebase Functions config and environment variables
// Note: For Firebase Functions v2+, use environment variables instead of functions.config()
const getSMTPConfig = () => {
  // Get SMTP configuration from environment variables
  // Set these in Firebase Console: Functions → Configuration → Environment variables
  const user = process.env.SMTP_USER || "";
  const password = process.env.SMTP_PASSWORD || "";
  const host = process.env.SMTP_HOST || "smtp.gmail.com";
  const port = parseInt(process.env.SMTP_PORT || "587");

  // Validate that credentials are provided
  if (!user || !password) {
    throw new Error(
      "SMTP credentials are not configured. " +
      "Please set SMTP_USER and SMTP_PASSWORD environment variables " +
      "in Firebase Console. " +
      "Go to: Firebase Console → Functions → Configuration → Environment variables.",
    );
  }

  return {
    host: host,
    port: port,
    secure: false, // true for 465, false for other ports
    auth: {
      user: user,
      pass: password,
    },
  };
};

// Create transporter factory function
const createTransporter = () => {
  const smtpConfig = getSMTPConfig();
  return nodemailer.createTransport(smtpConfig);
};

// Email templates
const emailTemplates = {
  PASSWORD_RESET: (data) => ({
    subject: data.subject || "Password Reset Request - UMelec",
    html: `
      <!DOCTYPE html>
      <html>
      <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Password Reset</title>
      </head>
      <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;">
        <div style="background-color: #00537A; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0;">
          <h1 style="margin: 0;">UMelec</h1>
        </div>
        <div style="background-color: #f9f9f9; padding: 30px; border-radius: 0 0 5px 5px;">
          <h2 style="color: #00537A;">Password Reset Request</h2>
          ${data.userName ? `<p>Hello ${data.userName},</p>` : "<p>Hello,</p>"}
          <p>You have requested to reset your password for your UMelec account.</p>
          <p>Click the button below to reset your password:</p>
          <div style="text-align: center; margin: 30px 0;">
            <a href="${data.resetLink}" 
               style="background-color: #00537A; color: white; padding: 12px 30px; 
                      text-decoration: none; border-radius: 5px; display: inline-block;">
              Reset Password
            </a>
          </div>
          <p>Or copy and paste this link into your browser:</p>
          <p style="word-break: break-all; color: #00537A;">${data.resetLink}</p>
          <p style="color: #666; font-size: 12px;">
            <strong>Note:</strong> This link will expire in 1 hour. If you didn't request this, 
            please ignore this email or contact support.
          </p>
          <hr style="border: none; border-top: 1px solid #ddd; margin: 20px 0;">
          <p style="color: #666; font-size: 12px; text-align: center;">
            This is an automated message from UMelec. Please do not reply to this email.
          </p>
        </div>
      </body>
      </html>
    `,
    text: `
      Password Reset Request - UMelec
      
      ${data.userName ? `Hello ${data.userName},` : "Hello,"}
      
      You have requested to reset your password for your UMelec account.
      
      Click the following link to reset your password:
      ${data.resetLink}
      
      Note: This link will expire in 1 hour. If you didn't request this, 
      please ignore this email or contact support.
      
      This is an automated message from UMelec. Please do not reply to this email.
    `,
  }),

  EMAIL_VERIFICATION: (data) => ({
    subject: data.subject || "Verify Your Email - UMelec",
    html: `
      <!DOCTYPE html>
      <html>
      <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Email Verification</title>
      </head>
      <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;">
        <div style="background-color: #00537A; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0;">
          <h1 style="margin: 0;">UMelec</h1>
        </div>
        <div style="background-color: #f9f9f9; padding: 30px; border-radius: 0 0 5px 5px;">
          <h2 style="color: #00537A;">Verify Your Email Address</h2>
          ${data.userName ? `<p>Hello ${data.userName},</p>` : "<p>Hello,</p>"}
          <p>Thank you for registering with UMelec. Please verify your email address to complete your registration.</p>
          <div style="text-align: center; margin: 30px 0;">
            <a href="${data.verificationLink}" 
               style="background-color: #00537A; color: white; padding: 12px 30px; 
                      text-decoration: none; border-radius: 5px; display: inline-block;">
              Verify Email
            </a>
          </div>
          <p>Or copy and paste this link into your browser:</p>
          <p style="word-break: break-all; color: #00537A;">${data.verificationLink}</p>
          <p style="color: #666; font-size: 12px;">
            <strong>Note:</strong> This link will expire in 24 hours. If you didn't create an account, 
            please ignore this email.
          </p>
        </div>
      </body>
      </html>
    `,
    text: `
      Verify Your Email Address - UMelec
      
      ${data.userName ? `Hello ${data.userName},` : "Hello,"}
      
      Thank you for registering with UMelec. Please verify your email address to complete your registration.
      
      Click the following link to verify your email:
      ${data.verificationLink}
      
      Note: This link will expire in 24 hours. If you didn't create an account, please ignore this email.
    `,
  }),

  WELCOME: (data) => ({
    subject: data.subject || "Welcome to UMelec!",
    html: `
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
          <p>Hello ${data.userName},</p>
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
    `,
    text: `
      Welcome to UMelec!
      
      Hello ${data.userName},
      
      Thank you for joining UMelec! We're excited to have you on board.
      
      Your account has been successfully created. You can now:
      - Participate in elections
      - View election results
      - Receive important notifications
      - Manage your profile
      
      If you have any questions, please don't hesitate to contact our support team.
      
      Best regards,
      The UMelec Team
    `,
  }),

  NOTIFICATION: (data) => ({
    subject: data.subject || data.title || "Notification from UMelec",
    html: `
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
          <h2 style="color: #00537A;">${data.title || "Notification"}</h2>
          ${data.userName ? `<p>Hello ${data.userName},</p>` : "<p>Hello,</p>"}
          <div style="background-color: white; padding: 20px; border-left: 4px solid #00537A; margin: 20px 0;">
            ${data.message}
          </div>
          <p style="color: #666; font-size: 12px;">
            This is an automated notification from UMelec.
          </p>
        </div>
      </body>
      </html>
    `,
    text: `
      ${data.title || "Notification"} - UMelec
      
      ${data.userName ? `Hello ${data.userName},` : "Hello,"}
      
      ${data.message}
      
      This is an automated notification from UMelec.
    `,
  }),

  VOTE_CONFIRMATION: (data) => {
    let voteDetailsHtml = "";
    if (data.electionTitle) {
      voteDetailsHtml += `<p><strong>Election:</strong> ${data.electionTitle}</p>`;
    }
    if (data.selections) {
      voteDetailsHtml += `<p><strong>Your Voting Choices:</strong></p><ul>${data.selections.split("\n").map(choice => `<li>${choice}</li>`).join("")}</ul>`;
    }
    if (data.voteId) {
      voteDetailsHtml += `<p><strong>Reference Code:</strong> ${data.voteId}</p>`;
    }
    if (data.submittedAt) {
      voteDetailsHtml += `<p><strong>Submission Date:</strong> ${data.submittedAt}</p>`;
    }
    
    return {
      subject: data.subject || "Vote Confirmation - UMelec",
      html: `
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>Vote Confirmation</title>
        </head>
        <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;">
          <div style="background-color: #00537A; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0;">
            <h1 style="margin: 0;">UMelec</h1>
          </div>
          <div style="background-color: #f9f9f9; padding: 30px; border-radius: 0 0 5px 5px;">
            <h2 style="color: #00537A;">Vote Confirmation</h2>
            <p>Hello ${data.userName},</p>
            <p>Your vote has been successfully recorded. Thank you for participating in the election.</p>
            <div style="background-color: white; padding: 20px; border-left: 4px solid #27A688; margin: 20px 0;">
              <p><strong>Vote Details:</strong></p>
              ${voteDetailsHtml}
            </div>
            <p style="color: #666; font-size: 12px;">
              This is your official vote confirmation. A PDF receipt has been attached to this email for your records.
            </p>
          </div>
        </body>
        </html>
      `,
      text: `
        Vote Confirmation - UMelec
        
        Hello ${data.userName},
        
        Your vote has been successfully recorded. Thank you for participating in the election.
        
        Vote Details:
        ${data.electionTitle ? `Election: ${data.electionTitle}` : ""}
        ${data.selections || ""}
        ${data.voteId ? `Reference Code: ${data.voteId}` : ""}
        ${data.submittedAt ? `Submission Date: ${data.submittedAt}` : ""}
        
        This is your official vote confirmation. A PDF receipt has been attached to this email for your records.
      `,
    };
  },

  ELECTION_REMINDER: (data) => ({
    subject: data.subject || "Election Reminder - UMelec",
    html: `
      <!DOCTYPE html>
      <html>
      <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Election Reminder</title>
      </head>
      <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;">
        <div style="background-color: #00537A; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0;">
          <h1 style="margin: 0;">UMelec</h1>
        </div>
        <div style="background-color: #f9f9f9; padding: 30px; border-radius: 0 0 5px 5px;">
          <h2 style="color: #00537A;">Election Reminder</h2>
          ${data.userName ? `<p>Hello ${data.userName},</p>` : "<p>Hello,</p>"}
          <p>This is a reminder that there is an ongoing election.</p>
          ${data.electionName ? `<p><strong>Election:</strong> ${data.electionName}</p>` : ""}
          ${data.deadline ? `<p><strong>Deadline:</strong> ${data.deadline}</p>` : ""}
          <div style="text-align: center; margin: 30px 0;">
            <a href="${data.voteLink || "#"}" 
               style="background-color: #00537A; color: white; padding: 12px 30px; 
                      text-decoration: none; border-radius: 5px; display: inline-block;">
              Cast Your Vote
            </a>
          </div>
          <p>Don't forget to exercise your right to vote!</p>
        </div>
      </body>
      </html>
    `,
    text: `
      Election Reminder - UMelec
      
      ${data.userName ? `Hello ${data.userName},` : "Hello,"}
      
      This is a reminder that there is an ongoing election.
      ${data.electionName ? `Election: ${data.electionName}` : ""}
      ${data.deadline ? `Deadline: ${data.deadline}` : ""}
      
      Don't forget to exercise your right to vote!
    `,
  }),
};

// Main Cloud Function to send emails (v2 API)
exports.sendEmail = onCall(async (request) => {
  try {
    const data = request.data;

    // Debug: Log received data
    console.log("Received email request data:", JSON.stringify(data));
    console.log("Data type:", typeof data);
    console.log("Data keys:", data ? Object.keys(data) : "data is null/undefined");

    // Validate request - check if data exists and has required fields
    if (!data) {
      throw new HttpsError(
        "invalid-argument",
        "Request data is missing",
      );
    }

    if (!data.to || !data.emailType) {
      console.error("Missing required fields. Data received:", JSON.stringify(data));
      throw new HttpsError(
        "invalid-argument",
        `Missing required fields: 'to' and 'emailType'. ` +
        `Received data: ${JSON.stringify(data)}`,
      );
    }

    // Optional: Check if user is authenticated (uncomment if needed)
    // const context = request.auth;
    // if (!context) {
    //   throw new HttpsError(
    //     "unauthenticated",
    //     "User must be authenticated to send emails"
    //   );
    // }

    const {to, emailType, subject, templateData = {}, customHtml, customText} = data;

    // Create a mutable copy of templateData
    const emailTemplateData = {...templateData};

    // Special handling for PASSWORD_RESET: Generate reset link using Firebase Admin
    if (emailType === "PASSWORD_RESET") {
      // Only generate link if not already provided
      if (!emailTemplateData.resetLink) {
        try {
          // Generate password reset link using Firebase Admin SDK
          const actionCodeSettings = {
            url: "https://umelec-70618.firebaseapp.com/__/auth/action",
            handleCodeInApp: false,
          };

          const resetLink = await admin.auth()
            .generatePasswordResetLink(to, actionCodeSettings);

          // Update emailTemplateData with the generated reset link
          emailTemplateData.resetLink = resetLink;
          console.log("Password reset link generated successfully");
        } catch (authError) {
          // If user doesn't exist, Firebase Admin will throw an error
          // For security, we still proceed (don't reveal if email exists)
          // But we need a valid reset link for the template
          console.warn(
            "Error generating reset link (user may not exist):",
            authError.message,
          );

          // For security: Don't reveal if email exists, but still send email
          // The email will be sent, but the link won't work if user doesn't exist
          // This maintains security by not revealing if an email exists
          emailTemplateData.resetLink =
            "https://umelec-70618.firebaseapp.com/__/auth/action?mode=resetPassword";

          // Note: In production, you might want to silently fail here
          // But for now, we'll send the email anyway
        }
      }
    }

    // Determine email content
    let emailContent;

    if (emailType === "CUSTOM") {
      if (!customHtml && !customText) {
        throw new HttpsError(
          "invalid-argument",
          "CUSTOM email type requires 'customHtml' or 'customText'",
        );
      }
      emailContent = {
        subject: subject || "Message from UMelec",
        html: customHtml,
        // Strip HTML if no text
        text: customText || (customHtml ? customHtml.replace(/<[^>]*>/g, "") : ""),
      };
    } else {
      const template = emailTemplates[emailType];
      if (!template) {
        throw new HttpsError(
          "invalid-argument",
          `Unknown email type: ${emailType}`,
        );
      }
      emailContent = template({...emailTemplateData, subject});
    }

    // Get SMTP config and create transporter
    let smtpConfig;
    let transporter;

    try {
      smtpConfig = getSMTPConfig();
      transporter = createTransporter();
    } catch (configError) {
      console.error("SMTP configuration error:", configError.message);
      throw new HttpsError(
        "failed-precondition",
        `SMTP configuration error: ${configError.message}. ` +
        "Please configure SMTP credentials in Firebase Functions environment variables.",
      );
    }

    // Prepare mail options
    const mailOptions = {
      from: `"UMelec" <${smtpConfig.auth.user}>`,
      to: to,
      subject: emailContent.subject,
      html: emailContent.html,
      text: emailContent.text,
    };

    // Handle PDF attachment for VOTE_CONFIRMATION emails
    if (emailType === "VOTE_CONFIRMATION" && emailTemplateData.pdfBase64 && emailTemplateData.pdfFileName) {
      try {
        // Decode base64 PDF
        const pdfBuffer = Buffer.from(emailTemplateData.pdfBase64, "base64");
        
        mailOptions.attachments = [
          {
            filename: emailTemplateData.pdfFileName,
            content: pdfBuffer,
            contentType: "application/pdf",
          },
        ];
        
        console.log(`PDF attachment added: ${emailTemplateData.pdfFileName} (${pdfBuffer.length} bytes)`);
      } catch (attachmentError) {
        console.error("Error preparing PDF attachment:", attachmentError.message);
        // Continue without attachment rather than failing the entire email
      }
    }

    // Send email
    const info = await transporter.sendMail(mailOptions);

    console.log("Email sent successfully:", info.messageId);

    return {
      success: true,
      message: "Email sent successfully",
      messageId: info.messageId,
    };
  } catch (error) {
    console.error("Error sending email:", error);

    if (error instanceof HttpsError) {
      throw error;
    }

    throw new HttpsError(
      "internal",
      `Failed to send email: ${error.message}`,
    );
  }
});

// Test function to verify SMTP configuration (v2 API)
exports.testEmail = onCall(async (_request) => {
  try {
    // Create transporter and verify SMTP connection
    const transporter = createTransporter();
    await transporter.verify();

    return {
      success: true,
      message: "SMTP configuration is valid",
    };
  } catch (error) {
    console.error("SMTP verification failed:", error);
    throw new HttpsError(
      "internal",
      `SMTP verification failed: ${error.message}`,
    );
  }
});

// Generate password reset action code (for use after verification code is verified)
exports.generatePasswordResetCode = onCall(async (request) => {
  try {
    const data = request.data;

    if (!data || !data.email) {
      throw new HttpsError(
        "invalid-argument",
        "Email address is required",
      );
    }

    const email = data.email;

    // Generate password reset link using Firebase Admin SDK
    const actionCodeSettings = {
      url: "https://umelec-70618.firebaseapp.com/__/auth/action",
      handleCodeInApp: false,
    };

    const resetLink = await admin.auth()
      .generatePasswordResetLink(email, actionCodeSettings);

    // Extract the oobCode from the reset link
    // Format: https://umelec-70618.firebaseapp.com/__/auth/action?mode=resetPassword&oobCode=CODE&apiKey=KEY
    const url = new URL(resetLink);
    const oobCode = url.searchParams.get("oobCode");

    if (!oobCode) {
      throw new HttpsError(
        "internal",
        "Failed to generate password reset code",
      );
    }

    console.log("Password reset code generated successfully for:", email);

    return {
      success: true,
      actionCode: oobCode,
      resetLink: resetLink,
    };
  } catch (error) {
    console.error("Error generating password reset code:", error);

    if (error instanceof HttpsError) {
      throw error;
    }

    // Check if user doesn't exist
    if (error.code === "auth/user-not-found") {
      throw new HttpsError(
        "not-found",
        "User not found",
      );
    }

    throw new HttpsError(
      "internal",
      `Failed to generate password reset code: ${error.message}`,
    );
  }
});
