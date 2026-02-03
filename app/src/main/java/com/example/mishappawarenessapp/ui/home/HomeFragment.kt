package com.example.mishappawarenessapp.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mishappawarenessapp.R
import com.example.mishappawarenessapp.model.Post
import com.example.mishappawarenessapp.ui.CommentBottomSheet
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HomeFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var postAdapter: PostAdapter
    private val postList = mutableListOf<Post>()
    private var isFirstLoad = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestore = FirebaseFirestore.getInstance()

        val recyclerView = view.findViewById<RecyclerView>(R.id.postRecycler)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        postAdapter = PostAdapter(postList)
        recyclerView.adapter = postAdapter

        postAdapter.onCommentClick = { postId: String ->
            CommentBottomSheet
                .newInstance(postId)
                .show(parentFragmentManager, "CommentBottomSheet")
        }

        fetchPosts()
    }

    private fun fetchPosts() {
        firestore.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->

                if (error != null) {
                    Log.e("HomeFragment", "Error fetching posts", error)
                    return@addSnapshotListener
                }

                if (snapshots == null) return@addSnapshotListener

                if (isFirstLoad) postList.clear()

                for (change in snapshots.documentChanges) {
                    val post = change.document.toObject(Post::class.java).apply {
                        id = change.document.id
                    }

                    when (change.type) {
                        DocumentChange.Type.ADDED -> {
                            postList.add(change.newIndex, post)
                            postAdapter.notifyItemInserted(change.newIndex)
                        }
                        DocumentChange.Type.MODIFIED -> {
                            postList[change.newIndex] = post
                            postAdapter.notifyItemChanged(change.newIndex)
                        }
                        DocumentChange.Type.REMOVED -> {
                            postList.removeAt(change.oldIndex)
                            postAdapter.notifyItemRemoved(change.oldIndex)
                        }
                    }
                }

                isFirstLoad = false
            }
    }
}
