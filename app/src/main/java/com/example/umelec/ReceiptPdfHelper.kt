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
import java.io.FileOutputStream
import java.io.IOException
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
        val electionTitle: String,
        val userName: String,
        val userEmail: String,
        val submittedAt: Date,
        val signatureBase64: String?,
        val selections: Map<String, Map<String, String>> // positionId -> (candidateId, candidateName, positionName)
    )
    
    /**
     * Generate PDF receipt file
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
            var page = document.startPage(pageInfo)
            var canvas = page.canvas
            
            var yPosition = MARGIN.toFloat()
            
            // Title
            val titlePaint = Paint().apply {
                color = android.graphics.Color.parseColor("#00537A")
                textSize = 28f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("VOTE RECEIPT", PAGE_WIDTH / 2f, yPosition, titlePaint)
            yPosition += 50
            
            // Election Title
            val electionPaint = Paint().apply {
                color = android.graphics.Color.parseColor("#333333")
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(receiptData.electionTitle, PAGE_WIDTH / 2f, yPosition, electionPaint)
            yPosition += 60
            
            // Draw line separator
            val linePaint = Paint().apply {
                color = android.graphics.Color.parseColor("#CCCCCC")
                strokeWidth = 2f
            }
            canvas.drawLine(MARGIN.toFloat(), yPosition, (PAGE_WIDTH - MARGIN).toFloat(), yPosition, linePaint)
            yPosition += 30
            
            // Receipt Information Section
            val sectionPaint = Paint().apply {
                color = android.graphics.Color.parseColor("#666666")
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.LEFT
            }
            val valuePaint = Paint().apply {
                color = android.graphics.Color.parseColor("#333333")
                textSize = 12f
                textAlign = Paint.Align.LEFT
            }
            
            // Reference Code
            canvas.drawText("Reference Code:", MARGIN.toFloat(), yPosition, sectionPaint)
            canvas.drawText(receiptData.voteId, MARGIN.toFloat() + 150, yPosition, valuePaint)
            yPosition += 25
            
            // Submission Date
            val dateFormat = SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
            val formattedDate = dateFormat.format(receiptData.submittedAt)
            canvas.drawText("Submission Date:", MARGIN.toFloat(), yPosition, sectionPaint)
            canvas.drawText(formattedDate, MARGIN.toFloat() + 150, yPosition, valuePaint)
            yPosition += 25
            
            // User Information
            canvas.drawText("Voter Name:", MARGIN.toFloat(), yPosition, sectionPaint)
            canvas.drawText(receiptData.userName, MARGIN.toFloat() + 150, yPosition, valuePaint)
            yPosition += 25
            
            canvas.drawText("Email:", MARGIN.toFloat(), yPosition, sectionPaint)
            canvas.drawText(receiptData.userEmail, MARGIN.toFloat() + 150, yPosition, valuePaint)
            yPosition += 40
            
            // Draw line separator
            canvas.drawLine(MARGIN.toFloat(), yPosition, (PAGE_WIDTH - MARGIN).toFloat(), yPosition, linePaint)
            yPosition += 30
            
            // Voting Choices Section
            val choicesTitlePaint = Paint().apply {
                color = android.graphics.Color.parseColor("#00537A")
                textSize = 16f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.LEFT
            }
            canvas.drawText("VOTING CHOICES", MARGIN.toFloat(), yPosition, choicesTitlePaint)
            yPosition += 30
            
            // Draw selections
            receiptData.selections.forEach { (positionId, candidateData) ->
                val positionName = candidateData["positionName"] ?: "Unknown Position"
                val candidateName = candidateData["candidateName"] ?: "Unknown Candidate"
                
                // Position name
                canvas.drawText(positionName, MARGIN.toFloat() + 20, yPosition, sectionPaint)
                yPosition += 20
                
                // Candidate name (indented)
                canvas.drawText("• $candidateName", MARGIN.toFloat() + 40, yPosition, valuePaint)
                yPosition += 25
                
                // Check if we need a new page
                if (yPosition > PAGE_HEIGHT - 150) {
                    document.finishPage(page)
                    val newPageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, document.pages.size + 1).create()
                    page = document.startPage(newPageInfo)
                    canvas = page.canvas
                    yPosition = MARGIN.toFloat()
                }
            }
            
            yPosition += 30
            
            // Draw line separator
            canvas.drawLine(MARGIN.toFloat(), yPosition, (PAGE_WIDTH - MARGIN).toFloat(), yPosition, linePaint)
            yPosition += 30
            
            // Signature Section
            val signatureTitlePaint = Paint().apply {
                color = android.graphics.Color.parseColor("#666666")
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.LEFT
            }
            canvas.drawText("Signature:", MARGIN.toFloat(), yPosition, signatureTitlePaint)
            yPosition += 20
            
            // Draw signature image if available
            receiptData.signatureBase64?.let { signatureBase64 ->
                try {
                    val signatureBytes = Base64.decode(signatureBase64, Base64.DEFAULT)
                    val signatureBitmap = BitmapFactory.decodeByteArray(signatureBytes, 0, signatureBytes.size)
                    
                    if (signatureBitmap != null) {
                        // Scale signature to fit (max width 200, maintain aspect ratio)
                        val maxWidth = 200f
                        val scale = if (signatureBitmap.width > maxWidth) {
                            maxWidth / signatureBitmap.width
                        } else {
                            1f
                        }
                        val scaledWidth = signatureBitmap.width * scale
                        val scaledHeight = signatureBitmap.height * scale
                        
                        val scaledBitmap = Bitmap.createScaledBitmap(signatureBitmap, scaledWidth.toInt(), scaledHeight.toInt(), true)
                        canvas.drawBitmap(scaledBitmap, MARGIN.toFloat(), yPosition, null)
                        yPosition += scaledHeight + 20
                        
                        signatureBitmap.recycle()
                        scaledBitmap.recycle()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error decoding signature: ${e.message}")
                    canvas.drawText("Signature: [Encoded]", MARGIN.toFloat() + 20, yPosition, valuePaint)
                    yPosition += 20
                }
            } ?: run {
                canvas.drawText("Signature: N/A", MARGIN.toFloat() + 20, yPosition, valuePaint)
                yPosition += 20
            }
            
            yPosition += 40
            
            // Footer
            val footerPaint = Paint().apply {
                color = android.graphics.Color.parseColor("#999999")
                textSize = 10f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("This is an official receipt of your vote submission.", PAGE_WIDTH / 2f, yPosition, footerPaint)
            yPosition += 15
            canvas.drawText("Please keep this receipt for your records.", PAGE_WIDTH / 2f, yPosition, footerPaint)
            yPosition += 15
            canvas.drawText("Generated by UMelec", PAGE_WIDTH / 2f, yPosition, footerPaint)
            
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

