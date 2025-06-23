package com.dhansanchay.ui.scheme

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dhansanchay.databinding.FragmentSchemeListBinding
import com.dhansanchay.ui.scheme.adapter.PagedSchemeListAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FragmentSchemeList : Fragment() {

    private val tag = this.javaClass.simpleName

    private var _binding: FragmentSchemeListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ViewModelSchemeList by viewModels()

    private val schemeListAdapter: PagedSchemeListAdapter = PagedSchemeListAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.i(tag, "In $tag onCreateView")
        _binding = FragmentSchemeListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(tag, "In $tag onViewCreated")

        initView()
        initObserver()

    }

    private fun initView() {
        with(binding) {
            rvSchemeList.apply {
                setHasFixedSize(true)
                adapter = schemeListAdapter
            }
            // Setup SwipeRefreshLayout
            swipeRefreshLayout.setOnRefreshListener {
                Log.d(tag, "Swipe to refresh triggered.")
                // Call a refresh function in your ViewModel
                // This assumes your ViewModel has a method like `refreshSchemes()`
                // which will eventually update the uiState (including isLoading).
                viewModel.forceRefreshSchemes()
            }
        }


    }

    private fun initObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    Log.d(tag, "Collected UI State: isLoading=${state.isLoading}, schemes=${state.schemes.size}, error=${state.errorMessage}")

                    schemeListAdapter.submitList(state.schemes)

                    // Manage SwipeRefreshLayout's refreshing state
                    // The SwipeRefreshLayout's own progress indicator will show during refresh.
                    // If state.isLoading is true because of swipe-to-refresh,
                    // swipeRefreshLayout.isRefreshing will be true.
                    // If state.isLoading becomes false, we must set isRefreshing to false.
                    if (!state.isLoading && binding.swipeRefreshLayout.isRefreshing) {
                        binding.swipeRefreshLayout.isRefreshing = false
                    }
                    // If you want the swipe refresh to show for the initial load too,
                    // you might do: binding.swipeRefreshLayout.isRefreshing = state.isLoading
                    // However, this might conflict if you also have a central progress bar.
                    // Typically, the central progress_bar is for initial load or full screen blocking load,
                    // and swipeRefreshLayout is for user-initiated refreshes.

                    // Manage central ProgressBar visibility (for initial load or other non-swipe loads)
                    // Show central progress bar only if it's loading and not a swipe refresh action
                    // (assuming swipe refresh sets its own indicator)
                    if (state.isLoading && !binding.swipeRefreshLayout.isRefreshing) {
                        binding.progressBar.visibility = View.VISIBLE
                    } else {
                        binding.progressBar.visibility = View.GONE
                    }


                    if (state.errorMessage != null) {
                        Toast.makeText(context, state.errorMessage, Toast.LENGTH_LONG).show()
                        Log.e(tag, "Error state: ${state.errorMessage}")
                        // If swipe refresh resulted in an error, ensure its indicator is hidden
                        if (binding.swipeRefreshLayout.isRefreshing) {
                            binding.swipeRefreshLayout.isRefreshing = false
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.i(tag, "In $tag onDestroyView")
        _binding = null
    }

}