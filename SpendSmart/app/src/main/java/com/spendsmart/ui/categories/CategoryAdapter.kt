package com.spendsmart.ui.categories

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.spendsmart.databinding.ItemCategoryBinding
import com.spendsmart.utils.FormatUtils

data class CategoryItem(
    val name: String,
    val amount: Double,
    val colorHex: String,
    val percentage: Double
)

class CategoryAdapter(private val items: List<CategoryItem>) : RecyclerView.Adapter<CategoryAdapter.VH>() {

    inner class VH(val binding: ItemCategoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.catDot.setBackgroundColor(FormatUtils.parseColor(item.colorHex))
        holder.binding.tvCatName.text = item.name
        holder.binding.tvCatAmount.text = FormatUtils.formatRand(item.amount)
        holder.binding.tvCatPercent.text = "${String.format("%.1f", item.percentage)}% of total"
        holder.binding.progressBar.progress = item.percentage.toInt().coerceIn(0, 100)
        holder.binding.progressBar.progressTintList = android.content.res.ColorStateList.valueOf(FormatUtils.parseColor(item.colorHex))
    }

    override fun getItemCount() = items.size
}
