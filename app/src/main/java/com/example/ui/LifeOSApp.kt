package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// --- MAIN LIFEOS APPLICATION COMPOSABLE ---

@Composable
fun LifeOSApp(viewModel: MainActivityViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val showSettings by viewModel.showSettings.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpaceDark)
            // Beautiful mesh gradients represented behind the entire application
            .drawBehind {
                // Top-Left Indigo Blur
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(GradientIndigo.copy(alpha = 0.25f), Color.Transparent),
                        center = Offset(0f, 0f),
                        radius = size.width * 0.8f
                    )
                )
                // Bottom-Right Emerald Blur
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(GradientTeal.copy(alpha = 0.15f), Color.Transparent),
                        center = Offset(size.width, size.height),
                        radius = size.width * 0.8f
                    )
                )
            }
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        if (!isLoggedIn) {
            AuthScreen(viewModel = viewModel)
        } else if (!profile.isOnboarded) {
            OnboardingScreen(
                onComplete = { name, college, dept, sem, grad, goal, hours ->
                    viewModel.completeOnboarding(name, college, dept, sem, grad, goal, hours)
                }
            )
        } else {
            val isKeyboardVisible = WindowInsets.isImeVisible

            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header (Greeting & Level) - Hide on AI screen when keyboard is open to maximize typing space
                if (!isKeyboardVisible || currentTab != TabScreen.AI) {
                    HeaderSection(viewModel = viewModel)
                }

                // Scrollable content area
                Box(modifier = Modifier.weight(1f)) {
                    when (currentTab) {
                        TabScreen.Home -> DashboardScreen(viewModel = viewModel)
                        TabScreen.Study -> StudyPlannerScreen(viewModel = viewModel)
                        TabScreen.Planner -> PlannerScreen(viewModel = viewModel)
                        TabScreen.AI -> AIScreen(viewModel = viewModel)
                        TabScreen.ProfileTools -> ProfileToolsScreen(viewModel = viewModel)
                    }
                }

                // Frosted glass bottom navigation - Hide when keyboard is open
                if (!isKeyboardVisible) {
                    BottomNavigationBar(viewModel = viewModel)
                }
            }
        }

        // Global Settings Overlay
        if (showSettings) {
            SettingsDialog(viewModel = viewModel)
        }
    }
}

// --- REUSABLE COMPONENT: GLASS CARD ---

@Composable
fun FrostedGlassCard(
    modifier: Modifier = Modifier,
    borderBrush: Brush = Brush.linearGradient(
        colors = listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.05f))
    ),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .background(GlassCardBackground, RoundedCornerShape(24.dp))
            .border(1.dp, borderBrush, RoundedCornerShape(24.dp))
            .padding(16.dp),
        content = content
    )
}

// --- HEADER SECTION ---

