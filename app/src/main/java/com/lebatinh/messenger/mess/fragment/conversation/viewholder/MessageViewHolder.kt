package com.lebatinh.messenger.mess.fragment.conversation.viewholder

import android.view.View
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lebatinh.messenger.R
import com.lebatinh.messenger.mess.fragment.conversation.Message
import com.lebatinh.messenger.user.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class MessageViewHolder(
    itemView: View,
    protected val currentUserId: String,
    protected val searchUserById: suspend (String) -> User?
) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(item: Message, position: Int, items: MutableList<Message>)

    protected fun setupMessageLayout(
        messageItem: MotionLayout,
        cvAvatar: CardView,
        imgAvatar: ImageView,
        isCurrentId: Boolean,
        position: Int,
        items: List<Message>
    ) {
        val currentMessage = items[position]
        val nextMessage = if (position < items.size - 1) items[position + 1] else null

        val isSameSenderAsNext = nextMessage?.senderId == currentMessage.senderId

        val params = messageItem.layoutParams as ConstraintLayout.LayoutParams
        if (isCurrentId) {
            params.startToStart = ConstraintLayout.LayoutParams.UNSET
            params.endToEnd = R.id.guideRight
            cvAvatar.visibility = View.GONE
        } else {
            params.endToEnd = ConstraintLayout.LayoutParams.UNSET
            params.startToStart = R.id.guideLeft
            cvAvatar.visibility = if (nextMessage == null || !isSameSenderAsNext) {
                View.VISIBLE
            } else {
                View.INVISIBLE
            }
        }
        messageItem.layoutParams = params

        if (!currentMessage.senderId.isNullOrEmpty() && !isCurrentId && cvAvatar.visibility == View.VISIBLE) {
            loadAvatar(imgAvatar, currentMessage.senderId, position)
        }
    }

    protected fun getBackgroundForMessage(
        isCurrentId: Boolean,
        isSameSenderAsPrevious: Boolean,
        isSameSenderAsNext: Boolean
    ): Int = when {
        isSameSenderAsPrevious && isSameSenderAsNext -> R.drawable.bg_msg
        isSameSenderAsPrevious && !isSameSenderAsNext -> {
            if (isCurrentId) R.drawable.bg_msg_top_right else R.drawable.bg_msg_top_left
        }

        !isSameSenderAsPrevious && isSameSenderAsNext -> {
            if (isCurrentId) R.drawable.bg_msg_bottom_right else R.drawable.bg_msg_bottom_left
        }

        else -> R.drawable.bg_msg
    }

    private fun loadAvatar(imgAvatar: ImageView, senderId: String, position: Int) {
        imgAvatar.tag = "$position$senderId"
        CoroutineScope(Dispatchers.Main).launch {
            val user = searchUserById(senderId)
            if (user != null && imgAvatar.tag == "$position$senderId") {
                Glide.with(imgAvatar.context)
                    .load(user.avatar)
                    .placeholder(R.drawable.default_avatar)
                    .error(R.drawable.default_avatar)
                    .into(imgAvatar)
            }
        }
    }
}