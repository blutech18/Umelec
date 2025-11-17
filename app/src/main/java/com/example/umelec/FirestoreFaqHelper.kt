package com.example.umelec

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp

/**
 * Helper class for FAQ Firestore operations
 */
object FirestoreFaqHelper {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private const val FAQS_COLLECTION = "faqs"
    private const val TAG = "FirestoreFaqHelper"

    /**
     * Get all FAQs grouped by category
     */
    fun getAllFaqs(
        onSuccess: (List<FaqItem>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        getAllFaqsWithIds(
            onSuccess = { faqsWithIds ->
                onSuccess(faqsWithIds.map { it.faqItem })
            },
            onFailure = onFailure
        )
    }

    /**
     * Get all FAQs with their IDs (for update/delete operations)
     */
    fun getAllFaqsWithIds(
        onSuccess: (List<FaqWithId>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        firestore.collection(FAQS_COLLECTION)
            .whereEqualTo("isActive", true)
            .get()
            .addOnSuccessListener { documents ->
                Log.d(TAG, "Found ${documents.size()} FAQs")
                val faqs = documents.documents.mapNotNull { doc ->
                    val data = doc.data ?: run {
                        Log.w(TAG, "FAQ document ${doc.id} has no data")
                        return@mapNotNull null
                    }
                    val order = (data["order"] as? Long)?.toInt() ?: 0
                    FaqWithId(
                        faqId = doc.id,
                        faqItem = FaqItem(
                            category = data["category"] as? String ?: "General",
                            question = data["question"] as? String ?: "",
                            answer = data["answer"] as? String ?: ""
                        )
                    )
                }
                // Sort by order, then by category, then by question
                val sortedFaqs = faqs.sortedWith(compareBy(
                    { it.faqItem.category },
                    { it.faqItem.question }
                ))
                Log.d(TAG, "Returning ${sortedFaqs.size} sorted FAQs")
                onSuccess(sortedFaqs)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting FAQs: ${exception.message}", exception)
                onFailure(exception.message ?: "Failed to get FAQs")
            }
    }

    /**
     * Create a new FAQ
     */
    fun createFaq(
        category: String,
        question: String,
        answer: String,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        // Get max order for this category
        firestore.collection(FAQS_COLLECTION)
            .whereEqualTo("category", category)
            .whereEqualTo("isActive", true)
            .orderBy("order", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                val nextOrder = if (documents.isEmpty) {
                    0
                } else {
                    val lastOrder = documents.documents[0].get("order") as? Long ?: 0L
                    lastOrder + 1
                }

                val faqData = hashMapOf<String, Any>(
                    "category" to category,
                    "question" to question,
                    "answer" to answer,
                    "order" to nextOrder,
                    "isActive" to true,
                    "createdAt" to Timestamp.now()
                )

                firestore.collection(FAQS_COLLECTION)
                    .add(faqData)
                    .addOnSuccessListener { documentReference ->
                        Log.d(TAG, "FAQ created: ${documentReference.id}")
                        onSuccess(documentReference.id)
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "Error creating FAQ: ${exception.message}", exception)
                        onFailure(exception.message ?: "Failed to create FAQ")
                    }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting FAQ order: ${exception.message}", exception)
                onFailure(exception.message ?: "Failed to create FAQ")
            }
    }

    /**
     * Update an existing FAQ
     */
    fun updateFaq(
        faqId: String,
        question: String,
        answer: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val updateData = hashMapOf<String, Any>(
            "question" to question,
            "answer" to answer,
            "updatedAt" to Timestamp.now()
        )

        firestore.collection(FAQS_COLLECTION)
            .document(faqId)
            .update(updateData)
            .addOnSuccessListener {
                Log.d(TAG, "FAQ updated: $faqId")
                onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error updating FAQ: ${exception.message}", exception)
                onFailure(exception.message ?: "Failed to update FAQ")
            }
    }

    /**
     * Delete an FAQ (soft delete)
     */
    fun deleteFaq(
        faqId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        firestore.collection(FAQS_COLLECTION)
            .document(faqId)
            .update("isActive", false)
            .addOnSuccessListener {
                Log.d(TAG, "FAQ deleted: $faqId")
                onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error deleting FAQ: ${exception.message}", exception)
                onFailure(exception.message ?: "Failed to delete FAQ")
            }
    }

    /**
     * Get FAQ by ID
     */
    fun getFaqById(
        faqId: String,
        onSuccess: (FaqItem?) -> Unit,
        onFailure: (String) -> Unit
    ) {
        firestore.collection(FAQS_COLLECTION)
            .document(faqId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists() && document.getBoolean("isActive") == true) {
                    val data = document.data ?: run {
                        onSuccess(null)
                        return@addOnSuccessListener
                    }
                    val faq = FaqItem(
                        category = data["category"] as? String ?: "General",
                        question = data["question"] as? String ?: "",
                        answer = data["answer"] as? String ?: ""
                    )
                    onSuccess(faq)
                } else {
                    onSuccess(null)
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting FAQ: ${exception.message}", exception)
                onFailure(exception.message ?: "Failed to get FAQ")
            }
    }

    /**
     * Get all unique categories
     */
    fun getCategories(
        onSuccess: (List<String>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        firestore.collection(FAQS_COLLECTION)
            .whereEqualTo("isActive", true)
            .get()
            .addOnSuccessListener { documents ->
                val categories = documents.documents
                    .mapNotNull { it.getString("category") }
                    .distinct()
                    .sorted()
                onSuccess(categories)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting categories: ${exception.message}", exception)
                onFailure(exception.message ?: "Failed to get categories")
            }
    }
}

/**
 * Data class to hold FAQ with its ID
 */
data class FaqWithId(
    val faqId: String,
    val faqItem: FaqItem
)

