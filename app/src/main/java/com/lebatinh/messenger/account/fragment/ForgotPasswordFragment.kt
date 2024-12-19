package com.lebatinh.messenger.account.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionInflater
import com.google.android.material.snackbar.Snackbar
import com.lebatinh.messenger.databinding.FragmentForgotPasswordBinding
import com.lebatinh.messenger.other.ReturnResult
import com.lebatinh.messenger.user.UserRepository
import com.lebatinh.messenger.user.UserViewModel
import com.lebatinh.messenger.user.UserViewModelFactory
import com.lebatinh.messenger.user.Validator

class ForgotPasswordFragment : Fragment() {

    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!

    private lateinit var userViewModel: UserViewModel
    private lateinit var validator: Validator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = TransitionInflater.from(requireContext())
            .inflateTransition(android.R.transition.move)
        sharedElementReturnTransition = TransitionInflater.from(requireContext())
            .inflateTransition(android.R.transition.move)

        val repository = UserRepository()
        userViewModel =
            ViewModelProvider(this, UserViewModelFactory(repository))[UserViewModel::class.java]

        validator = Validator()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.imgBackForgotPass.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnConfirm.setOnClickListener {
            val email = binding.edtEmailForgotPass.text.toString().trim()
            val name = binding.edtNameForgotPass.text.toString().trim()

            if (!validator.isValidEmail(email)) {
                Snackbar.make(root, "Email không hợp lệ!", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (name.isEmpty()) {
                Snackbar.make(
                    root,
                    "Tên không được để trống",
                    Snackbar.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            userViewModel.forgot(email, name)
        }
        return root
    }

    override fun onResume() {
        super.onResume()

        userViewModel.returnResult.observe(viewLifecycleOwner) { returnResult ->
            when (returnResult) {
                is ReturnResult.Loading -> {
                    binding.frLoading.visibility = View.VISIBLE
                }

                is ReturnResult.Success -> {
                    val email = binding.edtEmailForgotPass.text.toString().trim()
                    userViewModel.sendResetPasswordEmail(email)
                    userViewModel.unitResult.observe(viewLifecycleOwner) { unitResult ->
                        when (unitResult) {
                            ReturnResult.Loading -> {
                                binding.frLoading.visibility = View.VISIBLE
                            }

                            is ReturnResult.Success -> {
                                binding.frLoading.visibility = View.GONE
                                binding.ctlForgotPassword.visibility = View.GONE
                                binding.ctlForgotPasswordSuccess.visibility = View.VISIBLE

                                binding.btnBack.setOnClickListener {
                                    findNavController().popBackStack()
                                }
                                userViewModel.resetReturnResult()
                            }

                            is ReturnResult.Error -> {
                                binding.frLoading.visibility = View.GONE
                                Snackbar.make(
                                    binding.root,
                                    unitResult.message,
                                    Snackbar.LENGTH_SHORT
                                ).show()
                                userViewModel.resetReturnResult()
                            }

                            null -> {
                                binding.frLoading.visibility = View.GONE
                            }
                        }
                    }
                    userViewModel.resetReturnResult()
                }

                is ReturnResult.Error -> {
                    binding.frLoading.visibility = View.GONE
                    Snackbar.make(binding.root, returnResult.message, Snackbar.LENGTH_SHORT).show()
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