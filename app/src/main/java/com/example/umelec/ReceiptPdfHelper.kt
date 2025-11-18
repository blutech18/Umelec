package com.example.umelec

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.Typeface
import android.os.Build
import android.util.Base64
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

/**
 * Helper class for generating PDF vote receipts
 */
object ReceiptPdfHelper {
    private const val TAG = "ReceiptPdfHelper"
    
    // PDF dimensions (A4 size in points, converted to pixels at 72 DPI)
    private const val PAGE_WIDTH = 595 // A4 width at 72 DPI
    private const val PAGE_HEIGHT = 842 // A4 height at 72 DPI
    private const val MARGIN = 40
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2)
    
    /**
     * Data class for receipt information
     */
    data class ReceiptData(
        val voteId: String,
        val electionId: String,
        val electionTitle: String,
        val userName: String,
        val userEmail: String,
        val submittedAt: Date,
        val signatureBase64: String?,
        val selections: Map<String, Map<String, String>>, // positionId -> (candidateId, candidateName, positionName)
        val digitalSignaturePreview: String? = null, // First 8 characters of DSA digital signature for verification
        val digitalSignature: String? = null,
        val dsaPublicKey: String? = null,
        val isSignatureVerified: Boolean = false
    )
    
    /**
     * Generate SHA-256 hash from signature for digital signature
     */
    private fun generateDigitalSignatureHash(signatureBase64: String?): String {
        return try {
            if (signatureBase64 == null) {
                return "0x0000000000000000000000000000000000000000000000000000000000000000"
            }
            val signatureBytes = Base64.decode(signatureBase64, Base64.DEFAULT)
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(signatureBytes)
            "0x" + hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating hash: ${e.message}")
            "0x0000000000000000000000000000000000000000000000000000000000000000"
        }
    }

    /**
     * Draw multi-line text with word wrapping
     */
    private fun drawWrappedText(
        canvas: Canvas,
        text: String,
        paint: Paint,
        x: Float,
        y: Float,
        maxWidth: Float
    ): Float {
        var currentY = y
        val words = text.split(" ")
        var currentLine = ""
        
        words.forEach { word ->
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val width = paint.measureText(testLine)
            
            if (width > maxWidth && currentLine.isNotEmpty()) {
                canvas.drawText(currentLine, x, currentY, paint)
                currentY += paint.textSize + 4
                currentLine = word
            } else {
                currentLine = testLine
            }
        }
        
        if (currentLine.isNotEmpty()) {
            canvas.drawText(currentLine, x, currentY, paint)
            currentY += paint.textSize + 4
        }
        
        return currentY
    }

    /**
     * Generate PDF receipt file matching the official format
     * 
     * @param context Application context
     * @param receiptData Receipt data to include in PDF
     * @param onSuccess Callback with file path when PDF is generated
     * @param onFailure Callback with error message when generation fails
     */
    fun generateReceiptPdf(
        context: Context,
        receiptData: ReceiptData,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        try {
            val document = PdfDocument()
            
            // Create a page
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            
            var yPosition = MARGIN.toFloat() + 20
            
            // Load logo first to calculate dimensions
            var logoBitmap: Bitmap? = null
            var logoHeight = 0f
            var logoWidth = 0f
            
            // Note: Logo should be placed in one of these locations:
            // 1. app/src/main/assets/ic_launcher-playstore.png (preferred)
            // 2. app/src/main/res/drawable/ic_launcher_playstore.png (without hyphen)
            // 3. app/src/main/res/raw/ic_launcher_playstore.png
            try {
                // Try loading from assets first (preferred location)
                try {
                    val assetManager = context.assets
                    val inputStream = assetManager.open("ic_launcher-playstore.png")
                    logoBitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()
                    Log.d(TAG, "Logo loaded from assets")
                } catch (e: Exception) {
                    // Try loading from drawable resources
                    try {
                        // Try with underscore first (standard Android resource naming)
                        var logoResourceId = context.resources.getIdentifier("ic_launcher_playstore", "drawable", context.packageName)
                        
                        // If not found, try with hyphen (in case file was copied with original name)
                        if (logoResourceId == 0) {
                            logoResourceId = context.resources.getIdentifier("ic_launcher-playstore", "drawable", context.packageName)
                        }
                        
                        if (logoResourceId != 0) {
                            logoBitmap = BitmapFactory.decodeResource(context.resources, logoResourceId)
                            Log.d(TAG, "Logo loaded from drawable")
                        } else {
                            // Try loading from raw resources
                            var rawResourceId = context.resources.getIdentifier("ic_launcher_playstore", "raw", context.packageName)
                            if (rawResourceId == 0) {
                                rawResourceId = context.resources.getIdentifier("ic_launcher-playstore", "raw", context.packageName)
                            }
                            if (rawResourceId != 0) {
                                logoBitmap = BitmapFactory.decodeResource(context.resources, rawResourceId)
                                Log.d(TAG, "Logo loaded from raw")
                            } else {
                                // Try loading from mipmap as final fallback
                                val mipmapResourceId = context.resources.getIdentifier("ic_launcher_foreground", "mipmap", context.packageName)
                                if (mipmapResourceId != 0) {
                                    logoBitmap = BitmapFactory.decodeResource(context.resources, mipmapResourceId)
                                    Log.d(TAG, "Logo loaded from mipmap")
                                }
                            }
                        }
                    } catch (e2: Exception) {
                        Log.e(TAG, "Could not load logo from resources: ${e2.message}")
                    }
                }
                
                // Calculate logo dimensions
                logoBitmap?.let { bitmap ->
                    // Scale logo to appropriate size (max width 80, maintain aspect ratio)
                    val maxLogoWidth = 80f
                    val scale = if (bitmap.width > maxLogoWidth) {
                        maxLogoWidth / bitmap.width
                    } else {
                        1f
                    }
                    logoWidth = bitmap.width * scale
                    logoHeight = bitmap.height * scale
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading logo: ${e.message}")
                // Continue without logo if loading fails
            }
            
            // Main Title: "UMelec Official Vote Receipt" - aligned with logo on same row
            val mainTitlePaint = Paint().apply {
                color = android.graphics.Color.parseColor("#333333")
                textSize = 24f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.LEFT
            }
            
            // Calculate title position: to the right of logo with spacing
            val logoSpacing = 20f
            val titleX = if (logoWidth > 0) {
                MARGIN.toFloat() + logoWidth + logoSpacing
            } else {
                MARGIN.toFloat()
            }
            
            // Calculate vertical center alignment for title with logo
            val titleY = if (logoHeight > 0) {
                // Center the title vertically with the logo
                MARGIN.toFloat() + (logoHeight / 2f) + (mainTitlePaint.textSize / 3f)
            } else {
                yPosition
            }
            
            // Draw logo on the left
            logoBitmap?.let { bitmap ->
                val scaledLogo = Bitmap.createScaledBitmap(bitmap, logoWidth.toInt(), logoHeight.toInt(), true)
                canvas.drawBitmap(scaledLogo, MARGIN.toFloat(), MARGIN.toFloat(), null)
                
                // Recycle bitmaps
                if (scaledLogo != bitmap) {
                    scaledLogo.recycle()
                }
                bitmap.recycle()
            }
            
            // Draw title to the right of logo
            canvas.drawText("UMelec Official Vote Receipt", titleX, titleY, mainTitlePaint)
            
            // Calculate position for subtitle below the title (not relative to logo)
            // Subtitle should be positioned below the title with proper spacing
            val titleBottomY = titleY + mainTitlePaint.textSize
            val subtitleSpacing = 8f // Spacing between title and subtitle
            
            // Subtitle: "Commission on Student Election (COSEL)" - below the header title
            val subtitlePaint = Paint().apply {
                color = android.graphics.Color.parseColor("#666666")
                textSize = 14f
                textAlign = Paint.Align.LEFT
            }
            val subtitleY = titleBottomY + subtitleSpacing
            canvas.drawText("Commission on Student Election (COSEL)", titleX, subtitleY, subtitlePaint)
            
            // Set yPosition for next elements (below subtitle)
            yPosition = subtitleY + subtitlePaint.textSize + 30
            
            // Election Title
            val electionPaint = Paint().apply {
                color = android.graphics.Color.parseColor("#333333")
                textSize = 16f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.LEFT
            }
            canvas.drawText(receiptData.electionTitle, MARGIN.toFloat(), yPosition, electionPaint)
            yPosition += 30
            
            // Submitted Date/Time
            val dateFormat = SimpleDateFormat("MMMM dd, yyyy, hh:mm a 'PST'", Locale.getDefault())
            val formattedDate = dateFormat.format(receiptData.submittedAt)
            val datePaint = Paint().apply {
                color = android.graphics.Color.parseColor("#333333")
                textSize = 14f
                textAlign = Paint.Align.LEFT
            }
            canvas.drawText("Submitted: $formattedDate", MARGIN.toFloat(), yPosition, datePaint)
            yPosition += 50
            
            // Vote ID (Reference Code)
            val labelPaint = Paint().apply {
                color = android.graphics.Color.parseColor("#333333")
                textSize = 12f
                textAlign = Paint.Align.LEFT
            }
            val valuePaint = Paint().apply {
                color = android.graphics.Color.parseColor("#333333")
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.LEFT
            }
            canvas.drawText("Vote ID (Reference Code):", MARGIN.toFloat(), yPosition, labelPaint)
            yPosition += 20
            canvas.drawText(receiptData.voteId, MARGIN.toFloat(), yPosition, valuePaint)
            yPosition += 30
            
            // Digital Signature Snippet (for verification)
            val digitalSignatureSnippet = receiptData.digitalSignaturePreview
                ?: receiptData.digitalSignature?.take(8)
                ?: generateDigitalSignatureHash(receiptData.signatureBase64).take(8) // Fallback to hash snippet if DSA signature not available
            canvas.drawText("Digital Signature Snippet:", MARGIN.toFloat(), yPosition, labelPaint)
            yPosition += 20
            canvas.drawText(digitalSignatureSnippet, MARGIN.toFloat(), yPosition, valuePaint)
            yPosition += 40
            
            // "VOTE SECURELY RECORDED" heading
            val recordedPaint = Paint().apply {
                color = android.graphics.Color.parseColor("#00537A")
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.LEFT
            }
            canvas.drawText("VOTE SECURELY RECORDED", MARGIN.toFloat(), yPosition, recordedPaint)
            yPosition += 40
            
            // Privacy Notice
            val noticePaint = Paint().apply {
                color = android.graphics.Color.parseColor("#666666")
                textSize = 11f
                textAlign = Paint.Align.LEFT
            }
            
            val noticeTitlePaint = Paint().apply {
                color = android.graphics.Color.parseColor("#666666")
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.LEFT
            }
            
            val maxTextWidth = (PAGE_WIDTH - (MARGIN * 2)).toFloat()
            
            // Draw "IMPORTANT PRIVACY NOTICE" title
            canvas.drawText("IMPORTANT PRIVACY NOTICE", MARGIN.toFloat(), yPosition, noticeTitlePaint)
            yPosition += 20
            
            // Draw first paragraph with word wrapping
            val paragraph1 = "This receipt serves as cryptographic evidence that a valid ballot was cast and included in the final tally. " +
                    "This document does NOT contain any record of your candidate selections to protect the secrecy of your vote."
            yPosition = drawWrappedText(canvas, paragraph1, noticePaint, MARGIN.toFloat(), yPosition, maxTextWidth)
            yPosition += 10
            
            // Draw second paragraph with word wrapping
            val paragraph2 = "You may use the Digital Signature to verify your vote's inclusion against the official public audit log published by the Adviser post-election."
            yPosition = drawWrappedText(canvas, paragraph2, noticePaint, MARGIN.toFloat(), yPosition, maxTextWidth)
            
            // Finish the page
            document.finishPage(page)
            
            // Save the document to file
            val fileName = "vote_receipt_${receiptData.voteId}_${System.currentTimeMillis()}.pdf"
            val pdfFile = File(context.getExternalFilesDir(null), fileName)
            
            try {
                pdfFile.parentFile?.mkdirs()
                FileOutputStream(pdfFile).use { outputStream: FileOutputStream ->
                    document.writeTo(outputStream)
                }
                document.close()
                
                Log.d(TAG, "PDF generated successfully: ${pdfFile.absolutePath}")
                onSuccess(pdfFile.absolutePath)
            } catch (e: IOException) {
                document.close()
                Log.e(TAG, "Error writing PDF file: ${e.message}", e)
                onFailure("Failed to save PDF: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating PDF: ${e.message}", e)
            onFailure("Failed to generate PDF: ${e.message}")
        }
    }
    
    /**
     * Get PDF file as byte array for email attachment
     */
    fun getPdfAsByteArray(filePath: String): ByteArray? {
        return try {
            File(filePath).readBytes()
        } catch (e: Exception) {
            Log.e(TAG, "Error reading PDF file: ${e.message}", e)
            null
        }
    }
    
    /**
     * Clean up temporary PDF file
     */
    fun deletePdfFile(filePath: String) {
        try {
            File(filePath).delete()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting PDF file: ${e.message}", e)
        }
    }
}

