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
import com.unoshield.mdm.data.MDMDatabase
import com.unoshield.mdm.data.WhitelistNumber
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Whitelist Activity - Manage whitelisted phone numbers
 */
class WhitelistActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: WhitelistAdapter
    private lateinit var addButton: FloatingActionButton
    private lateinit var database: MDMDatabase
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phone_list)
        
        database = MDMDatabase.getDatabase(this)
        
        setupToolbar()
        setupRecyclerView()
        setupAddButton()
        loadWhitelistNumbers()
    }
    
    private fun setupToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.whitelist)
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.phone_list_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        adapter = WhitelistAdapter(
            onDeleteClick = { number ->
                deleteWhitelistNumber(number)
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
    
    private fun loadWhitelistNumbers() {
        lifecycleScope.launch {
            database.whitelistNumberDao().getAllWhitelistNumbers().collect { numbers ->
                adapter.submitList(numbers)
            }
        }
    }
    
    private fun showAddNumberDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_phone_number, null)
        val phoneInput: TextInputEditText = dialogView.findViewById(R.id.phone_input)
        val nameInput: TextInputEditText = dialogView.findViewById(R.id.name_input)
        
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.add_to_whitelist))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.add)) { _, _ ->
                val phoneNumber = phoneInput.text?.toString()?.trim()
                val name = nameInput.text?.toString()?.trim()
                
                if (phoneNumber.isNullOrEmpty()) {
                    Toast.makeText(this, "Phone number is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                addWhitelistNumber(phoneNumber, name)
            }
            .setNegativeButton(getString(android.R.string.cancel), null)
            .show()
    }
    
    private fun addWhitelistNumber(phoneNumber: String, name: String?) {
        lifecycleScope.launch {
            try {
                val whitelistNumber = WhitelistNumber(
                    phoneNumber = phoneNumber,
                    name = name
                )
                database.whitelistNumberDao().insertWhitelistNumber(whitelistNumber)
                Toast.makeText(this@WhitelistActivity, "Added to whitelist", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@WhitelistActivity, "Error adding number: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun deleteWhitelistNumber(number: WhitelistNumber) {
        AlertDialog.Builder(this)
            .setTitle("Delete Number")
            .setMessage("Remove ${number.phoneNumber} from whitelist?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    database.whitelistNumberDao().deleteWhitelistNumber(number)
                    Toast.makeText(this@WhitelistActivity, "Removed from whitelist", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private class WhitelistAdapter(
        private val onDeleteClick: (WhitelistNumber) -> Unit
    ) : RecyclerView.Adapter<WhitelistAdapter.ViewHolder>() {
        
        private var numbers = emptyList<WhitelistNumber>()
        
        fun submitList(newNumbers: List<WhitelistNumber>) {
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
            private val onDeleteClick: (WhitelistNumber) -> Unit
        ) : RecyclerView.ViewHolder(itemView) {
            
            private val phoneText: TextView = itemView.findViewById(R.id.phone_text)
            private val nameText: TextView = itemView.findViewById(R.id.name_text)
            private val deleteButton: View = itemView.findViewById(R.id.delete_button)
            
            fun bind(number: WhitelistNumber) {
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

