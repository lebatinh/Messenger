package com.lebatinh.messenger.mess.fragment.conversation.media

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer

object PlayerPool {
    private const val POOL_SIZE = 3 // Số lượng player tối đa trong pool
    private val players = ArrayDeque<ExoPlayer>()

    fun obtain(context: Context): ExoPlayer {
        return if (players.isEmpty()) {
            ExoPlayer.Builder(context).build()
        } else {
            players.removeFirst()
        }
    }

    fun release(player: ExoPlayer) {
        player.stop()
        player.clearMediaItems()
        if (players.size < POOL_SIZE) {
            players.addLast(player)
        } else {
            player.release()
        }
    }

    fun clear() {
        players.forEach { it.release() }
        players.clear()
    }
}