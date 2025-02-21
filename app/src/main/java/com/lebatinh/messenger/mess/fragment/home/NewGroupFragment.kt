package com.lebatinh.messenger.mess.fragment.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.lebatinh.messenger.R
import com.lebatinh.messenger.databinding.FragmentNewGroupBinding
import com.lebatinh.messenger.mess.fragment.conversation.ConversationViewModel
import com.lebatinh.messenger.other.ReturnResult
import com.lebatinh.messenger.user.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NewGroupFragment : Fragment(), MenuProvider {

    private var _binding: FragmentNewGroupBinding? = null
    private val binding get() = _binding!!

    private val userViewModel: UserViewModel by viewModels()
    private val conversationViewModel: ConversationViewModel by viewModels()

    private lateinit var userAdapter: UserAdapter

    private var listUser = mutableListOf<String>()
    private var currentUserUID: String? = null

    private val args: NewGroupFragmentArgs by navArgs()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        currentUserUID = args.currentUID
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewGroupBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.svNewGroup.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null && !currentUserUID.isNullOrEmpty()) {
                    lifecycleScope.launch {
                        userViewModel.searchUsers(query, currentUserUID!!)
                            .collectLatest { pagingData ->
                                userAdapter.submitData(pagingData)
                            }
                    }
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText != null && !currentUserUID.isNullOrEmpty()) {
                    lifecycleScope.launch {
                        userViewModel.searchUsers(newText, currentUserUID!!)
                            .collectLatest { pagingData ->
                                userAdapter.submitData(pagingData)
                            }
                    }
                }
                return true
            }
        })

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()

        userViewModel.selectedItems.observe(viewLifecycleOwner) { listUser ->
            this.listUser.clear()
            this.listUser.add(currentUserUID!!)
            listUser?.forEach { user ->
                this.listUser.add(user.userUID.toString())
            }
            userAdapter.updateSelectedUsers(this.listUser)
            requireActivity().invalidateMenu()
        }

        conversationViewModel.conversationResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                ReturnResult.Loading -> {}

                is ReturnResult.Success -> {
                    val newGroupId = result.data.conversationId
                    val action =
                        NewGroupFragmentDirections.actionNewGroupFragmentToConversationFragment(
                            currentUserUID!!, null, newGroupId, true
                        )
                    findNavController().navigate(action)
                    conversationViewModel.resetResult()
                }

                is ReturnResult.Error -> {
                    Snackbar.make(requireView(), result.message, Snackbar.LENGTH_SHORT).show()
                    conversationViewModel.resetResult()
                }

                null -> {}
            }
        }
    }

    private fun setupRecyclerView() {
        userAdapter = UserAdapter(true) { user ->
            userViewModel.toggleSelection(user)
        }

        binding.rcvNewGroup.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@NewGroupFragment.userAdapter.withLoadStateHeaderAndFooter(
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
        userViewModel.resetReturnResult()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.new_group, menu)
    }

    override fun onPrepareMenu(menu: Menu) {
        super.onPrepareMenu(menu)
        val itemCreate = menu.findItem(R.id.item_create)
        itemCreate.isEnabled = (listUser.size > 2)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.item_create -> {
                conversationViewModel.createConversation(
                    true,
                    binding.edtNameGroup.text.toString(), null, listUser.toList()
                )
                return true
            }
        }

        return false
    }
}