package com.navajasuiza.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.navajasuiza.data.model.CopiedTextEntity
import com.navajasuiza.features.history.databinding.ItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(
    private val onItemClick: (CopiedTextEntity) -> Unit,
    private val onCopyClick: (CopiedTextEntity) -> Unit,
    private val onDeleteClick: (CopiedTextEntity) -> Unit
) : ListAdapter<CopiedTextEntity, HistoryAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CopiedTextEntity) {
            binding.tvTextPreview.text = item.text
            
            // Format timestamp
            val date = Date(item.timestamp)
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())
            val sourceText = if (item.source == "ocr") "📸 OCR" else "📋 Clipboard"
            binding.tvMetadata.text = "${format.format(date)} • $sourceText"

            binding.root.setOnClickListener { onItemClick(item) }
            binding.btnCopy.setOnClickListener { onCopyClick(item) }
            binding.btnDelete.setOnClickListener { onDeleteClick(item) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<CopiedTextEntity>() {
        override fun areItemsTheSame(oldItem: CopiedTextEntity, newItem: CopiedTextEntity) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: CopiedTextEntity, newItem: CopiedTextEntity) = oldItem == newItem
    }
}
