package com.dhansanchay.ui.scheme.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dhansanchay.databinding.ListRowSchemeBinding
import com.dhansanchay.domain.model.SchemeModel

// Type alias for better readability of the click listener
typealias OnSchemeClickListener = (schemeModel: SchemeModel) -> Unit

class SchemeListAdapter(
    private val onSchemeClicked: OnSchemeClickListener // Pass callback in constructor
) : ListAdapter<SchemeModel, SchemeListAdapter.SchemeViewHolder>(DiffCallback) { // Specify ViewHolder type directly

    // DiffCallback remains a static object, good for performance
    object DiffCallback : DiffUtil.ItemCallback<SchemeModel>() {
        override fun areItemsTheSame(oldItem: SchemeModel, newItem: SchemeModel): Boolean {
            return oldItem.schemeCode == newItem.schemeCode
        }

        override fun areContentsTheSame(oldItem: SchemeModel, newItem: SchemeModel): Boolean {
            // Ensure SchemeModel has a proper equals() implementation if it's a data class or if you need deep comparison.
            // If SchemeModel is a data class, the default equals() will compare all properties.
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SchemeViewHolder {
        val binding = ListRowSchemeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SchemeViewHolder(binding, onSchemeClicked) // Pass click listener to ViewHolder
    }

    override fun onBindViewHolder(holder: SchemeViewHolder, position: Int) { // Directly use SchemeViewHolder
        val schemeModel = getItem(position)
        holder.bind(schemeModel)
    }

    // ViewHolder is now a primary constructor inner class, can be static if it doesn't need adapter instance access
    // Made it static here as it only needs the click listener passed to it.
    class SchemeViewHolder(
        private val binding: ListRowSchemeBinding,
        private val onSchemeClicked: OnSchemeClickListener // Receive listener
    ) : RecyclerView.ViewHolder(binding.root) {

        // It's good practice to keep a reference to the current item if needed for multiple actions
        // or if the click listener needed the item from the ViewHolder directly (though passing from bind is often cleaner)
        // private var currentSchemeModel: SchemeModel? = null

        init {
            // Set the click listener on the root view once during ViewHolder creation
            // This is generally more efficient than setting it in bind() if the listener logic doesn't change per item
            // However, to pass the specific 'schemeModel' on click, we need it in scope.
            // Option 1 (as you had, setting in bind): Simple and clear.
            // Option 2 (setting in init, needs item): See below.
        }

        fun bind(schemeModel: SchemeModel) {
            // this.currentSchemeModel = schemeModel // If needed for Option 2 click listener

            binding.tvSchemeCode.text = schemeModel.schemeCode.toString()
            // Using template string for slightly better efficiency and readability
            binding.tvSchemeName.text = "${adapterPosition + 1}. ${schemeModel.schemeName}"

            // Set the click listener for the specific item
            binding.root.setOnClickListener {
                // Check for NO_POSITION to be safe, though getItem(position) in onBindViewHolder usually guards this
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onSchemeClicked(schemeModel)
                }
            }
        }
    }
}