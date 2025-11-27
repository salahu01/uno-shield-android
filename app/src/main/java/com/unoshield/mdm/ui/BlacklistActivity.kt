package com.unoshield.mdm.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.unoshield.mdm.R
import com.unoshield.mdm.data.BlacklistNumber
import com.unoshield.mdm.data.MDMDatabase
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Blacklist Activity - Manage blacklisted phone numbers
 */
class BlacklistActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BlacklistAdapter
    private lateinit var addButton: FloatingActionButton
    private lateinit var database: MDMDatabase
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phone_list)
        
        database = MDMDatabase.getDatabase(this)
        
        setupToolbar()
        setupRecyclerView()
        setupAddButton()
        loadBlacklistNumbers()
    }
    
    private fun setupToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.blacklist)
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.phone_list_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        adapter = BlacklistAdapter(
            onDeleteClick = { number ->
                deleteBlacklistNumber(number)
            }
        )
        recyclerView.adapter = adapter
    }
    
    private fun setupAddButton() {
        addButton = findViewById(R.id.add_button)
        addButton.setOnClickListener {
            showAddNumberDialog()
        }
    }
    
    private fun loadBlacklistNumbers() {
        lifecycleScope.launch {
            database.blacklistNumberDao().getAllBlacklistNumbers().collect { numbers ->
                adapter.submitList(numbers)
            }
        }
    }
    
    private fun showAddNumberDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_phone_number, null)
        val phoneInput: TextInputEditText = dialogView.findViewById(R.id.phone_input)
        val nameInput: TextInputEditText = dialogView.findViewById(R.id.name_input)
        
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.add_to_blacklist))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.add)) { _, _ ->
                val phoneNumber = phoneInput.text?.toString()?.trim()
                val name = nameInput.text?.toString()?.trim()
                
                if (phoneNumber.isNullOrEmpty()) {
                    Toast.makeText(this, "Phone number is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                addBlacklistNumber(phoneNumber, name)
            }
            .setNegativeButton(getString(android.R.string.cancel), null)
            .show()
    }
    
    private fun addBlacklistNumber(phoneNumber: String, name: String?) {
        lifecycleScope.launch {
            try {
                val blacklistNumber = BlacklistNumber(
                    phoneNumber = phoneNumber,
                    name = name
                )
                database.blacklistNumberDao().insertBlacklistNumber(blacklistNumber)
                Toast.makeText(this@BlacklistActivity, "Added to blacklist", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@BlacklistActivity, "Error adding number: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun deleteBlacklistNumber(number: BlacklistNumber) {
        AlertDialog.Builder(this)
            .setTitle("Delete Number")
            .setMessage("Remove ${number.phoneNumber} from blacklist?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    database.blacklistNumberDao().deleteBlacklistNumber(number)
                    Toast.makeText(this@BlacklistActivity, "Removed from blacklist", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private class BlacklistAdapter(
        private val onDeleteClick: (BlacklistNumber) -> Unit
    ) : RecyclerView.Adapter<BlacklistAdapter.ViewHolder>() {
        
        private var numbers = emptyList<BlacklistNumber>()
        
        fun submitList(newNumbers: List<BlacklistNumber>) {
            numbers = newNumbers
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_phone_number, parent, false)
            return ViewHolder(view, onDeleteClick)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(numbers[position])
        }
        
        override fun getItemCount(): Int = numbers.size
        
        class ViewHolder(
            itemView: View,
            private val onDeleteClick: (BlacklistNumber) -> Unit
        ) : RecyclerView.ViewHolder(itemView) {
            
            private val phoneText: TextView = itemView.findViewById(R.id.phone_text)
            private val nameText: TextView = itemView.findViewById(R.id.name_text)
            private val deleteButton: View = itemView.findViewById(R.id.delete_button)
            
            fun bind(number: BlacklistNumber) {
                phoneText.text = number.phoneNumber
                nameText.text = number.name ?: "Unknown"
                nameText.visibility = if (number.name.isNullOrEmpty()) View.GONE else View.VISIBLE
                
                deleteButton.setOnClickListener {
                    onDeleteClick(number)
                }
            }
        }
    }
}

