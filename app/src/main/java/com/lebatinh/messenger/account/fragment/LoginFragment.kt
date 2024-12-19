package com.lebatinh.messenger.account.fragment

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionInflater
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.lebatinh.messenger.Key_Password.KEY_LOGIN_SAVED
import com.lebatinh.messenger.R
import com.lebatinh.messenger.animation.CustomLottieAnimation
import com.lebatinh.messenger.databinding.FragmentLoginBinding
import com.lebatinh.messenger.helper.DataStoreManager
import com.lebatinh.messenger.mess.MessageActivity
import com.lebatinh.messenger.other.ReturnResult
import com.lebatinh.messenger.user.UserRepository
import com.lebatinh.messenger.user.UserViewModel
import com.lebatinh.messenger.user.UserViewModelFactory
import com.lebatinh.messenger.user.Validator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var userViewModel: UserViewModel

    private lateinit var validator: Validator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedElementEnterTransition = TransitionInflater.from(requireContext())
            .inflateTransition(android.R.transition.move)
        sharedElementReturnTransition = TransitionInflater.from(requireContext())
            .inflateTransition(android.R.transition.move)

        validator = Validator()

        val repository = UserRepository()
        userViewModel =
            ViewModelProvider(this, UserViewModelFactory(repository))[UserViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        val root: View = binding.root

        lifecycleScope.launch {
            val isLoginSaved =
                DataStoreManager.getData(
                    requireContext(),
                    booleanPreferencesKey(KEY_LOGIN_SAVED),
                    false
                ).first()
            hideOnLogin(isLoginSaved)
        }

        setupAnimation()

        binding.btnLogin.setOnClickListener {
            val email = binding.edtEmailLogin.text.toString().trim()
            val password = binding.edtPasswordLogin.text.toString().trim()

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

        binding.tvLoginToForgotPassword.setOnClickListener {
            val extras = FragmentNavigatorExtras(
                binding.tvTitleLogin to "title",
                binding.imgTitleLogin to "animation",
                binding.cvLogin to "card"
            )
            findNavController().navigate(
                R.id.action_loginFragment_to_forgotPasswordFragment,
                null,
                null,
                extras
            )
        }
        binding.tvLoginToChangePassword.setOnClickListener {
            val extras = FragmentNavigatorExtras(
                binding.tvTitleLogin to "title",
                binding.imgTitleLogin to "animation",
                binding.cvLogin to "card"
            )
            findNavController().navigate(
                R.id.action_loginFragment_to_changePasswordFragment,
                null,
                null,
                extras
            )
        }
        binding.tvLoginToRegister.setOnClickListener {
            val extras = FragmentNavigatorExtras(
                binding.tvTitleLogin to "title",
                binding.imgTitleLogin to "animation",
                binding.cvLogin to "card"
            )
            findNavController().navigate(
                R.id.action_loginFragment_to_registerFragment,
                null,
                null,
                extras
            )
        }

        return root
    }

    override fun onResume() {
        super.onResume()

        userViewModel.returnResult.observe(viewLifecycleOwner) {
            when (it) {
                is ReturnResult.Loading -> {
                    binding.frLoading.visibility = View.VISIBLE
                }

                is ReturnResult.Success -> {
                    val user = it.data
                    binding.frLoading.visibility = View.GONE

                    if (!user.email.isNullOrEmpty()) {
                        val intent = Intent(requireContext(), MessageActivity::class.java)
                        intent.putExtra("email", user.email)
                        startActivity(intent)
                        requireActivity().finish()
                    }

                    userViewModel.resetReturnResult()
                }

                is ReturnResult.Error -> {
                    binding.frLoading.visibility = View.GONE
                    Snackbar.make(requireView(), it.message, Snackbar.LENGTH_SHORT).show()
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

    private fun hideOnLogin(isLoginSaved: Boolean) {
        binding.tilEmailLogin.isVisible = !isLoginSaved
        binding.ctlHello.isVisible = isLoginSaved
        binding.tvLoginSavedToLoginNormal.isVisible = isLoginSaved
    }

    private fun setupAnimation() {
        val customLottieAnimation = CustomLottieAnimation(binding.imgTitleLogin)
        var isPasswordHidden = true // Trạng thái ban đầu là che mắt
        var lastFocusedEditText: TextInputEditText? = null

        // Hàm điều chỉnh animation dựa trên trạng thái
        fun adjustAnimationBasedOnState() {
            when (lastFocusedEditText) {
                binding.edtEmailLogin -> customLottieAnimation.resetEyesToInitial()
                binding.edtPasswordLogin -> customLottieAnimation.toggleEyeCover(isPasswordHidden)
                else -> customLottieAnimation.resetEyesToDefault()
            }
        }

        // Tạo TextWatcher chung
        fun attachTextWatcher(editText: TextInputEditText, maxChar: Int = 10) {
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    val charCount = s?.length ?: 0
                    if (editText.hasFocus() && (!isPasswordHidden || editText == binding.edtEmailLogin)) {
                        customLottieAnimation.moveEyes(charCount, maxChar)
                    }
                }
            })
        }

        // Đặt trạng thái ban đầu cho mật khẩu
        binding.edtPasswordLogin.transformationMethod = PasswordTransformationMethod.getInstance()

        // Xử lý toggle che mật khẩu
        binding.tilPasswordLogin.setEndIconOnClickListener {
            isPasswordHidden = !isPasswordHidden
            binding.edtPasswordLogin.transformationMethod = if (isPasswordHidden) {
                PasswordTransformationMethod.getInstance()
            } else {
                null
            }
            binding.edtPasswordLogin.setSelection(binding.edtPasswordLogin.text?.length ?: 0)
            adjustAnimationBasedOnState()
        }

        // Lắng nghe sự thay đổi focus
        binding.edtEmailLogin.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                lastFocusedEditText = binding.edtEmailLogin
                adjustAnimationBasedOnState()
            }
        }

        binding.edtPasswordLogin.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                lastFocusedEditText = binding.edtPasswordLogin
                adjustAnimationBasedOnState()
            } else {
                customLottieAnimation.toggleEyeCover(false) // Khi bỏ focus khỏi Password, bỏ che mắt
            }
        }

        // Gắn TextWatcher cho các EditText
        attachTextWatcher(binding.edtEmailLogin)
        attachTextWatcher(binding.edtPasswordLogin)
    }
}