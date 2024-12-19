package com.lebatinh.messenger.account.fragment

import android.animation.Animator
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.transition.TransitionInflater
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.lebatinh.messenger.databinding.FragmentAccountInfoBinding
import com.lebatinh.messenger.other.ReturnResult
import com.lebatinh.messenger.user.AreaCode
import com.lebatinh.messenger.user.UserRepository
import com.lebatinh.messenger.user.UserViewModel
import com.lebatinh.messenger.user.UserViewModelFactory
import com.lebatinh.messenger.user.Validator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AccountInfoFragment : Fragment() {
    private var _binding: FragmentAccountInfoBinding? = null
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
        _binding = FragmentAccountInfoBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val args: OTPFragmentArgs by navArgs()
        type = args.type
        email = args.email

        binding.imgBackAccountInfo.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.tvBirthday.setOnClickListener {
            showDatePickerDialog(binding.tvBirthday)
        }

        binding.tvAreaCode.setOnClickListener {
            showAreaCodePickerDialog(binding.tvAreaCode)
        }

        binding.btnConfirm.setOnClickListener {
            val name = binding.edtName.text.toString().trim()
            val birthday = binding.tvBirthday.text.toString().trim()

            val areaCode = binding.tvAreaCode.text.toString().trim()
            val phone = binding.edtPhone.text.toString().trim()

            if (name.isEmpty()) {
                Snackbar.make(requireView(), "Tên không được để trống!", Snackbar.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            if (birthday.isEmpty()) {
                Snackbar.make(
                    requireView(),
                    "Ngày sinh không được để trống!",
                    Snackbar.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (!validator.isValidPhoneNumber(phone)) {
                Snackbar.make(
                    requireView(),
                    "Số điện thoại không hợp lệ!",
                    Snackbar.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (email.isNullOrEmpty() || !validator.isValidEmail(email!!) || !type.equals("register")) {
                Snackbar.make(
                    requireView(),
                    "Có lỗi xảy ra! Hãy thử lại sau.",
                    Snackbar.LENGTH_SHORT
                ).show()

                navigateToLogin()

                return@setOnClickListener
            }

            val numberphone = if (phone.startsWith("0")) {
                "$areaCode${phone.removePrefix("0")}"
            } else {
                "$areaCode$phone"
            }

            userViewModel.updateUserInfo(email!!, name, numberphone, birthday, null)
        }

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userViewModel.unitResult.observe(viewLifecycleOwner) { unitResult ->
            when (unitResult) {
                ReturnResult.Loading -> {
                    binding.frLoading.visibility = View.VISIBLE
                }

                is ReturnResult.Success -> {
                    binding.frLoading.visibility = View.GONE
                    binding.ctlInfo.visibility = View.GONE
                    binding.ctlRegisterSuccess.visibility = View.VISIBLE

                    binding.imgTitleAccountInfo.repeatCount = 0
                    binding.imgTitleAccountInfo.setAnimation("animation_lottie/register_successfully.json")
                    binding.imgTitleAccountInfo.addAnimatorListener(object :
                        Animator.AnimatorListener {
                        override fun onAnimationStart(p0: Animator) {}
                        override fun onAnimationEnd(p0: Animator) {
                            binding.imgTitleAccountInfo.pauseAnimation()
                        }

                        override fun onAnimationCancel(p0: Animator) {}
                        override fun onAnimationRepeat(p0: Animator) {}
                    })

                    binding.imgTitleAccountInfo.playAnimation()

                    binding.btnBack.setOnClickListener {
                        navigateToLogin()
                    }

                    binding.imgBackAccountInfo.setOnClickListener {
                        navigateToLogin()
                    }
                }

                is ReturnResult.Error -> {
                    binding.frLoading.visibility = View.GONE
                    Snackbar.make(binding.root, unitResult.message, Snackbar.LENGTH_SHORT).show()
                }

                null -> {
                    binding.frLoading.visibility = View.GONE
                }
            }
        }
    }

    private fun showDatePickerDialog(tvBirthday: TextView) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay)

                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val selectedDate = dateFormat.format(calendar.time)

                tvBirthday.text = selectedDate
            },
            year, month, day
        )

        datePickerDialog.show()
    }

    private fun showAreaCodePickerDialog(tvAreaCode: TextView) {
        val json =
            requireContext().assets.open("area_code.json").bufferedReader().use { it.readText() }
        val areaCodes = Gson().fromJson(json, Array<AreaCode>::class.java).toList()

        val areaCodeList = areaCodes.map { "${it.code} - ${it.country}" }

        AlertDialog.Builder(requireContext())
            .setTitle("Chọn mã vùng")
            .setItems(areaCodeList.toTypedArray()) { _, which ->
                val selectedAreaCode = areaCodes[which].code
                tvAreaCode.text = selectedAreaCode
            }
            .create()
            .show()
    }

    private fun navigateToLogin() {
        val extras = FragmentNavigatorExtras(
            binding.tvTitleAccountInfo to "title",
            binding.imgTitleAccountInfo to "animation",
            binding.cvAccountInfo to "card"
        )
        val action = AccountInfoFragmentDirections.actionAccountInfoFragmentToLogin()
        findNavController().navigate(action, extras)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}