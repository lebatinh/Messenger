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
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.lebatinh.messenger.R
import com.lebatinh.messenger.databinding.FragmentHomeMessengerBinding
import com.lebatinh.messenger.mess.fragment.conversation.ConversationAdapter
import com.lebatinh.messenger.mess.fragment.conversation.ConversationViewModel
import com.lebatinh.messenger.notification.NotiHelper
import com.lebatinh.messenger.other.ReturnResult
import com.lebatinh.messenger.user.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HomeMessenger : Fragment(), MenuProvider {
    private var _binding: FragmentHomeMessengerBinding? = null
    private val binding get() = _binding!!

    private val userViewModel: UserViewModel by viewModels()
    private val conversationViewModel: ConversationViewModel by viewModels()

    private lateinit var adapter: ConversationAdapter

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

        currentUID?.let { conversationViewModel.getConversationsByUserId(it) }

        conversationViewModel.listConversationResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                ReturnResult.Loading -> {
                    binding.frLoading.visibility = View.VISIBLE
                }

                is ReturnResult.Success -> {
                    binding.frLoading.visibility = View.GONE
                    val listConversation = result.data

                    if (!currentUID.isNullOrEmpty()) {
                        if (::adapter.isInitialized) {
                            adapter.updateList(listConversation)
                        } else {
                            adapter = ConversationAdapter(
                                items = listConversation.toMutableList(),
                                currentUserId = currentUID!!,
                                searchUserById = { id ->
                                    userViewModel.getInfoUserByUID(id)
                                },
                                onClickItem = { conversation ->
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
                                },
                                onLongClickItem = {}
                            )
                        }

                        binding.rcvConversation.layoutManager =
                            LinearLayoutManager(requireContext())
                        binding.rcvConversation.adapter = adapter
                    }

                    conversationViewModel.resetResult()
                }

                is ReturnResult.Error -> {
                    binding.frLoading.visibility = View.GONE
                    Snackbar.make(requireView(), result.message, Snackbar.LENGTH_SHORT).show()
                    conversationViewModel.resetResult()
                }

                null -> {
                    binding.frLoading.visibility = View.GONE
                }
            }
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