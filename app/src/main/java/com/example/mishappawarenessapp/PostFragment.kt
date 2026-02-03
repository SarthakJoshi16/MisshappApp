package com.example.mishappawarenessapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import supabase.uploadPostMedia
import java.io.File

class PostFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private val selectedMedia = mutableListOf<Uri>()
    private val MAX_MEDIA = 5

    private lateinit var mediaAdapter: MediaPreviewAdapter
    private lateinit var mediaCountText: TextView
    private lateinit var postButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_post, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())

        val postContent = view.findViewById<EditText>(R.id.postContent)
        val addMediaButton = view.findViewById<Button>(R.id.btnAddMedia)
        postButton = view.findViewById(R.id.btnPost)

        progressBar = view.findViewById(R.id.uploadProgressBar)
        progressText = view.findViewById(R.id.uploadProgressText)
        mediaCountText = view.findViewById(R.id.mediaCountText)

        val mediaRecycler = view.findViewById<RecyclerView>(R.id.mediaPreviewRecycler)

        mediaAdapter = MediaPreviewAdapter(selectedMedia) { index ->
            selectedMedia.removeAt(index)
            mediaAdapter.notifyDataSetChanged()
            mediaCountText.text = "${selectedMedia.size} / 5 selected"
        }

        mediaRecycler.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        mediaRecycler.adapter = mediaAdapter

        addMediaButton.setOnClickListener { openMediaPicker() }

        postButton.setOnClickListener {
            val content = postContent.text.toString().trim()
            if (content.isEmpty()) {
                postContent.error = "Post cannot be empty"
                return@setOnClickListener
            }
            uploadMediaAndCreatePost(content)
        }
    }

    /* ---------------- MEDIA PICKER ---------------- */

    private val mediaPickerLauncher =
        registerForActivityResult(
            ActivityResultContracts.PickMultipleVisualMedia(MAX_MEDIA)
        ) { uris ->
            if (uris.isNotEmpty()) {
                selectedMedia.clear()
                selectedMedia.addAll(uris)
                mediaAdapter.notifyDataSetChanged()
                mediaCountText.text = "${selectedMedia.size} / 5 selected"
            }
        }

    private fun openMediaPicker() {
        mediaPickerLauncher.launch(
            PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.ImageAndVideo
            )
        )
    }

    /* ---------------- UPLOAD ---------------- */

    private fun uploadMediaAndCreatePost(content: String) {

        Log.d("PostFragment", "Upload started. Media count = ${selectedMedia.size}")

        val user = auth.currentUser ?: return
        setUploading(true)

        lifecycleScope.launch {
            try {
                val uploadedMedia = mutableListOf<Map<String, String>>()

                for (uri in selectedMedia) {

                    val isVideo = isVideo(uri)
                    Log.d("PostFragment", "Uploading URI=$uri isVideo=$isVideo")

                    val mediaFile = withContext(Dispatchers.IO) {
                        if (isVideo) uriToVideoFile(uri)
                        else compressImage(uri)
                    }

                    val mediaUrl = uploadPostMedia(mediaFile, user.uid)

                    var thumbUrl: String? = null
                    if (isVideo) {
                        generateVideoThumbnail(uri)?.let { bmp ->
                            val thumbFile = bitmapToFile(bmp)
                            thumbUrl = uploadPostMedia(thumbFile, user.uid)
                        }
                    }

                    uploadedMedia.add(
                        mutableMapOf(
                            "url" to mediaUrl,
                            "type" to if (isVideo) "video" else "image"
                        ).apply {
                            thumbUrl?.let { put("thumbnail", it) }
                        }
                    )
                }

                savePost(content, uploadedMedia)

            } catch (e: Exception) {
                Log.e("PostFragment", "Upload failed", e)
                Toast.makeText(requireContext(), "Upload failed", Toast.LENGTH_SHORT).show()
                setUploading(false)
            }
        }
    }

    /* ---------------- HELPERS ---------------- */

    private fun compressImage(uri: Uri): File {
        val input = requireContext().contentResolver.openInputStream(uri)!!
        val original = File.createTempFile("img_", ".jpg", requireContext().cacheDir)
        input.copyTo(original.outputStream())
        val bitmap = BitmapFactory.decodeFile(original.absolutePath)
            ?: throw IllegalStateException("Bitmap decode failed")
        val out = File.createTempFile("compressed_", ".jpg", requireContext().cacheDir)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, out.outputStream())
        return out
    }

    private fun uriToVideoFile(uri: Uri): File {
        val file = File.createTempFile("video_", ".mp4", requireContext().cacheDir)
        requireContext().contentResolver.openInputStream(uri)!!
            .copyTo(file.outputStream())
        return file
    }

    private fun generateVideoThumbnail(uri: Uri): Bitmap? =
        try {
            MediaMetadataRetriever().run {
                setDataSource(requireContext(), uri)
                val bmp = getFrameAtTime(1_000_000)
                release()
                bmp
            }
        } catch (e: Exception) {
            null
        }

    private fun bitmapToFile(bitmap: Bitmap): File {
        val file = File.createTempFile("thumb_", ".jpg", requireContext().cacheDir)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, file.outputStream())
        return file
    }

    private fun isVideo(uri: Uri): Boolean =
        requireContext().contentResolver.getType(uri)?.startsWith("video/") == true

    private fun setUploading(state: Boolean) {
        postButton.isEnabled = !state
        progressBar.visibility = if (state) View.VISIBLE else View.GONE
        progressText.visibility = if (state) View.VISIBLE else View.GONE
    }

    private fun savePost(content: String, media: List<Map<String, String>>) {
        val user = auth.currentUser ?: return
        firestore.collection("posts")
            .add(
                hashMapOf(
                    "userId" to user.uid,
                    "username" to (user.email ?: "Anonymous"),
                    "content" to content,
                    "media" to media,
                    "timestamp" to FieldValue.serverTimestamp()
                )
            )
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Post published", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
    }
}
