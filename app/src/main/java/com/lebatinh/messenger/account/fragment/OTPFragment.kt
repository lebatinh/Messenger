package com.lebatinh.messenger.account.fragment

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.transition.TransitionInflater
import com.google.android.material.snackbar.Snackbar
import com.lebatinh.messenger.R
import com.lebatinh.messenger.account.otp.OTPRepository
import com.lebatinh.messenger.account.otp.OTPViewModel
import com.lebatinh.messenger.account.otp.OTPViewModelFactory
import com.lebatinh.messenger.databinding.FragmentOtpBinding
import com.lebatinh.messenger.helper.GmailHelper
import com.lebatinh.messenger.other.ReturnResult
import com.lebatinh.messenger.user.Validator

class OTPFragment : Fragment() {
    private var _binding: FragmentOtpBinding? = null
    private val binding get() = _binding!!

    private lateinit var otpViewModel: OTPViewModel
    private lateinit var gmailHelper: GmailHelper
    private lateinit var validator: Validator

    private var type: String? = null
    private var email: String? = null

    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedElementEnterTransition = TransitionInflater.from(requireContext())
            .inflateTransition(android.R.transition.move)
        sharedElementReturnTransition = TransitionInflater.from(requireContext())
            .inflateTransition(android.R.transition.move)

        gmailHelper = GmailHelper()
        val repositoryOTP = OTPRepository()
        otpViewModel =
            ViewModelProvider(this, OTPViewModelFactory(repositoryOTP))[OTPViewModel::class.java]

        validator = Validator()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOtpBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val args: OTPFragmentArgs by navArgs()
        type = args.type
        email = args.email

        binding.imgBackOTP.setOnClickListener {
            findNavController().popBackStack()
        }

        startCountdown(binding.tvTimeOTP)

        if (binding.tvTimeOTP.text.equals("0s")) {
            resendOTP()
        }

        binding.btnConfirmOTP.setOnClickListener {
            val otp = binding.edtOTP.text.toString()
            if (otp.isEmpty() || otp.length != 6) {
                Snackbar.make(requireView(), "Mã OTP không hợp lệ!", Snackbar.LENGTH_SHORT)
                    .show()
            }

            if (email.isNullOrEmpty() || !validator.isValidEmail(email!!)) {
                Snackbar.make(requireView(), "Có lỗi xảy ra!", Snackbar.LENGTH_SHORT)
                    .show()
            }

            otpViewModel.verifyOtp(email!!, otp)
        }

        return root
    }

    override fun onResume() {
        super.onResume()

        otpViewModel.otpVerificationResult.observe(viewLifecycleOwner) { otpResult ->
            when (otpResult) {
                ReturnResult.Loading -> {
                    binding.frLoading.visibility = View.VISIBLE
                }

                is ReturnResult.Error -> {
                    binding.frLoading.visibility = View.GONE
                    Snackbar.make(
                        requireView(),
                        otpResult.message,
                        Snackbar.LENGTH_SHORT
                    ).show()
                }

                is ReturnResult.Success -> {
                    binding.frLoading.visibility = View.GONE
                    when (type) {
                        "register" -> {
                            val extras = FragmentNavigatorExtras(
                                binding.tvTitleOTP to "title",
                                binding.imgTitleOTP to "animation",
                                binding.cvOTP to "card"
                            )
                            if (!email.isNullOrEmpty() && !type.isNullOrEmpty()) {
                                val action =
                                    OTPFragmentDirections.actionOTPFragmentToAccountInfoFragment(
                                        type!!,
                                        email!!
                                    )
                                findNavController().navigate(action, extras)
                            }
                            otpViewModel.resetOtpVerificationResult()
                        }

                        "change_pass" -> {
                            val extras = FragmentNavigatorExtras(
                                binding.tvTitleOTP to "title",
                                binding.imgTitleOTP to "animation",
                                binding.cvOTP to "card"
                            )
                            if (!email.isNullOrEmpty() && !type.isNullOrEmpty()) {
                                val action =
                                    OTPFragmentDirections.actionOTPFragmentToNewPasswordFragment(
                                        type!!,
                                        email!!
                                    )
                                findNavController().navigate(action, extras)
                            }
                            otpViewModel.resetOtpVerificationResult()
                        }

                        else -> {
                            Snackbar.make(
                                requireView(),
                                "Có lỗi xảy ra! Hãy thử lại sau.",
                                Snackbar.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                null -> {
                    binding.frLoading.visibility = View.GONE
                }
            }
        }
    }

    private fun startCountdown(tvTimeOTP: TextView) {
        val totalTime = 180 * 1000L
        val interval = 1000L

        binding.tvReSendOTP.text = "Gửi lại OTP sau"

        val drawable = tvTimeOTP.background as GradientDrawable

        countDownTimer = object : CountDownTimer(totalTime, interval) {
            override fun onTick(millisUntilFinished: Long) {
                binding.tvReSendOTP.isEnabled = false
                val secondsRemaining = millisUntilFinished / 1000
                tvTimeOTP.text = "${secondsRemaining}s"
                if (secondsRemaining == 0L) {
                    drawable.setStroke(
                        2,
                        ContextCompat.getColor(requireContext(), R.color.border_color_enable)
                    )
                    binding.tvReSendOTP.text = "Gửi lại OTP"
                } else {
                    drawable.setStroke(
                        2,
                        ContextCompat.getColor(requireContext(), R.color.border_color_disable)
                    )
                }
            }

            override fun onFinish() {
                tvTimeOTP.text = "0s"
                binding.tvReSendOTP.isEnabled = true
                resendOTP()
                drawable.setStroke(
                    2,
                    ContextCompat.getColor(requireContext(), R.color.border_color_enable)
                )
            }
        }.start()
    }

    private fun resendOTP() {
        binding.tvReSendOTP.setOnClickListener {
            if (!validator.isValidEmail(email!!) || email.isNullOrEmpty()) {
                Snackbar.make(binding.root, "Email không hợp lệ!", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            otpViewModel.createAndSendOtp(email!!)

            otpViewModel.otpResult.observe(viewLifecycleOwner) { resultOtp ->
                when (resultOtp) {
                    ReturnResult.Loading -> {
                        binding.frLoading.visibility = View.VISIBLE
                    }

                    is ReturnResult.Success -> {
                        binding.frLoading.visibility = View.GONE
                        countDownTimer?.cancel()
                        startCountdown(binding.tvTimeOTP)
                        Snackbar.make(
                            binding.root,
                            "OTP đã được gửi lại. Hãy kiểm tra hòm thư của bạn!",
                            Snackbar.LENGTH_SHORT
                        ).show()
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

                    null -> {
                        binding.frLoading.visibility = View.GONE
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        countDownTimer?.cancel()
    }
}