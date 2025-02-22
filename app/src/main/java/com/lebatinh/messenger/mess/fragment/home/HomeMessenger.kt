package com.lebatinh.messenger.mess.fragment.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.lebatinh.messenger.R
import com.lebatinh.messenger.databinding.FragmentHomeMessengerBinding
import com.lebatinh.messenger.mess.fragment.conversation.Conversation
import com.lebatinh.messenger.mess.fragment.conversation.ConversationAdapter
import com.lebatinh.messenger.mess.fragment.conversation.ConversationViewModel
import com.lebatinh.messenger.notification.NotiHelper
import com.lebatinh.messenger.user.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HomeMessenger : Fragment(), MenuProvider {
    private var _binding: FragmentHomeMessengerBinding? = null
    private val binding get() = _binding!!

    private val userViewModel: UserViewModel by viewModels()
    private val conversationViewModel: ConversationViewModel by viewModels()

    private lateinit var conversationAdapter: ConversationAdapter

    private var currentUID: String? = null

    @Inject
    lateinit var notiHelper: NotiHelper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        currentUID = activity?.intent?.getStringExtra("userUID")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeMessengerBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        lifecycleScope.launch {
            currentUID.let {
                notiHelper.subscribeToTopic(it!!)
            }
        }

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeConversations()
    }

    override fun onResume() {
        super.onResume()
        refreshConversations()
    }

    private fun refreshConversations() {
        conversationViewModel.refresh()
        observeConversations()
    }

    private fun observeConversations() {
        currentUID?.let { uid ->
            // Observe paging data
            viewLifecycleOwner.lifecycleScope.launch {
                conversationViewModel.getConversationsByUserId(uid)
                    .collectLatest { pagingData ->
                        conversationAdapter.submitData(pagingData)
                    }
            }

            // Observe realtime updates
            viewLifecycleOwner.lifecycleScope.launch {
                conversationViewModel.realtimeConversations
                    .collectLatest {
                        // Refresh the adapter when new data arrives
                        conversationAdapter.refresh()
                    }
            }
        }
    }

    private fun setupRecyclerView() {
        conversationAdapter = ConversationAdapter(
            currentUserId = currentUID!!,
            searchUserById = { id ->
                userViewModel.getInfoUserByUID(id)
            },
            onClickItem = { conversation ->
                handleConversationClick(conversation)
            },
            onLongClickItem = {}
        )

        binding.rcvConversation.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@HomeMessenger.conversationAdapter.withLoadStateHeaderAndFooter(
                header = CustomLoadStateAdapter { conversationAdapter.retry() },
                footer = CustomLoadStateAdapter { conversationAdapter.retry() }
            )
        }

        // Handle load states
        viewLifecycleOwner.lifecycleScope.launch {
            conversationAdapter.loadStateFlow
                .distinctUntilChangedBy { it.refresh }
                .collect { loadStates ->
                    binding.frLoading.isVisible = loadStates.refresh is LoadState.Loading

                    if (loadStates.refresh is LoadState.Error) {
                        Snackbar.make(
                            requireView(),
                            (loadStates.refresh as LoadState.Error).error.localizedMessage
                                ?: "Lỗi khi tải cuộc hội thoại",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
        }
    }

    private fun handleConversationClick(conversation: Conversation) {
        val action = when (conversation.group) {
            true -> {
                HomeMessengerDirections.actionHomeMessengerToConversationFragment(
                    currentUID!!,
                    null,
                    conversation.conversationId,
                    true
                )
            }

            false -> {
                HomeMessengerDirections.actionHomeMessengerToConversationFragment(
                    currentUID!!,
                    conversation.listIdChatPerson?.first { it != currentUID },
                    conversation.conversationId,
                    false
                )
            }

            else -> {
                null
            }
        }

        if (action != null) {
            findNavController().navigate(action)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.home_messenger, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.item_edit -> {
                // Mở fragment chọn người/nhóm muốn gửi tin nhắn
                val action = HomeMessengerDirections.actionHomeMessengerToUserFragment(currentUID!!)
                findNavController().navigate(action)
                return true
            }
        }
        return false
    }
}