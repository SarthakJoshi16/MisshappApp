package com.example.mishappawarenessapp.model

import com.example.mishappawarenessapp.model.PostMedia
import com.google.firebase.Timestamp

data class Post(
    var id: String = "",
    var userId: String = "",
    var username: String = "",
    var content: String = "",

    var media: List<PostMedia> = emptyList(),

    var likes: Long = 0,
    var dislikes: Long = 0,
    var commentCount: Long = 0,

    var likedBy: List<String> = emptyList(),
    var dislikedBy: List<String> = emptyList(),

    var timestamp: Timestamp? = null
)