@Composable
fun HeaderSection(viewModel: MainActivityViewModel) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "LifeOS • Fall Semester",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Text(
                text = "Hi, ${profile.name}. ✦",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Settings Button (Trigger Settings Dialog globally)
            IconButton(
                onClick = { viewModel.setShowSettings(true) },
                modifier = Modifier
                    .size(44.dp)
                    .background(GlassCardBackground, RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Premium branded logo badge on the right
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(GlassCardBackground, RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { viewModel.setTab(TabScreen.ProfileTools) },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = com.example.R.drawable.img_logo_1784367604495),
                    contentDescription = "LifeOS Logo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

// --- GLOBAL SETTINGS DIALOG (SOLID OPAQUE DARK BACKGROUND) ---

@Composable
fun SettingsDialog(viewModel: MainActivityViewModel) {
    var selectedTheme by remember { mutableStateOf("Deep Space Dark") }
    var selectedLang by remember { mutableStateOf("English") }
    var isCheckingUpdates by remember { mutableStateOf(false) }
    var updateChecked by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { viewModel.setShowSettings(false) },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = GradientTeal)
                Text("System Settings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                // APP THEME SELECTOR
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("App Theme", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    listOf("Deep Space Dark", "Midnight Nebula", "Cyberpunk Purple").forEach { theme ->
                        val isSel = selectedTheme == theme
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isSel) Color.White.copy(alpha = 0.05f) else Color.Transparent, RoundedCornerShape(10.dp))
                                .border(1.dp, if (isSel) GradientIndigo else Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                                .clickable { selectedTheme = theme }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(theme, color = Color.White, fontSize = 13.sp)
                            RadioButton(
                                selected = isSel,
                                onClick = { selectedTheme = theme },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = GradientIndigo,
                                    unselectedColor = Color.White.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

                // LANGUAGE SELECTOR
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("App Language", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    listOf("English", "हिन्दी (Hindi)", "Español").forEach { lang ->
                        val isSel = selectedLang == lang
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isSel) Color.White.copy(alpha = 0.05f) else Color.Transparent, RoundedCornerShape(10.dp))
                                .border(1.dp, if (isSel) GradientIndigo else Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                                .clickable { selectedLang = lang }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(lang, color = Color.White, fontSize = 13.sp)
                            RadioButton(
                                selected = isSel,
                                onClick = { selectedLang = lang },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = GradientIndigo,
                                    unselectedColor = Color.White.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

                // APP UPDATE SECTION
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("App Version & Update", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(12.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Current Version", color = TextSecondary, fontSize = 11.sp)
                                    Text("v2.4.0 (Stable)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                
                                Button(
                                    onClick = {
                                        if (!isCheckingUpdates && !updateChecked) {
                                            isCheckingUpdates = true
                                            scope.launch {
                                                delay(1200)
                                                isCheckingUpdates = false
                                                updateChecked = true
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (updateChecked) Color.Transparent else GradientTeal),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.height(36.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                    enabled = !isCheckingUpdates && !updateChecked
                                ) {
                                    if (isCheckingUpdates) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = DeepSpaceDark, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Checking...", color = DeepSpaceDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    } else if (updateChecked) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = GradientTeal, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Up to Date", color = GradientTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    } else {
                                        Text("Check Update", color = DeepSpaceDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            
                            if (updateChecked) {
                                Text(
                                    "✦ Congratulations! You are already using the newest version of LifeOS.",
                                    color = GradientTeal,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 8.dp),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.setShowSettings(false) },
                colors = ButtonDefaults.buttonColors(containerColor = GradientTeal),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Done", color = DeepSpaceDark, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = DeepSpaceDark, // Solid Opaque Dark Background
        modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(28.dp))
    )
}

// --- ONBOARDING SCREEN ---

@Composable
fun OnboardingScreen(onComplete: (String, String, String, Int, Int, String, Int) -> Unit) {
    var step by remember { mutableStateOf(1) }
    var name by remember { mutableStateOf("") }
    var college by remember { mutableStateOf("") }
    var dept by remember { mutableStateOf("") }
    var semester by remember { mutableStateOf(1) }
    var gradYear by remember { mutableStateOf(2026) }
    var careerGoal by remember { mutableStateOf("") }
    var studyHours by remember { mutableStateOf(6) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(GlassCardBackground, RoundedCornerShape(32.dp))
                .border(
                    1.dp,
                    Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = 0.2f), Color.White.copy(alpha = 0.05f))
                    ),
                    RoundedCornerShape(32.dp)
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(4) { idx ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .background(
                                if (step > idx) GradientIndigo else Color.White.copy(alpha = 0.1f),
                                RoundedCornerShape(2.dp)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (step) {
                1 -> {
                    Text(
                        text = "Initialize LifeOS ✦",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Enter your primary name and academy credentials.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("What should we call you?") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = GradientIndigo,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("onboarding_name")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = college,
                        onValueChange = { college = it },
                        label = { Text("College / University") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = GradientIndigo,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("onboarding_college")
                    )
                }
                2 -> {
                    Text(
                        text = "Academic Branch",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Select your department and current semester progress.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                    )

                    OutlinedTextField(
                        value = dept,
                        onValueChange = { dept = it },
                        label = { Text("Major / Department") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = GradientIndigo,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("onboarding_dept")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = semester.toString(),
                            onValueChange = { semester = it.toIntOrNull() ?: 1 },
                            label = { Text("Semester") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = GradientIndigo,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.weight(1f).testTag("onboarding_semester")
                        )
                        OutlinedTextField(
                            value = gradYear.toString(),
                            onValueChange = { gradYear = it.toIntOrNull() ?: 2026 },
                            label = { Text("Graduation Year") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = GradientIndigo,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.weight(1f).testTag("onboarding_grad")
                        )
                    }
                }
                3 -> {
                    Text(
                        text = "Career Alignment",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "What is your main career aspiration?",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                    )

                    OutlinedTextField(
                        value = careerGoal,
                        onValueChange = { careerGoal = it },
                        label = { Text("Career Goal (e.g. Android Lead)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = GradientIndigo,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("onboarding_career")
                    )
                }
                4 -> {
                    Text(
                        text = "Set Goals",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "How many study hours do you target daily?",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = { studyHours = (studyHours - 1).coerceAtLeast(1) }) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = Color.White)
                        }
                        Text(
                            text = "$studyHours Hours",
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        IconButton(onClick = { studyHours = (studyHours + 1).coerceAtMost(24) }) {
                            Icon(Icons.Default.Add, contentDescription = "Increase", tint = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (step > 1) {
                    TextButton(onClick = { step-- }) {
                        Text("Back", color = Color.White)
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }

                Button(
                    onClick = {
                        if (step < 4) {
                            step++
                        } else {
                            onComplete(
                                name.ifBlank { "Alex" },
                                college.ifBlank { "Stanford University" },
                                dept.ifBlank { "Computer Science" },
                                semester,
                                gradYear,
                                careerGoal.ifBlank { "Senior Software Architect" },
                                studyHours
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GradientIndigo),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("onboarding_next")
                ) {
                    Text(if (step == 4) "Activate ✦" else "Next", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// --- TAB 1: HOME (DASHBOARD) ---

@Composable
fun DashboardScreen(viewModel: MainActivityViewModel) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val timetable by viewModel.timetable.collectAsStateWithLifecycle()
    val assignments by viewModel.assignments.collectAsStateWithLifecycle()
    val habits by viewModel.habits.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Stats/Gamification Quick Info
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Streak Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(GlassCardBackground, RoundedCornerShape(20.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                        .clickable { viewModel.setTab(TabScreen.ProfileTools) }
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🔥", fontSize = 24.sp)
                        Column {
                            Text("STREAK", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("${stats.streak} Days", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Level Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(GlassCardBackground, RoundedCornerShape(20.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                        .clickable { viewModel.setTab(TabScreen.ProfileTools) }
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("⭐", fontSize = 24.sp)
                        Column {
                            Text("ACADEMIC", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("${stats.academicScore} CGPA", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // AI Insight Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(listOf(GradientIndigo.copy(alpha = 0.3f), GradientPurple.copy(alpha = 0.1f))),
                        RoundedCornerShape(24.dp)
                    )
                    .border(1.dp, GradientIndigo.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("AI INSIGHT", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("✦", color = GradientTeal, fontSize = 14.sp)
                    }

                    Text(
                        text = "\"You have OS Lab 3 due in 4 hours and are currently at 70% attendance. Complete the assignment now to safeguard your record!\"",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Button(
                        onClick = { viewModel.setTab(TabScreen.AI) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Ask AI for Advice", color = DeepSpaceDark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // Today's Classes List
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("TODAY'S CLASSES", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text(
                        "View Timetable",
                        color = GradientIndigo,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { viewModel.setTab(TabScreen.Planner) }
                    )
                }

                val classesToday = timetable.take(2)
                if (classesToday.isEmpty()) {
                    Text("No classes scheduled today! 🎉", color = TextSecondary, fontSize = 13.sp)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        classesToday.forEach { c ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(GlassCardBackground, RoundedCornerShape(16.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                                    .clickable {
                                        viewModel.sendChatMessage("Help me prepare for my lecture in ${c.subject}! What are the key topics or concepts I should review?")
                                    }
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(GradientIndigo.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("🧬", fontSize = 18.sp)
                                    }
                                    Column {
                                        Text(c.subject, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("${c.room} • ${c.time}", color = TextSecondary, fontSize = 11.sp)
                                    }
                                }
                                Text(
                                    "80%",
                                    color = GradientTeal,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .background(GradientTeal.copy(alpha = 0.1f), CircleShape)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Habits Checklist Quick section
        item {
            var showAddHabitDialog by remember { mutableStateOf(false) }

            if (showAddHabitDialog) {
                var habitName by remember { mutableStateOf("") }
                var habitIcon by remember { mutableStateOf("💧") }

                AlertDialog(
                    onDismissRequest = { showAddHabitDialog = false },
                    title = { Text("Add Custom Daily Habit", color = Color.White, fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = habitName,
                                onValueChange = { habitName = it },
                                label = { Text("Habit Name (e.g. Study 1 hour)") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = GradientIndigo,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text("Select Emoji Icon:", color = TextSecondary, fontSize = 12.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                listOf("💧", "💻", "📖", "🏋️", "🧘", "🍏").forEach { emoji ->
                                    val isSel = habitIcon == emoji
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(if (isSel) GradientIndigo else Color.White.copy(alpha = 0.05f), CircleShape)
                                            .clickable { habitIcon = emoji },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(emoji, fontSize = 20.sp)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (habitName.isNotBlank()) {
                                    viewModel.addHabit(habitName, habitIcon)
                                    showAddHabitDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GradientTeal)
                        ) {
                            Text("Create Habit", color = DeepSpaceDark, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddHabitDialog = false }) {
                            Text("Cancel", color = TextSecondary)
                        }
                    },
                    containerColor = DeepSpaceDark,
                    modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(28.dp))
                )
            }

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HABIT TRACKER",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "+ Add Habit",
                        color = GradientIndigo,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { showAddHabitDialog = true }
                    )
                }

                if (habits.isEmpty()) {
                    Text("No habits tracked yet. Click '+ Add Habit' to start!", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp))
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        habits.forEach { habit ->
                            val completed = habit.isCompletedToday
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (completed) GradientTeal.copy(alpha = 0.2f) else GlassCardBackground,
                                        RoundedCornerShape(16.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (completed) GradientTeal.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable { viewModel.toggleHabit(habit.name) }
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(habit.icon, fontSize = 20.sp)
                                    Text(
                                        habit.name.split(" ")[0],
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                    Text(
                                        if (completed) "Done 🎉" else "Track",
                                        color = if (completed) GradientTeal else TextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- TAB 2: STUDY (POMODORO & STUDY PLANNER) ---

@Composable
fun StudyPlannerScreen(viewModel: MainActivityViewModel) {
    val seconds by viewModel.pomodoroSeconds.collectAsStateWithLifecycle()
    val active by viewModel.pomodoroActive.collectAsStateWithLifecycle()
    val isWork by viewModel.pomodoroIsWork.collectAsStateWithLifecycle()
    val totalSeconds by viewModel.pomodoroTotalSeconds.collectAsStateWithLifecycle()
    val studyPlans by viewModel.studyPlans.collectAsStateWithLifecycle()

    var subjectText by remember { mutableStateOf("") }
    var taskText by remember { mutableStateOf("") }
    var planDays by remember { mutableStateOf(5) }

    var showAddStudyPlanDialog by remember { mutableStateOf(false) }

    if (showAddStudyPlanDialog) {
        var subjectName by remember { mutableStateOf("") }
        var planDescription by remember { mutableStateOf("") }
        var priority by remember { mutableStateOf("Medium") }

        AlertDialog(
            onDismissRequest = { showAddStudyPlanDialog = false },
            title = { Text("Add Revision Objective", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = subjectName,
                        onValueChange = { subjectName = it },
                        label = { Text("Subject Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = GradientIndigo,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = planDescription,
                        onValueChange = { planDescription = it },
                        label = { Text("Objective Description") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = GradientIndigo,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Priority:", color = TextSecondary, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("High", "Medium", "Low").forEach { p ->
                            val isSel = priority == p
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isSel) GradientIndigo else Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                    .clickable { priority = p }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(p, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (planDescription.isNotBlank()) {
                            viewModel.addManualStudyPlan(subjectName, planDescription, priority)
                            showAddStudyPlanDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GradientTeal)
                ) {
                    Text("Add Objective", color = DeepSpaceDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStudyPlanDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DeepSpaceDark,
            modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(28.dp))
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Title
        item {
            Text("Smart Study & Pomodoro ⏲️", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        // Pomodoro Clock card
        item {
            FrostedGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isWork) "WORK SESSION" else "SHORT BREAK",
                        color = if (isWork) GradientIndigo else GradientTeal,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val minutes = seconds / 60
                    val remainingSecs = seconds % 60
                    val timeString = String.format("%02d:%02d", minutes, remainingSecs)

                    Text(
                        text = timeString,
                        color = Color.White,
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Circular countdown bar representation
                    LinearProgressIndicator(
                        progress = { seconds.toFloat() / (if (isWork) totalSeconds.toFloat() else 5 * 60f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = if (isWork) GradientIndigo else GradientTeal,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "SESSION DURATION",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        listOf(25, 30, 45, 60).forEach { mins ->
                            val isSelected = (totalSeconds / 60) == mins
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) GradientIndigo else Color.White.copy(alpha = 0.05f)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        viewModel.setPomodoroDuration(mins)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (mins == 60) "1 Hour" else "$mins Min",
                                    color = if (isSelected) Color.White else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(
                            onClick = {
                                if (active) viewModel.pausePomodoro() else viewModel.startPomodoro()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (active) Color.White.copy(alpha = 0.2f) else GradientIndigo),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (active) "Pause" else "Start Timer", color = Color.White)
                        }

                        OutlinedButton(
                            onClick = { viewModel.resetPomodoro() },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                        ) {
                            Text("Reset", color = Color.White)
                        }
                    }
                }
            }
        }

        // AI-Generated Study Plan Planner
        item {
            FrostedGlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("AI Study Plan Architect ✦", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    "Let Gemini architect a day-by-day customized study plan and automatically add the tasks below.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                OutlinedTextField(
                    value = subjectText,
                    onValueChange = { subjectText = it },
                    label = { Text("Enter Syllabus Subject") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = GradientIndigo,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("ai_subject_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Duration: $planDays Days", color = Color.White, fontSize = 13.sp)
                    Slider(
                        value = planDays.toFloat(),
                        onValueChange = { planDays = it.roundToInt() },
                        valueRange = 3f..14f,
                        steps = 10,
                        colors = SliderDefaults.colors(
                            thumbColor = GradientIndigo,
                            activeTrackColor = GradientIndigo,
                            inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier.width(180.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.generateStudyPlanWithAI(subjectText, planDays)
                        subjectText = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GradientTeal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Architect with Gemini AI", color = DeepSpaceDark, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Personal Goal / Target Tracker Checklist
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("My Revision Objectives", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                TextButton(
                    onClick = { showAddStudyPlanDialog = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = GradientTeal)
                ) {
                    Text("+ Add Objective")
                }
            }
        }

        items(studyPlans) { plan ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GlassCardBackground, RoundedCornerShape(16.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                    .clickable { viewModel.toggleStudyPlan(plan.id) }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = plan.isCompleted,
                        onCheckedChange = { viewModel.toggleStudyPlan(plan.id) },
                        colors = CheckboxDefaults.colors(checkedColor = GradientTeal)
                    )
                    Column {
                        Text(
                            text = plan.task,
                            color = if (plan.isCompleted) TextSecondary else Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(plan.subject, color = TextSecondary, fontSize = 11.sp)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (plan.priority == "High") Color.Red.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.1f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            plan.priority,
                            color = if (plan.priority == "High") Color.Red else Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = { viewModel.deleteStudyPlan(plan.id) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Objective",
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// --- TAB 3: PLANNER (WEEKLY TIMETABLE & ASSIGNMENTS) ---

@Composable
fun PlannerScreen(viewModel: MainActivityViewModel) {
    val timetable by viewModel.timetable.collectAsStateWithLifecycle()
    val assignments by viewModel.assignments.collectAsStateWithLifecycle()
    val exams by viewModel.examPlans.collectAsStateWithLifecycle()

    var activeSubTab by remember { mutableStateOf(0) } // 0: Timetable, 1: Assignments, 2: Exams

    var showAddClassDialog by remember { mutableStateOf(false) }
    var showAddAssignmentDialog by remember { mutableStateOf(false) }
    var showAddExamDialog by remember { mutableStateOf(false) }

    if (showAddClassDialog) {
        var subjectName by remember { mutableStateOf("") }
        var roomName by remember { mutableStateOf("") }
        var classTime by remember { mutableStateOf("10:30 AM") }
        var daySelected by remember { mutableStateOf("Mon") }
        val colorHexSelected = "#10B981"

        AlertDialog(
            onDismissRequest = { showAddClassDialog = false },
            title = { Text("Add Lecture to Timetable", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = subjectName,
                        onValueChange = { subjectName = it },
                        label = { Text("Subject Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = GradientIndigo,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = roomName,
                        onValueChange = { roomName = it },
                        label = { Text("Room / Location") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = GradientIndigo,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = classTime,
                        onValueChange = { classTime = it },
                        label = { Text("Time (e.g., 10:30 AM)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = GradientIndigo,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Day of Week:", color = TextSecondary, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Mon", "Tue", "Wed", "Thu", "Fri").forEach { day ->
                            val isSel = daySelected == day
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isSel) GradientIndigo else Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                    .clickable { daySelected = day }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(day, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (subjectName.isNotBlank()) {
                            viewModel.addTimetableClass(subjectName, roomName, classTime, daySelected, colorHexSelected)
                            showAddClassDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GradientTeal)
                ) {
                    Text("Add Lecture", color = DeepSpaceDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddClassDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DeepSpaceDark,
            modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(28.dp))
        )
    }

    if (showAddAssignmentDialog) {
        var title by remember { mutableStateOf("") }
        var subjectName by remember { mutableStateOf("") }
        var deadline by remember { mutableStateOf("Due in 2 days") }
        var priority by remember { mutableStateOf("High") }

        AlertDialog(
            onDismissRequest = { showAddAssignmentDialog = false },
            title = { Text("Create Academic Task", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Task Title (e.g., Lab 3 Report)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = GradientIndigo,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = subjectName,
                        onValueChange = { subjectName = it },
                        label = { Text("Subject / Course") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = GradientIndigo,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = deadline,
                        onValueChange = { deadline = it },
                        label = { Text("Deadline (e.g., Friday 5 PM)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = GradientIndigo,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Priority:", color = TextSecondary, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("High", "Medium", "Low").forEach { p ->
                            val isSel = priority == p
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isSel) GradientIndigo else Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                    .clickable { priority = p }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(p, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            viewModel.addAssignment(title, subjectName, deadline, priority)
                            showAddAssignmentDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GradientTeal)
                ) {
                    Text("Create Task", color = DeepSpaceDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAssignmentDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DeepSpaceDark,
            modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(28.dp))
        )
    }

    if (showAddExamDialog) {
        var subjectName by remember { mutableStateOf("") }
        var syllabusText by remember { mutableStateOf("") }
        var countdownDays by remember { mutableStateOf(7) }

        AlertDialog(
            onDismissRequest = { showAddExamDialog = false },
            title = { Text("Add Exam Schedule", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = subjectName,
                        onValueChange = { subjectName = it },
                        label = { Text("Subject / Course Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = GradientIndigo,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = syllabusText,
                        onValueChange = { syllabusText = it },
                        label = { Text("Syllabus / Key Topics") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = GradientIndigo,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Days Until Exam: $countdownDays", color = Color.White, fontSize = 13.sp)
                        Slider(
                            value = countdownDays.toFloat(),
                            onValueChange = { countdownDays = it.roundToInt() },
                            valueRange = 1f..30f,
                            steps = 30,
                            colors = SliderDefaults.colors(
                                thumbColor = GradientIndigo,
                                activeTrackColor = GradientIndigo,
                                inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier.width(140.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (subjectName.isNotBlank()) {
                            viewModel.addExamPlan(subjectName, countdownDays, syllabusText, "High", 0)
                            showAddExamDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GradientTeal)
                ) {
                    Text("Add Exam", color = DeepSpaceDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddExamDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DeepSpaceDark,
            modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(28.dp))
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Sub tabs
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                    .padding(4.dp)
            ) {
                listOf("Timetable", "Assignments", "Exams").forEachIndexed { idx, label ->
                    val selected = activeSubTab == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (selected) GradientIndigo else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { activeSubTab = idx }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (selected) Color.White else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        when (activeSubTab) {
            0 -> { // TIMETABLE
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Weekly Lectures", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        TextButton(
                            onClick = { showAddClassDialog = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = GradientTeal)
                        ) {
                            Text("+ Add Class")
                        }
                    }
                }

                items(timetable) { slot ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GlassCardBackground, RoundedCornerShape(16.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                            .clickable {
                                viewModel.sendChatMessage("Give me a quick high-yield revision list for ${slot.subject}! What should I focus on for this course?")
                            }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color(android.graphics.Color.parseColor(slot.colorHex)), CircleShape)
                            )
                            Column {
                                Text(slot.subject, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("${slot.room} • ${slot.time}", color = TextSecondary, fontSize = 11.sp)
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                slot.dayOfWeek.uppercase(),
                                color = GradientIndigo,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                modifier = Modifier
                                    .background(GradientIndigo.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )

                            IconButton(
                                onClick = { viewModel.deleteTimetableClass(slot.subject, slot.time, slot.dayOfWeek) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Class Slot",
                                    tint = Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
            1 -> { // ASSIGNMENTS
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Assignments checklist", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        TextButton(
                            onClick = { showAddAssignmentDialog = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = GradientTeal)
                        ) {
                            Text("+ Add Task")
                        }
                    }
                }

                items(assignments) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GlassCardBackground, RoundedCornerShape(16.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                            .clickable { viewModel.toggleAssignment(item.id) }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = item.isSubmitted,
                                onCheckedChange = { viewModel.toggleAssignment(item.id) },
                                colors = CheckboxDefaults.colors(checkedColor = GradientTeal)
                            )
                            Column {
                                Text(
                                    text = item.title,
                                    color = if (item.isSubmitted) TextSecondary else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text("${item.subject} • ${item.deadline}", color = TextSecondary, fontSize = 11.sp)
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (item.priority == "High") Color.Red.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.1f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    item.priority,
                                    color = if (item.priority == "High") Color.Red else Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            IconButton(
                                onClick = { viewModel.deleteAssignment(item.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Assignment",
                                    tint = Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
            2 -> { // EXAMS
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Revision Countdown 🗓️", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        TextButton(
                            onClick = { showAddExamDialog = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = GradientTeal)
                        ) {
                            Text("+ Add Exam")
                        }
                    }
                }

                items(exams) { exam ->
                    FrostedGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.sendChatMessage("Help me design a custom daily study plan and preparation timeline for my upcoming exam in '${exam.subject}'. The syllabus is: ${exam.syllabus}. There are only ${exam.countdownDays} days left!")
                            }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(exam.subject, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(
                                    "Syllabus: ${exam.syllabus}",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                                )

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    LinearProgressIndicator(
                                        progress = { exam.preparationProgress / 100f },
                                        modifier = Modifier
                                            .width(100.dp)
                                            .height(4.dp)
                                            .clip(CircleShape),
                                        color = GradientTeal,
                                        trackColor = Color.White.copy(alpha = 0.1f)
                                    )
                                    Text("Prep: ${exam.preparationProgress}%", color = Color.White, fontSize = 10.sp)
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "${exam.countdownDays}",
                                    color = if (exam.countdownDays <= 3) Color.Red else Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text("DAYS LEFT", color = TextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold)

                                Spacer(modifier = Modifier.height(4.dp))

                                IconButton(
                                    onClick = { viewModel.deleteExamPlan(exam.subject) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Exam",
                                        tint = Color.Red.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- TAB 4: AI CHAT ASSISTANT (GEMINI) ---

@Composable
fun AIScreen(viewModel: MainActivityViewModel) {
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val loading by viewModel.isChatLoading.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val selectedNote by viewModel.selectedNote.collectAsStateWithLifecycle()

    val quizQuestion by viewModel.activeQuizQuestion.collectAsStateWithLifecycle()
    val quizAnswers by viewModel.activeQuizAnswers.collectAsStateWithLifecycle()
    val quizFeedback by viewModel.activeQuizFeedback.collectAsStateWithLifecycle()

    var chatInput by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Auto-scroll to bottom when keyboard opens, loading state changes, or new messages arrive
    LaunchedEffect(messages.size, loading) {
        if (messages.isNotEmpty()) {
            delay(100)
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        // Chat Header with Delete Option
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Ask Anything LifeOS ✦",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            TextButton(
                onClick = { viewModel.clearChatHistory() },
                colors = ButtonDefaults.textButtonColors(contentColor = Color.Red.copy(alpha = 0.8f))
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Chat",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Delete Chat", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // Message Scrollable Container
        Box(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                messages.forEach { msg ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 280.dp)
                                .background(
                                    if (msg.isUser) GradientIndigo else GlassCardBackground,
                                    RoundedCornerShape(
                                        topStart = 20.dp,
                                        topEnd = 20.dp,
                                        bottomStart = if (msg.isUser) 20.dp else 4.dp,
                                        bottomEnd = if (msg.isUser) 4.dp else 20.dp
                                    )
                                )
                                .border(
                                    1.dp,
                                    if (msg.isUser) GradientIndigo else Color.White.copy(alpha = 0.1f),
                                    RoundedCornerShape(
                                        topStart = 20.dp,
                                        topEnd = 20.dp,
                                        bottomStart = if (msg.isUser) 20.dp else 4.dp,
                                        bottomEnd = if (msg.isUser) 4.dp else 20.dp
                                    )
                                )
                                .padding(14.dp)
                        ) {
                            Text(
                                text = msg.content,
                                color = Color.White,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }

                if (loading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .background(GlassCardBackground, RoundedCornerShape(20.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text("Gemini is composing... ✦", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Inline Quiz Question Player if loaded
                if (quizQuestion.isNotEmpty()) {
                    FrostedGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text("QUIZ GENERATOR", color = GradientTeal, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = quizQuestion,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            quizAnswers.forEachIndexed { idx, opt ->
                                OutlinedButton(
                                    onClick = { viewModel.answerQuiz(idx) },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(opt, textAlign = TextAlign.Left, modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }

                        if (quizFeedback.isNotEmpty()) {
                            Text(
                                text = quizFeedback,
                                color = if (quizFeedback.startsWith("Correct")) GradientTeal else Color.Red,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }
                    }
                }
            }
        }

        // Horizontal Quick Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(GlassCardBackground, RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .clickable { viewModel.optimizeResume() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text("ATS Resume Optimizer 📑", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            if (notes.isNotEmpty()) {
                val firstNote = notes.first()
                Box(
                    modifier = Modifier
                        .background(GlassCardBackground, RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .clickable {
                            viewModel.selectNoteForAI(firstNote)
                            viewModel.summarizeSelectedNote()
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("Summarize: ${firstNote.title} 📝", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Box(
                    modifier = Modifier
                        .background(GlassCardBackground, RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .clickable {
                            viewModel.selectNoteForAI(firstNote)
                            viewModel.generateQuizFromNote()
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("Generate Quiz 🧠", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Chat Input box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = chatInput,
                onValueChange = { chatInput = it },
                placeholder = { Text("Ask LifeOS anything...") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = GradientIndigo,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                    focusedContainerColor = GlassCardBackground,
                    unfocusedContainerColor = GlassCardBackground
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.weight(1f).testTag("chat_input")
            )

            IconButton(
                onClick = {
                    if (chatInput.trim().isNotEmpty()) {
                        viewModel.sendChatMessage(chatInput)
                        chatInput = ""
                        scope.launch {
                            delay(200)
                            scrollState.animateScrollTo(scrollState.maxValue)
                        }
                    }
                },
                modifier = Modifier
                    .background(GradientIndigo, CircleShape)
                    .size(48.dp)
                    .testTag("chat_send_button")
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
            }
        }
    }
}

// --- TAB 5: PROFILE & STUDENT TOOLS (ATTENDANCE, CGPA, PLACEMENT HUB) ---

@Composable
fun ProfileToolsScreen(viewModel: MainActivityViewModel) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val attendanceList by viewModel.attendanceList.collectAsStateWithLifecycle()
    val placements by viewModel.placements.collectAsStateWithLifecycle()

    var activeSubTab by remember { mutableStateOf(0) } // 0: Attendance, 1: CGPA, 2: Career/Placements

    var showAddSubjectDialog by remember { mutableStateOf(false) }
    var showAddPlacementDialog by remember { mutableStateOf(false) }

    if (showAddSubjectDialog) {
        var subjectName by remember { mutableStateOf("") }
        var facultyName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddSubjectDialog = false },
            title = { Text("Add Course / Subject", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = subjectName,
                        onValueChange = { subjectName = it },
                        label = { Text("Subject Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = GradientIndigo,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = facultyName,
                        onValueChange = { facultyName = it },
                        label = { Text("Faculty / Professor Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = GradientIndigo,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (subjectName.isNotBlank()) {
                            viewModel.addNewSubject(subjectName, facultyName)
                            showAddSubjectDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GradientTeal)
                ) {
                    Text("Add Course", color = DeepSpaceDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSubjectDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DeepSpaceDark,
            modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(28.dp))
        )
    }

    if (showAddPlacementDialog) {
        var companyName by remember { mutableStateOf("") }
        var roleName by remember { mutableStateOf("") }
        var appStatus by remember { mutableStateOf("Applied") }
        var timelineText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddPlacementDialog = false },
            title = { Text("Add Placement Application", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = companyName,
                        onValueChange = { companyName = it },
                        label = { Text("Company Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = GradientIndigo,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = roleName,
                        onValueChange = { roleName = it },
                        label = { Text("Role (e.g., Software Eng Intern)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = GradientIndigo,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = timelineText,
                        onValueChange = { timelineText = it },
                        label = { Text("Timeline (e.g., July 2026)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = GradientIndigo,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Application Status:", color = TextSecondary, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Applied", "Interview Scheduled", "Offer Received").forEach { state ->
                            val isSel = appStatus == state
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isSel) GradientIndigo else Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                    .clickable { appStatus = state }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(state, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (companyName.isNotBlank()) {
                            viewModel.addPlacement(companyName, roleName, appStatus, timelineText)
                            showAddPlacementDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GradientTeal)
                ) {
                    Text("Add Tracker", color = DeepSpaceDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPlacementDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DeepSpaceDark,
            modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(28.dp))
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Academic Profile Card & Reset Control
        item {
            var showConfirmReset by remember { mutableStateOf(false) }
            var showEditProfileDialog by remember { mutableStateOf(false) }
            
            FrostedGlassCard(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 36.dp), // Space for edit icon
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(GradientIndigo.copy(alpha = 0.15f), CircleShape)
                                .border(2.0.dp, GradientIndigo, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🎓", fontSize = 28.sp)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile.name,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "${profile.college} • Sem ${profile.semester}",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Major: ${profile.department}",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Target: ${profile.careerGoal}",
                                color = GradientTeal,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    // Edit Icon button on top-right of profile card
                    IconButton(
                        onClick = { showEditProfileDialog = true },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Profile",
                            tint = GradientTeal,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Edit Profile Dialog
                if (showEditProfileDialog) {
                    var editName by remember { mutableStateOf(profile.name) }
                    var editCollege by remember { mutableStateOf(profile.college) }
                    var editDept by remember { mutableStateOf(profile.department) }
                    var editSem by remember { mutableStateOf(profile.semester.toString()) }
                    var editGoal by remember { mutableStateOf(profile.careerGoal) }

                    AlertDialog(
                        onDismissRequest = { showEditProfileDialog = false },
                        title = {
                            Text(
                                text = "Edit Academic Profile ✦",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        },
                        text = {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                OutlinedTextField(
                                    value = editName,
                                    onValueChange = { editName = it },
                                    label = { Text("Name") },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = GradientIndigo,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = editCollege,
                                    onValueChange = { editCollege = it },
                                    label = { Text("College / University") },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = GradientIndigo,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = editDept,
                                    onValueChange = { editDept = it },
                                    label = { Text("Department / Major") },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = GradientIndigo,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = editSem,
                                    onValueChange = { editSem = it },
                                    label = { Text("Semester (e.g. 5)") },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = GradientIndigo,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = editGoal,
                                    onValueChange = { editGoal = it },
                                    label = { Text("Target Career Goal") },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = GradientIndigo,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (editName.isNotBlank() && editCollege.isNotBlank() && editDept.isNotBlank()) {
                                        viewModel.updateProfile(
                                            name = editName.trim(),
                                            college = editCollege.trim(),
                                            dept = editDept.trim(),
                                            sem = editSem.trim().toIntOrNull() ?: profile.semester,
                                            goal = editGoal.trim()
                                        )
                                        showEditProfileDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GradientTeal),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Save Changes", color = DeepSpaceDark, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showEditProfileDialog = false }) {
                                Text("Cancel", color = TextSecondary)
                            }
                        },
                        containerColor = DeepSpaceDark, // Solid Opaque Opaque dark background
                        modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(28.dp))
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                if (!showConfirmReset) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.logout() },
                            colors = ButtonDefaults.buttonColors(containerColor = GradientIndigo),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Log Out",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Log Out", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = { showConfirmReset = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.15f)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset",
                                tint = Color.Red,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset Data ⚠️", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.resetAllData() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Confirm Reset", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { showConfirmReset = false },
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Sub-tabs switcher
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                    .padding(4.dp)
            ) {
                listOf("Attendance", "GPA Simulation", "Placement Hub").forEachIndexed { idx, label ->
                    val selected = activeSubTab == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (selected) GradientIndigo else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { activeSubTab = idx }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (selected) Color.White else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        when (activeSubTab) {
            0 -> { // ATTENDANCE
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Subject Attendance Logs", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        TextButton(
                            onClick = { showAddSubjectDialog = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = GradientTeal)
                        ) {
                            Text("+ Add Course")
                        }
                    }
                }

                items(attendanceList) { subject ->
                    FrostedGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.sendChatMessage("How can I optimize my attendance and performance in '${subject.subject}'? Right now my attendance is ${subject.percentage.roundToInt()}% (${subject.attended}/${subject.total} sessions held).")
                            }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(subject.subject, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Faculty: ${subject.faculty}", color = TextSecondary, fontSize = 11.sp)
                                
                                Text(
                                    text = subject.getBunkAdvice(),
                                    color = if (subject.isSafe) GradientTeal else Color.Red,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "${subject.percentage.roundToInt()}%",
                                    color = if (subject.isSafe) GradientTeal else Color.Red,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text("${subject.attended}/${subject.total} Held", color = TextSecondary, fontSize = 9.sp)

                                val actionButton: @Composable (onClick: () -> Unit, bgColor: Color, content: @Composable BoxScope.() -> Unit) -> Unit = { onClick, bgColor, content ->
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(bgColor)
                                            .clickable(onClick = onClick),
                                        contentAlignment = Alignment.Center,
                                        content = content
                                    )
                                }

                                Row(
                                    modifier = Modifier.padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // RESET
                                    actionButton(
                                        { viewModel.resetAttendance(subject.subject) },
                                        Color.White.copy(alpha = 0.08f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Reset",
                                            tint = Color.LightGray,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }

                                    // DELETE
                                    actionButton(
                                        { viewModel.deleteAttendance(subject.subject) },
                                        Color.Red.copy(alpha = 0.2f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = Color.Red,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }

                                    // DECREASE (-)
                                    actionButton(
                                        { viewModel.changeAttendance(subject.subject, 0, 1) },
                                        Color.White.copy(alpha = 0.08f)
                                    ) {
                                        Text("-", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.offset(y = (-1).dp))
                                    }

                                    // INCREASE (+)
                                    actionButton(
                                        { viewModel.changeAttendance(subject.subject, 1, 1) },
                                        Color.White.copy(alpha = 0.08f)
                                    ) {
                                        Text("+", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.offset(y = (-1).dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            1 -> { // CGPA & SIMULATION
                item {
                    FrostedGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Current Cumulative CGPA (out of 10.0)", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("${stats.academicScore}", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Black)

                        Spacer(modifier = Modifier.height(16.dp))

                        var simGpa by remember { mutableStateOf(stats.academicScore) }

                        Text("What-If Simulation Slider", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            "Simulate how raising your grade this term will improve your overall graduation record.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Slider(
                            value = simGpa,
                            onValueChange = { simGpa = String.format("%.2f", it).toFloat() },
                            valueRange = 0.0f..10.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = GradientIndigo,
                                activeTrackColor = GradientIndigo,
                                inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Simulation: $simGpa CGPA", color = Color.White, fontWeight = FontWeight.Bold)
                            TextButton(
                                onClick = { viewModel.setCgpa(simGpa) },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Apply as Target", color = GradientTeal, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            2 -> { // PLACEMENT HUB
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Internship & Placements tracker", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        TextButton(
                            onClick = { showAddPlacementDialog = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = GradientTeal)
                        ) {
                            Text("+ Add Job")
                        }
                    }
                }

                items(placements) { job ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GlassCardBackground, RoundedCornerShape(16.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                            .clickable {
                                val nextStatus = when (job.status) {
                                    "Applied" -> "Interview Scheduled"
                                    "Interview Scheduled" -> "Offer Received"
                                    "Offer Received" -> "Rejected"
                                    else -> "Applied"
                                }
                                viewModel.changePlacementStatus(job.company, nextStatus)
                            }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(job.company, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("${job.role} • ${job.dateString}", color = TextSecondary, fontSize = 11.sp)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                job.status,
                                color = when (job.status) {
                                    "Offer Received" -> GradientTeal
                                    "Interview Scheduled" -> GradientPurple
                                    else -> Color.White
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )

                            IconButton(
                                onClick = { viewModel.deletePlacement(job.company) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Placement",
                                    tint = Color.Red.copy(alpha = 0.8f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- BOTTOM NAVIGATION BAR WITH GLASSMORPHISM ---

@Composable
fun BottomNavigationBar(viewModel: MainActivityViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DeepSpaceDark.copy(alpha = 0.85f))
            .border(1.dp, Color.White.copy(alpha = 0.05f))
            .navigationBarsPadding()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tab 1: Home
        BottomNavItem(
            icon = Icons.Default.Home,
            label = "Home",
            selected = currentTab == TabScreen.Home,
            onClick = { viewModel.setTab(TabScreen.Home) }
        )

        // Tab 2: Study
        BottomNavItem(
            icon = Icons.Default.Book,
            label = "Study",
            selected = currentTab == TabScreen.Study,
            onClick = { viewModel.setTab(TabScreen.Study) }
        )

        // Floating AI Trigger Center Node
        Box(
            modifier = Modifier
                .offset(y = (-14).dp)
                .clickable { viewModel.setTab(TabScreen.AI) }
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(
                        Brush.linearGradient(listOf(GradientIndigo, GradientPurple)),
                        CircleShape
                    )
                    .border(3.dp, DeepSpaceDark, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("✨", fontSize = 22.sp)
            }
        }

        // Tab 3: Planner
        BottomNavItem(
            icon = Icons.Default.CalendarToday,
            label = "Planner",
            selected = currentTab == TabScreen.Planner,
            onClick = { viewModel.setTab(TabScreen.Planner) }
        )

        // Tab 5: Profile/Tools
        BottomNavItem(
            icon = Icons.Default.Person,
            label = "Profile",
            selected = currentTab == TabScreen.ProfileTools,
            onClick = { viewModel.setTab(TabScreen.ProfileTools) }
        )
    }
}

@Composable
fun BottomNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) GradientIndigo else TextSecondary,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            color = if (selected) GradientIndigo else TextSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun AuthScreen(viewModel: MainActivityViewModel) {
    var isLogin by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }

    // Dialog states
    var showGoogleDialog by remember { mutableStateOf(false) }
    var showPhoneDialog by remember { mutableStateOf(false) }

    // Phone Dialog input states
    var phoneNumber by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }
    var otpCode by remember { mutableStateOf("") }
    var phoneErrorMessage by remember { mutableStateOf("") }
    var isSendingOtp by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        FrostedGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // Logo/Branding
                Text(
                    text = "✦ LifeOS ✦",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Your Academic & Career Copilot",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                // Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(true to "Login", false to "Sign Up").forEach { (tabIsLogin, title) ->
                        val isSelected = isLogin == tabIsLogin
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) GradientIndigo else Color.Transparent
                                )
                                .clickable {
                                    isLogin = tabIsLogin
                                    errorMessage = ""
                                    successMessage = ""
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Email Input
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email / Username", color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = GradientTeal,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        focusedLabelColor = GradientTeal,
                        unfocusedLabelColor = TextSecondary,
                        cursorColor = GradientTeal
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("auth_email_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password Input
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password", color = TextSecondary) },
                    singleLine = true,
                    visualTransformation = if (isPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle password visibility",
                                tint = TextSecondary
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = GradientTeal,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        focusedLabelColor = GradientTeal,
                        unfocusedLabelColor = TextSecondary,
                        cursorColor = GradientTeal
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("auth_password_input")
                )

                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = errorMessage,
                        color = Color.Red,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (successMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = successMessage,
                        color = GradientTeal,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Submit Button
                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            errorMessage = "Please fill in all fields"
                            return@Button
                        }
                        if (isLogin) {
                            val success = viewModel.login(email, password)
                            if (success) {
                                errorMessage = ""
                            } else {
                                errorMessage = "Invalid username or password"
                            }
                        } else {
                            val success = viewModel.register(email, password)
                            if (success) {
                                successMessage = "Account registered! You can now log in."
                                errorMessage = ""
                                isLogin = true
                            } else {
                                errorMessage = "Registration failed"
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("auth_submit_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = GradientIndigo),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isLogin) "Sign In" else "Create Account",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // OR Continue With Divider
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.12f))
                    Text(
                        text = "OR CONTINUE WITH",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.12f))
                }

                // Google & Phone Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Google
                    Button(
                        onClick = { showGoogleDialog = true },
                        modifier = Modifier.weight(1f).height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Google Sign In",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Google", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Phone
                    Button(
                        onClick = { showPhoneDialog = true },
                        modifier = Modifier.weight(1f).height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Phone Sign In",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Phone", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Helpful Info
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "💡 QUICK ACCESS INFO",
                            color = GradientTeal,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "You can sign up for a new account, or log in instantly using the demo credentials:\n• Username: demo\n• Password: demo",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }

    // --- Google Account Selector Dialog ---
    if (showGoogleDialog) {
        AlertDialog(
            onDismissRequest = { showGoogleDialog = false },
            title = {
                Text(
                    text = "✦ Choose Google Account ✦",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    // Actual user's email
                    Button(
                        onClick = {
                            viewModel.loginWithGoogle("sahpankajkumar690@gmail.com")
                            showGoogleDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        border = BorderStroke(1.dp, GradientTeal.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(GradientTeal, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("P", color = DeepSpaceDark, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(horizontalAlignment = Alignment.Start) {
                                Text("Pankaj Kumar", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("sahpankajkumar690@gmail.com", color = TextSecondary, fontSize = 10.sp)
                            }
                        }
                    }

                    // Demo guest account
                    Button(
                        onClick = {
                            viewModel.loginWithGoogle("student.copilot@gmail.com")
                            showGoogleDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.04f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(54.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(GradientIndigo, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("S", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(horizontalAlignment = Alignment.Start) {
                                Text("Student Copilot", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("student.copilot@gmail.com", color = TextSecondary, fontSize = 10.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showGoogleDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DeepSpaceDark,
            modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(28.dp))
        )
    }

    // --- Phone Number Authentication Dialog ---
    if (showPhoneDialog) {
        val coroutineScope = rememberCoroutineScope()
        AlertDialog(
            onDismissRequest = { showPhoneDialog = false },
            title = {
                Text(
                    text = "✦ OTP Verification ✦",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    if (!otpSent) {
                        Text(
                            text = "Enter your mobile number to receive a 6-digit verification code.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )

                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            label = { Text("Phone Number (e.g. +91 9876543210)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = GradientIndigo,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (phoneErrorMessage.isNotEmpty()) {
                            Text(phoneErrorMessage, color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                if (phoneNumber.trim().length < 8) {
                                    phoneErrorMessage = "Please enter a valid phone number"
                                    return@Button
                                }
                                isSendingOtp = true
                                coroutineScope.launch {
                                    delay(1500) // Beautiful simulated network dispatch delay
                                    isSendingOtp = false
                                    otpSent = true
                                    phoneErrorMessage = ""
                                }
                            },
                            enabled = !isSendingOtp,
                            colors = ButtonDefaults.buttonColors(containerColor = GradientIndigo),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isSendingOtp) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Send Verification Code", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Text(
                            text = "Simulated SMS sent to $phoneNumber!\nUse Verification Code: 2026",
                            color = GradientTeal,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        )

                        OutlinedTextField(
                            value = otpCode,
                            onValueChange = { otpCode = it },
                            label = { Text("6-Digit SMS Code") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = GradientTeal,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (phoneErrorMessage.isNotEmpty()) {
                            Text(phoneErrorMessage, color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(
                                onClick = { otpSent = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Back", color = TextSecondary)
                            }

                            Button(
                                onClick = {
                                    if (otpCode.trim() == "2026" || otpCode.trim() == "123456") {
                                        viewModel.loginWithPhone(phoneNumber)
                                        showPhoneDialog = false
                                    } else {
                                        phoneErrorMessage = "Invalid verification code! Use code 2026."
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GradientTeal),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Verify & Login", color = DeepSpaceDark, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                if (!otpSent) {
                    TextButton(onClick = { showPhoneDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            },
            containerColor = DeepSpaceDark,
            modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(28.dp))
        )
    }
}
