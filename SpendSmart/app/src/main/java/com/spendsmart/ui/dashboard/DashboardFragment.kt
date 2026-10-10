package com.spendsmart.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.spendsmart.data.SpendSmartDatabase
import com.spendsmart.databinding.FragmentDashboardBinding
import com.spendsmart.demo.DemoData
import com.spendsmart.utils.FormatUtils
import com.spendsmart.utils.SessionManager
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var session: SessionManager
    private lateinit var db: SpendSmartDatabase

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        session = SessionManager(requireContext())
        db = SpendSmartDatabase.getDatabase(requireContext())
        loadData()
    }

    private fun loadData() {
        if (session.isDemoMode()) loadDemoData() else loadRealData()
    }

    private fun loadDemoData() {
        binding.tvTotalSpent.text = FormatUtils.formatRand(DemoData.TOTAL_SPENT)
        binding.tvTotalIncome.text = FormatUtils.formatRand(DemoData.TOTAL_INCOME)
        binding.tvAccountChange.text = "+${FormatUtils.formatRand(DemoData.ACCOUNT_CHANGE)}"
        binding.tvAvgMonthly.text = FormatUtils.formatRand(DemoData.AVG_MONTHLY)
        binding.tvLabelSpent.text = "Total spent (6 months)"
        binding.tvLabelIncome.text = "Salary income (6 months)"
        binding.tvLabelChange.text = "Net surplus"
        binding.tvLabelAvg.text = "Avg. monthly spend"
        binding.tagSpent.text = "13 categories"
        binding.tagIncome.text = "R 35 000/month"
        binding.tagChange.text = "Under budget"
        binding.tagAvg.text = "R 28 951/month"
        binding.tagChange.setTextColor(android.graphics.Color.parseColor("#1a6b3c"))
        binding.tagChange.setBackgroundColor(android.graphics.Color.parseColor("#edf7f1"))
        binding.tvAccountChange.setTextColor(android.graphics.Color.parseColor("#1a6b3c"))
        binding.demoBanner.visibility = View.VISIBLE

        buildDonuts(
            DemoData.CATEGORIES.map { it.name to it.amount },
            DemoData.CATEGORIES.map { it.colorHex }
        )
        buildBar(
            DemoData.MONTHLY.map { it.month },
            DemoData.MONTHLY.map { it.spending.toFloat() },
            DemoData.MONTHLY.map { it.income.toFloat() }
        )
        buildLine(
            DemoData.MONTHLY.map { it.month },
            DemoData.MONTHLY.map { it.spending.toFloat() }
        )

        binding.recyclerRecent.adapter = RecentAdapter(DemoData.TRANSACTIONS.take(8).map {
            RecentItem(it.date, it.description, it.category, it.amount)
        })
    }

    private fun loadRealData() {
        binding.demoBanner.visibility = View.GONE
        binding.tvAccountChange.setTextColor(android.graphics.Color.parseColor("#111110"))
        val userId = session.getUserId()
        val today = FormatUtils.todayString()
        val firstDay = FormatUtils.firstDayOfMonth()
        lifecycleScope.launch {
            val total = db.expenseDao().getTotalInRange(userId, "2000-01-01", today) ?: 0.0
            binding.tvTotalSpent.text = FormatUtils.formatRand(total)
            binding.tvTotalIncome.text = "—"
            binding.tvAccountChange.text = "—"
            val monthlyTotal = db.expenseDao().getTotalInRange(userId, firstDay, today) ?: 0.0
            binding.tvAvgMonthly.text = FormatUtils.formatRand(monthlyTotal)
            binding.tvLabelSpent.text = "Total spent (all time)"
            binding.tvLabelIncome.text = "Income tracked"
            binding.tvLabelChange.text = "Net change"
            binding.tvLabelAvg.text = "Spent this month"
            binding.tagSpent.text = "All categories"
            binding.tagIncome.text = "Add income to track"
            binding.tagChange.text = ""
            binding.tagAvg.text = "Current month"

            val catTotals = db.expenseDao().getCategoryTotals(userId, "2000-01-01", today)
            if (catTotals.isNotEmpty()) {
                buildDonuts(
                    catTotals.map { (it.categoryName ?: "Uncategorised") to it.total },
                    catTotals.map { it.colorHex ?: "#111110" }
                )
            } else {
                binding.pieChart.setNoDataText("No expenses yet")
            }

            val dailyTotals = db.expenseDao().getDailyTotals(userId, firstDay, today)
            if (dailyTotals.isNotEmpty()) {
                buildLine(dailyTotals.map { it.date.takeLast(5) }, dailyTotals.map { it.total.toFloat() })
            }

            val allExpenses = db.expenseDao().getInRange(userId, "2000-01-01", today)
            val recentItems = allExpenses.take(8).map {
                val cat = if (it.categoryId != null) db.categoryDao().findById(it.categoryId)?.name ?: "Uncategorised" else "Uncategorised"
                RecentItem(it.date, it.description, cat, it.amount)
            }
            binding.recyclerRecent.adapter = RecentAdapter(recentItems)
        }
    }

    private fun buildDonuts(pairs: List<Pair<String, Double>>, colors: List<String>) {
        val entries = pairs.map { (name, amt) -> PieEntry(amt.toFloat(), name) }
        val colorInts = colors.map { FormatUtils.parseColor(it) }
        val ds = PieDataSet(entries, "").apply {
            this.colors = colorInts
            sliceSpace = 2f
            setDrawValues(false)
        }
        binding.pieChart.apply {
            data = PieData(ds)
            description.isEnabled = false
            legend.isEnabled = false
            isDrawHoleEnabled = true
            holeRadius = 55f
            setHoleColor(android.graphics.Color.TRANSPARENT)
            setDrawEntryLabels(false)
            setTouchEnabled(false)
            invalidate()
        }
    }

    private fun buildBar(labels: List<String>, spending: List<Float>, income: List<Float>) {
        val spendEntries = spending.mapIndexed { i, v -> BarEntry(i.toFloat() * 2f, v) }
        val incomeEntries = income.mapIndexed { i, v -> BarEntry(i.toFloat() * 2f + 0.5f, v) }
        val dsSpend = BarDataSet(spendEntries, "Spending").apply {
            color = android.graphics.Color.parseColor("#c0392b")
            setDrawValues(false)
        }
        val dsIncome = BarDataSet(incomeEntries, "Income").apply {
            color = android.graphics.Color.parseColor("#1a6b3c")
            setDrawValues(false)
        }
        binding.barChart.apply {
            data = BarData(dsSpend, dsIncome).also { it.barWidth = 0.4f }
            description.isEnabled = false
            legend.isEnabled = true
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(labels)
                granularity = 2f
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                labelRotationAngle = -45f
            }
            axisLeft.setDrawGridLines(false)
            axisRight.isEnabled = false
            setTouchEnabled(false)
            invalidate()
        }
    }

    private fun buildLine(labels: List<String>, values: List<Float>) {
        val entries = values.mapIndexed { i, v -> Entry(i.toFloat(), v) }
        val ds = LineDataSet(entries, "Spending trend").apply {
            color = android.graphics.Color.parseColor("#1a1a1a")
            lineWidth = 2f
            circleRadius = 3f
            setCircleColor(android.graphics.Color.parseColor("#1a1a1a"))
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillAlpha = 15
        }
        binding.lineChart.apply {
            data = LineData(ds)
            description.isEnabled = false
            legend.isEnabled = false
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(labels)
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                labelRotationAngle = -45f
            }
            axisLeft.setDrawGridLines(false)
            axisRight.isEnabled = false
            setTouchEnabled(false)
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
