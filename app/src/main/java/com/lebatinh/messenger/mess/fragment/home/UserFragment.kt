package com.lebatinh.messenger.mess.fragment.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.lebatinh.messenger.databinding.FragmentUserBinding
import com.lebatinh.messenger.mess.fragment.conversation.ConversationViewModel
import com.lebatinh.messenger.other.ReturnResult
import com.lebatinh.messenger.user.UserViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UserFragment : Fragment() {
    private var _binding: FragmentUserBinding? = null
    private val binding get() = _binding!!

    private val userViewModel: UserViewModel by viewModels()
    private val conversationViewModel: ConversationViewModel by viewModels()

    private lateinit var adapter: UserAdapter

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
                    userViewModel.searchUsers(query, currentUserUID)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText != null) {
                    userViewModel.searchUsers(newText, currentUserUID)
                }
                return true
            }
        })

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userViewModel.listResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                ReturnResult.Loading -> {}

                is ReturnResult.Success -> {
                    val listUser = result.data

                    if (::adapter.isInitialized) {
                        adapter.updateList(listUser)
                    } else {
                        adapter = UserAdapter(listUser.toMutableList(), false) { user ->
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
                    }

                    binding.rcvUser.layoutManager = LinearLayoutManager(requireContext())
                    binding.rcvUser.adapter = adapter
                    userViewModel.resetReturnResult()
                }

                is ReturnResult.Error -> {
                    Snackbar.make(requireView(), result.message, Snackbar.LENGTH_SHORT).show()
                    userViewModel.resetReturnResult()
                }

                null -> {}
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}