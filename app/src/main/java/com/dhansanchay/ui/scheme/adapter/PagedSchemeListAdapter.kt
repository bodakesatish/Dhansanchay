package com.dhansanchay.ui.scheme.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dhansanchay.databinding.ListRowSchemeBinding
import com.dhansanchay.domain.model.SchemeModel

class PagedSchemeListAdapter() : ListAdapter<SchemeModel, RecyclerView.ViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<SchemeModel>() {

        override fun areItemsTheSame(oldItem: SchemeModel, newItem: SchemeModel): Boolean {
            return oldItem.schemeCode == newItem.schemeCode
        }

        override fun areContentsTheSame(oldItem: SchemeModel, newItem: SchemeModel): Boolean {
            return oldItem == newItem
        }

    }

//    private val asyncListDiffer = AsyncListDiffer(this, diffUtil)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SchemeViewHolder {
        val binding =
            ListRowSchemeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SchemeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val serviceItem = getItem(position)
        when (holder) {
            is SchemeViewHolder -> {
                holder.bind(serviceItem)
            }
        }
    }

    class SchemeViewHolder(private val binding: ListRowSchemeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(schemeModel: SchemeModel) {

            with(binding) {

                tvSchemeCode.text = schemeModel.schemeCode.toString()
                tvSchemeName.text = (adapterPosition + 1).toString() + ". " + schemeModel.schemeName

            }

        }
    }

}