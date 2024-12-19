package com.lebatinh.messenger.mess.fragment.conversation.camera

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.lebatinh.messenger.databinding.FragmentPreviewBinding
import com.lebatinh.messenger.mess.CloudinaryViewModel
import com.lebatinh.messenger.mess.fragment.conversation.ConversationViewModel
import com.lebatinh.messenger.other.MessageType
import com.lebatinh.messenger.other.ReturnResult
import com.lebatinh.messenger.user.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class PreviewFragment : Fragment() {

    private var _binding: FragmentPreviewBinding? = null
    private val binding get() = _binding!!

    private val userViewModel: UserViewModel by viewModels()
    private val conversationViewModel: ConversationViewModel by viewModels()
    private val cloudinaryViewModel: CloudinaryViewModel by viewModels()

    private var currentUID: String? = null
    private var receiverUID: String? = null
    private var conversationId: String? = null
    private var isGroup: Boolean? = null
    private var isImage: Boolean? = null
    private var mediaPath: String? = null

    private val args: PreviewFragmentArgs by navArgs()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        currentUID = args.currentUID
        receiverUID = args.receiverUID
        conversationId = args.conversationID
        isGroup = args.isGroup
        isImage = args.isImage
        mediaPath = args.mediaPath
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPreviewBinding.inflate(inflater, container, false)
        val root: View = binding.root

        mediaPath?.let {
            val file = File(it)

            when (isImage) {
                true -> {
                    binding.imgPreview.visibility = View.VISIBLE
                    binding.pvPreview.visibility = View.GONE

                    Glide.with(root.context).load(Uri.fromFile(file)).into(binding.imgPreview)
                }

                false -> {
                    binding.imgPreview.visibility = View.GONE
                    binding.pvPreview.visibility = View.VISIBLE

                    val player = ExoPlayer.Builder(requireContext()).build()
                    binding.pvPreview.player = player
                    val mediaItem = MediaItem.fromUri(mediaPath!!)
                    player.setMediaItem(mediaItem)
                    player.prepare()
                }

                else -> {
                    binding.imgPreview.visibility = View.GONE
                    binding.pvPreview.visibility = View.GONE
                }
            }

            if (!file.exists()) {
                Snackbar.make(requireView(), "File không tồn tại", Snackbar.LENGTH_SHORT).show()
            }

            // Lấy content URI từ file thông qua FileProvider
            val contentUri = FileProvider.getUriForFile(
                requireContext(),
                "com.lebatinh.messenger",
                file
            )

            binding.imgSend.setOnClickListener {
                if (!currentUID.isNullOrEmpty() && isGroup != null) {
                    if (isImage == true) {
                        cloudinaryViewModel.uploadImage(listOf(contentUri), requireContext())
                    } else if (isImage == false) {
                        cloudinaryViewModel.uploadVideo(listOf(contentUri), requireContext())
                    }
                    binding.imgSend.isClickable = false
                }
            }
        }

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (isGroup == true) {
            conversationId?.let { conversationViewModel.getConversationByGroupId(it) }
        } else if (isGroup == false) {
            receiverUID?.let { userViewModel.getUserByUID(it) }
        }

        userViewModel.returnResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                ReturnResult.Loading -> {}

                is ReturnResult.Success -> {
                    binding.tvNameReceiver.text = result.data.fullName

                    userViewModel.resetReturnResult()
                }

                is ReturnResult.Error -> {
                    Snackbar.make(requireView(), result.message, Snackbar.LENGTH_SHORT).show()
                    userViewModel.resetReturnResult()
                }

                null -> {}
            }
        }

        conversationViewModel.conversationResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                ReturnResult.Loading -> {}

                is ReturnResult.Success -> {
                    binding.tvNameReceiver.text = result.data.conversationName

                    userViewModel.resetReturnResult()
                }

                is ReturnResult.Error -> {
                    Snackbar.make(requireView(), result.message, Snackbar.LENGTH_SHORT).show()
                    userViewModel.resetReturnResult()
                }

                null -> {}
            }
        }

        cloudinaryViewModel.imageUrl.observe(viewLifecycleOwner) { imageUrls ->
            if (!currentUID.isNullOrEmpty() && isGroup != null) {
                conversationViewModel.sendMessage(
                    currentUID!!,
                    receiverUID,
                    isGroup!!,
                    conversationId,
                    MessageType.IMAGE,
                    null,
                    imageUrls
                )
                val action =
                    PreviewFragmentDirections.actionPreviewFragmentToConversationFragment(
                        currentUID!!,
                        receiverUID,
                        conversationId,
                        isGroup!!
                    )
                findNavController().navigate(action)
            }
        }
        cloudinaryViewModel.videoUrl.observe(viewLifecycleOwner) { videoUrls ->
            if (!currentUID.isNullOrEmpty() && isGroup != null) {
                conversationViewModel.sendMessage(
                    currentUID!!,
                    receiverUID,
                    isGroup!!,
                    conversationId,
                    MessageType.VIDEO,
                    null,
                    videoUrls
                )
                val action =
                    PreviewFragmentDirections.actionPreviewFragmentToConversationFragment(
                        currentUID!!,
                        receiverUID,
                        conversationId,
                        isGroup!!
                    )
                findNavController().navigate(action)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}