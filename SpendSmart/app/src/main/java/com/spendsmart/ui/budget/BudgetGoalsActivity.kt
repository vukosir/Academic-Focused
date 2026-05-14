package com.spendsmart.ui.budget

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.spendsmart.data.SpendSmartDatabase
import com.spendsmart.data.entities.BudgetGoal
import com.spendsmart.databinding.ActivityBudgetGoalsBinding
import com.spendsmart.utils.BadgeEvaluator
import com.spendsmart.utils.SessionManager
import kotlinx.coroutines.launch

/**
 * Activity for users to set and manage their monthly budget goals.
 * Allows setting minimum and maximum spending thresholds.
 */
class BudgetGoalsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBudgetGoalsBinding
    private lateinit var session: SessionManager
    private lateinit var db: SpendSmartDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize ViewBinding
        binding = ActivityBudgetGoalsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize helpers and database
        session = SessionManager(this)
        db = SpendSmartDatabase.getDatabase(this)
        
        // Setup Toolbar with back navigation
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Budget Goals"
        
        // Load existing goals from database
        loadCurrentGoals()
        
        // Set click listener for save button
        binding.btnSaveGoals.setOnClickListener { saveGoals() }
    }

    /**
     * Fetches existing budget goals for the current user and populates the UI.
     */
    private fun loadCurrentGoals() {
        lifecycleScope.launch {
            val existing = db.budgetGoalDao().getOverallGoal(session.getUserId())
            if (existing != null) {
                binding.etMinMonthly.setText(existing.minMonthly.toString())
                binding.etMaxMonthly.setText(existing.maxMonthly.toString())
            }
        }
    }

    /**
     * Validates and saves the budget goals to the local Room database.
     */
    private fun saveGoals() {
        val minText = binding.etMinMonthly.text.toString().trim()
        val maxText = binding.etMaxMonthly.text.toString().trim()
        
        // Basic validation for empty fields
        if (minText.isEmpty() || maxText.isEmpty()) {
            Toast.makeText(this, "Please enter both min and max values", Toast.LENGTH_SHORT).show()
            return
        }
        
        val min = minText.toDoubleOrNull()
        val max = maxText.toDoubleOrNull()
        
        // Validation for valid numbers and non-negative values
        if (min == null || max == null || min < 0 || max < 0) {
            Toast.makeText(this, "Please enter valid amounts", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Logic validation: min should not be greater than max
        if (min > max) {
            Toast.makeText(this, "Minimum cannot exceed maximum", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            val userId = session.getUserId()
            val existing = db.budgetGoalDao().getOverallGoal(userId)
            
            if (existing != null) {
                // Update existing goal record
                db.budgetGoalDao().update(existing.copy(minMonthly = min, maxMonthly = max))
            } else {
                // Create new goal record
                db.budgetGoalDao().insert(BudgetGoal(userId = userId, categoryId = null, minMonthly = min, maxMonthly = max))
            }
            
            // Re-evaluate badges as changing goals might trigger new achievements
            BadgeEvaluator(db).evaluate(userId)
            
            Toast.makeText(this@BudgetGoalsActivity, "Budget goals saved", Toast.LENGTH_SHORT).show()
            finish() // Return to previous screen
        }
    }

    /**
     * Handles back button click in the toolbar.
     */
    override fun onSupportNavigateUp(): Boolean { 
        finish()
        return true 
    }
}
