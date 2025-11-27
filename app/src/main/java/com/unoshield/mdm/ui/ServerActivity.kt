package com.unoshield.mdm.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
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
                id = "call_filter_policy",
                title = getString(R.string.call_filter_policy),
                description = getString(R.string.call_filter_policy_description),
                iconRes = android.R.drawable.ic_menu_call
            ),
            MDMFeature(
                id = "application_control",
                title = getString(R.string.application_control),
                description = getString(R.string.application_control_description),
                iconRes = android.R.drawable.ic_menu_manage
            ),
            MDMFeature(
                id = "restriction_policy",
                title = getString(R.string.restriction_policy),
                description = getString(R.string.restriction_policy_description),
                iconRes = android.R.drawable.ic_lock_lock
            )
            // More features will be added here in the future
        )
        featuresAdapter.submitList(features)
    }
    
    private fun handleFeatureClick(feature: MDMFeature) {
        when (feature.id) {
            "call_filter_policy" -> {
                val intent = Intent(this, CallFilterPolicyActivity::class.java)
                startActivity(intent)
            }
            "application_control" -> {
                val intent = Intent(this, ApplicationControlActivity::class.java)
                startActivity(intent)
            }
            "restriction_policy" -> {
                val intent = Intent(this, RestrictionPolicyActivity::class.java)
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

