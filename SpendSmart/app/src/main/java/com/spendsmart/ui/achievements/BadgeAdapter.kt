package com.spendsmart.ui.achievements

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.spendsmart.R
import com.spendsmart.databinding.ItemBadgeBinding

class BadgeAdapter(private val items: List<BadgeItem>) : RecyclerView.Adapter<BadgeAdapter.VH>() {

    inner class VH(val binding: ItemBadgeBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemBadgeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.tvBadgeName.text = item.label
        holder.binding.tvBadgeDesc.text = item.desc
        if (item.earned) {
            holder.binding.badgeIconBg.setBackgroundColor(android.graphics.Color.parseColor("#111110"))
            holder.binding.ivBadgeIcon.setColorFilter(android.graphics.Color.WHITE)
            holder.binding.tvBadgeStatus.text = "EARNED"
            holder.binding.tvBadgeStatus.setTextColor(android.graphics.Color.parseColor("#1a6b3c"))
            holder.binding.tvBadgeStatus.setBackgroundColor(android.graphics.Color.parseColor("#edf7f1"))
            holder.binding.root.alpha = 1f
            holder.binding.root.strokeColor = android.graphics.Color.parseColor("#111110")
            holder.binding.root.strokeWidth = 2
        } else {
            holder.binding.badgeIconBg.setBackgroundColor(android.graphics.Color.parseColor("#f5f4f2"))
            holder.binding.ivBadgeIcon.setColorFilter(android.graphics.Color.parseColor("#9c9892"))
            holder.binding.tvBadgeStatus.text = "LOCKED"
            holder.binding.tvBadgeStatus.setTextColor(android.graphics.Color.parseColor("#9c9892"))
            holder.binding.tvBadgeStatus.setBackgroundColor(android.graphics.Color.parseColor("#f5f4f2"))
            holder.binding.root.alpha = 0.7f
            holder.binding.root.strokeWidth = 0
        }
    }

    override fun getItemCount() = items.size
}
