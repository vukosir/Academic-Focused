package com.spendsmart.ui.categories

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.spendsmart.data.SpendSmartDatabase
import com.spendsmart.databinding.FragmentCategoriesBinding
import com.spendsmart.demo.DemoData
import com.spendsmart.utils.FormatUtils
import com.spendsmart.utils.SessionManager
import kotlinx.coroutines.launch
import java.util.*

class CategoriesFragment : Fragment() {

    private var _binding: FragmentCategoriesBinding? = null
    private val binding get() = _binding!!
    private lateinit var session: SessionManager
    private lateinit var db: SpendSmartDatabase
    private var fromDate = FormatUtils.firstDayOfMonth()
    private var toDate = FormatUtils.todayString()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCategoriesBinding.inflate(inflater, container, false)
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
        binding.btnApplyFilter.setOnClickListener { loadCategories() }
        binding.btnManageCategories.setOnClickListener {
            if (!session.isDemoMode()) {
                startActivity(android.content.Intent(requireContext(), ManageCategoriesActivity::class.java))
            }
        }
        loadCategories()
    }

    override fun onResume() {
        super.onResume()
        loadCategories()
    }

    private fun pickDate(isFrom: Boolean) {
        val cal = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, y, m, d ->
            val date = "%04d-%02d-%02d".format(y, m + 1, d)
            if (isFrom) { fromDate = date; binding.tvFromDate.text = date }
            else { toDate = date; binding.tvToDate.text = date }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun loadCategories() {
        if (session.isDemoMode()) {
            val total = DemoData.CATEGORIES.sumOf { it.amount }
            val items = DemoData.CATEGORIES.map {
                CategoryItem(it.name, it.amount, it.colorHex, if (total > 0) it.amount / total * 100 else 0.0)
            }
            binding.recyclerCategories.adapter = CategoryAdapter(items)
            binding.tvTotal.text = "Total: ${FormatUtils.formatRand(total)} across ${items.size} categories"
            binding.btnManageCategories.visibility = View.GONE
        } else {
            binding.btnManageCategories.visibility = View.VISIBLE
            val userId = session.getUserId()
            lifecycleScope.launch {
                val totals = db.expenseDao().getCategoryTotals(userId, fromDate, toDate)
                val grandTotal = totals.sumOf { it.total }
                val items = totals.map {
                    CategoryItem(
                        name = it.categoryName ?: "Uncategorised",
                        amount = it.total,
                        colorHex = it.colorHex ?: "#111110",
                        percentage = if (grandTotal > 0) it.total / grandTotal * 100 else 0.0
                    )
                }
                binding.recyclerCategories.adapter = CategoryAdapter(items)
                binding.tvTotal.text = "Total: ${FormatUtils.formatRand(grandTotal)} across ${items.size} categories"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
