package com.spendsmart.ui.budget

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.spendsmart.databinding.ItemBudgetCatBinding
import com.spendsmart.utils.FormatUtils

data class BudgetCatItem(
    val name: String,
    val colorHex: String,
    val spent: Double,
    val min: Double,
    val max: Double
)

class BudgetCatAdapter(private val items: List<BudgetCatItem>) : RecyclerView.Adapter<BudgetCatAdapter.VH>() {

    inner class VH(val binding: ItemBudgetCatBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemBudgetCatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.catDot.setBackgroundColor(FormatUtils.parseColor(item.colorHex))
        holder.binding.tvCatName.text = item.name
        holder.binding.tvSpent.text = "Actual: ${FormatUtils.formatRand(item.spent)}"
        holder.binding.tvMin.text = "Min: ${FormatUtils.formatRand(item.min)}"
        holder.binding.tvMax.text = "Max: ${FormatUtils.formatRand(item.max)}"
        val progress = if (item.max > 0) ((item.spent / item.max) * 100).toInt().coerceIn(0, 100) else 0
        holder.binding.progressBar.progress = progress
        val color = when {
            item.max > 0 && item.spent > item.max -> android.graphics.Color.parseColor("#c0392b")
            item.min > 0 && item.spent < item.min -> android.graphics.Color.parseColor("#b45309")
            else -> android.graphics.Color.parseColor("#1a6b3c")
        }
        holder.binding.progressBar.progressTintList = android.content.res.ColorStateList.valueOf(color)
        val statusText = when {
            item.max > 0 && item.spent > item.max -> "OVER BUDGET"
            item.min > 0 && item.spent < item.min -> "UNDER MIN"
            item.max > 0 -> "ON TRACK"
            else -> "NO GOAL SET"
        }
        holder.binding.tvStatus.text = statusText
        holder.binding.tvStatus.setTextColor(color)
    }

    override fun getItemCount() = items.size
}
