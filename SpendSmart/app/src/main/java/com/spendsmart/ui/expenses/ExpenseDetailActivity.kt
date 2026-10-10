package com.spendsmart.ui.expenses

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.spendsmart.data.SpendSmartDatabase
import com.spendsmart.databinding.ActivityExpenseDetailBinding
import com.spendsmart.utils.FormatUtils
import com.spendsmart.utils.SessionManager
import kotlinx.coroutines.launch
import java.io.File

class ExpenseDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExpenseDetailBinding
    private lateinit var db: SpendSmartDatabase
    private lateinit var session: SessionManager
    private var expenseId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExpenseDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        session = SessionManager(this)
        db = SpendSmartDatabase.getDatabase(this)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Expense Detail"
        expenseId = intent.getIntExtra("expense_id", 0)
        loadExpense()
        binding.btnDelete.setOnClickListener { confirmDelete() }
    }

    private fun loadExpense() {
        lifecycleScope.launch {
            val expense = db.expenseDao().findById(expenseId) ?: run { finish(); return@launch }
            val cat = if (expense.categoryId != null) db.categoryDao().findById(expense.categoryId) else null
            binding.tvDate.text = FormatUtils.displayDate(expense.date)
            binding.tvDescription.text = expense.description
            binding.tvAmount.text = FormatUtils.formatRand(expense.amount)
            binding.tvCategory.text = cat?.name ?: "Uncategorised"
            binding.tvTime.text = if (expense.startTime.isNotEmpty()) "${expense.startTime} – ${expense.endTime}" else "No time recorded"
            if (expense.photoPath != null) {
                binding.cardReceipt.visibility = View.VISIBLE
                try {
                    if (expense.photoPath.startsWith("content://")) {
                        binding.ivReceipt.setImageURI(Uri.parse(expense.photoPath))
                    } else {
                        binding.ivReceipt.setImageURI(Uri.fromFile(File(expense.photoPath)))
                    }
                } catch (e: Exception) {
                    binding.cardReceipt.visibility = View.GONE
                }
            } else {
                binding.cardReceipt.visibility = View.GONE
            }
        }
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle("Delete Expense")
            .setMessage("Are you sure you want to delete this expense?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    val expense = db.expenseDao().findById(expenseId)
                    expense?.let { db.expenseDao().delete(it) }
                    Toast.makeText(this@ExpenseDetailActivity, "Expense deleted", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
