package com.spendsmart.ui.achievements

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.spendsmart.data.SpendSmartDatabase
import com.spendsmart.databinding.FragmentAchievementsBinding
import com.spendsmart.demo.DemoData
import com.spendsmart.utils.BadgeDefinitions
import com.spendsmart.utils.SessionManager
import kotlinx.coroutines.launch

data class BadgeItem(
    val id: String,
    val label: String,
    val desc: String,
    val earned: Boolean
)

class AchievementsFragment : Fragment() {

    private var _binding: FragmentAchievementsBinding? = null
    private val binding get() = _binding!!
    private lateinit var session: SessionManager
    private lateinit var db: SpendSmartDatabase

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAchievementsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        session = SessionManager(requireContext())
        db = SpendSmartDatabase.getDatabase(requireContext())
        loadBadges()
    }

    private fun loadBadges() {
        if (session.isDemoMode()) {
            val items = DemoData.BADGES.map { BadgeItem(it.id, it.label, it.desc, it.earned) }
            binding.recyclerBadges.adapter = BadgeAdapter(items)
            updateProgress(items)
        } else {
            val userId = session.getUserId()
            lifecycleScope.launch {
                val earned = db.earnedBadgeDao().getAllForUser(userId).map { it.badgeId }.toSet()
                val allBadges = listOf(
                    BadgeItem(BadgeDefinitions.FIRST_EXPENSE,    "First Step",        "Log your first expense",                BadgeDefinitions.FIRST_EXPENSE    in earned),
                    BadgeItem(BadgeDefinitions.FIVE_EXPENSES,    "Getting Started",   "Log 5 or more expenses",               BadgeDefinitions.FIVE_EXPENSES    in earned),
                    BadgeItem(BadgeDefinitions.CONSISTENT_LOGGER,"Consistent Logger", "Log 10 or more expenses",              BadgeDefinitions.CONSISTENT_LOGGER in earned),
                    BadgeItem(BadgeDefinitions.CATEGORY_CREATOR, "Organiser",         "Create 3 or more categories",          BadgeDefinitions.CATEGORY_CREATOR  in earned),
                    BadgeItem(BadgeDefinitions.BUDGET_SET,       "Budget Setter",     "Set your first monthly budget goal",   BadgeDefinitions.BUDGET_SET        in earned),
                    BadgeItem(BadgeDefinitions.WITHIN_BUDGET,    "Within Budget",     "Stay under budget for a month",        BadgeDefinitions.WITHIN_BUDGET     in earned),
                    BadgeItem(BadgeDefinitions.PHOTO_ATTACHED,   "Receipt Keeper",    "Attach a photo to an expense",         BadgeDefinitions.PHOTO_ATTACHED    in earned),
                    BadgeItem(BadgeDefinitions.SAVER_BADGE,      "Super Saver",       "Stay 20% under max budget in a month", BadgeDefinitions.SAVER_BADGE       in earned)
                )
                binding.recyclerBadges.adapter = BadgeAdapter(allBadges)
                updateProgress(allBadges)
            }
        }
    }

    private fun updateProgress(badges: List<BadgeItem>) {
        val earnedCount = badges.count { it.earned }
        val total = badges.size
        val pct = if (total > 0) earnedCount * 100 / total else 0
        binding.tvProgress.text = "$earnedCount / $total badges earned"
        binding.tvProgressPct.text = "$pct%"
        binding.progressScore.progress = pct
        binding.tvProgressHint.text = if (earnedCount < total)
            "Keep logging expenses and setting budget goals to unlock more badges."
        else
            "You have earned all available badges. Outstanding!"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
