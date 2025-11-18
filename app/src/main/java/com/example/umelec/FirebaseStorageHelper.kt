package com.example.umelec

import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.UUID

/**
 * Helper class for Firebase Storage operations
 */
object FirebaseStorageHelper {
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
    private const val TAG = "FirebaseStorageHelper"
    private const val CANDIDATES_FOLDER = "candidate_photos"

    /**
     * Upload a candidate photo to Firebase Storage
     * @param imageUri The local URI of the image to upload
     * @param candidateId The ID of the candidate (used for filename)
     * @param onSuccess Callback with the download URL when upload succeeds
     * @param onFailure Callback when upload fails
     * @param onProgress Optional callback for upload progress (0-100)
     */
    fun uploadCandidatePhoto(
        imageUri: Uri,
        candidateId: String,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit,
        onProgress: ((Int) -> Unit)? = null
    ) {
        try {
            // Create a unique filename for the photo
            val fileName = "${candidateId}_${UUID.randomUUID()}.jpg"
            val photoRef: StorageReference = storage.reference
                .child(CANDIDATES_FOLDER)
                .child(fileName)

            Log.d(TAG, "Starting photo upload for candidate: $candidateId")
            Log.d(TAG, "Upload path: $CANDIDATES_FOLDER/$fileName")

            val uploadTask = photoRef.putFile(imageUri)

            // Add progress listener if provided
            onProgress?.let { progressCallback ->
                uploadTask.addOnProgressListener { taskSnapshot ->
                    val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                    progressCallback(progress)
                }
            }

            uploadTask.addOnSuccessListener { taskSnapshot ->
                Log.d(TAG, "Photo upload successful for candidate: $candidateId")
                
                // Get the download URL
                photoRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    val downloadUrl = downloadUri.toString()
                    Log.d(TAG, "Download URL obtained: $downloadUrl")
                    onSuccess(downloadUrl)
                }.addOnFailureListener { exception ->
                    Log.e(TAG, "Failed to get download URL: ${exception.message}", exception)
                    onFailure("Failed to get download URL: ${exception.message}")
                }
            }.addOnFailureListener { exception ->
                Log.e(TAG, "Photo upload failed for candidate $candidateId: ${exception.message}", exception)
                onFailure("Upload failed: ${exception.message}")
            }

        } catch (exception: Exception) {
            Log.e(TAG, "Error starting photo upload: ${exception.message}", exception)
            onFailure("Error starting upload: ${exception.message}")
        }
    }

    /**
     * Delete a candidate photo from Firebase Storage
     * @param photoUrl The download URL of the photo to delete
     * @param onSuccess Callback when deletion succeeds
     * @param onFailure Callback when deletion fails
     */
    fun deleteCandidatePhoto(
        photoUrl: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        try {
            if (photoUrl.isBlank()) {
                onSuccess() // Nothing to delete
                return
            }

            // Create a reference from the URL
            val photoRef = storage.getReferenceFromUrl(photoUrl)
            
            Log.d(TAG, "Deleting photo: $photoUrl")
            
            photoRef.delete().addOnSuccessListener {
                Log.d(TAG, "Photo deleted successfully")
                onSuccess()
            }.addOnFailureListener { exception ->
                Log.e(TAG, "Failed to delete photo: ${exception.message}", exception)
                onFailure("Failed to delete photo: ${exception.message}")
            }

        } catch (exception: Exception) {
            Log.e(TAG, "Error deleting photo: ${exception.message}", exception)
            onFailure("Error deleting photo: ${exception.message}")
        }
    }

    /**
     * Check if a URL is a valid Firebase Storage URL
     */
    fun isFirebaseStorageUrl(url: String?): Boolean {
        return url?.contains("firebasestorage.googleapis.com") == true ||
               url?.contains("firebasestorage.app") == true
    }
}
