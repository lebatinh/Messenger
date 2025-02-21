package com.lebatinh.messenger.mess.fragment.conversation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.lebatinh.messenger.databinding.MessageImageItemBinding
import com.lebatinh.messenger.databinding.MessageTextItemBinding
import com.lebatinh.messenger.databinding.MessageVideoItemBinding
import com.lebatinh.messenger.mess.fragment.ItemDiffCallback
import com.lebatinh.messenger.mess.fragment.conversation.viewholder.ImageViewHolder
import com.lebatinh.messenger.mess.fragment.conversation.viewholder.MessageViewHolder
import com.lebatinh.messenger.mess.fragment.conversation.viewholder.TextViewHolder
import com.lebatinh.messenger.mess.fragment.conversation.viewholder.VideoViewHolder
import com.lebatinh.messenger.other.MessageType
import com.lebatinh.messenger.user.User

class MessageAdapter(
    private val currentUserId: String,
    private val searchUserById: suspend (String) -> User?,
    private val onClickItem: (Message) -> Unit,
    private val onLongClickItem: (Message) -> Unit,
    private val lifecycleOwner: LifecycleOwner
) : RecyclerView.Adapter<MessageViewHolder>() {

    private var items: MutableList<Message> = mutableListOf()

    companion object {
        const val VIEW_TYPE_TEXT = 1
        const val VIEW_TYPE_IMAGE = 2
        const val VIEW_TYPE_VIDEO = 3
    }

    override fun onViewRecycled(holder: MessageViewHolder) {
        super.onViewRecycled(holder)
        holder.onRecycled()
    }

    override fun onViewDetachedFromWindow(holder: MessageViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.onDetachedFromWindow()
    }

    class Builder {
        private var currentUserId: String = ""
        private var searchUserById: (suspend (String) -> User?)? = null
        private var onClickItem: ((Message) -> Unit)? = null
        private var onLongClickItem: ((Message) -> Unit)? = null
        private var lifecycleOwner: LifecycleOwner? = null

        fun setCurrentUserId(userId: String) = apply {
            this.currentUserId = userId
        }

        fun setSearchUserById(search: suspend (String) -> User?) = apply {
            this.searchUserById = search
        }

        fun setOnClickItem(onClick: (Message) -> Unit) = apply {
            this.onClickItem = onClick
        }

        fun setOnLongClickItem(onLongClick: (Message) -> Unit) = apply {
            this.onLongClickItem = onLongClick
        }

        fun setLifecycleOwner(owner: LifecycleOwner) = apply {
            this.lifecycleOwner = owner
        }

        fun build(): MessageAdapter {
            return MessageAdapter(
                currentUserId,
                searchUserById!!,
                onClickItem!!,
                onLongClickItem!!,
                lifecycleOwner!!
            )
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_TEXT -> TextViewHolder(
                MessageTextItemBinding.inflate(inflater, parent, false),
                currentUserId,
                searchUserById,
                onClickItem,
                onLongClickItem
            )

            VIEW_TYPE_IMAGE -> ImageViewHolder(
                MessageImageItemBinding.inflate(inflater, parent, false),
                currentUserId,
                searchUserById,
                onClickItem,
                onLongClickItem
            )

            VIEW_TYPE_VIDEO -> VideoViewHolder(
                MessageVideoItemBinding.inflate(inflater, parent, false),
                currentUserId,
                searchUserById,
                onClickItem,
                onLongClickItem,
                lifecycleOwner
            )

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(items[position], position, items)
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int {
        return when (items[position].type) {
            MessageType.TEXT -> VIEW_TYPE_TEXT
            MessageType.IMAGE -> VIEW_TYPE_IMAGE
            MessageType.VIDEO -> VIEW_TYPE_VIDEO
            MessageType.LIKE -> VIEW_TYPE_IMAGE
        }
    }

    fun updateList(newList: List<Message>) {
        val diffCallback = ItemDiffCallback(
            oldList = items,
            newList = newList,
            areItemsTheSame = { old, new -> old.id == new.id },
            areContentsTheSame = { old, new -> old == new }
        )
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        items.clear()
        items.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }
}