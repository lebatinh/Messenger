package com.lebatinh.messenger.mess.fragment.conversation.media

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lebatinh.messenger.databinding.MediaPageItemBinding

class MediaPageAdapter(
    private val uris: List<String>,
    private val isVideo: Boolean
) : RecyclerView.Adapter<MediaPageAdapter.MediaPageViewHolder>() {
    private val playerMap = mutableMapOf<Int, ExoPlayer>()

    inner class MediaPageViewHolder(private val binding: MediaPageItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(position: Int) {
            val uri = uris[position]
            when (isVideo) {
                true -> {
                    binding.videoFullscreen.visibility = View.VISIBLE
                    binding.photoFullscreen.visibility = View.GONE

                    val player = ExoPlayer.Builder(binding.root.context).build().apply {
                        setMediaItem(MediaItem.fromUri(uri))
                        prepare()
                    }

                    binding.videoFullscreen.player = player
                    playerMap[position] = player
                }

                false -> {
                    binding.videoFullscreen.visibility = View.GONE
                    binding.photoFullscreen.visibility = View.VISIBLE

                    Glide.with(binding.root.context).load(uri).into(binding.photoFullscreen)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaPageViewHolder {
        val binding =
            MediaPageItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MediaPageViewHolder(binding)
    }

    override fun getItemCount(): Int = uris.size

    override fun onBindViewHolder(holder: MediaPageViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun onViewRecycled(holder: MediaPageViewHolder) {
        super.onViewRecycled(holder)

        val position = holder.absoluteAdapterPosition

        if (isVideo && position != RecyclerView.NO_POSITION) {
            playerMap[position]?.release()
            playerMap.remove(position)
        }
    }

    fun releaseAllPlayer() {
        playerMap.forEach { (_, player) ->
            player.release()
        }
        playerMap.clear()
    }
}