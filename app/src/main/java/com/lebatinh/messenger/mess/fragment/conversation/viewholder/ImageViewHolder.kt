package com.lebatinh.messenger.mess.fragment.conversation.viewholder

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lebatinh.messenger.R
import com.lebatinh.messenger.databinding.MessageImageItemBinding
import com.lebatinh.messenger.helper.TimeHelper
import com.lebatinh.messenger.mess.fragment.conversation.Message
import com.lebatinh.messenger.other.MessageType
import com.lebatinh.messenger.user.User

class ImageViewHolder(
    private val binding: MessageImageItemBinding,
    currentUserId: String,
    searchUserById: suspend (String) -> User?,
    onClickItem: (Message) -> Unit,
    onLongClickItem: (Message) -> Unit
) :
    MessageViewHolder(
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

        binding.imgMessage.apply {
            setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onClickItem(messages[position])
                }
            }
        }
    }

    override fun bind(item: Message, position: Int, items: List<Message>) {
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

        if (item.type == MessageType.IMAGE) {
            item.urlMedia?.first()?.let {
                binding.imgMessage.apply {
                    val size =
                        context.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._100sdp)
                    layoutParams = layoutParams.apply {
                        width = size
                        height = size
                    }
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    adjustViewBounds = false
                    Glide.with(context)
                        .load(it)
                        .placeholder(R.drawable.image_upload)
                        .error(R.drawable.image_error)
                        .into(this)
                }
            }

            val overlay = item.urlMedia?.size?.minus(1)
            if (overlay != null) {
                binding.tvOverlay.apply {
                    visibility = View.VISIBLE
                    text = if (overlay > 0) "+$overlay" else null
                }
            }
        } else if (item.type == MessageType.LIKE) {
            binding.imgMessage.apply {
                layoutParams = layoutParams.apply {
                    width = ViewGroup.LayoutParams.WRAP_CONTENT
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                }
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                adjustViewBounds = true
                setImageResource(R.drawable.like)
            }
        }

        binding.tvStatus.text = item.status
        binding.tvTimeSend.text = item.timeSend?.let { TimeHelper().formatTimeSend(it) }
    }
}