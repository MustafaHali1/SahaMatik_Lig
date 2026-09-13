package com.example.sahamatik_lig.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class ImagePickerHelper(
    private val activity: AppCompatActivity,
    private val onImageSelected: (Uri) -> Unit
) {
    private val launcher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                try {
                    activity.contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) { }

                onImageSelected(it)
            }
        }

    fun pickImage() {
        launcher.launch("image/*")
    }

    companion object {
        private const val PREF_NAME = "SahamatikGorseller"

        fun uriKaydet(context: Context, anahtar: String, uriString: String) {
            val sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            sharedPref.edit().putString(anahtar, uriString).apply()
        }

        fun uriGetir(context: Context, anahtar: String): Uri? {
            val sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val uriString = sharedPref.getString(anahtar, null)
            return if (!uriString.isNullOrEmpty()) Uri.parse(uriString) else null
        }
    }
}