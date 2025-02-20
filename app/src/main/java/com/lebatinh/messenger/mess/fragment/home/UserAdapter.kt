package com.lebatinh.messenger.mess.fragment.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lebatinh.messenger.R
import com.lebatinh.messenger.databinding.UserItemBinding
import com.lebatinh.messenger.user.User

class UserAdapter(
    private val isChoose: Boolean,
    private val onClickItem: (User) -> Unit
) : PagingDataAdapter<User, UserAdapter.UserViewHolder>(DIFF_CALLBACK) {

    private val selectedUsers = mutableSetOf<String>()

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<User>() {
            override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
                return oldItem.userUID == newItem.userUID
            }

            override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
                return oldItem == newItem
            }
        }
    }

    inner class UserViewHolder(private val binding: UserItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                onClickChekedItem()
            }

            binding.ckbUser.setOnClickListener {
                onClickChekedItem()
            }
        }

        fun bind(item: User?) {
            item?.let {
                binding.apply {
                    item.avatar?.let { avatar ->
                        Glide.with(root.context)
                            .load(avatar)
                            .placeholder(R.drawable.default_avatar)
                            .error(R.drawable.image_error)
                            .circleCrop()
                            .into(imgAvatar)
                    }
                    tvName.text = item.fullName
                    item.desc?.let { desc -> tvDesc.text = desc }
                    ckbUser.isVisible = isChoose
                    ckbUser.isChecked = selectedUsers.contains(item.userUID)
                }
            }
        }

        private fun onClickChekedItem() {
            val position = bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                getItem(position)?.let { user ->
                    if (selectedUsers.contains(user.userUID)) {
                        selectedUsers.remove(user.userUID)
                    } else {
                        user.userUID?.let { selectedUsers.add(it) }
                    }
                    notifyItemChanged(position)
                    onClickItem(user)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = UserItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun updateSelectedUsers(selected: List<String>) {
        selectedUsers.clear()
        selectedUsers.addAll(selected)
        notifyDataSetChanged()
    }
}