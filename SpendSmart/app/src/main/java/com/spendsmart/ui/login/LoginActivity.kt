package com.spendsmart.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.spendsmart.data.SpendSmartDatabase
import com.spendsmart.data.entities.User
import com.spendsmart.databinding.ActivityLoginBinding
import com.spendsmart.ui.MainActivity
import com.spendsmart.utils.HashUtils
import com.spendsmart.utils.SessionManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var session: SessionManager
    private lateinit var db: SpendSmartDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session = SessionManager(this)
        AppCompatDelegate.setDefaultNightMode(
            if (session.isDarkMode()) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
        if (session.isLoggedIn()) { startMain(); return }
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = SpendSmartDatabase.getDatabase(this)

        binding.btnSignIn.setOnClickListener { attemptLogin() }
        binding.tvRegister.setOnClickListener { showRegisterMode() }
        binding.btnRegister.setOnClickListener { attemptRegister() }
        binding.tvBackLogin.setOnClickListener { showLoginMode() }
        binding.etPassword.setOnEditorActionListener { _, _, _ -> attemptLogin(); true }

        binding.btnDemoMode.setOnClickListener {
            session.setDemoMode(true)
            session.login(-1, "Demo User")
            startMain()
        }
    }

    private fun attemptLogin() {
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString()
        if (username.isEmpty() || password.isEmpty()) { showError("Please enter your username and password."); return }
        lifecycleScope.launch {
            val user = db.userDao().findByUsername(username)
            if (user == null || user.passwordHash != HashUtils.sha256(password)) {
                showError("Invalid username or password.")
            } else {
                session.setDemoMode(false)
                session.login(user.id, user.username)
                startMain()
            }
        }
    }

    private fun attemptRegister() {
        val username = binding.etRegUsername.text.toString().trim()
        val password = binding.etRegPassword.text.toString()
        val confirm  = binding.etRegConfirm.text.toString()
        if (username.isEmpty() || password.isEmpty()) { showRegisterError("All fields are required."); return }
        if (username.length < 3)  { showRegisterError("Username must be at least 3 characters."); return }
        if (password.length < 6)  { showRegisterError("Password must be at least 6 characters."); return }
        if (password != confirm)  { showRegisterError("Passwords do not match."); return }
        lifecycleScope.launch {
            val existing = db.userDao().findByUsername(username)
            if (existing != null) { showRegisterError("Username already taken."); return@launch }
            val id = db.userDao().insert(User(username = username, passwordHash = HashUtils.sha256(password))).toInt()
            session.setDemoMode(false)
            session.login(id, username)
            startMain()
        }
    }

    private fun showError(msg: String) {
        binding.tvError.text = msg
        binding.tvError.visibility = View.VISIBLE
    }

    private fun showRegisterError(msg: String) {
        binding.tvRegError.text = msg
        binding.tvRegError.visibility = View.VISIBLE
    }

    private fun showRegisterMode() {
        binding.layoutLogin.visibility = View.GONE
        binding.layoutRegister.visibility = View.VISIBLE
        binding.tvError.visibility = View.GONE
        binding.tvRegError.visibility = View.GONE
    }

    private fun showLoginMode() {
        binding.layoutRegister.visibility = View.GONE
        binding.layoutLogin.visibility = View.VISIBLE
        binding.tvError.visibility = View.GONE
        binding.tvRegError.visibility = View.GONE
    }

    private fun startMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
