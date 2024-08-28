package com.dhansanchay.ui.scheme

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import com.dhansanchay.databinding.FragmentSchemeListBinding
import com.dhansanchay.ui.scheme.adapter.PagedSchemeListAdapter
import com.dhansanchay.ui.scheme.adapter.PaginationScrollListener
import com.dhansanchay.ui.scheme.adapter.SchemeListAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FragmentSchemeList : Fragment() {

    private val tag = this.javaClass.simpleName

    private var _binding : FragmentSchemeListBinding? = null
    private val binding get() = _binding!!

    private val viewModel : ViewModelSchemeList by viewModels()

    private val adapterScheme : PagedSchemeListAdapter = PagedSchemeListAdapter()

    val PAGE_START: Int = 1
    private val isLoading = false
    private val isLastPage = false

    private var isLoadingEmails = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.i(tag , "In $tag onCreateView")
        _binding = FragmentSchemeListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(tag , "In $tag onViewCreated")

        initView()
        initObserver()
        initListener()
        fetchSchemeList()
    }

    private fun fetchSchemeList() {
        Log.i(tag , "In $tag fetchSchemeList")
       // binding.postsProgressBar.visibility = View.VISIBLE
//        viewModel.getSchemeList()

        viewModel.getPagedSchemeItems()
    }

    private fun initView() {
        with(binding) {
            rvSchemeList.apply {
                setHasFixedSize(true)
                adapter = adapterScheme
            }
        }
    }

    private fun initObserver() {
//        viewModel.schemeResponse.observe(viewLifecycleOwner) {
//            binding.postsProgressBar.visibility = View.GONE
//
//           // adapterScheme.submitData(it.flow)
//        }

        viewModel.topRatedMovieList.asLiveData().observe(viewLifecycleOwner) {
            adapterScheme.submitData(lifecycle, it)
        }
    }

    private fun initListener() {
        adapterScheme.setOnClickListener {

        }
        adapterScheme.setOnLongClickListener {

        }
        addScrollListener()
    }

    private fun addScrollListener() {
        with(binding) {
            rvSchemeList.addOnScrollListener(object :
                PaginationScrollListener(rvSchemeList.layoutManager as LinearLayoutManager) {
                override fun loadMoreItems() {
                    viewModel.getPaginatedSchemeList()
                }

                override fun isLastPage() = false//viewModel.isLoadingEmails

                override fun isLoading() = isLoadingEmails
            })
        }

    }


}