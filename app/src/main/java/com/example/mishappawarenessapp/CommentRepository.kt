package com.example.mishappawarenessapp

import com.google.firebase.firestore.FirebaseFirestore

class CommentRepository {

    private val firestore = FirebaseFirestore.getInstance()

    fun addComment(
        postId: String,
        commentText: String,
        userId: String,
        username: String
    ) {
        val comment = hashMapOf(
            "commentText" to commentText,
            "userId" to userId,
            "username" to username,
            "timestamp" to System.currentTimeMillis()
        )

        firestore
            .collection("posts")
            .document(postId)
            .collection("comments")
            .add(comment)
    }
}
