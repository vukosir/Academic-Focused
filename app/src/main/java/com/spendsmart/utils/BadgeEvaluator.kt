package com.spendsmart.utils

import android.util.Log
import com.spendsmart.data.SpendSmartDatabase
import com.spendsmart.data.entities.EarnedBadge

object BadgeDefinitions {

    data class Badge(
        val id: String,
        val label: String,
        val desc: String,
        val iconRes: Int
    )

    const val FIRST_EXPENSE     = "first_expense"
    const val FIVE_EXPENSES     = "five_expenses"
    const val BUDGET_SET        = "budget_set"
    const val WITHIN_BUDGET     = "within_budget"
    const val CATEGORY_CREATOR  = "category_creator"
    const val CONSISTENT_LOGGER = "consistent_logger"
    const val SAVER_BADGE       = "saver_badge"
    const val PHOTO_ATTACHED    = "photo_attached"
}

/**
 * Evaluates all badge conditions for a given user and awards any newly earned badges.
 * Called from AddExpenseActivity after each save so badges update in real-time.
 *
 * Badges:
 *  FIRST_EXPENSE     – logged at least 1 expense
 *  FIVE_EXPENSES     – logged at least 5 expenses
 *  CONSISTENT_LOGGER – logged at least 10 expenses
 *  CATEGORY_CREATOR  – created at least 3 categories
 *  BUDGET_SET        – set at least one budget goal
 *  WITHIN_BUDGET     – current month spending is within the overall max budget
 *  SAVER_BADGE       – current month spending is under 80 % of the overall max budget
 *  PHOTO_ATTACHED    – at least one expense has a receipt photo (evaluated externally on save)
 */
class BadgeEvaluator(private val db: SpendSmartDatabase) {

    suspend fun evaluate(userId: Int): List<String> {
        val newBadges = mutableListOf<String>()

        // --- Expense count badges ---
        val expenseCount = db.expenseDao().countForUser(userId)
        Log.d("BadgeEvaluator", "userId=$userId expenseCount=$expenseCount")

        if (expenseCount >= 1)  newBadges.tryAward(userId, BadgeDefinitions.FIRST_EXPENSE)
        if (expenseCount >= 5)  newBadges.tryAward(userId, BadgeDefinitions.FIVE_EXPENSES)
        if (expenseCount >= 10) newBadges.tryAward(userId, BadgeDefinitions.CONSISTENT_LOGGER)

        // --- Category count badge ---
        val categories = db.categoryDao().getAllForUserSync(userId)
        if (categories.size >= 3) newBadges.tryAward(userId, BadgeDefinitions.CATEGORY_CREATOR)

        // --- Budget goal badge ---
        val goals = db.budgetGoalDao().getAllSync(userId)
        if (goals.isNotEmpty()) newBadges.tryAward(userId, BadgeDefinitions.BUDGET_SET)

        // --- Budget adherence badges ---
        val overallGoal = db.budgetGoalDao().getOverallGoal(userId)
        if (overallGoal != null && overallGoal.maxMonthly > 0) {
            val today    = FormatUtils.todayString()
            val firstDay = FormatUtils.firstDayOfMonth()
            val monthSpent = db.expenseDao().getTotalInRange(userId, firstDay, today) ?: 0.0

            Log.d("BadgeEvaluator", "monthSpent=$monthSpent maxMonthly=${overallGoal.maxMonthly}")

            // WITHIN_BUDGET: spending is at or below the overall monthly max
            if (monthSpent <= overallGoal.maxMonthly) {
                newBadges.tryAward(userId, BadgeDefinitions.WITHIN_BUDGET)
            }

            // SAVER_BADGE: spending is under 80 % of the overall monthly max (super saver)
            if (monthSpent <= overallGoal.maxMonthly * 0.80) {
                newBadges.tryAward(userId, BadgeDefinitions.SAVER_BADGE)
            }
        }

        if (newBadges.isNotEmpty()) Log.d("BadgeEvaluator", "Awarded: $newBadges")
        return newBadges
    }

    private suspend fun MutableList<String>.tryAward(userId: Int, badgeId: String) {
        val alreadyEarned = db.earnedBadgeDao().hasBadge(userId, badgeId) > 0
        if (!alreadyEarned) {
            db.earnedBadgeDao().insert(EarnedBadge(userId = userId, badgeId = badgeId))
            add(badgeId)
            Log.d("BadgeEvaluator", "Badge awarded: $badgeId to userId=$userId")
        }
    }
}
