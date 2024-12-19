package com.lebatinh.messenger.account.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionInflater
import com.google.android.material.snackbar.Snackbar
import com.lebatinh.messenger.account.otp.OTPViewModel
import com.lebatinh.messenger.databinding.FragmentChangePasswordBinding
import com.lebatinh.messenger.other.ReturnResult
import com.lebatinh.messenger.user.UserViewModel
import com.lebatinh.messenger.user.Validator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChangePasswordFragment : Fragment() {

    private var _binding: FragmentChangePasswordBinding? = null
    private val binding get() = _binding!!

    private val userViewModel: UserViewModel by viewModels()
    private val otpViewModel: OTPViewModel by viewModels()
    private lateinit var validator: Validator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedElementEnterTransition = TransitionInflater.from(requireContext())
            .inflateTransition(android.R.transition.move)
        sharedElementReturnTransition = TransitionInflater.from(requireContext())
            .inflateTransition(android.R.transition.move)

        validator = Validator()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChangePasswordBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.imgBackChangePass.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnChangePass.setOnClickListener {
            val email = binding.edtEmailChangePass.text.toString().trim()
            val password = binding.edtPasswordChangePass.text.toString().trim()

            if (!validator.isValidEmail(email)) {
                Snackbar.make(root, "Email không hợp lệ!", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!validator.isValidPassword(password)) {
                Snackbar.make(
                    root,
                    "Mật khẩu không hợp lệ!.\nChỉ được phép chứa chữ, số và các ký tự @.*#",
                    Snackbar.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            userViewModel.login(email, password)
        }
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userViewModel.returnResult.observe(viewLifecycleOwner) { resultUser ->
            when (resultUser) {
                is ReturnResult.Loading -> {
                    binding.frLoading.visibility = View.VISIBLE
                }

                is ReturnResult.Success -> {
                    val email = binding.edtEmailChangePass.text.toString().trim()
                    otpViewModel.createAndSendOtp(email)

                    otpViewModel.otpResult.observe(viewLifecycleOwner) { resultOtp ->
                        when (resultOtp) {
                            is ReturnResult.Success -> {
                                binding.frLoading.visibility = View.GONE
                                Snackbar.make(
                                    binding.root,
                                    "OTP đã được gửi. Hãy kiểm tra hòm thư của bạn!",
                                    Snackbar.LENGTH_SHORT
                                ).show()
                                val extras = FragmentNavigatorExtras(
                                    binding.tvTitleChangePass to "title",
                                    binding.imgTitleChangePass to "animation",
                                    binding.cvChangePass to "card"
                                )
                                if (!resultUser.data.email.isNullOrEmpty()) {
                                    val action =
                                        ChangePasswordFragmentDirections.actionChangePasswordToOTPFragment(
                                            "change_pass",
                                            resultUser.data.email
                                        )
                                    findNavController().navigate(action, extras)
                                }
                                otpViewModel.resetReturnResult()
                            }

                            is ReturnResult.Error -> {
                                binding.frLoading.visibility = View.GONE
                                Snackbar.make(
                                    binding.root,
                                    resultOtp.message,
                                    Snackbar.LENGTH_SHORT
                                ).show()
                                otpViewModel.resetReturnResult()
                            }

                            ReturnResult.Loading -> {
                                binding.frLoading.visibility = View.VISIBLE
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
                    Snackbar.make(binding.root, resultUser.message, Snackbar.LENGTH_SHORT).show()
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