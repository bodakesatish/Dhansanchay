package com.dhansanchay.ui.login

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.dhansanchay.R
import com.dhansanchay.databinding.FragmentLoginBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FragmentLogin : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel : ViewModelLogin by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(tag, "In $tag onCreate")
//        if(sessionManager.getBoolean(SessionConstants.SESSION_REMEMBER_ME, false)) {
//            findNavController().navigate(R.id.fragment_scheme_list_dest)
//        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.i(tag, "In $tag onCreateView")
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initListeners()

    }

    private fun initListeners() {
        with(binding) {
            cbRememberMe.setOnClickListener {

            }
            btnLogin.setOnClickListener {
                if(cbRememberMe.isChecked) {
                    findNavController().navigate(R.id.fragment_scheme_list_dest)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}