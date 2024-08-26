package com.dhansanchay.ui.scheme.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.dhansanchay.databinding.ListRowSchemeBinding
import com.dhansanchay.domain.model.response.SchemeModel

class SchemeListAdapter() : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val tag = this.javaClass.simpleName
    var onSchemeSelected: ((scheme: SchemeModel) -> Unit)? = null
    var isCheckBoxEnabled = false

    private val selectedSchemeList = mutableListOf<SchemeModel>()

    private val diffUtil = object : DiffUtil.ItemCallback<SchemeModel>() {

        override fun areItemsTheSame(oldItem: SchemeModel, newItem: SchemeModel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SchemeModel, newItem: SchemeModel): Boolean {
            return oldItem == newItem
        }

    }

    private val asyncListDiffer = AsyncListDiffer(this, diffUtil)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding =
            ListRowSchemeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SchemeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is SchemeViewHolder) {
            holder.bind(asyncListDiffer.currentList[position])
        }
    }

    override fun getItemCount(): Int {
        return asyncListDiffer.currentList.size
    }

    fun submitList(list: List<SchemeModel>) {
        val combinedList = asyncListDiffer.currentList + list

        asyncListDiffer.submitList(combinedList)
    }

    fun getSelectedSchemeList(): List<SchemeModel> {
        return selectedSchemeList
    }

    fun clearSelectedSchemeList() {
        selectedSchemeList.clear()
    }

    fun onClickSchemeItem(selectedScheme: SchemeModel, position: Int) {
        if (selectedSchemeList.contains(selectedScheme)) {
            selectedSchemeList.remove(selectedScheme)
        } else {
            selectedSchemeList.add(selectedScheme)
        }
        if (selectedSchemeList.isEmpty()) {
            isCheckBoxEnabled = false
            removeSelection()
        } else {
            isCheckBoxEnabled = true
            notifyItemRangeChanged(0, asyncListDiffer.currentList.size)
        }
    }

    fun removeSelection() {
        selectedSchemeList.clear()
        isCheckBoxEnabled = false
        notifyItemRangeChanged(0, asyncListDiffer.currentList.size)
    }

    fun setOnClickListener(onSchemeSelected: (SchemeModel) -> Unit) {
        this.onSchemeSelected = onSchemeSelected
    }

    fun setOnLongClickListener(onSchemeSelected: (SchemeModel) -> Unit) {
        this.onSchemeSelected = onSchemeSelected
    }

    inner class SchemeViewHolder(private val binding: ListRowSchemeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(schemeModel: SchemeModel) {

            with(binding) {

                tvSchemeCode.text = schemeModel.schemeCode
                tvSchemeName.text = (adapterPosition+1).toString() + ". " + schemeModel.schemeName
                cbScheme.visibility = if (isCheckBoxEnabled) View.VISIBLE else View.GONE

                root.setOnClickListener {
                    if (isCheckBoxEnabled) {
                        onClickSchemeItem(selectedScheme = schemeModel, position = adapterPosition)
                    } else {
                        onSchemeSelected?.invoke(schemeModel)
                    }
                }

                root.setOnLongClickListener {
                    if (!isCheckBoxEnabled) {
                        onClickSchemeItem(selectedScheme = schemeModel, position = adapterPosition)
                    }
                    true
                }

                cbScheme.setOnClickListener {
                    onClickSchemeItem(selectedScheme = schemeModel, position = adapterPosition)
                }

            }

        }
    }
}