package com.launcher.tcl

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.io.FileOutputStream

class WallpaperPickerActivity : AppCompatActivity() {

    private lateinit var previewImage: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnSelect: Button
    private lateinit var btnFromUsb: Button

    private var selectedPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallpaper_picker)

        previewImage = findViewById(R.id.previewImage)
        recyclerView = findViewById(R.id.wallpaperGrid)
        btnSelect = findViewById(R.id.btnSelect)
        btnFromUsb = findViewById(R.id.btnFromUsb)

        // Mostrar imágenes internas (assets/wallpapers)
        loadBuiltinWallpapers()

        btnSelect.setOnClickListener {
            if (selectedPath != null) {
                val result = Intent()
                result.putExtra("wallpaper_path", selectedPath)
                setResult(Activity.RESULT_OK, result)
                finish()
            } else {
                Toast.makeText(this, "Selecciona un fondo primero", Toast.LENGTH_SHORT).show()
            }
        }

        btnFromUsb.setOnClickListener {
            openFilePicker()
        }

        btnSelect.isFocusable = true
        btnFromUsb.isFocusable = true
    }

    private fun loadBuiltinWallpapers() {
        // Lista de fondos internos en assets
        val builtinWallpapers = try {
            assets.list("wallpapers")?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        val wallpaperItems = builtinWallpapers.map { filename ->
            WallpaperItem(
                name = filename.removeSuffix(".jpg").removeSuffix(".png"),
                isBuiltin = true,
                path = "wallpapers/$filename"
            )
        }

        // También buscar en almacenamiento externo/USB
        val externalItems = findExternalWallpapers()

        val allItems = wallpaperItems + externalItems

        val adapter = WallpaperAdapter(this, allItems) { item ->
            selectedPath = if (item.isBuiltin) {
                // Copiar el asset a almacenamiento interno
                copyAssetToInternal(item.path)
            } else {
                item.path
            }
            // Mostrar preview
            if (selectedPath != null) {
                val bmp = BitmapFactory.decodeFile(selectedPath)
                previewImage.setImageBitmap(bmp)
            }
        }

        recyclerView.layoutManager = GridLayoutManager(this, 3)
        recyclerView.adapter = adapter
        recyclerView.requestFocus()
    }

    private fun findExternalWallpapers(): List<WallpaperItem> {
        val items = mutableListOf<WallpaperItem>()
        val externalDirs = getExternalFilesDirs(null)
        for (dir in externalDirs) {
            dir?.parentFile?.parentFile?.let { root ->
                val pictures = File(root, "Pictures")
                val dcim = File(root, "DCIM")
                for (folder in listOf(pictures, dcim, root)) {
                    if (folder.exists()) {
                        folder.listFiles()?.filter {
                            it.extension.lowercase() in listOf("jpg", "jpeg", "png")
                        }?.forEach {
                            items.add(WallpaperItem(it.name, false, it.absolutePath))
                        }
                    }
                }
            }
        }
        return items
    }

    private fun copyAssetToInternal(assetPath: String): String {
        val filename = File(assetPath).name
        val outFile = File(filesDir, filename)
        if (!outFile.exists()) {
            assets.open(assetPath).use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
        return outFile.absolutePath
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
        }
        startActivityForResult(intent, REQUEST_PICK_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            val uri = data?.data ?: return
            val path = saveImageFromUri(uri)
            if (path != null) {
                selectedPath = path
                val bmp = BitmapFactory.decodeFile(path)
                previewImage.setImageBitmap(bmp)
                Toast.makeText(this, "Imagen cargada. Presiona Seleccionar.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveImageFromUri(uri: Uri): String? {
        return try {
            val outFile = File(filesDir, "custom_wallpaper_${System.currentTimeMillis()}.jpg")
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }
            outFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        const val REQUEST_PICK_IMAGE = 200
    }
}

data class WallpaperItem(
    val name: String,
    val isBuiltin: Boolean,
    val path: String
)
