package com.example.examplemediastore

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import com.example.examplemediastore.databinding.ActivityMainBinding
import com.example.examplemediastore.mediastore.MediaStoreManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var collection = MediaStoreManager.COLLECTION_IMAGE // default image

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        permissionCheck()

        initUI()
        setListener()
    }

    private fun initUI() {

        val decoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        binding.rvFileList.itemAnimator = DefaultItemAnimator()
        binding.rvFileList.addItemDecoration(decoration)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            binding.tvSdk.text = "test app supports only SDK >= 30 (current device SDK ${Build.VERSION.SDK_INT})"
            binding.tvSdk.setTextColor(Color.GREEN)
        } else {
            binding.tvSdk.text = "test app supports only SDK >= 30 (current device SDK ${Build.VERSION.SDK_INT})"
            binding.tvSdk.setTextColor(Color.RED)
        }
    }

    private fun setListener() {

        binding.rgCollection.setOnCheckedChangeListener { _, checkedId ->

            collection = when (checkedId) {
                binding.rbImage.id -> MediaStoreManager.COLLECTION_IMAGE
                binding.rbAudio.id -> MediaStoreManager.COLLECTION_AUDIO
                binding.rbVideo.id -> MediaStoreManager.COLLECTION_VIDEO
                binding.rbDownload.id -> MediaStoreManager.COLLECTION_DOWNLOAD
                else -> MediaStoreManager.COLLECTION_FILE
            }
        }

        binding.btnFileSave.setOnClickListener {

            val sampleFileName = when (collection) {
                MediaStoreManager.COLLECTION_IMAGE -> "sampleImage.jpg"
                MediaStoreManager.COLLECTION_AUDIO -> "sampleAudio.mp3"
                MediaStoreManager.COLLECTION_VIDEO -> "sampleVideo.mp4"
                MediaStoreManager.COLLECTION_DOWNLOAD -> "sampleFile.txt"
                else -> "sampleFile.txt"
            }

            resources.assets.open(sampleFileName).use { inputStream ->

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStoreManager.saveMediaFile(this, sampleFileName, collection, inputStream)
                }
            }
        }

        binding.btnFileList.setOnClickListener {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val mediaStoreFiles = MediaStoreManager.queryMediaFiles(this, collection, null)
                binding.rvFileList.adapter = MediaStoreFilesAdapter(this, mediaStoreFiles)
            }
        }
    }

    private fun permissionCheck(): Boolean {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ), 100
                )
                return false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {

            when (requestCode) {
                REQUEST_PERMISSION_READ_WRITE -> {

                }
                REQUEST_PERMISSION_DELETE -> {

                }
            }
        }
    }

    companion object {
        const val REQUEST_PERMISSION_READ_WRITE = 100
        const val REQUEST_PERMISSION_DELETE = 200
    }
}