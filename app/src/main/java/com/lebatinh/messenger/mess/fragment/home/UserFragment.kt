package com.lebatinh.messenger.mess.fragment.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.lebatinh.messenger.databinding.FragmentUserBinding
import com.lebatinh.messenger.mess.fragment.conversation.ConversationViewModel
import com.lebatinh.messenger.user.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UserFragment : Fragment() {
    private var _binding: FragmentUserBinding? = null
    private val binding get() = _binding!!

    private val userViewModel: UserViewModel by viewModels()
    private val conversationViewModel: ConversationViewModel by viewModels()

    private lateinit var userAdapter: UserAdapter

    private val args: UserFragmentArgs by navArgs()
    private lateinit var currentUserUID: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        currentUserUID = args.currentUID
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.tvGroup.setOnClickListener {
            val action = UserFragmentDirections.actionUserFragmentToNewGroupFragment(currentUserUID)
            findNavController().navigate(action)
        }

        binding.svUser.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null) {
                    lifecycleScope.launch {
                        userViewModel.searchUsers(query, currentUserUID)
                            .collectLatest { pagingData ->
                                userAdapter.submitData(pagingData)
                            }
                    }

                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText != null) {
                    lifecycleScope.launch {
                        userViewModel.searchUsers(newText, currentUserUID)
                            .collectLatest { pagingData ->
                                userAdapter.submitData(pagingData)
                            }
                    }

                }
                return true
            }
        })

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        userAdapter = UserAdapter(false) { user ->
            user.userUID?.let {
                conversationViewModel.getConversationIdForOneToOne(
                    currentUserUID,
                    it
                )
                conversationViewModel.conversationId.observe(viewLifecycleOwner) { conversationId ->
                    val action =
                        UserFragmentDirections.actionUserFragmentToConversationFragment(
                            currentUserUID,
                            it,
                            conversationId,
                            false
                        )
                    findNavController().navigate(action)
                }

            }
        }

        binding.rcvUser.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@UserFragment.userAdapter.withLoadStateHeaderAndFooter(
                header = CustomLoadStateAdapter { userAdapter.retry() },
                footer = CustomLoadStateAdapter { userAdapter.retry() }
            )
        }

        lifecycleScope.launch {
            userAdapter.loadStateFlow.collectLatest { loadStates ->
                // Show loading spinner during initial load or refresh
                binding.frLoading.isVisible = loadStates.refresh is LoadState.Loading

                // Show retry button if there was an error
                binding.frLoading.isVisible = loadStates.refresh is LoadState.Error
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}