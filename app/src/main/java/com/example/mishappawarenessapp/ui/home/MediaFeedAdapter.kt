package com.example.mishappawarenessapp.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mishappawarenessapp.R
import com.example.mishappawarenessapp.model.PostMedia

class MediaFeedAdapter(
    private val mediaList: List<PostMedia>
) : RecyclerView.Adapter<MediaFeedAdapter.MediaViewHolder>() {

    inner class MediaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.feedMediaImage)
        val playIcon: ImageView = view.findViewById(R.id.playIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_feed_media, parent, false)
        return MediaViewHolder(view)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        val media = mediaList[position]

        Glide.with(holder.imageView.context)
            .load(media.url)
            .into(holder.imageView)

        // show play icon only for video
        holder.playIcon.visibility =
            if (media.type == "video") View.VISIBLE else View.GONE
    }

    override fun getItemCount(): Int = mediaList.size
}
