package com.lebatinh.messenger.mess.fragment.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lebatinh.messenger.databinding.UserItemBinding
import com.lebatinh.messenger.mess.fragment.ItemDiffCallback
import com.lebatinh.messenger.user.User

class UserAdapter(
    private val items: MutableList<User>,
    private val isChoose: Boolean,
    private val onClickItem: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    private val selectedUsers = mutableSetOf<String>()

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

        fun bind(item: User) {
            item.avatar?.let { Glide.with(binding.root).load(it).into(binding.imgAvatar) }
            binding.tvName.text = item.fullName
            item.desc?.let { binding.tvDesc.text = it }
            binding.ckbUser.isVisible = isChoose
            binding.ckbUser.isChecked = selectedUsers.contains(item.userUID)
        }
    }

    private fun UserViewHolder.onClickChekedItem() {
        val user = items[absoluteAdapterPosition]
        if (selectedUsers.contains(user.userUID)) {
            selectedUsers.remove(user.userUID)
        } else {
            selectedUsers.add(user.userUID!!)
        }
        notifyItemChanged(absoluteAdapterPosition)
        onClickItem(user)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = UserItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(items[position])
    }

    fun updateList(newList: List<User>) {
        val diffCallback = ItemDiffCallback(
            oldList = items,
            newList = newList,
            areItemsTheSame = { oldItem, newItem -> oldItem.userUID == newItem.userUID },
            areContentsTheSame = { oldItem, newItem -> oldItem == newItem }
        )
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        items.clear()
        items.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    fun updateSelectedUsers(selected: List<String>) {
        selectedUsers.clear()
        selectedUsers.addAll(selected)
        notifyDataSetChanged()
    }
}