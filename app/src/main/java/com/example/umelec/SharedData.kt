package com.example.umelec

// Data class to represent a notification item (MOVED HERE)
//data class NotificationItem(val title: String, val isRead: Boolean)

// If you have it, also move the FaqItem data class here
//data class FaqItem(val question: String, val answer: String)

// ⭐️ NEW: Data structure for Candidate's detailed platform/comparison data
data class CandidatePlatformDetails(
    val candidateId: String,
    val name: String,
    val position: String,
    val courseInfo: String,
    val profilePictureResource: Int,
    val credentials: String,
    val advocacy: String
)

enum class ElectionState {
    ONGOING,
    NO_ELECTION,
    UPCOMING,
    ENDED
}

data class ElectionDetails(val title: String, val period: String, val status: String)

data class CandidateChoices(val id: String, val name: String)
data class VotingPosition(val id: String, val title: String, val candidates: List<CandidateChoices>)

// ⭐️ Data class for the leading candidate carousel content (Copied from Results.kt)
data class LeadingCandidate(
    val position: String,
    val name: String,
    val votes: Int,
    val profileResId: Int // Resource ID for the drawable/image (e.g., R.drawable.ic_profile)
)

// ⭐️ Enum to manage the state of the results card (Copied from Results.kt)
enum class ResultCardState { UPCOMING, ONGOING, NO_ELECTION, ENDED }

data class ElectionDateandTimeDetails(
    val endDate: String,
    val endTime: String
)