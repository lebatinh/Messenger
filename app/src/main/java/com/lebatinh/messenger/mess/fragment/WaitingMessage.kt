package com.lebatinh.messenger.mess.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.lebatinh.messenger.databinding.FragmentWaitingMessageBinding

class WaitingMessage : Fragment() {
    private var _binding: FragmentWaitingMessageBinding? = null
    private val binding get() = _binding!!

    private var currentUID: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        currentUID = activity?.intent?.getStringExtra("userUID")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWaitingMessageBinding.inflate(inflater, container, false)
        val root: View = binding.root

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}