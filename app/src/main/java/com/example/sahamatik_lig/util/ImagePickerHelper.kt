package com.example.sahamatik_lig.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

class ImagePickerHelper(
    private val activity: AppCompatActivity,
    private var onImageSelected: (Uri) -> Unit = {}
) {
    private var onlyFace: Boolean = false

    private val launcher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { selectedUri ->
                // Kalıcı iç depolamaya kopyala (SecurityException ve URI erişim kaybını kökten çözer)
                val kaliciUri = kaliciDosyayaKopyala(selectedUri)

                if (onlyFace) {
                    yuzDogrulaVeSec(kaliciUri)
                } else {
                    onImageSelected(kaliciUri)
                }
            }
        }

    fun setOnImageSelectedListener(listener: (Uri) -> Unit) {
        this.onImageSelected = listener
    }

    /**
     * Takım logoları vb. için standart görsel seçici (Yüz şartı aranmaz)
     */
    fun pickImage(onSelected: ((Uri) -> Unit)? = null) {
        if (onSelected != null) {
            this.onImageSelected = onSelected
        }
        onlyFace = false
        launcher.launch("image/*")
    }

    /**
     * Oyuncu profil fotoğrafları için özel yüz doğrulayıcı seçici (Google ML Kit ile taranır)
     */
    fun pickFaceImage(onSelected: ((Uri) -> Unit)? = null) {
        if (onSelected != null) {
            this.onImageSelected = onSelected
        }
        onlyFace = true
        launcher.launch("image/*")
    }

    private fun kaliciDosyayaKopyala(sourceUri: Uri): Uri {
        return try {
            val klasor = java.io.File(activity.filesDir, "profil_fotolari")
            if (!klasor.exists()) {
                klasor.mkdirs()
            }
            val dosyaAdi = "foto_${System.currentTimeMillis()}_${(1000..9999).random()}.jpg"
            val hedefDosya = java.io.File(klasor, dosyaAdi)

            activity.contentResolver.openInputStream(sourceUri)?.use { input ->
                java.io.FileOutputStream(hedefDosya).use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(hedefDosya)
        } catch (e: Exception) {
            sourceUri
        }
    }

    private fun yuzDogrulaVeSec(uri: Uri) {
        val progressDialog = AlertDialog.Builder(activity)
            .setTitle("Yüz Taranıyor")
            .setMessage("Profil fotoğrafında insan yüzü doğrulanıyor, lütfen bekleyin...")
            .setCancelable(false)
            .create()
        progressDialog.show()

        try {
            val image = try {
                InputImage.fromFilePath(activity, uri)
            } catch (e: Exception) {
                if (uri.scheme == "file" && uri.path != null) {
                    val bitmap = android.graphics.BitmapFactory.decodeFile(uri.path)
                    InputImage.fromBitmap(bitmap, 0)
                } else {
                    throw e
                }
            }

            val options = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .build()

            val detector = FaceDetection.getClient(options)

            detector.process(image)
                .addOnSuccessListener { faces ->
                    progressDialog.dismiss()
                    if (faces.isNotEmpty()) {
                        Toast.makeText(activity, "✅ Yüz fotoğrafı başarıyla doğrulandı!", Toast.LENGTH_SHORT).show()
                        onImageSelected(uri)
                    } else {
                        AlertDialog.Builder(activity)
                            .setTitle("⚠️ Yüz Tespit Edilemedi")
                            .setMessage("Lütfen yalnızca yüzünüzün net bir şekilde göründüğü bir profil fotoğrafı seçin!\n\nManzara, nesne, logo veya yüz görünmeyen fotoğraflar profil için kabul edilmez.")
                            .setPositiveButton("Farklı Fotoğraf Seç") { _, _ ->
                                pickFaceImage()
                            }
                            .setNegativeButton("İptal", null)
                            .show()
                    }
                }
                .addOnFailureListener { e ->
                    progressDialog.dismiss()
                    Toast.makeText(activity, "Fotoğraf taranırken hata: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            progressDialog.dismiss()
            Toast.makeText(activity, "Görsel yüklenemedi: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
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

object ProfilFotoHelper {
    /**
     * Seçilen görseli 160x160 piksel boyutunda, kaliteli JPEG formatında Base64 metnine dönüştürür (~10-15 KB).
     * Bu sayede görsel Firebase Storage gerektirmeden doğrudan Firestore'a kaydedilir
     * ve maçı izleyen veya detay sayfasına bakan TÜM cihazlarda anında görünür!
     */
    fun uriToBase64(context: Context, uri: Uri, maxSize: Int = 160): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val original = android.graphics.BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            if (original == null) return null

            val ratio = Math.min(maxSize.toFloat() / original.width, maxSize.toFloat() / original.height)
            val width = Math.max(1, Math.round(ratio * original.width))
            val height = Math.max(1, Math.round(ratio * original.height))
            val scaled = android.graphics.Bitmap.createScaledBitmap(original, width, height, true)

            val outputStream = java.io.ByteArrayOutputStream()
            scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
            val bytes = outputStream.toByteArray()
            "data:image/jpeg;base64," + android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * İster Base64 metni olsun ister yerel URI, güvenle Bitmap olarak çözer.
     * Buluttan gelen diğer oyuncuların (örn: Mahmud) fotoğraflarını anında ekrana basar.
     */
    fun gorselYukle(context: Context, uriOrBase64: String): android.graphics.Bitmap? {
        if (uriOrBase64.isBlank()) return null
        return try {
            if (uriOrBase64.startsWith("data:image") || uriOrBase64.length > 500) {
                val clean = if (uriOrBase64.contains(",")) uriOrBase64.substringAfter(",") else uriOrBase64
                val bytes = android.util.Base64.decode(clean, android.util.Base64.DEFAULT)
                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } else {
                val uri = Uri.parse(uriOrBase64)
                if (uri.scheme == "file" && uri.path != null) {
                    android.graphics.BitmapFactory.decodeFile(uri.path)
                } else {
                    context.contentResolver.openInputStream(uri)?.use {
                        android.graphics.BitmapFactory.decodeStream(it)
                    }
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}