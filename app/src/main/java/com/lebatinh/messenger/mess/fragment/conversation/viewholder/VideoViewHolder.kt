package com.lebatinh.messenger.mess.fragment.conversation.viewholder

import android.graphics.Rect
import android.view.View
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
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
    onLongClickItem: (Message) -> Unit,
    private val lifecycleOwner: LifecycleOwner
) : MessageViewHolder(
    binding.root,
    currentUserId,
    searchUserById
), DefaultLifecycleObserver {

    private var messages: List<Message> = emptyList()
    private var player: ExoPlayer? = null
    private var currentUrl: String? = null
    private var isViewVisible = false

    init {
        lifecycleOwner.lifecycle.addObserver(this)

        binding.root.apply {
            setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    binding.messageItem.run {
                        if (currentState == R.id.start) {
                            transitionToEnd()
                            playVideo()
                        } else {
                            transitionToStart()
                            pauseVideo()
                        }
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

        binding.root.viewTreeObserver.addOnScrollChangedListener {
            checkVisibility()
        }
    }

    private fun checkVisibility() {
        val rect = Rect()
        val wasVisible = isViewVisible
        isViewVisible = binding.root.getGlobalVisibleRect(rect)

        // Nếu trạng thái visibility thay đổi
        if (wasVisible != isViewVisible) {
            if (!isViewVisible) {
                pauseVideo()
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

        // Setup video player
        item.urlMedia?.first()?.let { url ->
            if (url != currentUrl) {
                releasePlayer()
                setupPlayer(url)
                currentUrl = url
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

    private fun setupPlayer(url: String) {
        player = ExoPlayer.Builder(binding.root.context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = false
            prepare()
        }
        binding.pvMessage.player = player
    }

    private fun playVideo() {
        if (isViewVisible && lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            player?.play()
        }
    }

    private fun pauseVideo() {
        player?.pause()
    }

    private fun releasePlayer() {
        player?.let {
            it.stop()
            it.release()
        }
        player = null
        currentUrl = null
    }

    override fun onPause(owner: LifecycleOwner) {
        pauseVideo()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        releasePlayer()
        lifecycleOwner.lifecycle.removeObserver(this)
    }

    override fun onRecycled() {
        pauseVideo()
    }

    override fun onDetachedFromWindow() {
        pauseVideo()
    }
}