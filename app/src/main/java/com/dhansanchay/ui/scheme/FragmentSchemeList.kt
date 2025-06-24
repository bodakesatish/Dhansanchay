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
import androidx.work.WorkInfo
import com.dhansanchay.databinding.FragmentSchemeListBinding
import com.dhansanchay.domain.model.SchemeModel
import com.dhansanchay.ui.scheme.adapter.SchemeListAdapter
import com.dhansanchay.work.AllSchemeDetailsSyncWorker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
        binding.buttonSyncAllDetails.setOnClickListener {
            Log.d(tag, "Sync All Details button clicked by user.")
            showConfirmationDialogForFullSync() // Show confirmation before starting
        }

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
            viewModel.forceRefreshSchemes() // This refreshes the scheme list, not the full detail sync
        }
    }

    private fun navigateToSchemeDetail(schemeModel: SchemeModel) {
        val action = FragmentSchemeListDirections.actionSchemeListToSchemeDetail(schemeModel.schemeCode)
        findNavController().navigate(action)
    }

    private fun observeViewModel() {
        // Observe LiveData outside the repeatOnLifecycle for UI state if preferred,
        // or keep it within if you want its observation tied to STARTED state strictly.
        // For WorkInfo, it's often fine to observe it as long as the view is alive.
        viewModel.syncWorkInfo.observe(viewLifecycleOwner) { workInfos ->
            val workInfo = workInfos?.firstOrNull { it.tags.contains(AllSchemeDetailsSyncWorker.WORK_NAME) } // Ensure it's our worker

            if (workInfo != null) {
                Log.d(tag, "WorkInfo received: State=${workInfo.state}, Progress=${workInfo.progress}")
                binding.buttonSyncAllDetails.isEnabled = !workInfo.state.isFinished && workInfo.state != WorkInfo.State.ENQUEUED && workInfo.state != WorkInfo.State.RUNNING && workInfo.state != WorkInfo.State.BLOCKED

                when (workInfo.state) {
                    WorkInfo.State.ENQUEUED -> {
                        binding.textViewSyncStatus.text = "Sync: Scheduled..." // Assuming you add a TextView
                        binding.buttonSyncAllDetails.isEnabled = false
                        // Show a specific progress bar for the sync process
                        binding.syncProgressBar.visibility = View.VISIBLE // Assuming you have a dedicated progress bar
                        binding.syncProgressBar.isIndeterminate = true
                    }
                    WorkInfo.State.RUNNING -> {
                        val current = workInfo.progress.getInt(AllSchemeDetailsSyncWorker.KEY_PROGRESS_CURRENT, 0)
                        val total = workInfo.progress.getInt(AllSchemeDetailsSyncWorker.KEY_PROGRESS_TOTAL, 0)
                        binding.textViewSyncStatus.text = "Sync: In Progress ($current/$total)..."
                        binding.buttonSyncAllDetails.isEnabled = false
                        binding.syncProgressBar.visibility = View.VISIBLE
                        if (total > 0) {
                            binding.syncProgressBar.isIndeterminate = false
                            binding.syncProgressBar.max = total
                            binding.syncProgressBar.progress = current
                        } else {
                            binding.syncProgressBar.isIndeterminate = true
                        }
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        val totalProcessed = workInfo.progress.getInt(AllSchemeDetailsSyncWorker.KEY_PROGRESS_CURRENT, 0)
                        binding.textViewSyncStatus.text = "Sync: Complete! ($totalProcessed items)"
                        binding.buttonSyncAllDetails.isEnabled = true
                        binding.syncProgressBar.visibility = View.GONE
                        Toast.makeText(context, "All scheme details synced successfully.", Toast.LENGTH_SHORT).show()
                    }
                    WorkInfo.State.FAILED -> {
                        binding.textViewSyncStatus.text = "Sync: Failed. Check logs."
                        binding.buttonSyncAllDetails.isEnabled = true
                        binding.syncProgressBar.visibility = View.GONE
                        Toast.makeText(context, "Scheme detail sync failed.", Toast.LENGTH_LONG).show()
                    }
                    WorkInfo.State.CANCELLED -> {
                        binding.textViewSyncStatus.text = "Sync: Cancelled."
                        binding.buttonSyncAllDetails.isEnabled = true
                        binding.syncProgressBar.visibility = View.GONE
                    }
                    WorkInfo.State.BLOCKED -> {
                        binding.textViewSyncStatus.text = "Sync: Blocked (waiting for conditions)..."
                        binding.buttonSyncAllDetails.isEnabled = false // Or reflect that it's waiting
                        binding.syncProgressBar.visibility = View.VISIBLE
                        binding.syncProgressBar.isIndeterminate = true
                    }
                }
            } else {
                // No active sync worker with that name, or it's completed and pruned.
                // UI could reflect "Ready to sync" or similar.
                binding.textViewSyncStatus.text = "Sync: Idle" // Or empty
                binding.buttonSyncAllDetails.isEnabled = true
                binding.syncProgressBar.visibility = View.GONE
            }
        }

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

    private fun showConfirmationDialogForFullSync() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Start Full Scheme Detail Sync?")
            .setMessage("This will download details for all schemes and may take some time and data. Ensure you are on a stable network connection. Continue?")
            .setNegativeButton("Cancel") { dialog, _ ->
                Log.d(tag, "Full sync cancelled by user.")
                dialog.dismiss()
                // Ensure swipe refresh indicator is hidden if it was active and dialog was triggered from there
                if (binding.swipeRefreshLayout.isRefreshing) {
                    binding.swipeRefreshLayout.isRefreshing = false
                }
            }
            .setPositiveButton("Start Sync") { dialog, _ ->
                Log.d(tag, "User confirmed full sync.")
                viewModel.onTestSimpleWorkerClicked()
                dialog.dismiss()
                // Ensure swipe refresh indicator is hidden if it was active and dialog was triggered from there
                if (binding.swipeRefreshLayout.isRefreshing) {
                    binding.swipeRefreshLayout.isRefreshing = false
                }
            }
            .setOnDismissListener {
                // This is important if the dialog was triggered by SwipeRefreshLayout
                // to stop the refreshing animation if the user dismisses the dialog
                // without making a choice (e.g. back button).
                if (binding.swipeRefreshLayout.isRefreshing) {
                    binding.swipeRefreshLayout.isRefreshing = false
                }
            }
            .show()
    }

}