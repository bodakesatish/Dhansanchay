package com.dhansanchay.ui.scheme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.semantics.text
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
// import com.google.android.material.snackbar.Snackbar // For error display
import com.dhansanchay.databinding.FragmentSchemeMetaBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SchemeDetailFragment : Fragment() {

    private var _binding: FragmentSchemeMetaBinding? = null
    private val binding get() = _binding!! // Non-null assertion

    private val viewModel: SchemeMetaViewModel by viewModels()
    private val args: SchemeDetailFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View { // Return type can be non-nullable
        _binding = FragmentSchemeMetaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ViewModel's loadSchemeDetail should be robust enough to handle being called,
        // potentially checking against its current schemeCode or SavedStateHandle.
        // It's the ViewModel's responsibility to decide if a new fetch is needed.
        viewModel.loadSchemeDetail(args.schemeCodeArg)

        setupViews()
        observeUiState()
    }

    private fun setupViews() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.forceRefreshSchemeDetail()
            // The isRefreshing state will be managed by observing viewModel.uiState.isLoading
        }
        // You can group detail views for easier visibility toggling
        // e.g., binding.schemeDetailGroup.isVisible = false (initially or on error)
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.isVisible = state.isLoading && !binding.swipeRefreshLayout.isRefreshing

                    // Manage SwipeRefreshLayout's refreshing state
                    // This assumes any `isLoading` during a swipe action should show the indicator.
                    // If viewModel.forceRefreshSchemeDetail() sets its own flag for "swipe in progress"
                    // that could be more precise.
                    if (state.isLoading) {
                        // If swipeRefreshLayout is not already refreshing (e.g. initial load),
                        // and you want it to show for ALL loading, you might set it here.
                        // However, typically it's only set by user swipe or if you explicitly want
                        // the swipe indicator for a programmatic refresh.
                        // binding.swipeRefreshLayout.isRefreshing = true // If you want all loading to show swipe indicator
                    } else {
                        // Always hide swipe refresh when not loading
                        if (binding.swipeRefreshLayout.isRefreshing) {
                            binding.swipeRefreshLayout.isRefreshing = false
                        }
                    }

                    // Handle content visibility
                    binding.contentGroup.isVisible = state.schemeDetail != null && state.errorMessage == null // Assuming contentGroup wraps all detail TextViews

                    state.schemeDetail?.let { detail ->
                        binding.textViewSchemeName.text = detail.schemeName
                        binding.textViewSchemeCode.text = detail.schemeCode.toString()
                        binding.textViewFundHouse.text = detail.fundHouse
                        binding.textViewCategory.text = detail.schemeCategory
                        // Assuming isInGrowth is a Boolean, ensure it's converted to a displayable String
                        binding.textViewGrowth.text = detail.isInGrowth//if (detail.isInGrowth) "Growth" else "Dividend/Other"
                        // ... update other UI elements
                        binding.textViewError.isVisible = false // Hide error view if data is present
                    }

                    if (state.errorMessage != null) {
                        // Display error message
                        binding.textViewError.text = state.errorMessage
                        binding.textViewError.isVisible = true
                        // Optionally hide content group on error
                        // binding.contentGroup.isVisible = false
                        // Snackbar.make(binding.root, state.errorMessage, Snackbar.LENGTH_LONG).show()
                    } else {
                        binding.textViewError.isVisible = false
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Important to prevent memory leaks
    }
}