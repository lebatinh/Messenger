package com.lebatinh.messenger.mess.fragment.conversation.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.lebatinh.messenger.R
import com.lebatinh.messenger.databinding.MessageTextItemBinding
import com.lebatinh.messenger.helper.TimeHelper
import com.lebatinh.messenger.mess.fragment.conversation.Message
import com.lebatinh.messenger.user.User

class TextViewHolder(
    private val binding: MessageTextItemBinding,
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
                    onClickItem(messages[position])
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

        binding.tvMessage.apply {
            maxWidth = context.resources.displayMetrics.widthPixels * 2 / 3
            setBackgroundResource(
                getBackgroundForMessage(
                    isCurrentId,
                    position > 0 && items[position - 1].senderId == item.senderId,
                    position < items.lastIndex && items[position + 1].senderId == item.senderId
                )
            )
            text = item.message
        }

        binding.tvStatus.text = item.status
        binding.tvTimeSend.text = item.timeSend?.let { TimeHelper().formatTimeSend(it) }
    }
}