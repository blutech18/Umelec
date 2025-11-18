package com.example.umelec

import android.util.Base64
import android.util.Log
import org.json.JSONObject
import java.security.*
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.TreeMap

/**
 * Helper class for vote cryptography using DSA (Digital Signature Algorithm)
 * Generates key pairs and signs vote data for verification
 */
object VoteCryptographyHelper {
    private const val TAG = "VoteCryptographyHelper"
    private const val ALGORITHM = "DSA"
    private const val KEY_SIZE = 2048 // DSA key size (1024, 2048, or 3072 bits)

    /**
     * Data class to hold DSA key pair
     */
    data class DSAKeyPair(
        val publicKey: String,  // Base64 encoded public key
        val privateKey: String   // Base64 encoded private key
    )

    /**
     * Generate DSA key pair for vote submission
     * @return DSAKeyPair containing base64-encoded public and private keys
     */
    fun generateDSAKeyPair(): DSAKeyPair? {
        return try {
            val keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM)
            keyPairGenerator.initialize(KEY_SIZE, SecureRandom())
            val keyPair = keyPairGenerator.generateKeyPair()

            val publicKeyBytes = keyPair.public.encoded
            val privateKeyBytes = keyPair.private.encoded

            val publicKeyBase64 = Base64.encodeToString(publicKeyBytes, Base64.NO_WRAP)
            val privateKeyBase64 = Base64.encodeToString(privateKeyBytes, Base64.NO_WRAP)

            Log.d(TAG, "DSA key pair generated successfully (${KEY_SIZE} bits)")

            DSAKeyPair(
                publicKey = publicKeyBase64,
                privateKey = privateKeyBase64
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error generating DSA key pair: ${e.message}", e)
            null
        }
    }

    /**
     * Sign vote data using DSA private key
     * @param voteData The vote data to sign (e.g., voteId + selections as JSON string)
     * @param privateKeyBase64 Base64 encoded private key
     * @return Base64 encoded signature, or null if signing fails
     */
    fun signVoteData(voteData: String, privateKeyBase64: String): String? {
        return try {
            // Decode private key
            val privateKeyBytes = Base64.decode(privateKeyBase64, Base64.NO_WRAP)
            val keySpec = PKCS8EncodedKeySpec(privateKeyBytes)
            val keyFactory = KeyFactory.getInstance(ALGORITHM)
            val privateKey = keyFactory.generatePrivate(keySpec)

            // Create signature
            val signature = Signature.getInstance("SHA256withDSA")
            signature.initSign(privateKey)
            signature.update(voteData.toByteArray(Charsets.UTF_8))
            val signatureBytes = signature.sign()

            // Return base64 encoded signature
            val signatureBase64 = Base64.encodeToString(signatureBytes, Base64.NO_WRAP)
            Log.d(TAG, "Vote data signed successfully")
            signatureBase64
        } catch (e: Exception) {
            Log.e(TAG, "Error signing vote data: ${e.message}", e)
            null
        }
    }

    /**
     * Verify vote signature using DSA public key
     * @param voteData The original vote data that was signed
     * @param signatureBase64 Base64 encoded signature
     * @param publicKeyBase64 Base64 encoded public key
     * @return true if signature is valid, false otherwise
     */
    fun verifyVoteSignature(
        voteData: String,
        signatureBase64: String,
        publicKeyBase64: String
    ): Boolean {
        return try {
            // Decode public key
            val publicKeyBytes = Base64.decode(publicKeyBase64, Base64.NO_WRAP)
            val keySpec = X509EncodedKeySpec(publicKeyBytes)
            val keyFactory = KeyFactory.getInstance(ALGORITHM)
            val publicKey = keyFactory.generatePublic(keySpec)

            // Decode signature
            val signatureBytes = Base64.decode(signatureBase64, Base64.NO_WRAP)

            // Verify signature
            val signature = Signature.getInstance("SHA256withDSA")
            signature.initVerify(publicKey)
            signature.update(voteData.toByteArray(Charsets.UTF_8))
            val isValid = signature.verify(signatureBytes)

            Log.d(TAG, "Signature verification result: $isValid")
            isValid
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying signature: ${e.message}", e)
            false
        }
    }

    /**
     * Build the canonical vote data string used for signing and verification.
     * Ensures consistent ordering of selections and candidate data.
     */
    fun buildVoteDataString(
        voteId: String,
        electionId: String,
        selections: Map<String, Map<String, String>>
    ): String {
        val sortedSelections = TreeMap<String, Map<String, String>>()
        sortedSelections.putAll(selections)

        val selectionsJson = JSONObject()
        sortedSelections.forEach { (positionId, candidateData) ->
            val sortedCandidateData = TreeMap<String, String>()
            sortedCandidateData.putAll(candidateData)

            val candidateJson = JSONObject()
            sortedCandidateData.forEach { (key, value) ->
                candidateJson.put(key, value)
            }
            selectionsJson.put(positionId, candidateJson)
        }

        return "$voteId|$electionId|${selectionsJson.toString()}"
    }

    /**
     * Get a preview snippet of the signature (first 8 characters)
     * Used for display in receipt verification
     */
    fun getSignaturePreview(signatureBase64: String?): String {
        return signatureBase64?.take(8) ?: ""
    }
}

