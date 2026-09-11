package com.example.ginggong.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ginggong.data.db.GinggongDatabase
import com.example.ginggong.data.model.ActivityLevel
import com.example.ginggong.data.model.BodyLog
import com.example.ginggong.data.model.FlameState
import com.example.ginggong.data.model.FoodLog
import com.example.ginggong.data.model.GamificationState
import com.example.ginggong.data.model.Gender
import com.example.ginggong.data.model.MealType
import com.example.ginggong.data.model.ScanNutritionResult
import com.example.ginggong.data.model.UserProfile
import com.example.ginggong.data.model.WaterLog
import com.example.ginggong.data.model.getTodayDateString
import com.example.ginggong.data.repository.HealthRepository
import com.example.ginggong.network.gemini.GeminiAiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ScanState {
    object Idle : ScanState
    object Loading : ScanState
    data class Success(val result: ScanNutritionResult, val mode: String) : ScanState
    data class Error(val message: String) : ScanState
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = GinggongDatabase.getDatabase(application)
    val repository = HealthRepository(db)

    val todayDate = getTodayDateString()

    val userProfile: StateFlow<UserProfile> = repository.userProfile
        .combine(MutableStateFlow(UserProfile())) { dbProfile, fallback ->
            dbProfile ?: fallback
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    val flameState: StateFlow<FlameState> = repository.flameState
        .combine(MutableStateFlow(FlameState())) { dbState, fallback ->
            dbState ?: fallback
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FlameState())

    val gamificationState: StateFlow<GamificationState> = repository.gamificationState
        .combine(MutableStateFlow(GamificationState())) { dbState, fallback ->
            dbState ?: fallback
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GamificationState())

    val waterLogsToday: StateFlow<List<WaterLog>> = repository.getWaterLogsByDate(todayDate)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalWaterTodayMl: StateFlow<Int> = repository.getTotalWaterByDate(todayDate)
        .combine(MutableStateFlow(0)) { total, _ -> total ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val foodLogsToday: StateFlow<List<FoodLog>> = repository.getFoodLogsByDate(todayDate)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFoodLogs: StateFlow<List<FoodLog>> = repository.getAllFoodLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bodyLogs: StateFlow<List<BodyLog>> = repository.bodyLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _showCelebration = MutableStateFlow(false)
    val showCelebration: StateFlow<Boolean> = _showCelebration.asStateFlow()

    private val _appLanguage = MutableStateFlow("EN")
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedSampleDataIfEmpty()
        }
    }

    fun setSelectedTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun setLanguage(lang: String) {
        _appLanguage.value = lang
    }

    fun addWater(amountMl: Int) {
        viewModelScope.launch {
            val previousWater = totalWaterTodayMl.value
            val goal = userProfile.value.dailyWaterGoalMl
            repository.addWaterLog(amountMl)
            
            val newWater = previousWater + amountMl
            if (previousWater < goal && newWater >= goal) {
                _showCelebration.value = true
            }
        }
    }

    fun dismissCelebration() {
        _showCelebration.value = false
    }

    fun addFoodLog(
        mealType: MealType,
        title: String,
        calories: Int,
        protein: Float,
        carbs: Float,
        fat: Float,
        satFat: Float = 0f,
        fiber: Float = 0f,
        sugar: Float = 0f,
        sodium: Float = 0f,
        servingSize: String = "1 portion"
    ) {
        viewModelScope.launch {
            val log = FoodLog(
                date = todayDate,
                mealType = mealType,
                foodName = title,
                calories = calories,
                proteinG = protein,
                carbsG = carbs,
                fatG = fat,
                saturatedFatG = satFat,
                fiberG = fiber,
                sugarG = sugar,
                sodiumMg = sodium,
                servingSize = servingSize
            )
            repository.addFoodLog(log)
            _scanState.value = ScanState.Idle
        }
    }

    fun deleteFoodLog(id: Long) {
        viewModelScope.launch {
            repository.deleteFoodLog(id)
        }
    }

    fun deleteWaterLog(id: Long) {
        viewModelScope.launch {
            repository.deleteWaterLog(id)
        }
    }

    fun updateWeightAndHeight(weightKg: Float, heightCm: Float) {
        viewModelScope.launch {
            repository.addBodyLog(weightKg, heightCm)
        }
    }

    fun analyzeFoodPhoto(bitmap: Bitmap) {
        _scanState.value = ScanState.Loading
        viewModelScope.launch {
            val result = GeminiAiService.analyzeFoodPhoto(bitmap)
            result.onSuccess {
                _scanState.value = ScanState.Success(it, mode = "FOOD_AI")
            }.onFailure {
                _scanState.value = ScanState.Error(it.message ?: "Failed to analyze photo")
            }
        }
    }

    fun analyzeNutritionLabelPhoto(bitmap: Bitmap) {
        _scanState.value = ScanState.Loading
        viewModelScope.launch {
            val result = GeminiAiService.analyzeNutritionLabelPhoto(bitmap)
            result.onSuccess {
                _scanState.value = ScanState.Success(it, mode = "NUTRITION_LABEL")
            }.onFailure {
                _scanState.value = ScanState.Error(it.message ?: "Failed to scan nutrition label")
            }
        }
    }

    fun lookupBarcode(code: String) {
        _scanState.value = ScanState.Loading
        viewModelScope.launch {
            val res = GeminiAiService.lookupBarcode(code)
            _scanState.value = ScanState.Success(res, mode = "BARCODE")
        }
    }

    fun resetScanState() {
        _scanState.value = ScanState.Idle
    }

    fun recoverFlame() {
        viewModelScope.launch {
            repository.recoverFlame()
        }
    }

    fun completeOnboarding(
        name: String,
        age: Int,
        gender: Gender,
        heightCm: Float,
        weightKg: Float,
        birthday: String,
        activityLevel: ActivityLevel,
        authMode: String
    ) {
        viewModelScope.launch {
            val newProfile = UserProfile(
                name = name,
                age = age,
                gender = gender,
                heightCm = heightCm,
                weightKg = weightKg,
                birthday = birthday,
                activityLevel = activityLevel,
                isSetupCompleted = true,
                authMode = authMode
            )
            repository.saveUserProfile(newProfile)
            repository.addBodyLog(weightKg, heightCm)
        }
    }
}
