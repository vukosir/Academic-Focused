package com.spendsmart.ui.categories

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.spendsmart.data.SpendSmartDatabase
import com.spendsmart.data.entities.Category
import com.spendsmart.databinding.ActivityManageCategoriesBinding
import com.spendsmart.databinding.ItemManageCategoryBinding
import com.spendsmart.utils.FormatUtils
import com.spendsmart.utils.SessionManager
import kotlinx.coroutines.launch

class ManageCategoriesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageCategoriesBinding
    private lateinit var session: SessionManager
    private lateinit var db: SpendSmartDatabase

    private val colorOptions = listOf(
        "#111110", "#4338ca", "#0369a1", "#065f46", "#c2410c", "#0f766e",
        "#6d28d9", "#1d4ed8", "#b45309", "#0e7490", "#7e22ce", "#9f1239",
        "#155e75", "#7f1d1d", "#134e4a", "#14532d", "#78350f", "#1e3a5f"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageCategoriesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        session = SessionManager(this)
        db = SpendSmartDatabase.getDatabase(this)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Manage Categories"
        binding.btnAddCategory.setOnClickListener { showAddDialog() }
        observeCategories()
    }

    private fun observeCategories() {
        db.categoryDao().getAllForUser(session.getUserId()).observe(this) { cats ->
            binding.recyclerCategories.adapter = ManageCatAdapter(cats) { cat ->
                AlertDialog.Builder(this)
                    .setTitle("Delete Category")
                    .setMessage("Delete '${cat.name}'? Expenses in this category will become uncategorised.")
                    .setPositiveButton("Delete") { _, _ ->
                        lifecycleScope.launch { db.categoryDao().delete(cat) }
                    }
                    .setNegativeButton("Cancel", null).show()
            }
        }
    }

    private fun showAddDialog() {
        val dialogView = LayoutInflater.from(this).inflate(com.spendsmart.R.layout.dialog_add_category, null)
        val etName = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(com.spendsmart.R.id.etCatName)
        var selectedColor = colorOptions[0]
        val colorGrid = dialogView.findViewById<RecyclerView>(com.spendsmart.R.id.rvColors)
        colorGrid.adapter = ColorPickerAdapter(colorOptions, selectedColor) { color -> selectedColor = color }
        colorGrid.layoutManager = androidx.recyclerview.widget.GridLayoutManager(this, 6)
        AlertDialog.Builder(this)
            .setTitle("Add Category")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = etName?.text.toString().trim()
                if (name.isEmpty()) { Toast.makeText(this, "Category name required", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                lifecycleScope.launch {
                    val exists = db.categoryDao().countByName(session.getUserId(), name) > 0
                    if (exists) { Toast.makeText(this@ManageCategoriesActivity, "Category already exists", Toast.LENGTH_SHORT).show(); return@launch }
                    db.categoryDao().insert(Category(userId = session.getUserId(), name = name, colorHex = selectedColor))
                    Toast.makeText(this@ManageCategoriesActivity, "Category added", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null).show()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}

class ManageCatAdapter(
    private val items: List<Category>,
    private val onDelete: (Category) -> Unit
) : RecyclerView.Adapter<ManageCatAdapter.VH>() {

    inner class VH(val binding: ItemManageCategoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemManageCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val cat = items[position]
        holder.binding.catDot.setBackgroundColor(FormatUtils.parseColor(cat.colorHex))
        holder.binding.tvCatName.text = cat.name
        holder.binding.btnDelete.setOnClickListener { onDelete(cat) }
    }

    override fun getItemCount() = items.size
}

class ColorPickerAdapter(
    private val colors: List<String>,
    private var selected: String,
    private val onPick: (String) -> Unit
) : RecyclerView.Adapter<ColorPickerAdapter.VH>() {

    inner class VH(val view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = View(parent.context).apply {
            layoutParams = ViewGroup.MarginLayoutParams(80, 80).also { it.setMargins(6, 6, 6, 6) }
        }
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val color = colors[position]
        holder.view.setBackgroundColor(FormatUtils.parseColor(color))
        holder.view.alpha = if (color == selected) 1f else 0.5f
        (holder.view.layoutParams as ViewGroup.MarginLayoutParams).also {
            it.width = if (color == selected) 96 else 80
            it.height = if (color == selected) 96 else 80
        }
        holder.view.setOnClickListener {
            selected = color
            onPick(color)
            notifyDataSetChanged()
        }
    }

    override fun getItemCount() = colors.size
}
