package com.lebatinh.messenger.mess.fragment.conversation

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.lebatinh.messenger.R
import com.lebatinh.messenger.databinding.MessageItemBinding
import com.lebatinh.messenger.helper.TimeHelper
import com.lebatinh.messenger.mess.fragment.ItemDiffCallback
import com.lebatinh.messenger.mess.fragment.conversation.media.PlayerPool
import com.lebatinh.messenger.other.MessageType
import com.lebatinh.messenger.user.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MessageAdapter(
    private val items: MutableList<Message>,
    private val currentUserId: String,
    private val searchUserById: suspend (String) -> User?,
    private val onClickItem: (Message) -> Unit,
    private val onLongClickItem: (Message) -> Unit
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(private val binding: MessageItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var player: ExoPlayer? = null
        private var playbackPosition = 0L
        private var currentUrl: String? = null
        private var isPlaying = false
        private var isPrepared = false
        private var playbackStateListener: Player.Listener? = null

        fun playVideo() {
            if (!isPrepared) {
                player?.prepare()
                isPrepared = true
            }
            player?.playWhenReady = true
            isPlaying = true
        }

        fun pauseVideo() {
            player?.playWhenReady = false
            isPlaying = false
        }

        private fun releasePlayer() {
            player?.let {
                playbackPosition = it.currentPosition
                playbackStateListener?.let { listener ->
                    it.removeListener(listener)
                }
                PlayerPool.release(it)
                player = null
                isPrepared = false
            }
            currentUrl = null
        }

        @OptIn(UnstableApi::class)
        private fun prepareVideo(url: String) {
            binding.pvMessage.tag = bindingAdapterPosition.toString() + url

            if (url == currentUrl && player != null) {
                return // Không cần khởi tạo lại nếu URL không thay đổi
            }

            releasePlayer()

            player = PlayerPool.obtain(binding.root.context).apply {
                playbackStateListener = createPlaybackStateListener()
                addListener(playbackStateListener!!)

                binding.pvMessage.player = this
                setMediaItem(MediaItem.fromUri(url))
                seekTo(playbackPosition)

                playWhenReady = false
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING

                // Optional: Add error handling
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {

                    }
                })
            }

            player?.setMediaItem(MediaItem.fromUri(url))
            player?.prepare()

            currentUrl = url
        }

        private fun createPlaybackStateListener(): Player.Listener {
            return object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> {}
                        Player.STATE_READY -> {
                            isPrepared = true
                        }

                        Player.STATE_ENDED -> {
                            // Handle video completion
                            player?.seekTo(0)
                            pauseVideo()
                        }

                        Player.STATE_IDLE -> {}
                    }
                }
            }
        }

        init {
            binding.messageItemView.setOnClickListener {
                onClickItem(items[bindingAdapterPosition])

                // Đảm bảo chuyển đổi trạng thái chính xác
                if (binding.messageItem.currentState == R.id.start) {
                    binding.messageItem.transitionToEnd()
                } else {
                    binding.messageItem.transitionToStart()
                }
            }

            binding.messageItemView.setOnLongClickListener {
                onLongClickItem(items[bindingAdapterPosition])
                true
            }
        }

        fun bind(item: Message, position: Int) {
            val isCurrentId = item.senderId == currentUserId

            val currentMessage = items[position]
            val previousMessage = if (position > 0) items[position - 1] else null
            val nextMessage = if (position < items.size - 1) items[position + 1] else null

            // Kiểm tra người gửi trước đó và tiếp theo
            val isSameSenderAsPrevious = previousMessage?.senderId == currentMessage.senderId
            val isSameSenderAsNext = nextMessage?.senderId == currentMessage.senderId

            // Logic đặt background
            val backgroundResId = when {
                // Trường hợp có cả tin nhắn trước và sau của cùng một người gửi
                isSameSenderAsPrevious && isSameSenderAsNext -> R.drawable.bg_msg

                // Chỉ có tin nhắn trước cùng người gửi
                isSameSenderAsPrevious && !isSameSenderAsNext -> {
                    if (isCurrentId) R.drawable.bg_msg_top_right else R.drawable.bg_msg_top_left
                }

                // Chỉ có tin nhắn sau cùng người gửi
                !isSameSenderAsPrevious && isSameSenderAsNext -> {
                    if (isCurrentId) R.drawable.bg_msg_bottom_right else R.drawable.bg_msg_bottom_left
                }

                // Không có tin nhắn trước và sau của cùng một người gửi
                else -> R.drawable.bg_msg
            }

            binding.tvMessage.setBackgroundResource(backgroundResId)

            // Điều chỉnh padding giữa các tin nhắn
            val bottomPadding = if (isSameSenderAsNext) 4 else 8
            binding.messageItemView.setPadding(4, 4, 4, bottomPadding)

            val params = binding.messageItem.layoutParams as ConstraintLayout.LayoutParams
            if (isCurrentId) {
                params.startToStart = ConstraintLayout.LayoutParams.UNSET
                params.endToEnd = R.id.guideRight
                binding.cvAvatar.visibility = View.GONE
            } else {
                params.endToEnd = ConstraintLayout.LayoutParams.UNSET
                params.startToStart = R.id.guideLeft
                if (nextMessage == null || !isSameSenderAsNext) {
                    binding.cvAvatar.visibility = View.VISIBLE
                } else {
                    binding.cvAvatar.visibility = View.INVISIBLE
                }
            }
            binding.messageItem.layoutParams = params

            binding.ctlMessage.maxWidth =
                binding.root.context.resources.displayMetrics.widthPixels * 2 / 3

            if (!item.senderId.isNullOrEmpty() && !isCurrentId && binding.cvAvatar.visibility == View.VISIBLE) {
                binding.imgAvatar.tag = position.toString() + item.senderId
                CoroutineScope(Dispatchers.Main).launch {
                    val user = searchUserById(item.senderId)
                    if (user != null && (binding.imgAvatar.tag == position.toString() + item.senderId)) {
                        Glide.with(binding.root.context).load(user.avatar)
                            .placeholder(R.drawable.default_avatar)
                            .error(R.drawable.default_avatar)
                            .into(binding.imgAvatar)
                    }
                }
            }

            when (item.type) {
                MessageType.TEXT -> {
                    binding.tvMessage.visibility = View.VISIBLE
                    binding.ctlView.visibility = View.GONE
                    binding.tvMessage.text = item.message
                }

                MessageType.IMAGE -> {
                    binding.pvMessage.visibility = View.GONE
                    binding.tvMessage.visibility = View.GONE
                    binding.imgMessage.tag = position.toString() + item.id
                    item.urlMedia?.let { urlImages ->
                        Glide.with(binding.root.context).load(urlImages.first())
                            .listener(object : RequestListener<Drawable> {
                                override fun onLoadFailed(
                                    e: GlideException?,
                                    model: Any?,
                                    target: Target<Drawable>,
                                    isFirstResource: Boolean
                                ): Boolean = false

                                override fun onResourceReady(
                                    resource: Drawable,
                                    model: Any,
                                    target: Target<Drawable>?,
                                    dataSource: DataSource,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    return binding.imgMessage.tag != position.toString() + item.id
                                }
                            })
                            .into(binding.imgMessage)

                        if (urlImages.size > 1) {
                            binding.tvOverlay.text = "+${urlImages.size - 1}"
                        }
                    }
                }

                MessageType.VIDEO -> {
                    binding.imgMessage.visibility = View.GONE
                    binding.tvMessage.visibility = View.GONE
                    binding.pvMessage.visibility = View.VISIBLE

                    binding.pvMessage.tag = "$position${item.id}"

                    item.urlMedia?.firstOrNull()?.let { url ->
                        prepareVideo(url)

                        if (item.urlMedia.size > 1) {
                            binding.tvOverlay.text = "+${item.urlMedia.size - 1}"
                        }
                    }
                }

                MessageType.LIKE -> {
                    binding.pvMessage.visibility = View.GONE
                    binding.tvMessage.visibility = View.GONE
                    binding.imgMessage.setImageResource(R.drawable.like)
                }
            }
            binding.tvStatus.text = item.status
            binding.tvTimeSend.text = item.timeSend?.let { TimeHelper().formatTimeSend(it) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = MessageItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MessageViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    fun getItem(position: Int): Message = items[position]

    fun updateList(newList: List<Message>) {
        val diffCallback = ItemDiffCallback(
            oldList = items,
            newList = newList,
            areItemsTheSame = { oldItem, newItem -> oldItem.id == newItem.id },
            areContentsTheSame = { oldItem, newItem -> oldItem == newItem }
        )
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        items.clear()
        items.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }
}