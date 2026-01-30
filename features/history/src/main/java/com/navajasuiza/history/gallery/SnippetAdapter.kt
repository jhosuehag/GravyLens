package com.navajasuiza.history.gallery

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.navajasuiza.data.repository.SnippetRepository
import com.navajasuiza.features.history.R
import kotlinx.coroutines.*
import java.io.File

class SnippetAdapter(
    private val repository: SnippetRepository,
    private val scope: CoroutineScope,
    private val onItemClick: (File) -> Unit,
    private val onItemLongClick: (File) -> Unit
) : ListAdapter<File, SnippetAdapter.ViewHolder>(SnippetDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_snippet, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.img_thumbnail)
        private var job: Job? = null

        fun bind(file: File) {
            imageView.setImageBitmap(null) // Clear previous
            job?.cancel()

            // Load thumbnail async
            job = scope.launch(Dispatchers.IO) {
                // Request ~150px thumbnail
                val bitmap = repository.getThumbnail(file, 200, 200)
                withContext(Dispatchers.Main) {
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap)
                    } else {
                        // Set error/placeholder
                        imageView.setImageResource(android.R.drawable.ic_menu_report_image)
                    }
                }
            }

            itemView.setOnClickListener { onItemClick(file) }
            itemView.setOnLongClickListener { 
                onItemLongClick(file)
                true 
            }
        }
    }

    class SnippetDiffCallback : DiffUtil.ItemCallback<File>() {
        override fun areItemsTheSame(oldItem: File, newItem: File): Boolean {
            return oldItem.absolutePath == newItem.absolutePath
        }

        override fun areContentsTheSame(oldItem: File, newItem: File): Boolean {
            return oldItem.lastModified() == newItem.lastModified() // Check modification time
        }
    }
}
