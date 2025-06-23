package com.dhansanchay.ui.scheme

// import com.google.android.material.snackbar.Snackbar // Alternative for error display
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.dhansanchay.databinding.FragmentSchemeListBinding
import com.dhansanchay.domain.model.SchemeModel
import com.dhansanchay.ui.scheme.adapter.SchemeListAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FragmentSchemeList : Fragment() {

    private val tag = this.javaClass.simpleName

    private var _binding: FragmentSchemeListBinding? = null
    private val binding get() = _binding!! // Non-null assertion

    private val viewModel: ViewModelSchemeList by viewModels()

    // Initialize adapter with click listener via constructor (assuming adapter was modified)
    private val schemeListAdapter: SchemeListAdapter by lazy {
        SchemeListAdapter { selectedSchemeModel ->
            navigateToSchemeDetail(selectedSchemeModel)
        }
    }
    // If SchemeListAdapter still uses setOnClickListener:
    // private lateinit var schemeListAdapter: SchemeListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View { // Return type can be non-nullable View
        Log.i(tag, "In $tag onCreateView")
        _binding = FragmentSchemeListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(tag, "In $tag onViewCreated")

        setupViews()
        observeViewModel()
    }

    private fun setupViews() {
        // If not using constructor injection for adapter's click listener:
        // schemeListAdapter = SchemeListAdapter()
        // schemeListAdapter.setOnClickListener { navigateToSchemeDetail(it) }

        binding.rvSchemeList.apply {
            setHasFixedSize(true)
            adapter = schemeListAdapter
            // Optionally, add ItemDecorations here if needed
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            Log.d(tag, "Swipe to refresh triggered.")
            viewModel.forceRefreshSchemes()
        }
    }

    private fun navigateToSchemeDetail(schemeModel: SchemeModel) {
        val action = FragmentSchemeListDirections.actionSchemeListToSchemeDetail(schemeModel.schemeCode)
        findNavController().navigate(action)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    Log.d(tag, "Collected UI State: isLoading=${state.isLoading}, schemes=${state.schemes.size}, error=${state.errorMessage}")

                    schemeListAdapter.submitList(state.schemes)

                    // Simplified loading state management
                    val isRefreshingByUser = binding.swipeRefreshLayout.isRefreshing

                    // Show central progress bar only for initial loads or non-swipe refresh loads
                    binding.progressBar.isVisible = state.isLoading && !isRefreshingByUser && state.schemes.isEmpty()

                    // Manage SwipeRefreshLayout's refreshing state
                    // It should only be true if a swipe caused the loading,
                    // or if you want it to show for any loading state.
                    // The common pattern: if swipeRefreshLayout initiated the load, it shows its own progress.
                    // We only need to turn it OFF when loading finishes.
                    if (!state.isLoading && isRefreshingByUser) {
                        binding.swipeRefreshLayout.isRefreshing = false
                    }
                    // Optional: If you want swipe-to-refresh to also indicate initial loading
                    // binding.swipeRefreshLayout.isRefreshing = state.isLoading

                    if (state.errorMessage != null) {
                        // Consider using Snackbar for better UX or an inline error TextView
                        Toast.makeText(context, state.errorMessage, Toast.LENGTH_LONG).show()
                        // Snackbar.make(binding.root, state.errorMessage, Snackbar.LENGTH_LONG).show()
                        Log.e(tag, "Error state: ${state.errorMessage}")
                        // Ensure swipe refresh indicator is hidden on error if it was active
                        if (isRefreshingByUser) { // Or simply if (binding.swipeRefreshLayout.isRefreshing)
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
        binding.rvSchemeList.adapter = null // Good practice to clear adapter
        _binding = null
    }
}