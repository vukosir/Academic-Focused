package com.spendsmart.ui.expenses

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.spendsmart.data.SpendSmartDatabase
import com.spendsmart.data.entities.Expense
import com.spendsmart.databinding.ActivityAddExpenseBinding
import com.spendsmart.utils.BadgeEvaluator
import com.spendsmart.utils.FormatUtils
import com.spendsmart.utils.SessionManager
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AddExpenseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddExpenseBinding
    private lateinit var session: SessionManager
    private lateinit var db: SpendSmartDatabase
    private var photoUri: Uri? = null
    private var photoPath: String? = null
    private var categoryIds = listOf<Int>()

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            binding.ivReceiptPreview.visibility = View.VISIBLE
            binding.ivReceiptPreview.setImageURI(photoUri)
        }
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            photoUri = it
            photoPath = it.toString()
            binding.ivReceiptPreview.visibility = View.VISIBLE
            binding.ivReceiptPreview.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddExpenseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        session = SessionManager(this)
        db = SpendSmartDatabase.getDatabase(this)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Add Expense"
        binding.etDate.setText(FormatUtils.todayString())
        binding.etDate.setOnClickListener { pickDate() }
        binding.btnTakePhoto.setOnClickListener { takePhoto() }
        binding.btnPickPhoto.setOnClickListener { pickImage.launch("image/*") }
        binding.btnSave.setOnClickListener { saveExpense() }
        loadCategories()
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            val cats = db.categoryDao().getAllForUserSync(session.getUserId())
            categoryIds = cats.map { it.id }
            val names = cats.map { it.name }
            val adapter = ArrayAdapter(this@AddExpenseActivity, android.R.layout.simple_spinner_item, names)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerCategory.adapter = adapter
            if (cats.isEmpty()) {
                binding.tvNoCats.visibility = View.VISIBLE
                binding.btnSave.isEnabled = false
            } else {
                binding.tvNoCats.visibility = View.GONE
                binding.btnSave.isEnabled = true
            }
        }
    }

    private fun pickDate() {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            binding.etDate.setText("%04d-%02d-%02d".format(y, m + 1, d))
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun takePhoto() {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageFile = File.createTempFile("RECEIPT_${timeStamp}_", ".jpg", storageDir)
        photoPath = imageFile.absolutePath
        photoUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", imageFile)
        takePicture.launch(photoUri)
    }

    private fun saveExpense() {
        val amtText = binding.etAmount.text.toString().trim()
        val desc = binding.etDescription.text.toString().trim()
        val date = binding.etDate.text.toString().trim()
        val start = binding.etStartTime.text.toString().trim()
        val end = binding.etEndTime.text.toString().trim()
        if (amtText.isEmpty()) { binding.tilAmount.error = "Amount required"; return }
        if (desc.isEmpty()) { binding.tilDescription.error = "Description required"; return }
        if (date.isEmpty()) { binding.tilDate.error = "Date required"; return }
        val amount = amtText.toDoubleOrNull()
        if (amount == null || amount <= 0) { binding.tilAmount.error = "Enter a valid amount"; return }
        binding.tilAmount.error = null
        binding.tilDescription.error = null
        binding.tilDate.error = null
        val catId = if (categoryIds.isNotEmpty()) categoryIds[binding.spinnerCategory.selectedItemPosition] else null
        val expense = Expense(
            userId = session.getUserId(),
            categoryId = catId,
            amount = amount,
            description = desc,
            date = date,
            startTime = start,
            endTime = end,
            photoPath = photoPath
        )
        lifecycleScope.launch {
            db.expenseDao().insert(expense)
            BadgeEvaluator(db).evaluate(session.getUserId())
            Toast.makeText(this@AddExpenseActivity, "Expense saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
