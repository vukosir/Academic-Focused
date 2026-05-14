package com.spendsmart.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.spendsmart.databinding.ItemRecentBinding
import com.spendsmart.utils.FormatUtils

data class RecentItem(
    val date: String,
    val description: String,
    val category: String,
    val amount: Double
)

class RecentAdapter(private val items: List<RecentItem>) : RecyclerView.Adapter<RecentAdapter.VH>() {

    inner class VH(val binding: ItemRecentBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemRecentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.tvDate.text = FormatUtils.displayDate(item.date)
        holder.binding.tvDescription.text = item.description
        holder.binding.tvCategory.text = item.category
        holder.binding.tvAmount.text = FormatUtils.formatRand(item.amount)
    }

    override fun getItemCount() = items.size
}
