package com.spendsmart.ui.expenses

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.spendsmart.databinding.FragmentExpensesBinding
import com.spendsmart.demo.DemoData
import com.spendsmart.utils.FormatUtils
import com.spendsmart.utils.SessionManager
import com.spendsmart.data.SpendSmartDatabase
import kotlinx.coroutines.launch
import java.util.*

class ExpensesFragment : Fragment() {

    private var _binding: FragmentExpensesBinding? = null
    private val binding get() = _binding!!
    private lateinit var session: SessionManager
    private lateinit var db: SpendSmartDatabase
    private var fromDate = FormatUtils.firstDayOfMonth()
    private var toDate = FormatUtils.todayString()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentExpensesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        session = SessionManager(requireContext())
        db = SpendSmartDatabase.getDatabase(requireContext())
        binding.tvFromDate.text = fromDate
        binding.tvToDate.text = toDate
        binding.tvFromDate.setOnClickListener { pickDate(true) }
        binding.tvToDate.setOnClickListener { pickDate(false) }
        binding.btnAddExpense.setOnClickListener {
            if (!session.isDemoMode()) {
                startActivity(Intent(requireContext(), AddExpenseActivity::class.java))
            }
        }
        binding.btnApplyFilter.setOnClickListener { loadExpenses() }
        loadExpenses()
    }

    override fun onResume() {
        super.onResume()
        loadExpenses()
    }

    private fun pickDate(isFrom: Boolean) {
        val cal = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, y, m, d ->
            val date = "%04d-%02d-%02d".format(y, m + 1, d)
            if (isFrom) { fromDate = date; binding.tvFromDate.text = date }
            else { toDate = date; binding.tvToDate.text = date }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun loadExpenses() {
        if (session.isDemoMode()) {
            val demoItems = DemoData.TRANSACTIONS.map {
                ExpenseListItem(0, it.date, it.description, it.category, it.amount, null, "#111110")
            }
            binding.recyclerExpenses.adapter = ExpenseAdapter(demoItems) {}
            binding.tvTotal.text = "Demo: ${demoItems.size} transactions · Total: ${FormatUtils.formatRand(demoItems.sumOf { it.amount })}"
            binding.btnAddExpense.visibility = View.GONE
        } else {
            binding.btnAddExpense.visibility = View.VISIBLE
            val userId = session.getUserId()
            lifecycleScope.launch {
                val expenses = db.expenseDao().getInRange(userId, fromDate, toDate)
                val items = expenses.map {
                    val catName = if (it.categoryId != null) db.categoryDao().findById(it.categoryId)?.name ?: "Uncategorised" else "Uncategorised"
                    val catColor = if (it.categoryId != null) db.categoryDao().findById(it.categoryId)?.colorHex ?: "#111110" else "#111110"
                    ExpenseListItem(it.id, it.date, it.description, catName, it.amount, it.photoPath, catColor)
                }
                binding.recyclerExpenses.adapter = ExpenseAdapter(items) { item ->
                    val intent = Intent(requireContext(), ExpenseDetailActivity::class.java)
                    intent.putExtra("expense_id", item.id)
                    startActivity(intent)
                }
                val total = items.sumOf { it.amount }
                binding.tvTotal.text = "${items.size} transaction${if (items.size != 1) "s" else ""} · Total: ${FormatUtils.formatRand(total)}"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
