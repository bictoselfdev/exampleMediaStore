package com.example.examplemediastore

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.examplemediastore.databinding.ItemMediaStoreFileBinding
import com.example.examplemediastore.mediastore.MediaFile
import com.example.examplemediastore.mediastore.MediaStoreManager
import java.text.SimpleDateFormat
import java.util.*

class MediaStoreFilesAdapter(private var context: Context, private var itemList: ArrayList<MediaFile>) :
    RecyclerView.Adapter<MediaStoreFilesAdapter.RecyclerViewHolder?>() {
    private lateinit var binding: ItemMediaStoreFileBinding

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder {
        binding = ItemMediaStoreFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecyclerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
        holder.updateViewHolder(itemList[position])
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    inner class RecyclerViewHolder(private val binding: ItemMediaStoreFileBinding) : RecyclerView.ViewHolder(binding.root) {
        fun updateViewHolder(mediaFile: MediaFile) {

            binding.tvName.text = mediaFile.name
            binding.tvPath.text = mediaFile.fullPath
            binding.tvDate.text = String.format("%s", SimpleDateFormat("yyyy-MM-dd\nHH:mm").format(Date(mediaFile.date)))
            binding.tvSize.text = getFileSize(mediaFile.size)
            binding.tvUri.text = mediaFile.contentUri.toString()

            binding.tvUri.setOnClickListener {
                MediaStoreManager.openFile(context, mediaFile)
            }

            binding.btnStream.setOnClickListener {
                MediaStoreManager.openInputStream(context, mediaFile)?.use { inputStream ->
                    val buffer = ByteArray(1024)
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        //TODO : 미구현 (not implemented), 아래 inputStream(FileData) 활용

                    }
                    inputStream.close()

                    Toast.makeText(context, "not implemented", Toast.LENGTH_SHORT).show()
                }
            }

            binding.btnDelete.setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (MediaStoreManager.deleteFile(context, mediaFile) > 0) { // Delete 가 정상적으로 된 경우
                        val index = itemList.indexOf(mediaFile)
                        itemList.remove(mediaFile)
                        notifyItemRemoved(index)
                    }
                }
            }
        }

        private fun getFileSize(size: Int): String {

            return when {
                size > 1000L * 1000L -> String.format("%.0fMB", size.toFloat() / (1000L * 1000L))
                size > 1000L -> String.format("%.0fKB", size.toFloat() / 1000L)
                else -> String.format("%.0fByte", size.toFloat())
            }
        }
    }
}