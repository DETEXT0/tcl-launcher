package com.launcher.tcl

import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class WallpaperAdapter(
    private val context: Context,
    private val items: List<WallpaperItem>,
    private val onSelect: (WallpaperItem) -> Unit
) : RecyclerView.Adapter<WallpaperAdapter.WallpaperViewHolder>() {

    private var selectedPosition = -1

    inner class WallpaperViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val thumbnail: ImageView = view.findViewById(R.id.wallpaperThumbnail)
        val name: TextView = view.findViewById(R.id.wallpaperName)
        val selectedBorder: View = view.findViewById(R.id.selectedBorder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WallpaperViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_wallpaper, parent, false)
        return WallpaperViewHolder(view)
    }

    override fun onBindViewHolder(holder: WallpaperViewHolder, position: Int) {
        val item = items[position]
        holder.name.text = item.name

        // Cargar thumbnail
        try {
            if (item.isBuiltin) {
                val bitmap = context.assets.open(item.path).use {
                    BitmapFactory.decodeStream(it)
                }
                holder.thumbnail.setImageBitmap(bitmap)
            } else {
                val bitmap = BitmapFactory.decodeFile(item.path)
                holder.thumbnail.setImageBitmap(bitmap)
            }
        } catch (e: Exception) {
            holder.thumbnail.setImageResource(R.drawable.wallpaper_default)
        }

        holder.selectedBorder.visibility =
            if (position == selectedPosition) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener {
            val prev = selectedPosition
            selectedPosition = holder.adapterPosition
            notifyItemChanged(prev)
            notifyItemChanged(selectedPosition)
            onSelect(item)
        }

        holder.itemView.setOnFocusChangeListener { v, hasFocus ->
            v.animate()
                .scaleX(if (hasFocus) 1.08f else 1.0f)
                .scaleY(if (hasFocus) 1.08f else 1.0f)
                .setDuration(120)
                .start()
        }

        holder.itemView.isFocusable = true
        holder.itemView.isFocusableInTouchMode = true
    }

    override fun getItemCount() = items.size
}
