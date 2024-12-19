package com.lebatinh.messenger.mess.fragment.conversation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lebatinh.messenger.databinding.ConversationItemBinding
import com.lebatinh.messenger.helper.TimeHelper
import com.lebatinh.messenger.mess.fragment.ItemDiffCallback
import com.lebatinh.messenger.other.MessageType
import com.lebatinh.messenger.user.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ConversationAdapter(
    private val items: MutableList<Conversation>,
    private val currentUserId: String,
    private val searchUserById: suspend (String) -> User?,
    private val onClickItem: (Conversation) -> Unit,
    private val onLongClickItem: (Conversation) -> Unit
) :
    RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder>() {

    inner class ConversationViewHolder(private val binding: ConversationItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                onClickItem(items[absoluteAdapterPosition])
            }

            binding.root.setOnLongClickListener {
                onLongClickItem(items[absoluteAdapterPosition])
                true
            }
        }

        fun bind(item: Conversation) {
            if (item.group == true) {
                item.imageGroup?.let { Glide.with(binding.root).load(it).into(binding.imgAvatar) }
                binding.tvName.text = item.conversationName ?: "Nhóm chưa đặt tên"
                val senderId = item.lastMessage?.senderId
                if (!senderId.isNullOrEmpty()) {
                    CoroutineScope(Dispatchers.Main).launch {
                        val user = searchUserById(senderId)
                        binding.tvNameSenderLastMessage.text = "${user?.fullName}: "
                    }
                }
            } else if (item.group == false) {
                val otherUserId = item.listIdChatPerson?.firstOrNull { it != currentUserId }
                if (!otherUserId.isNullOrEmpty()) {
                    CoroutineScope(Dispatchers.Main).launch {
                        val otherUser = searchUserById(otherUserId)
                        if (otherUser != null) {
                            binding.tvName.text = otherUser.fullName
                            otherUser.avatar.let {
                                Glide.with(binding.root).load(it).into(binding.imgAvatar)
                            }
                        }
                    }
                }
            }
            val message = when (item.lastMessage?.type) {
                MessageType.TEXT -> {
                    item.lastMessage.message
                }

                MessageType.IMAGE -> {
                    "Đã gửi ${item.lastMessage.urlMedia?.size} hình ảnh"
                }

                MessageType.VIDEO -> {
                    "Đã gửi ${item.lastMessage.urlMedia?.size} video"
                }

                MessageType.LIKE -> {
                    "Đã gửi 1 like"
                }

                null -> {
                    ""
                }
            }
            binding.tvLastMessage.text = message
            val senderId = item.lastMessage?.senderId
            if (!senderId.isNullOrEmpty()) {
                CoroutineScope(Dispatchers.Main).launch {
                    val user = searchUserById(senderId)
                    binding.tvNameSenderLastMessage.text =
                        if (currentUserId == user?.userUID) "Tôi:" else "${user?.fullName}: "
                }
            }
            binding.tvLastTimeMessage.text = item.lastMessage?.let {
                it.timeSend?.let { it1 -> TimeHelper().formatElapsedTime(it1) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val binding =
            ConversationItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ConversationViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        holder.bind(items[position])
    }

    fun updateList(newList: List<Conversation>) {
        val diffCallback = ItemDiffCallback(
            oldList = items,
            newList = newList,
            areItemsTheSame = { oldItem, newItem -> oldItem.conversationId == newItem.conversationId },
            areContentsTheSame = { oldItem, newItem -> oldItem == newItem }
        )
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        items.clear()
        items.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }
}