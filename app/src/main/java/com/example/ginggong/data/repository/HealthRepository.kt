package com.example.ginggong.data.repository

import com.example.ginggong.data.db.GinggongDatabase
import com.example.ginggong.data.model.ActivityLevel
import com.example.ginggong.data.model.BodyLog
import com.example.ginggong.data.model.FlameLevel
import com.example.ginggong.data.model.FlameState
import com.example.ginggong.data.model.FoodLog
import com.example.ginggong.data.model.GamificationState
import com.example.ginggong.data.model.Gender
import com.example.ginggong.data.model.UserProfile
import com.example.ginggong.data.model.WaterLog
import com.example.ginggong.data.model.getCurrentMonthInt
import com.example.ginggong.data.model.getTodayDateString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class HealthRepository(private val db: GinggongDatabase) {

    val userProfile: Flow<UserProfile?> = db.userProfileDao().getUserProfile()
    val flameState: Flow<FlameState?> = db.flameDao().getFlameState()
    val gamificationState: Flow<GamificationState?> = db.gamificationDao().getGamificationState()
    val bodyLogs: Flow<List<BodyLog>> = db.bodyLogDao().getAllBodyLogs()

    fun getWaterLogsByDate(date: String): Flow<List<WaterLog>> = db.waterLogDao().getWaterLogsByDate(date)
    fun getTotalWaterByDate(date: String): Flow<Int?> = db.waterLogDao().getTotalWaterByDate(date)
    fun getFoodLogsByDate(date: String): Flow<List<FoodLog>> = db.foodLogDao().getFoodLogsByDate(date)
    fun getAllFoodLogs(): Flow<List<FoodLog>> = db.foodLogDao().getAllFoodLogs()

    suspend fun saveUserProfile(profile: UserProfile) {
        db.userProfileDao().insertOrUpdateProfile(profile)
    }

    suspend fun addWaterLog(amountMl: Int, date: String = getTodayDateString()) {
        db.waterLogDao().insertWaterLog(WaterLog(date = date, amountMl = amountMl))
        addXpAndCoins(xpGain = 15, coinsGain = 5)
        checkAndUpdateStreak(date)
    }

    suspend fun deleteWaterLog(id: Long) {
        db.waterLogDao().deleteWaterLog(id)
    }

    suspend fun addFoodLog(foodLog: FoodLog) {
        db.foodLogDao().insertFoodLog(foodLog)
        addXpAndCoins(xpGain = 25, coinsGain = 10)
        checkAndUpdateStreak(foodLog.date)
    }

    suspend fun deleteFoodLog(id: Long) {
        db.foodLogDao().deleteFoodLog(id)
    }

    suspend fun addBodyLog(weightKg: Float, heightCm: Float) {
        val heightM = heightCm / 100f
        val bmi = if (heightM > 0) weightKg / (heightM * heightM) else 22f
        db.bodyLogDao().insertBodyLog(
            BodyLog(date = getTodayDateString(), weightKg = weightKg, heightCm = heightCm, bmi = bmi)
        )
        // Also update User Profile
        val currentProfile = userProfile.firstOrNull() ?: UserProfile()
        saveUserProfile(currentProfile.copy(weightKg = weightKg, heightCm = heightCm))
        addXpAndCoins(xpGain = 50, coinsGain = 20)
    }

    suspend fun checkAndUpdateStreak(currentDate: String = getTodayDateString()) {
        var currentFlame = db.flameDao().getFlameState().firstOrNull() ?: FlameState()
        val currentMonth = getCurrentMonthInt()

        // Monthly recovery reset check
        if (currentMonth != currentFlame.lastRecoveryMonth) {
            currentFlame = currentFlame.copy(recoveriesRemaining = 3, lastRecoveryMonth = currentMonth)
        }

        if (currentFlame.lastActiveDate != currentDate) {
            val newStreak = currentFlame.currentStreak + 1
            val newFlameLevel = when {
                newStreak >= 100 -> FlameLevel.LEGENDARY
                newStreak >= 30 -> FlameLevel.GOLDEN
                newStreak >= 7 -> FlameLevel.LARGE
                newStreak >= 3 -> FlameLevel.MEDIUM
                else -> FlameLevel.SMALL
            }
            db.flameDao().updateFlameState(
                currentFlame.copy(
                    currentStreak = newStreak,
                    flameLevel = newFlameLevel,
                    lastActiveDate = currentDate
                )
            )
        }
    }

    suspend fun recoverFlame(): Boolean {
        val currentFlame = db.flameDao().getFlameState().firstOrNull() ?: FlameState()
        if (currentFlame.recoveriesRemaining > 0) {
            db.flameDao().updateFlameState(
                currentFlame.copy(
                    currentStreak = (currentFlame.currentStreak + 1).coerceAtLeast(1),
                    recoveriesRemaining = currentFlame.recoveriesRemaining - 1
                )
            )
            return true
        }
        return false
    }

    suspend fun addXpAndCoins(xpGain: Int, coinsGain: Int) {
        val current = db.gamificationDao().getGamificationState().firstOrNull() ?: GamificationState()
        val newXp = current.xp + xpGain
        val newLevel = (newXp / 100) + 1
        db.gamificationDao().updateGamificationState(
            current.copy(xp = newXp, level = newLevel, coins = current.coins + coinsGain)
        )
    }

    suspend fun seedSampleDataIfEmpty() {
        val profile = db.userProfileDao().getUserProfileOnce()
        if (profile == null) {
            val defaultProfile = UserProfile(
                name = "Anek Ginggong",
                age = 26,
                gender = Gender.MALE,
                heightCm = 175f,
                weightKg = 68f,
                activityLevel = ActivityLevel.MODERATE,
                isSetupCompleted = true,
                authMode = "GUEST"
            )
            db.userProfileDao().insertOrUpdateProfile(defaultProfile)
            db.flameDao().updateFlameState(FlameState(currentStreak = 5, flameLevel = FlameLevel.MEDIUM))
            db.gamificationDao().updateGamificationState(GamificationState(xp = 240, level = 3, coins = 520))

            val today = getTodayDateString()
            // Initial water logs
            db.waterLogDao().insertWaterLog(WaterLog(date = today, amountMl = 250))
            db.waterLogDao().insertWaterLog(WaterLog(date = today, amountMl = 500))
            db.waterLogDao().insertWaterLog(WaterLog(date = today, amountMl = 500))

            // Initial food logs
            db.foodLogDao().insertFoodLog(
                FoodLog(
                    date = today,
                    mealType = com.example.ginggong.data.model.MealType.BREAKFAST,
                    foodName = "Oatmeal with Berries & Honey",
                    calories = 320,
                    proteinG = 12f,
                    carbsG = 52f,
                    fatG = 6f,
                    saturatedFatG = 1f,
                    fiberG = 7f,
                    sugarG = 14f,
                    sodiumMg = 120f,
                    servingSize = "1 bowl (250g)"
                )
            )
            db.foodLogDao().insertFoodLog(
                FoodLog(
                    date = today,
                    mealType = com.example.ginggong.data.model.MealType.LUNCH,
                    foodName = "Grilled Chicken Salad & Avocado",
                    calories = 480,
                    proteinG = 42f,
                    carbsG = 18f,
                    fatG = 22f,
                    saturatedFatG = 3f,
                    fiberG = 8f,
                    sugarG = 5f,
                    sodiumMg = 450f,
                    servingSize = "1 large plate"
                )
            )

            // Initial body log
            db.bodyLogDao().insertBodyLog(
                BodyLog(date = today, weightKg = 68f, heightCm = 175f, bmi = 22.2f)
            )
        }
    }
}
