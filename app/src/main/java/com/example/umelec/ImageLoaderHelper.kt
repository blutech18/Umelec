package com.example.umelec

import android.widget.ImageView
import com.bumptech.glide.Glide

/**
 * Helper object for loading candidate images from URLs or drawable resources
 */
object ImageLoaderHelper {
    /**
     * Load candidate image into ImageView
     * @param imageView Target ImageView to load image into
     * @param photoUrl Optional URL for the candidate photo
     * @param defaultResource Default drawable resource ID if URL is not available
     */
    fun loadCandidateImage(
        imageView: ImageView,
        photoUrl: String?,
        defaultResource: Int = R.drawable.ic_profile
    ) {
        // Default avatar URL to use when photoUrl is not available
        val defaultAvatarUrl = "https://images.icon-icons.com/1378/PNG/512/avatardefault_92824.png"
        
        // Always use URL - either the provided photoUrl or the default avatar URL
        // Check for null, empty, or blank strings
        val urlToLoad = if (!photoUrl.isNullOrBlank() && photoUrl.isNotEmpty()) {
            photoUrl.trim()
        } else {
            defaultAvatarUrl
        }
        
        android.util.Log.d("ImageLoaderHelper", "Loading image: photoUrl=$photoUrl, urlToLoad=$urlToLoad")
        
        // Load from URL using Glide
        Glide.with(imageView.context)
            .load(urlToLoad)
            .placeholder(defaultResource)
            .error(defaultResource)
            .centerCrop()
            .into(imageView)
    }
}

