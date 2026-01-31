package com.example.mishappawarenessapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth

class CommentBottomSheet : BottomSheetDialogFragment() {

    private val commentRepository = CommentRepository()
    private lateinit var postId: String


    companion object {
        fun newInstance(postId: String): CommentBottomSheet {
            val sheet = CommentBottomSheet()
            sheet.arguments = Bundle().apply {
                putString("postId", postId)
            }
            return sheet
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_comments, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.commentRecycler)
        val commentInput = view.findViewById<EditText>(R.id.commentInput)
        val sendBtn = view.findViewById<ImageView>(R.id.sendBtn)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        sendBtn.setOnClickListener {

            val text = commentInput.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener

            val user = FirebaseAuth.getInstance().currentUser
                ?: return@setOnClickListener

            commentRepository.addComment(
                postId = postId,
                commentText = text,
                userId = user.uid,
                username = user.email ?: "user"
            )

            commentInput.text.clear()
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        postId = requireArguments().getString("postId")!!
    }

}
