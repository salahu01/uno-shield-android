package com.unoshield.mdm.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.unoshield.mdm.R

/**
 * Server Activity - Mock server interface for managing MDM policies
 * This will be replaced with actual server integration in the future
 * Shows a list of available MDM features/policies
 */
class ServerActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var featuresAdapter: FeaturesAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server)
        
        setupToolbar()
        setupRecyclerView()
        loadFeatures()
    }
    
    private fun setupToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.server_manager)
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.features_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        featuresAdapter = FeaturesAdapter { feature ->
            handleFeatureClick(feature)
        }
        recyclerView.adapter = featuresAdapter
    }
    
    private fun loadFeatures() {
        val features = listOf(
            MDMFeature(
                id = "block_apps",
                title = getString(R.string.block_apps),
                description = getString(R.string.block_apps_description),
                iconRes = android.R.drawable.ic_menu_manage
            )
            // More features will be added here in the future
        )
        featuresAdapter.submitList(features)
    }
    
    private fun handleFeatureClick(feature: MDMFeature) {
        when (feature.id) {
            "block_apps" -> {
                val intent = Intent(this, BlockAppsActivity::class.java)
                startActivity(intent)
            }
            // Handle other features here
        }
    }
    
    /**
     * Data class representing an MDM feature/policy
     */
    data class MDMFeature(
        val id: String,
        val title: String,
        val description: String,
        val iconRes: Int
    )
    
    /**
     * Adapter for displaying MDM features
     */
    private class FeaturesAdapter(
        private val onFeatureClick: (MDMFeature) -> Unit
    ) : RecyclerView.Adapter<FeaturesAdapter.FeatureViewHolder>() {
        
        private var features = emptyList<MDMFeature>()
        
        fun submitList(newFeatures: List<MDMFeature>) {
            features = newFeatures
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeatureViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_feature, parent, false)
            return FeatureViewHolder(view, onFeatureClick)
        }
        
        override fun onBindViewHolder(holder: FeatureViewHolder, position: Int) {
            holder.bind(features[position])
        }
        
        override fun getItemCount(): Int = features.size
        
        class FeatureViewHolder(
            itemView: View,
            private val onFeatureClick: (MDMFeature) -> Unit
        ) : RecyclerView.ViewHolder(itemView) {
            
            private val titleText: TextView = itemView.findViewById(R.id.feature_title)
            private val descriptionText: TextView = itemView.findViewById(R.id.feature_description)
            
            fun bind(feature: MDMFeature) {
                titleText.text = feature.title
                descriptionText.text = feature.description
                
                itemView.setOnClickListener {
                    onFeatureClick(feature)
                }
            }
        }
    }
}

