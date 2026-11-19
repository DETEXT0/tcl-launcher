package com.launcher.tcl

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppManagerAdapter(
    private val apps: List<AppInfo>,
    private val hiddenPackages: MutableSet<String>,
    private val onVisibilityChanged: (packageName: String, isVisible: Boolean) -> Unit
) : RecyclerView.Adapter<AppManagerAdapter.ManageViewHolder>() {

    inner class ManageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.manageAppIcon)
        val name: TextView = view.findViewById(R.id.manageAppName)
        val status: TextView = view.findViewById(R.id.manageAppStatus)
        val checkbox: CheckBox = view.findViewById(R.id.manageAppCheckbox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ManageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_manage_app, parent, false)
        return ManageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ManageViewHolder, position: Int) {
        val app = apps[position]
        val isHidden = app.packageName in hiddenPackages
        val isVisible = !isHidden

        holder.icon.setImageDrawable(app.icon)
        holder.name.text = app.name
        holder.checkbox.isChecked = isVisible

        if (isVisible) {
            holder.status.text = "Visible en inicio"
            holder.status.setTextColor(0xFF4CAF50.toInt())
            holder.itemView.alpha = 1.0f
        } else {
            holder.status.text = "Oculta"
            holder.status.setTextColor(0x88FFFFFF.toInt())
            holder.itemView.alpha = 0.55f
        }

        // Toggle al hacer click con OK en el control remoto
        holder.itemView.setOnClickListener {
            val newVisible = !holder.checkbox.isChecked
            if (newVisible) {
                hiddenPackages.remove(app.packageName)
            } else {
                hiddenPackages.add(app.packageName)
            }
            notifyItemChanged(holder.adapterPosition)
            onVisibilityChanged(app.packageName, newVisible)
        }

        // Efecto de foco para navegación con control remoto
        holder.itemView.setOnFocusChangeListener { v, hasFocus ->
            v.animate()
                .scaleX(if (hasFocus) 1.03f else 1.0f)
                .scaleY(if (hasFocus) 1.03f else 1.0f)
                .setDuration(120)
                .start()
        }

        holder.itemView.isFocusable = true
        holder.itemView.isFocusableInTouchMode = true
    }

    override fun getItemCount(): Int = apps.size
}
