package com.unoshield.mdm.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.unoshield.mdm.R

/**
 * Adapter for displaying installed apps in a RecyclerView
 */
class AppAdapter(
    private val onAppClick: (AppInfo) -> Unit
) : ListAdapter<AppInfo, AppAdapter.AppViewHolder>(AppDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return AppViewHolder(view, onAppClick)
    }
    
    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class AppViewHolder(
        itemView: View,
        private val onAppClick: (AppInfo) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val appIcon: ImageView = itemView.findViewById(R.id.app_icon)
        private val appName: TextView = itemView.findViewById(R.id.app_name)
        
        fun bind(appInfo: AppInfo) {
            // Set icon with proper scaling - show actual app icon without cropping
            appIcon.setImageDrawable(appInfo.icon)
            appIcon.scaleType = ImageView.ScaleType.FIT_CENTER
            appIcon.adjustViewBounds = false
            appIcon.cropToPadding = false
            
            appName.text = appInfo.name
            
            itemView.setOnClickListener {
                onAppClick(appInfo)
            }
        }
    }
    
    private class AppDiffCallback : DiffUtil.ItemCallback<AppInfo>() {
        override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem.packageName == newItem.packageName
        }
        
        override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem == newItem
        }
    }
}

