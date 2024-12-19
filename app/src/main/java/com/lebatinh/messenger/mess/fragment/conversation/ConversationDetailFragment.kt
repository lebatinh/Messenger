package com.lebatinh.messenger.mess.fragment.conversation

import android.os.Bundle
import android.util.Log
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
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.lebatinh.messenger.R
import com.lebatinh.messenger.databinding.FragmentConversationDetailBinding
import com.lebatinh.messenger.other.ReturnResult
import com.lebatinh.messenger.user.UserViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConversationDetailFragment : Fragment(), MenuProvider {

    private var _binding: FragmentConversationDetailBinding? = null
    private val binding get() = _binding!!

    private val userViewModel: UserViewModel by viewModels()
    private val conversationViewModel: ConversationViewModel by viewModels()

    private var currentUID: String? = null
    private var receiverUID: String? = null
    private var isGroup: Boolean? = null
    private var conversationId: String? = null

    private val args: ConversationDetailFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        currentUID = args.currentUID
        receiverUID = args.receiverUID
        isGroup = args.isGroup
        conversationId = args.conversationID
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConversationDetailBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        conversationViewModel.setConversationId(conversationId)
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
                ReturnResult.Loading -> {
                    binding.frLoading.visibility = View.VISIBLE
                }

                is ReturnResult.Success -> {
                    Glide.with(this).load(result.data.avatar).into(binding.imgAvatar)
                    binding.tvName.text = result.data.fullName
                    binding.tvDesc.text = result.data.desc

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
                ReturnResult.Loading -> {
                    binding.frLoading.visibility = View.VISIBLE
                }

                is ReturnResult.Success -> {
                    Glide.with(this).load(result.data.imageGroup).into(binding.imgAvatar)
                    binding.tvName.text = result.data.conversationName
                    result.data.listIdChatPerson?.size.toString()
                        .also { binding.tvDesc.text = "$it thành viên" }

                    userViewModel.resetReturnResult()
                }

                is ReturnResult.Error -> {
                    binding.frLoading.visibility = View.GONE
                    Snackbar.make(requireView(), result.message, Snackbar.LENGTH_SHORT).show()
                    Log.d("result", result.message)
                    userViewModel.resetReturnResult()
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
        menuInflater.inflate(R.menu.conversation_detail, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.item_more -> {

                return true
            }
        }
        return false
    }
}