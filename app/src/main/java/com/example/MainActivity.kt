package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ginggong.ui.screens.ChartsScreen
import com.example.ginggong.ui.screens.FlameGamificationScreen
import com.example.ginggong.ui.screens.FoodDiaryScreen
import com.example.ginggong.ui.screens.HomeScreen
import com.example.ginggong.ui.screens.OnboardingScreen
import com.example.ginggong.ui.screens.SettingsScreen
import com.example.ginggong.ui.screens.WaterTrackerScreen
import com.example.ginggong.ui.screens.WeeklyCheckScreen
import com.example.ginggong.ui.viewmodel.MainViewModel
import com.example.ui.theme.GinggongTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
            val flameState by viewModel.flameState.collectAsStateWithLifecycle()
            val gamificationState by viewModel.gamificationState.collectAsStateWithLifecycle()
            val waterLogs by viewModel.waterLogsToday.collectAsStateWithLifecycle()
            val totalWaterMl by viewModel.totalWaterTodayMl.collectAsStateWithLifecycle()
            val foodLogs by viewModel.foodLogsToday.collectAsStateWithLifecycle()
            val bodyLogs by viewModel.bodyLogs.collectAsStateWithLifecycle()
            val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
            val scanState by viewModel.scanState.collectAsStateWithLifecycle()
            val showCelebration by viewModel.showCelebration.collectAsStateWithLifecycle()
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
            val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()

            GinggongTheme(darkTheme = isDarkMode) {
                if (!userProfile.isSetupCompleted) {
                    OnboardingScreen(
                        onComplete = { name, age, gender, heightCm, weightKg, birthday, activityLevel, authMode ->
                            viewModel.completeOnboarding(
                                name, age, gender, heightCm, weightKg, birthday, activityLevel, authMode
                            )
                        }
                    )
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            NavigationBar(
                                containerColor = Color.White,
                                contentColor = Color(0xFF0066FF)
                            ) {
                                val navItems = listOf(
                                    Triple(0, "Home", Icons.Default.Home),
                                    Triple(1, "Water", Icons.Default.WaterDrop),
                                    Triple(2, "Food AI", Icons.Default.AutoAwesome),
                                    Triple(3, "Charts", Icons.Default.BarChart),
                                    Triple(4, "Flame", Icons.Default.LocalFireDepartment),
                                    Triple(5, "Settings", Icons.Default.Settings)
                                )

                                navItems.forEach { (index, label, icon) ->
                                    NavigationBarItem(
                                        selected = selectedTab == index,
                                        onClick = { viewModel.setSelectedTab(index) },
                                        icon = { Icon(imageVector = icon, contentDescription = label) },
                                        label = {
                                            Text(
                                                text = label,
                                                fontSize = 10.sp,
                                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = Color(0xFF0066FF),
                                            selectedTextColor = Color(0xFF0066FF),
                                            indicatorColor = Color(0xFFE0EDFF)
                                        )
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Modifier.padding(innerPadding)
                        when (selectedTab) {
                            0 -> HomeScreen(
                                userProfile = userProfile,
                                flameState = flameState,
                                gamificationState = gamificationState,
                                waterIntakeMl = totalWaterMl,
                                foodLogs = foodLogs,
                                showCelebration = showCelebration,
                                onAddWater = { viewModel.addWater(it) },
                                onDismissCelebration = { viewModel.dismissCelebration() },
                                onNavigateToTab = { viewModel.setSelectedTab(it) }
                            )
                            1 -> WaterTrackerScreen(
                                currentWaterMl = totalWaterMl,
                                goalWaterMl = userProfile.dailyWaterGoalMl,
                                waterLogs = waterLogs,
                                onAddWater = { viewModel.addWater(it) },
                                onDeleteLog = { viewModel.deleteWaterLog(it) }
                            )
                            2 -> FoodDiaryScreen(
                                foodLogs = foodLogs,
                                scanState = scanState,
                                onAddFoodLog = { meal, title, cal, pro, carb, fat, sat, fib, sug, sod, serv ->
                                    viewModel.addFoodLog(meal, title, cal, pro, carb, fat, sat, fib, sug, sod, serv)
                                },
                                onDeleteFoodLog = { viewModel.deleteFoodLog(it) },
                                onAnalyzeFoodPhoto = { viewModel.analyzeFoodPhoto(it) },
                                onAnalyzeNutritionLabelPhoto = { viewModel.analyzeNutritionLabelPhoto(it) },
                                onLookupBarcode = { viewModel.lookupBarcode(it) },
                                onResetScanState = { viewModel.resetScanState() }
                            )
                            3 -> ChartsScreen(
                                waterLogs = waterLogs,
                                foodLogs = foodLogs,
                                bodyLogs = bodyLogs
                            )
                            4 -> FlameGamificationScreen(
                                flameState = flameState,
                                gamificationState = gamificationState,
                                onRecoverFlame = { viewModel.recoverFlame() }
                            )
                            5 -> SettingsScreen(
                                userProfile = userProfile,
                                isDarkMode = isDarkMode,
                                appLanguage = appLanguage,
                                onToggleDarkMode = { viewModel.toggleDarkMode() },
                                onSetLanguage = { viewModel.setLanguage(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}
