package com.lebatinh.messenger.mess.fragment.conversation.media

import android.app.Dialog
import android.os.Bundle
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import androidx.viewpager2.widget.ViewPager2
import com.lebatinh.messenger.R
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator

class FullscreenMediaDialog(private val uris: List<String>, private val isVideo: Boolean) :
    DialogFragment() {

    private lateinit var mediaPageAdapter: MediaPageAdapter

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_fullscreen_media)

        val btnBack = dialog.findViewById<ImageView>(R.id.btnBack)
        val viewpagerMedia = dialog.findViewById<ViewPager2>(R.id.viewpagerMedia)
        val dotsIndicator = dialog.findViewById<DotsIndicator>(R.id.dotsIndicator)

        btnBack.setOnClickListener { dismiss() }

        mediaPageAdapter = MediaPageAdapter(uris, isVideo)
        viewpagerMedia.adapter = mediaPageAdapter

        viewpagerMedia.offscreenPageLimit = 1

        dotsIndicator.attachTo(viewpagerMedia)

        dialog.setOnDismissListener {
            mediaPageAdapter.releaseAllPlayer()
        }

        return dialog
    }
}