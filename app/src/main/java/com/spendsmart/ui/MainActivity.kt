package com.spendsmart.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.spendsmart.R
import com.spendsmart.databinding.ActivityMainBinding
import com.spendsmart.ui.achievements.AchievementsFragment
import com.spendsmart.ui.budget.BudgetFragment
import com.spendsmart.ui.categories.CategoriesFragment
import com.spendsmart.ui.dashboard.DashboardFragment
import com.spendsmart.ui.expenses.ExpensesFragment
import com.spendsmart.ui.insights.InsightsFragment
import com.spendsmart.ui.login.LoginActivity
import com.spendsmart.utils.SessionManager

/**
 * Main entry point of the application after login.
 * Manages the top-level navigation using a TabLayout and displays various feature fragments.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var session: SessionManager

    // Predefined list of fragments mapped to tabs
    private val fragments = listOf(
        DashboardFragment(),
        ExpensesFragment(),
        CategoriesFragment(),
        BudgetFragment(),
        InsightsFragment(),
        AchievementsFragment()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session = SessionManager(this)
        
        // Apply Dark Mode setting before setting the content view
        AppCompatDelegate.setDefaultNightMode(
            if (session.isDarkMode()) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Setup custom toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        
        // Display user info or Demo status
        binding.tvNavUser.text = if (session.isDemoMode()) "Demo" else session.getUsername()
        binding.tvDemoLabel.text = if (session.isDemoMode()) "DEMO" else ""
        
        setupTabs()
        loadFragment(0) // Default to Dashboard
    }

    /**
     * Configures the TabLayout with specific titles and sets up the selection listener.
     */
    private fun setupTabs() {
        val tabTitles = listOf("Dashboard", "Expenses", "Categories", "Budget", "Insights", "Achievements")
        tabTitles.forEach { title ->
            binding.tabBar.addTab(binding.tabBar.newTab().setText(title))
        }
        
        binding.tabBar.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) = loadFragment(tab.position)
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    /**
     * Swaps the current fragment in the container based on the selected tab index.
     */
    private fun loadFragment(index: Int) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragments[index])
            .commit()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    /**
     * Handles toolbar menu actions: Demo Mode, Dark Mode toggle, and Logout.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_demo -> {
                val isDemo = !session.isDemoMode()
                session.setDemoMode(isDemo)
                binding.tvDemoLabel.text = if (isDemo) "DEMO" else ""
                val msg = if (isDemo) "Demo mode ON — showing sample data" else "Demo mode OFF — showing your data"
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                refreshCurrentTab() // Refresh data view
                true
            }
            R.id.action_dark_mode -> {
                val isDark = !session.isDarkMode()
                session.setDarkMode(isDark)
                AppCompatDelegate.setDefaultNightMode(
                    if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                )
                true
            }
            R.id.action_logout -> {
                session.logout()
                startActivity(Intent(this, LoginActivity::class.java))
                finish() // Close main activity
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Reloads the currently visible fragment, typically after a setting change like Demo Mode.
     */
    private fun refreshCurrentTab() {
        val currentTab = binding.tabBar.selectedTabPosition
        loadFragment(currentTab)
    }
}
