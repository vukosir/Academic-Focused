package com.spendsmart.ui.expenses

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.spendsmart.databinding.ItemExpenseBinding
import com.spendsmart.utils.FormatUtils

data class ExpenseListItem(
    val id: Int,
    val date: String,
    val description: String,
    val category: String,
    val amount: Double,
    val photoPath: String?,
    val colorHex: String
)

class ExpenseAdapter(
    private val items: List<ExpenseListItem>,
    private val onClick: (ExpenseListItem) -> Unit
) : RecyclerView.Adapter<ExpenseAdapter.VH>() {

    inner class VH(val binding: ItemExpenseBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemExpenseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.tvDate.text = FormatUtils.displayDate(item.date)
        holder.binding.tvDescription.text = item.description
        holder.binding.tvCategory.text = item.category
        holder.binding.tvAmount.text = FormatUtils.formatRand(item.amount)
        holder.binding.catDot.setBackgroundColor(FormatUtils.parseColor(item.colorHex))
        holder.binding.ivReceipt.visibility = if (item.photoPath != null) View.VISIBLE else View.GONE
        holder.binding.root.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size
}
