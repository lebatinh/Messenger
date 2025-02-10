package com.lebatinh.messenger.mess.fragment.conversation.viewholder

import android.view.View
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.RecyclerView
import com.lebatinh.messenger.R
import com.lebatinh.messenger.databinding.MessageVideoItemBinding
import com.lebatinh.messenger.helper.TimeHelper
import com.lebatinh.messenger.mess.fragment.conversation.Message
import com.lebatinh.messenger.user.User

class VideoViewHolder(
    private val binding: MessageVideoItemBinding,
    currentUserId: String,
    searchUserById: suspend (String) -> User?,
    onClickItem: (Message) -> Unit,
    onLongClickItem: (Message) -> Unit
) : MessageViewHolder(
    binding.root,
    currentUserId,
    searchUserById
) {

    private var messages: List<Message> = emptyList()

    init {
        binding.root.apply {
            setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    binding.messageItem.run {
                        if (currentState == R.id.start) transitionToEnd()
                        else transitionToStart()
                    }
                }
            }
            setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onLongClickItem(messages[position])
                }
                true
            }
        }

        binding.pvMessage.apply {
            setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onClickItem(messages[position])
                }
            }
        }
    }

    override fun bind(item: Message, position: Int, items: MutableList<Message>) {
        messages = items
        val isCurrentId = item.senderId == currentUserId
        setupMessageLayout(
            binding.messageItem,
            binding.cvAvatar,
            binding.imgAvatar,
            isCurrentId,
            position,
            items
        )

        // Setup video player
        item.urlMedia?.first()?.let {
            binding.pvMessage.player = ExoPlayer.Builder(binding.root.context).build().apply {
                setMediaItem(MediaItem.fromUri(it))
                prepare()
            }
        }

        val overlay = item.urlMedia?.size?.minus(1)
        if (overlay != null) {
            binding.tvOverlay.apply {
                visibility = View.VISIBLE
                text = if (overlay > 0) "+$overlay" else null
            }
        }

        binding.tvStatus.text = item.status
        binding.tvTimeSend.text = item.timeSend?.let { TimeHelper().formatTimeSend(it) }
    }
}