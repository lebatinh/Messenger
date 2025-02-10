package com.lebatinh.messenger.mess.fragment.conversation

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.addCallback
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.snackbar.Snackbar
import com.lebatinh.messenger.Key_Password.MAX_IMAGE_SIZE
import com.lebatinh.messenger.Key_Password.MAX_VIDEO_SIZE
import com.lebatinh.messenger.R
import com.lebatinh.messenger.databinding.FragmentConversationBinding
import com.lebatinh.messenger.mess.CloudinaryViewModel
import com.lebatinh.messenger.mess.fragment.conversation.media.FullscreenMediaDialog
import com.lebatinh.messenger.mess.fragment.conversation.media.PlayerPool
import com.lebatinh.messenger.notification.NotiHelper
import com.lebatinh.messenger.other.MessageType
import com.lebatinh.messenger.other.NotificationType
import com.lebatinh.messenger.other.ReturnResult
import com.lebatinh.messenger.user.UserViewModel
import com.robertlevonyan.components.picker.ItemModel
import com.robertlevonyan.components.picker.ItemType
import com.robertlevonyan.components.picker.PickerDialog
import com.robertlevonyan.components.picker.pickerDialog
import com.vanniktech.emoji.EmojiPopup
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class ConversationFragment : Fragment(), MenuProvider {

    private var _binding: FragmentConversationBinding? = null
    private val binding get() = _binding!!

    private val userViewModel: UserViewModel by viewModels()
    private val conversationViewModel: ConversationViewModel by viewModels()
    private val cloudinaryViewModel: CloudinaryViewModel by viewModels()

    private var currentUID: String? = null
    private var receiverUID: String? = null
    private var conversationId: String? = null
    private var isGroup: Boolean? = null

    private val args: ConversationFragmentArgs by navArgs()

    private lateinit var messageAdapter: MessageAdapter
    private var currentPlayingPosition = -1

    private var listIdChatPerson = emptyList<String>()
    private lateinit var conversationName: String

    @Inject
    lateinit var notiHelper: NotiHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        currentUID = args.currentUID
        receiverUID = args.receiverUID
        conversationId = args.conversationID
        isGroup = args.isGroup

        conversationId?.let {
            conversationViewModel.setConversationId(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConversationBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        binding.edtMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                hideImage(!p0.isNullOrEmpty())
            }

            override fun afterTextChanged(p0: Editable?) {}

        })

        binding.ctlView.setOnClickListener {
            binding.edtMessage.clearFocus()
        }

        binding.imgSend.setOnClickListener {
            if (it.isVisible && !currentUID.isNullOrEmpty() && isGroup != null) {
                conversationViewModel.sendMessage(
                    currentUID!!, receiverUID, isGroup!!, conversationId, MessageType.TEXT,
                    binding.edtMessage.text.toString(), null
                )
                binding.edtMessage.text.clear()
            }
        }

        val emoji = EmojiPopup(root, binding.edtMessage)

        binding.edtMessage.setOnTouchListener { view, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = binding.edtMessage.compoundDrawables[2]
                if (drawableEnd != null) {
                    if (event.rawX >= (binding.edtMessage.right - drawableEnd.bounds.width())) {
                        emoji.toggle()
                        view.performClick()
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }

        binding.imgLike.setOnClickListener {
            if (!currentUID.isNullOrEmpty() && isGroup != null) {
                conversationViewModel.sendMessage(
                    currentUID!!,
                    receiverUID,
                    isGroup!!,
                    conversationId,
                    MessageType.LIKE,
                    null,
                    null
                )
            }
        }

        binding.imgUpload.setOnClickListener {
            val items = setOf(
                ItemModel(
                    ItemType.ImageGallery(),
                    "Thư viện ảnh\n(<=5MB)",
                    R.drawable.photo_library
                ),
                ItemModel(
                    ItemType.VideoGallery(),
                    "Thư viện video\n(<=20MB)",
                    R.drawable.video_library
                )
            )

            pickerDialog {
                setTitle("Chọn phương tiện")
                setTitleTextColor(ContextCompat.getColor(requireContext(), R.color.text_color))
                setListType(PickerDialog.ListType.TYPE_LIST)
                setItems(items)
                setTitleGravity(Gravity.CENTER_HORIZONTAL)
            }.setPickerCloseListener { type: ItemType, uris: List<Uri> ->
                when (type) {
                    is ItemType.ImageGallery -> {
                        val filterUri = uris.filter { uri ->
                            getFileSize(requireContext(), uri) <= MAX_IMAGE_SIZE
                        }
                        cloudinaryViewModel.uploadImage(filterUri, requireContext())
                    }

                    is ItemType.VideoGallery -> {
                        val filterUris = uris.filter { uri ->
                            getFileSize(requireContext(), uri) <= MAX_VIDEO_SIZE
                        }
                        cloudinaryViewModel.uploadVideo(filterUris, requireContext())
                    }

                    ItemType.Camera -> {}
                    is ItemType.AudioGallery -> {}
                    is ItemType.Files -> {}
                    ItemType.Video -> {}
                }
            }.show()
        }

        binding.imgOpenCamera.setOnClickListener {
            if (!currentUID.isNullOrEmpty() && isGroup != null) {
                val action =
                    ConversationFragmentDirections.actionConversationFragmentToCameraFragment(
                        currentUID!!, receiverUID, conversationId, isGroup!!
                    )
                findNavController().navigate(action)
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            findNavController().popBackStack(R.id.homeMessenger, false)
        }
        return root
    }

    private fun getFileSize(context: Context, uri: Uri): Long {
        return context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: 0
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeAdapter()

        setupRecyclerView()

        if (isGroup == true) {
            conversationId?.let { conversationViewModel.getConversationByGroupId(it) }
        } else if (isGroup == false) {
            receiverUID?.let { userViewModel.getUserByUID(it) }
        }

        val collapsingToolbar: CollapsingToolbarLayout =
            requireActivity().findViewById(R.id.collapsingToolbarLayout)
        val toolbarImage: ImageView = requireActivity().findViewById(R.id.toolbarImage)
        val appBarLayout: AppBarLayout = requireActivity().findViewById(R.id.appBarLayout)
        val cvAvatar: CardView = requireActivity().findViewById(R.id.cvAvatar)

        appBarLayout.addOnOffsetChangedListener { appBar, verticalOffset ->
            // verticalOffset < 0 có nghĩa là AppBarLayout đang thu gọn
            if (abs(verticalOffset.toDouble()) >= appBar.totalScrollRange) {
                // AppBarLayout đã co lại hoàn toàn
                cvAvatar.animate().scaleX(0.2F).scaleY(0.2F).setDuration(300).start()
            } else {
                // AppBarLayout đang mở rộng hoặc đang thu nhỏ dần
                cvAvatar.animate().scaleX(1F).scaleY(1F).setDuration(300).start()
            }
        }

        userViewModel.returnResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                ReturnResult.Loading -> {
                    binding.frLoading.visibility = View.VISIBLE
                }

                is ReturnResult.Success -> {
                    collapsingToolbar.title = result.data.fullName
                    if (result.data.avatar.isNullOrEmpty()) {
                        cvAvatar.visibility = View.INVISIBLE
                    } else {
                        cvAvatar.visibility = View.VISIBLE

                        Glide.with(this).load(result.data.avatar).into(toolbarImage)
                    }

                    userViewModel.resetReturnResult()
                }

                is ReturnResult.Error -> {
                    binding.frLoading.visibility = View.GONE
                    Snackbar.make(requireView(), result.message, Snackbar.LENGTH_SHORT).show()
                    userViewModel.resetReturnResult()
                }

                null -> {
                    binding.frLoading.visibility = View.GONE
                }
            }
        }

        conversationViewModel.conversationResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                ReturnResult.Loading -> {}

                is ReturnResult.Success -> {
                    collapsingToolbar.title = result.data.conversationName

                    if (result.data.imageGroup.isNullOrEmpty()) {
                        cvAvatar.visibility = View.INVISIBLE
                    } else {
                        cvAvatar.visibility = View.VISIBLE

                        Glide.with(this).load(result.data.imageGroup).into(toolbarImage)
                    }

                    listIdChatPerson = result.data.listIdChatPerson!!
                    conversationName = result.data.conversationName.toString()

                    userViewModel.resetReturnResult()
                }

                is ReturnResult.Error -> {
                    Snackbar.make(requireView(), result.message, Snackbar.LENGTH_SHORT).show()
                    userViewModel.resetReturnResult()
                }

                null -> {}
            }
        }

        conversationViewModel.messageResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                ReturnResult.Loading -> {}
                is ReturnResult.Success -> {
                    conversationViewModel.setConversationId(result.data.conversationId)

                    lifecycleScope.launch {
                        val user = currentUID?.let { userViewModel.getInfoUserByUID(it) }

                        val message = when (result.data.lastMessage?.type) {
                            MessageType.TEXT -> {
                                result.data.lastMessage.message
                            }

                            MessageType.IMAGE -> {
                                "Đã gửi ${result.data.lastMessage.urlMedia?.size} hình ảnh"
                            }

                            MessageType.VIDEO -> {
                                "Đã gửi ${result.data.lastMessage.urlMedia?.size} video"
                            }

                            MessageType.LIKE -> {
                                "Đã gửi 1 like"
                            }

                            null -> {
                                ""
                            }
                        }
                        if (isGroup == true) {
                            listIdChatPerson.filter { it != currentUID }.forEach {
                                if (user != null) {
                                    conversationViewModel.sendMessageNotification(
                                        senderId = currentUID!!,
                                        type = NotificationType.GroupMessage,
                                        receiverId = it,
                                        message = message.toString(),
                                        senderName = "${user.fullName!!} từ nhóm ${conversationName}: ",
                                        conversationId = conversationId!!
                                    )
                                }
                            }
                        } else if (isGroup == false) {
                            receiverUID?.let {
                                if (user != null) {
                                    conversationViewModel.sendMessageNotification(
                                        senderId = currentUID!!,
                                        type = NotificationType.Message,
                                        receiverId = it,
                                        message = message.toString(),
                                        senderName = "${user.fullName!!}: ",
                                        conversationId = conversationId ?: ""
                                    )
                                }
                            }
                        }
                    }
                }

                is ReturnResult.Error -> {}

                null -> {}
            }
        }

        conversationViewModel.conversationId.observe(viewLifecycleOwner) {
            it?.let {
                conversationViewModel.getMessages(it)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                conversationViewModel.listMessageResult.observe(viewLifecycleOwner) { result ->
                    when (result) {
                        is ReturnResult.Error -> {
                            Snackbar.make(requireView(), result.message, Snackbar.LENGTH_SHORT)
                                .show()
                            conversationViewModel.resetResult()
                        }

                        ReturnResult.Loading -> {}
                        is ReturnResult.Success -> {
                            loadMessage(result.data)
                            conversationViewModel.resetResult()
                        }

                        null -> {}
                    }
                }
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
            }
        }
    }

    private fun setupRecyclerView() {
        binding.rcvMessage.apply {
            adapter = messageAdapter
            layoutManager = LinearLayoutManager(requireContext())
            (layoutManager as LinearLayoutManager).initialPrefetchItemCount = 3
            setItemViewCacheSize(3)
            setHasFixedSize(true)

            if (messageAdapter.itemCount != 0) {
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        super.onScrollStateChanged(recyclerView, newState)
                        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                            checkAndPlayVisibleVideo()
                        } else {
                            pauseCurrentVideo()
                        }
                    }
                })
            }

        }
    }

    private fun initializeAdapter() {
        messageAdapter = MessageAdapter(
            items = mutableListOf(),
            currentUserId = currentUID!!,
            searchUserById = { userId ->
                withContext(Dispatchers.IO) {
                    userViewModel.getInfoUserByUID(userId)
                }
            },
            onClickItem = { message -> handleMessageClick(message) },
            onLongClickItem = { message -> /* Xử lý long click */ }
        )
    }

    private fun checkAndPlayVisibleVideo() {
        val layoutManager = binding.rcvMessage.layoutManager as LinearLayoutManager
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        val lastVisible = layoutManager.findLastVisibleItemPosition()

        for (position in firstVisible..lastVisible) {
            val message = messageAdapter.getItem(position)
            if (message.type == MessageType.VIDEO) {
                // Nếu tìm thấy video trong vùng nhìn thấy
                playVideoAtPosition(position)
                break
            }
        }
    }

    private fun playVideoAtPosition(position: Int) {
        if (currentPlayingPosition != position) {
            // Pause video cũ nếu có
            pauseCurrentVideo()

            // Update vị trí mới và play
            currentPlayingPosition = position
            val holder = binding.rcvMessage.findViewHolderForAdapterPosition(position)
                    as? MessageAdapter.MessageViewHolder
            holder?.playVideo()
        }
    }

    private fun pauseCurrentVideo() {
        if (currentPlayingPosition != -1) {
            val holder = binding.rcvMessage.findViewHolderForAdapterPosition(currentPlayingPosition)
                    as? MessageAdapter.MessageViewHolder
            holder?.pauseVideo()
            currentPlayingPosition = -1
        }
    }

    private fun loadMessage(data: List<Message>) {
        if (::messageAdapter.isInitialized) {
            initializeAdapter()
            binding.rcvMessage.adapter = messageAdapter
        }
        messageAdapter.updateList(data)
    }

    override fun onPause() {
        super.onPause()
        pauseCurrentVideo()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        PlayerPool.clear()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.conversation, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.item_info -> {
                val action =
                    isGroup?.let {
                        ConversationFragmentDirections.actionConversationFragmentToConversationDetailFragment(
                            currentUID!!, receiverUID, conversationId, it
                        )
                    }
                if (action != null) {
                    findNavController().navigate(action)
                }
                return true
            }
        }
        return false
    }

    private fun hideImage(showSend: Boolean) {
        if (showSend) {
            if (!binding.imgSend.isVisible) {
                binding.imgSend.apply {
                    alpha = 0f
                    isVisible = true
                    animate().alpha(1f).setDuration(200).start()
                }
                binding.imgLike.animate().alpha(0f).setDuration(200).withEndAction {
                    binding.imgLike.isVisible = false
                }.start()
            }

            if (!binding.imgOpen.isVisible) {
                binding.imgOpen.apply {
                    alpha = 0f
                    isVisible = true
                    animate().alpha(1f).setDuration(200).start()
                }
                binding.ctlToolOpen.animate().alpha(0f).setDuration(200).withEndAction {
                    binding.ctlToolOpen.isVisible = false
                }.start()
            } else {
                binding.imgOpen.setOnClickListener {
                    if (!binding.ctlToolOpen.isVisible) {
                        binding.ctlToolOpen.apply {
                            alpha = 0f
                            isVisible = true
                            animate().alpha(1f).setDuration(200).start()
                        }
                        binding.imgOpen.animate().alpha(0f).setDuration(200).withEndAction {
                            binding.imgOpen.isVisible = false
                        }.start()
                    }
                }
            }
        } else {
            if (!binding.imgLike.isVisible) {
                binding.imgLike.apply {
                    alpha = 0f
                    isVisible = true
                    animate().alpha(1f).setDuration(200).start()
                }
                binding.imgSend.animate().alpha(0f).setDuration(200).withEndAction {
                    binding.imgSend.isVisible = false
                }.start()
            }

            if (!binding.ctlToolOpen.isVisible) {
                binding.ctlToolOpen.apply {
                    alpha = 0f
                    isVisible = true
                    animate().alpha(1f).setDuration(200).start()
                }
                binding.imgOpen.animate().alpha(0f).setDuration(200).withEndAction {
                    binding.imgOpen.isVisible = false
                }.start()
            }
        }
    }

    private fun handleMessageClick(message: Message) {
        // Xử lý click vào tin nhắn
        when (message.type) {
            MessageType.IMAGE -> {
                message.urlMedia?.let { urls ->
                    FullscreenMediaDialog(urls, false).show(
                        parentFragmentManager,
                        "mediaDialog"
                    )
                }
            }

            MessageType.VIDEO -> {
                message.urlMedia?.let { urls ->
                    FullscreenMediaDialog(urls, true).show(
                        parentFragmentManager,
                        "mediaDialog"
                    )
                }
            }

            else -> {}
        }
    }
}