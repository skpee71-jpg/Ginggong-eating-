package com.example.ginggong.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Gender { MALE, FEMALE, OTHER }

enum class ActivityLevel(val displayName: String, val factor: Double) {
    SEDENTARY("Sedentary (Little to no exercise)", 1.2),
    LIGHT("Light (Exercise 1-3 days/week)", 1.375),
    MODERATE("Moderate (Exercise 3-5 days/week)", 1.55),
    HEAVY("Heavy (Exercise 6-7 days/week)", 1.725),
    ATHLETE("Athlete (Physical job or 2x training)", 1.9)
}

enum class BmiCategory(val label: String, val colorHex: String) {
    UNDERWEIGHT("Underweight", "#3892FF"),
    NORMAL("Normal weight", "#34C759"),
    OVERWEIGHT("Overweight", "#FF9500"),
    OBESE("Obese", "#FF3B30")
}

enum class FlameLevel(val displayName: String, val minDays: Int, val emoji: String, val colorHex: String) {
    SMALL("Small Flame", 1, "🔥", "#FF9500"),
    MEDIUM("Medium Flame", 3, "🔥", "#FF5E00"),
    LARGE("Large Flame", 7, "🔥", "#FF2A00"),
    GOLDEN("Golden Flame", 30, "✨🔥", "#FFD700"),
    LEGENDARY("Legendary Flame", 100, "👑🔥", "#E040FB")
}

enum class MealType(val displayName: String) {
    BREAKFAST("Breakfast"),
    LUNCH("Lunch"),
    DINNER("Dinner"),
    SNACK("Snack")
}

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "Ginggong User",
    val age: Int = 25,
    val gender: Gender = Gender.MALE,
    val heightCm: Float = 170f,
    val weightKg: Float = 65f,
    val birthday: String = "2000-01-01",
    val activityLevel: ActivityLevel = ActivityLevel.MODERATE,
    val isSetupCompleted: Boolean = false,
    val authMode: String = "GUEST" // GUEST, EMAIL, GOOGLE
) {
    val dailyWaterGoalMl: Int get() = (weightKg * 35).toInt().coerceAtLeast(1500)
    
    val bmi: Float get() {
        val heightM = heightCm / 100f
        return if (heightM > 0) weightKg / (heightM * heightM) else 22f
    }

    val bmiCategory: BmiCategory get() = when {
        bmi < 18.5f -> BmiCategory.UNDERWEIGHT
        bmi < 25f -> BmiCategory.NORMAL
        bmi < 30f -> BmiCategory.OVERWEIGHT
        else -> BmiCategory.OBESE
    }

    val bmr: Int get() {
        // Mifflin-St Jeor Formula
        return if (gender == Gender.MALE) {
            (10 * weightKg + 6.25 * heightCm - 5 * age + 5).toInt()
        } else {
            (10 * weightKg + 6.25 * heightCm - 5 * age - 161).toInt()
        }
    }

    val tdee: Int get() = (bmr * activityLevel.factor).toInt()
    val caloriesMaintain: Int get() = tdee
    val caloriesLose: Int get() = (tdee - 500).coerceAtLeast(1200)
    val caloriesGain: Int get() = tdee + 400
}

@Entity(tableName = "water_logs")
data class WaterLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val amountMl: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "food_logs")
data class FoodLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val mealType: MealType,
    val foodName: String,
    val calories: Int,
    val proteinG: Float,
    val carbsG: Float,
    val fatG: Float,
    val saturatedFatG: Float = 0f,
    val fiberG: Float = 0f,
    val sugarG: Float = 0f,
    val sodiumMg: Float = 0f,
    val cholesterolMg: Float = 0f,
    val waterMl: Int = 0,
    val servingSize: String = "1 portion",
    val ingredients: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "body_logs")
data class BodyLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val weightKg: Float,
    val heightCm: Float,
    val bmi: Float,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "flame_state")
data class FlameState(
    @PrimaryKey val id: Int = 1,
    val currentStreak: Int = 1,
    val flameLevel: FlameLevel = FlameLevel.SMALL,
    val recoveriesRemaining: Int = 3,
    val lastActiveDate: String = getTodayDateString(),
    val lastRecoveryMonth: Int = getCurrentMonthInt()
)

@Entity(tableName = "gamification")
data class GamificationState(
    @PrimaryKey val id: Int = 1,
    val xp: Int = 120,
    val level: Int = 2,
    val coins: Int = 350,
    val selectedTheme: String = "Bright Blue",
    val selectedFlameSkin: String = "Classic Blue Flame",
    val unlockedBadges: String = "FIRST_WATER,7_DAY_STREAK,AI_FOOD_SCAN",
    val completedMissions: String = "DRINK_1000ML,LOG_MEAL"
)

data class ScanNutritionResult(
    val title: String,
    val calories: Int,
    val proteinG: Float,
    val carbsG: Float,
    val fatG: Float,
    val saturatedFatG: Float = 0f,
    val fiberG: Float = 0f,
    val sugarG: Float = 0f,
    val sodiumMg: Float = 0f,
    val cholesterolMg: Float = 0f,
    val servingSize: String = "1 serving",
    val ingredients: String = "",
    val confidence: Int = 95
)

fun getTodayDateString(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date())
}

fun getCurrentMonthInt(): Int {
    val sdf = SimpleDateFormat("MM", Locale.getDefault())
    return sdf.format(Date()).toIntOrNull() ?: 1
}
