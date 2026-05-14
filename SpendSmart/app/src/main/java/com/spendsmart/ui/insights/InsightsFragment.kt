package com.spendsmart.ui.insights

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.spendsmart.data.SpendSmartDatabase
import com.spendsmart.databinding.FragmentInsightsBinding
import com.spendsmart.demo.DemoData
import com.spendsmart.utils.FormatUtils
import com.spendsmart.utils.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Displays spending insights and a bar chart showing category spending vs budget goals.
 * The chart date range is user-selectable via DatePickerDialogs.
 * Final POE feature: graph with min/max goal LimitLines per OPSC6311 Part 3 rubric.
 */
class InsightsFragment : Fragment() {

    private var _binding: FragmentInsightsBinding? = null
    private val binding get() = _binding!!
    private lateinit var session: SessionManager
    private lateinit var db: SpendSmartDatabase

    // Mutable chart date range – default to current month
    private var chartFrom: Calendar = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }
    private var chartTo: Calendar = Calendar.getInstance()

    private val displayFmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val dbFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInsightsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        session = SessionManager(requireContext())
        db = SpendSmartDatabase.getDatabase(requireContext())
        setupDateButtons()
        loadInsights()
    }

    // Wire up the two date-picker buttons and refresh the chart whenever the range changes
    private fun setupDateButtons() {
        updateDateButtonLabels()
        binding.btnChartFrom.setOnClickListener { showDatePicker(chartFrom) { cal -> chartFrom = cal; updateDateButtonLabels(); refreshChart() } }
        binding.btnChartTo.setOnClickListener   { showDatePicker(chartTo)   { cal -> chartTo   = cal; updateDateButtonLabels(); refreshChart() } }
    }

    private fun updateDateButtonLabels() {
        binding.btnChartFrom.text = "From: ${displayFmt.format(chartFrom.time)}"
        binding.btnChartTo.text   = "To: ${displayFmt.format(chartTo.time)}"
    }

    private fun showDatePicker(current: Calendar, onPick: (Calendar) -> Unit) {
        DatePickerDialog(
            requireContext(),
            { _, y, m, d -> onPick(Calendar.getInstance().apply { set(y, m, d) }) },
            current.get(Calendar.YEAR),
            current.get(Calendar.MONTH),
            current.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun loadInsights() {
        if (session.isDemoMode()) {
            showDemoInsights()
            buildDemoChart()
        } else {
            showRealInsights()
            refreshChart()
        }
    }

    // --- Demo mode -----------------------------------------------------------

    private fun showDemoInsights() {
        binding.tvInsightSummary.text = DemoData.INSIGHT_SUMMARY
        binding.tvMoneyGoes.text = DemoData.INSIGHT_MONEY_GOES
        binding.tvStandout.text = DemoData.INSIGHT_STANDOUT
        binding.tvTopMove.text = DemoData.TOP_SAVING_MOVE
        binding.layoutFlags.removeAllViews()
        DemoData.OVERSPENDING_FLAGS.forEach { (cat, reason) ->
            val tv = android.widget.TextView(requireContext()).apply {
                text = "• $cat: $reason"
                textSize = 12f
                setPadding(0, 8, 0, 8)
                setTextColor(Color.parseColor("#c0392b"))
            }
            binding.layoutFlags.addView(tv)
        }
    }

    /** Build chart from demo data: use CATEGORIES list, overall BUDGET_MIN / BUDGET_MAX as limit lines. */
    private fun buildDemoChart() {
        Log.d("InsightsFragment", "Building demo category chart")
        val cats = DemoData.CATEGORIES
        if (cats.isEmpty()) { showChartEmpty(true); return }
        showChartEmpty(false)

        // Divide total amount by 6 months to get avg monthly spend per category
        val entries = cats.mapIndexed { i, cat -> BarEntry(i.toFloat(), (cat.amount / 6.0).toFloat()) }
        val labels  = cats.map { it.name }
        val colors  = cats.map { FormatUtils.parseColor(it.colorHex) }

        val minGoal = DemoData.BUDGET_MIN.toFloat()
        val maxGoal = DemoData.BUDGET_MAX.toFloat()

        applyChart(entries, labels, colors, minGoal, maxGoal)
    }

    // --- Real data mode ------------------------------------------------------

    private fun showRealInsights() {
        val userId = session.getUserId()
        val today = FormatUtils.todayString()
        val firstDay = FormatUtils.firstDayOfMonth()
        lifecycleScope.launch {
            val totalAllTime   = db.expenseDao().getTotalInRange(userId, "2000-01-01", today) ?: 0.0
            val totalThisMonth = db.expenseDao().getTotalInRange(userId, firstDay, today) ?: 0.0
            val expCount = db.expenseDao().countForUser(userId)
            val catTotals = db.expenseDao().getCategoryTotals(userId, "2000-01-01", today)
            val topCat = catTotals.firstOrNull()
            val goals = db.budgetGoalDao().getOverallGoal(userId)

            binding.tvInsightSummary.text = buildString {
                append("You have logged $expCount expense${if (expCount != 1) "s" else ""} totalling ${FormatUtils.formatRand(totalAllTime)}.\n\n")
                append("This month you have spent ${FormatUtils.formatRand(totalThisMonth)}.")
                if (goals != null && goals.maxMonthly > 0) {
                    val diff = goals.maxMonthly - totalThisMonth
                    if (diff >= 0) append(" You are ${FormatUtils.formatRand(diff)} under your max budget.")
                    else append(" You are ${FormatUtils.formatRand(-diff)} over your max budget.")
                }
            }

            binding.tvMoneyGoes.text = if (topCat != null)
                "Your highest spending category is '${topCat.categoryName ?: "Uncategorised"}' at ${FormatUtils.formatRand(topCat.total)}."
            else
                "No expenses recorded yet. Start adding expenses to see insights."

            binding.tvStandout.text = if (goals != null && goals.maxMonthly > 0 && totalThisMonth > goals.maxMonthly)
                "Warning: You are over your monthly max budget of ${FormatUtils.formatRand(goals.maxMonthly)}."
            else if (goals == null)
                "Tip: Set a monthly budget goal on the Budget tab to get spending alerts."
            else
                "You are within budget this month. Keep it up!"

            binding.tvTopMove.text = if (topCat != null)
                "Focus on reducing spending in '${topCat.categoryName}' — it is your biggest expense category."
            else
                "Start logging your daily expenses to receive personalised savings tips."

            binding.layoutFlags.removeAllViews()
        }
    }

    /** Reload chart whenever the user changes the date range. */
    private fun refreshChart() {
        if (session.isDemoMode()) { buildDemoChart(); return }

        val userId = session.getUserId()
        val from = dbFmt.format(chartFrom.time)
        val to   = dbFmt.format(chartTo.time)
        Log.d("InsightsFragment", "Refreshing chart from=$from to=$to userId=$userId")

        lifecycleScope.launch {
            val catTotals = db.expenseDao().getCategoryTotals(userId, from, to)
            val goals = db.budgetGoalDao().getOverallGoal(userId)

            if (catTotals.isEmpty()) { showChartEmpty(true); return@launch }
            showChartEmpty(false)

            val entries = catTotals.mapIndexed { i, ct -> BarEntry(i.toFloat(), ct.total.toFloat()) }
            val labels  = catTotals.map { it.categoryName ?: "Other" }
            val colors  = catTotals.map { FormatUtils.parseColor(it.colorHex ?: "#888888") }

            val minGoal = goals?.minMonthly?.toFloat() ?: 0f
            val maxGoal = goals?.maxMonthly?.toFloat() ?: 0f

            applyChart(entries, labels, colors, minGoal, maxGoal)
        }
    }

    // --- Chart builder -------------------------------------------------------

    /**
     * Populates the HorizontalBarChart with per-category spending bars.
     * LimitLines mark the overall monthly min (amber) and max (red) budget goals.
     */
    private fun applyChart(
        entries: List<BarEntry>,
        labels: List<String>,
        colors: List<Int>,
        minGoal: Float,
        maxGoal: Float
    ) {
        val chart = binding.chartCategory

        // Dataset
        val dataSet = BarDataSet(entries, "Spending (R)").apply {
            this.colors = colors
            valueTextSize = 10f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float) =
                    if (value >= 1000f) "R${(value / 1000).toInt()}k" else "R${value.toInt()}"
            }
        }

        chart.data = BarData(dataSet).apply { barWidth = 0.6f }

        // X axis shows category names on the left
        chart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(labels)
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false)
            textSize = 10f
            granularity = 1f
            labelCount = labels.size
        }

        // Y axis (value axis) – right side for HorizontalBarChart
        chart.axisRight.apply {
            removeAllLimitLines()
            if (minGoal > 0f) {
                val ll = LimitLine(minGoal, "Min R${minGoal.toInt()}").apply {
                    lineWidth = 2f
                    lineColor = Color.parseColor("#b45309")
                    textColor = Color.parseColor("#b45309")
                    textSize = 10f
                    enableDashedLine(12f, 6f, 0f)
                }
                addLimitLine(ll)
            }
            if (maxGoal > 0f) {
                val ll = LimitLine(maxGoal, "Max R${maxGoal.toInt()}").apply {
                    lineWidth = 2f
                    lineColor = Color.parseColor("#c0392b")
                    textColor = Color.parseColor("#c0392b")
                    textSize = 10f
                    enableDashedLine(12f, 6f, 0f)
                }
                addLimitLine(ll)
            }
            setDrawLimitLinesBehindData(false)
        }
        chart.axisLeft.isEnabled = false

        // General chart config
        chart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setFitBars(true)
            setTouchEnabled(true)
            setPinchZoom(false)
            setDrawGridBackground(false)
            animateY(600)
            invalidate()
        }

        Log.d("InsightsFragment", "Chart updated: ${entries.size} categories, minGoal=$minGoal, maxGoal=$maxGoal")
    }

    private fun showChartEmpty(empty: Boolean) {
        binding.chartCategory.visibility = if (empty) View.GONE else View.VISIBLE
        binding.tvChartEmpty.visibility  = if (empty) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
