package com.lebatinh.messenger.account.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.transition.TransitionInflater
import com.google.android.material.snackbar.Snackbar
import com.lebatinh.messenger.databinding.FragmentNewPasswordBinding
import com.lebatinh.messenger.other.ReturnResult
import com.lebatinh.messenger.user.UserRepository
import com.lebatinh.messenger.user.UserViewModel
import com.lebatinh.messenger.user.UserViewModelFactory
import com.lebatinh.messenger.user.Validator

class NewPasswordFragment : Fragment() {
    private var _binding: FragmentNewPasswordBinding? = null
    private val binding get() = _binding!!

    private lateinit var userViewModel: UserViewModel
    private lateinit var validator: Validator

    private var type: String? = null
    private var email: String? = null
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
        _binding = FragmentNewPasswordBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val args: OTPFragmentArgs by navArgs()
        type = args.type
        email = args.email

        binding.imgBackNewPassword.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnNewPassword.setOnClickListener {
            val pass = binding.edtNewPassword.text.toString().trim()
            val repass = binding.edtReNewPassword.text.toString().trim()

            if (!validator.isValidEmail(email!!)) {
                Snackbar.make(root, "Email không hợp lệ!", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!validator.isValidRePassword(pass, repass)) {
                Snackbar.make(
                    root,
                    "Mật khẩu không hợp lệ!.\nChỉ được phép chứa chữ, số và các ký tự @.*#",
                    Snackbar.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (type.isNullOrEmpty()) {
                Snackbar.make(
                    root,
                    "Có lỗi xảy ra! Hãy thử lại sau",
                    Snackbar.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            userViewModel.changePassword(pass)
        }

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userViewModel.unitResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                ReturnResult.Loading -> {
                    binding.frLoading.visibility = View.VISIBLE
                }

                is ReturnResult.Success -> {
                    binding.frLoading.visibility = View.GONE
                    binding.ctlInfo.visibility = View.GONE
                    binding.ctlNewPasswordSuccess.visibility = View.VISIBLE
                    binding.btnBack.setOnClickListener { navigateToLogin() }
                    binding.imgBackNewPassword.setOnClickListener { navigateToLogin() }
                    userViewModel.resetReturnResult()
                }

                is ReturnResult.Error -> {
                    binding.frLoading.visibility = View.GONE
                    Snackbar.make(
                        binding.root,
                        result.message,
                        Snackbar.LENGTH_SHORT
                    ).show()
                    userViewModel.resetReturnResult()
                }

                null -> {
                    binding.frLoading.visibility = View.GONE
                }
            }
        }
    }

    private fun navigateToLogin() {
        val extras = FragmentNavigatorExtras(
            binding.tvTitleNewPassword to "title",
            binding.imgTitleNewPassword to "animation",
            binding.cvNewPassword to "card"
        )
        val action = NewPasswordFragmentDirections.actionNewPasswordFragmentToLogin()
        findNavController().navigate(action, extras)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}