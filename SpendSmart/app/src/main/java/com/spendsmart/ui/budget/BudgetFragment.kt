package com.spendsmart.ui.budget

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.spendsmart.data.SpendSmartDatabase
import com.spendsmart.data.entities.BudgetGoal
import com.spendsmart.databinding.FragmentBudgetBinding
import com.spendsmart.demo.DemoData
import com.spendsmart.utils.FormatUtils
import com.spendsmart.utils.SessionManager
import kotlinx.coroutines.launch

class BudgetFragment : Fragment() {

    private var _binding: FragmentBudgetBinding? = null
    private val binding get() = _binding!!
    private lateinit var session: SessionManager
    private lateinit var db: SpendSmartDatabase

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBudgetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        session = SessionManager(requireContext())
        db = SpendSmartDatabase.getDatabase(requireContext())
        binding.btnSetBudget.setOnClickListener {
            if (!session.isDemoMode()) startActivity(Intent(requireContext(), BudgetGoalsActivity::class.java))
        }
        loadBudget()
    }

    override fun onResume() {
        super.onResume()
        loadBudget()
    }

    private fun loadBudget() {
        if (session.isDemoMode()) loadDemoBudget() else loadRealBudget()
    }

    private fun loadDemoBudget() {
        binding.tvOverallMin.text = FormatUtils.formatRand(DemoData.BUDGET_MIN)
        binding.tvOverallMax.text = FormatUtils.formatRand(DemoData.BUDGET_MAX)
        val avgSpend = DemoData.AVG_MONTHLY
        val max = DemoData.BUDGET_MAX
        val pct = ((avgSpend / max) * 100).toInt().coerceIn(0, 100)
        binding.progressOverall.progress = pct
        binding.progressOverall.progressTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1a6b3c"))
        binding.tvProgressLabel.text = "Avg spend: ${FormatUtils.formatRand(avgSpend)} / Max: ${FormatUtils.formatRand(max)}  —  ON TRACK"
        binding.tvProgressLabel.setTextColor(android.graphics.Color.parseColor("#1a6b3c"))

        val catItems = DemoData.CATEGORIES.map { cat ->
            val monthlyAmt = cat.amount / 6.0
            val catMax = when (cat.name) {
                "Rent"          -> 12500.0
                "Groceries"     -> 5000.0
                "Medical"       -> 2800.0
                "Subscriptions" -> 2200.0
                "Clothing"      -> 1500.0
                "Fuel"          -> 1400.0
                "Electricity"   -> 1000.0
                "Home"          -> 1000.0
                "Transport"     -> 900.0
                "Takeaways"     -> 700.0
                "Water & Rates" -> 700.0
                "Cellphone"     -> 400.0
                "Personal Care" -> 500.0
                else            -> monthlyAmt * 1.2
            }
            BudgetCatItem(cat.name, cat.colorHex, monthlyAmt, catMax * 0.5, catMax)
        }
        binding.recyclerCatBudgets.adapter = BudgetCatAdapter(catItems)
        binding.btnSetBudget.visibility = View.GONE
    }

    private fun loadRealBudget() {
        binding.btnSetBudget.visibility = View.VISIBLE
        val userId = session.getUserId()
        val firstDay = FormatUtils.firstDayOfMonth()
        val today = FormatUtils.todayString()
        lifecycleScope.launch {
            val overallGoal = db.budgetGoalDao().getOverallGoal(userId)
            if (overallGoal != null) {
                binding.tvOverallMin.text = FormatUtils.formatRand(overallGoal.minMonthly)
                binding.tvOverallMax.text = FormatUtils.formatRand(overallGoal.maxMonthly)
                val spent = db.expenseDao().getTotalInRange(userId, firstDay, today) ?: 0.0
                val max = overallGoal.maxMonthly
                if (max > 0) {
                    val pct = ((spent / max) * 100).toInt().coerceIn(0, 100)
                    binding.progressOverall.progress = pct
                    binding.tvProgressLabel.text = "Spent: ${FormatUtils.formatRand(spent)} / Max: ${FormatUtils.formatRand(max)}"
                    val color = if (spent > max) android.graphics.Color.parseColor("#c0392b") else android.graphics.Color.parseColor("#1a6b3c")
                    binding.tvProgressLabel.setTextColor(color)
                    binding.progressOverall.progressTintList = android.content.res.ColorStateList.valueOf(color)
                }
            } else {
                binding.tvOverallMin.text = "Not set"
                binding.tvOverallMax.text = "Not set"
                binding.progressOverall.progress = 0
                binding.tvProgressLabel.text = "Set budget goals to track your progress"
            }
            val cats = db.categoryDao().getAllForUserSync(userId)
            val catTotals = db.expenseDao().getCategoryTotals(userId, firstDay, today)
            val catItems = cats.map { cat ->
                val spent = catTotals.find { it.categoryId == cat.id }?.total ?: 0.0
                val goal = db.budgetGoalDao().getForCategory(userId, cat.id)
                BudgetCatItem(cat.name, cat.colorHex, spent, goal?.minMonthly ?: 0.0, goal?.maxMonthly ?: 0.0)
            }
            binding.recyclerCatBudgets.adapter = BudgetCatAdapter(catItems)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
