package com.example.ginggong.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.ginggong.data.model.ActivityLevel
import com.example.ginggong.data.model.BodyLog
import com.example.ginggong.data.model.FlameLevel
import com.example.ginggong.data.model.FlameState
import com.example.ginggong.data.model.FoodLog
import com.example.ginggong.data.model.GamificationState
import com.example.ginggong.data.model.Gender
import com.example.ginggong.data.model.MealType
import com.example.ginggong.data.model.UserProfile
import com.example.ginggong.data.model.WaterLog
import kotlinx.coroutines.flow.Flow

class Converters {
    @TypeConverter
    fun fromGender(gender: Gender): String = gender.name
    @TypeConverter
    fun toGender(value: String): Gender = runCatching { Gender.valueOf(value) }.getOrDefault(Gender.MALE)

    @TypeConverter
    fun fromActivityLevel(level: ActivityLevel): String = level.name
    @TypeConverter
    fun toActivityLevel(value: String): ActivityLevel = runCatching { ActivityLevel.valueOf(value) }.getOrDefault(ActivityLevel.MODERATE)

    @TypeConverter
    fun fromFlameLevel(level: FlameLevel): String = level.name
    @TypeConverter
    fun toFlameLevel(value: String): FlameLevel = runCatching { FlameLevel.valueOf(value) }.getOrDefault(FlameLevel.SMALL)

    @TypeConverter
    fun fromMealType(type: MealType): String = type.name
    @TypeConverter
    fun toMealType(value: String): MealType = runCatching { MealType.valueOf(value) }.getOrDefault(MealType.BREAKFAST)
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getUserProfileOnce(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfile)
}

@Dao
interface WaterLogDao {
    @Query("SELECT * FROM water_logs WHERE date = :date ORDER BY timestamp DESC")
    fun getWaterLogsByDate(date: String): Flow<List<WaterLog>>

    @Query("SELECT SUM(amountMl) FROM water_logs WHERE date = :date")
    fun getTotalWaterByDate(date: String): Flow<Int?>

    @Query("SELECT * FROM water_logs ORDER BY timestamp DESC")
    fun getAllWaterLogs(): Flow<List<WaterLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterLog(log: WaterLog)

    @Query("DELETE FROM water_logs WHERE id = :id")
    suspend fun deleteWaterLog(id: Long)
}

@Dao
interface FoodLogDao {
    @Query("SELECT * FROM food_logs WHERE date = :date ORDER BY timestamp DESC")
    fun getFoodLogsByDate(date: String): Flow<List<FoodLog>>

    @Query("SELECT * FROM food_logs ORDER BY timestamp DESC LIMIT 50")
    fun getAllFoodLogs(): Flow<List<FoodLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodLog(log: FoodLog)

    @Query("DELETE FROM food_logs WHERE id = :id")
    suspend fun deleteFoodLog(id: Long)
}

@Dao
interface BodyLogDao {
    @Query("SELECT * FROM body_logs ORDER BY timestamp ASC")
    fun getAllBodyLogs(): Flow<List<BodyLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBodyLog(log: BodyLog)
}

@Dao
interface FlameDao {
    @Query("SELECT * FROM flame_state WHERE id = 1")
    fun getFlameState(): Flow<FlameState?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateFlameState(state: FlameState)
}

@Dao
interface GamificationDao {
    @Query("SELECT * FROM gamification WHERE id = 1")
    fun getGamificationState(): Flow<GamificationState?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateGamificationState(state: GamificationState)
}

@Database(
    entities = [
        UserProfile::class,
        WaterLog::class,
        FoodLog::class,
        BodyLog::class,
        FlameState::class,
        GamificationState::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class GinggongDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun waterLogDao(): WaterLogDao
    abstract fun foodLogDao(): FoodLogDao
    abstract fun bodyLogDao(): BodyLogDao
    abstract fun flameDao(): FlameDao
    abstract fun gamificationDao(): GamificationDao

    companion object {
        @Volatile
        private var INSTANCE: GinggongDatabase? = null

        fun getDatabase(context: Context): GinggongDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GinggongDatabase::class.java,
                    "ginggong_eating_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
