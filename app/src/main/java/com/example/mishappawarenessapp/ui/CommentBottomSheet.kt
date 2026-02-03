package com.example.mishappawarenessapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.mishappawarenessapp.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class CommentBottomSheet : BottomSheetDialogFragment() {

    companion object {
        fun newInstance(postId: String): CommentBottomSheet {
            val sheet = CommentBottomSheet()
            val args = Bundle()
            args.putString("postId", postId)
            sheet.arguments = args
            return sheet
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_comment, container, false)
    }
}
