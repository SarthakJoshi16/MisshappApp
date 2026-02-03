package com.example.mishappawarenessapp.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mishappawarenessapp.databinding.ItemPostBinding
import com.example.mishappawarenessapp.model.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class PostAdapter(private val posts: List<Post>) :
    RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    var onCommentClick: ((String) -> Unit)? = null

    inner class PostViewHolder(val binding: ItemPostBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(post: Post) {
            binding.username.text = post.username
            binding.contentText.text = post.content
            binding.upvotes.text = "↑ ${post.likes}"
            binding.downvotes.text = "↓ ${post.dislikes}"
            binding.commentCount.text = (post.commentCount ?: 0).toString()

            post.timestamp?.let {
                binding.timestamp.text = it.toDate().toString()
            }

            // MEDIA
            if (post.media.isNotEmpty()) {
                binding.postMediaRecycler.visibility = View.VISIBLE
                binding.postMediaRecycler.layoutManager =
                    LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)

                binding.postMediaRecycler.adapter =
                    MediaFeedAdapter(post.media)
            } else {
                binding.postMediaRecycler.visibility = View.GONE
            }


            binding.commentBtn.setOnClickListener {
                onCommentClick?.invoke(post.id)
            }

            binding.upvotes.setOnClickListener {
                val userId = auth.currentUser?.uid ?: return@setOnClickListener
                if (post.likedBy.contains(userId)) return@setOnClickListener

                firestore.collection("posts").document(post.id).update(
                    mapOf(
                        "likes" to FieldValue.increment(1),
                        "likedBy" to FieldValue.arrayUnion(userId)
                    )
                )
            }

            binding.downvotes.setOnClickListener {
                val userId = auth.currentUser?.uid ?: return@setOnClickListener
                if (post.dislikedBy.contains(userId)) return@setOnClickListener

                firestore.collection("posts").document(post.id).update(
                    mapOf(
                        "dislikes" to FieldValue.increment(1),
                        "dislikedBy" to FieldValue.arrayUnion(userId)
                    )
                )
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding =
            ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(posts[position])
    }

    override fun getItemCount(): Int = posts.size
}
