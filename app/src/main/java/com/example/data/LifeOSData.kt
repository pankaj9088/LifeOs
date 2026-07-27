package com.example.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

// --- DATA MODELS ---

data class OnboardingProfile(
    val name: String = "Alex",
    val college: String = "Stanford University",
    val department: String = "Computer Science",
    val semester: Int = 5,
    val graduationYear: Int = 2026,
    val careerGoal: String = "Senior AI Engineer",
    val dailyStudyHours: Int = 6,
    val isOnboarded: Boolean = true
)

data class StudyPlan(
    val id: String,
    val subject: String,
    val task: String,
    val isCompleted: Boolean,
    val priority: String // "High", "Medium", "Low"
)

data class Attendance(
    val subject: String,
    val faculty: String,
    val attended: Int,
    val total: Int,
    val minRequired: Int = 75
) {
    val percentage: Float
        get() = if (total == 0) 100f else (attended.toFloat() / total * 100)

    val isSafe: Boolean
        get() = percentage >= minRequired

    // Returns a summary string predicting if bunking is safe or how many classes to attend
    fun getBunkAdvice(): String {
        if (total == 0) return "No classes held yet."
        val pct = percentage
        if (pct < minRequired) {
            // Need to attend more classes to reach minRequired
            var needed = 0
            var tempAttended = attended
            var tempTotal = total
            while ((tempAttended.toFloat() / tempTotal * 100) < minRequired) {
                tempAttended++
                tempTotal++
                needed++
            }
            return "Warning: Attend the next $needed class${if (needed > 1) "es" else ""} to restore attendance!"
        } else {
            // Safe to miss how many?
            var safeBunks = 0
            var tempTotal = total
            while (((attended.toFloat() / (tempTotal + 1)) * 100) >= minRequired) {
                tempTotal++
                safeBunks++
            }
            return if (safeBunks > 0) {
                "Safe: You can safely miss $safeBunks class${if (safeBunks > 1) "es" else ""}!"
            } else {
                "Critical: Do not miss any class! Next class is crucial."
            }
        }
    }
}

data class Assignment(
    val id: String,
    val title: String,
    val subject: String,
    val deadline: String, // e.g. "Tomorrow, 5:00 PM" or "In 4 hours"
    val priority: String, // "High", "Medium", "Low"
    val progress: Int, // 0 - 100
    val isSubmitted: Boolean
)

data class Note(
    val id: String,
    val title: String,
    val content: String,
    val tags: List<String>,
    val isFavorite: Boolean = false,
    val dateString: String = "Jul 18"
)

data class TimetableClass(
    val subject: String,
    val room: String,
    val time: String,
    val dayOfWeek: String, // "Mon", "Tue", "Wed", "Thu", "Fri"
    val colorHex: String
)

data class ExamPlan(
    val subject: String,
    val countdownDays: Int,
    val syllabus: String,
    val priority: String, // "High", "Medium", "Low"
    val preparationProgress: Int // 0 to 100
)

data class Habit(
    val name: String,
    val isCompletedToday: Boolean,
    val streak: Int,
    val icon: String
)

data class CodingStats(
    val leetCodeSolved: Int = 142,
    val leetCodeGoal: Int = 300,
    val githubContributions: Int = 42,
    val languages: String = "Kotlin, TS, Python",
    val dailyCodingMinutes: Int = 90,
    val problemStreak: Int = 12
)

data class PlacementItem(
    val company: String,
    val role: String,
    val status: String, // "Applied", "Interview Scheduled", "Offer Received", "Rejected"
    val dateString: String,
    val prepPercentage: Int = 40
)

data class UserStats(
    val xp: Int = 0,
    val level: Int = 1,
    val streak: Int = 0,
    val academicScore: Float = 0.0f // CGPA
)

// --- STATE MANAGER (REPOSITORY) ---

class LifeOSRepository(context: Context) {
    private val prefs = context.getSharedPreferences("life_os_prefs", Context.MODE_PRIVATE)

    // --- In-Memory Current States ---
    var isLoggedIn = false
        private set

    var profile = OnboardingProfile()
        private set

    var studyPlans = mutableListOf<StudyPlan>()
        private set

    var attendanceList = mutableListOf<Attendance>()
        private set

    var assignments = mutableListOf<Assignment>()
        private set

    var notes = mutableListOf<Note>()
        private set

    var timetable = mutableListOf<TimetableClass>()
        private set

    var examPlans = mutableListOf<ExamPlan>()
        private set

    var habits = mutableListOf<Habit>()
        private set

    var codingStats = CodingStats()
        private set

    var placements = mutableListOf<PlacementItem>()
        private set

    var stats = UserStats()
        private set

    init {
        loadData()
    }

    private fun loadData() {
        isLoggedIn = prefs.getBoolean("is_logged_in", false)
        val profileJson = prefs.getString("profile", "")
        if (!profileJson.isNullOrEmpty()) {
            try {
                val obj = JSONObject(profileJson)
                profile = OnboardingProfile(
                    name = obj.optString("name", "Alex"),
                    college = obj.optString("college", "Stanford University"),
                    department = obj.optString("department", "Computer Science"),
                    semester = obj.optInt("semester", 5),
                    graduationYear = obj.optInt("graduationYear", 2026),
                    careerGoal = obj.optString("careerGoal", "Senior AI Engineer"),
                    dailyStudyHours = obj.optInt("dailyStudyHours", 6),
                    isOnboarded = obj.optBoolean("isOnboarded", true)
                )
            } catch (e: Exception) {
                // Ignore and use default
            }
        } else {
            // First time: set default
            profile = OnboardingProfile(isOnboarded = false)
        }

        // Load Study Plans
        val studyPlansJson = prefs.getString("study_plans", "")
        if (!studyPlansJson.isNullOrEmpty()) {
            try {
                studyPlans.clear()
                val arr = JSONArray(studyPlansJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    studyPlans.add(
                        StudyPlan(
                            id = obj.getString("id"),
                            subject = obj.getString("subject"),
                            task = obj.getString("task"),
                            isCompleted = obj.getBoolean("isCompleted"),
                            priority = obj.getString("priority")
                        )
                    )
                }
            } catch (e: Exception) { e.printStackTrace() }
        } else {
            // Start empty as requested
        }

        // Load Attendance
        val attendanceJson = prefs.getString("attendance", "")
        if (!attendanceJson.isNullOrEmpty()) {
            try {
                attendanceList.clear()
                val arr = JSONArray(attendanceJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    attendanceList.add(
                        Attendance(
                            subject = obj.getString("subject"),
                            faculty = obj.getString("faculty"),
                            attended = obj.getInt("attended"),
                            total = obj.getInt("total"),
                            minRequired = obj.optInt("minRequired", 75)
                        )
                    )
                }
            } catch (e: Exception) { e.printStackTrace() }
        } else {
            // Start empty as requested
        }

        // Load Assignments
        val assignmentsJson = prefs.getString("assignments", "")
        if (!assignmentsJson.isNullOrEmpty()) {
            try {
                assignments.clear()
                val arr = JSONArray(assignmentsJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    assignments.add(
                        Assignment(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            subject = obj.getString("subject"),
                            deadline = obj.getString("deadline"),
                            priority = obj.getString("priority"),
                            progress = obj.getInt("progress"),
                            isSubmitted = obj.getBoolean("isSubmitted")
                        )
                    )
                }
            } catch (e: Exception) { e.printStackTrace() }
        } else {
            // Start empty as requested
        }

        // Load Notes
        val notesJson = prefs.getString("notes", "")
        if (!notesJson.isNullOrEmpty()) {
            try {
                notes.clear()
                val arr = JSONArray(notesJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val tagsArr = obj.getJSONArray("tags")
                    val tags = mutableListOf<String>()
                    for (j in 0 until tagsArr.length()) {
                        tags.add(tagsArr.getString(j))
                    }
                    notes.add(
                        Note(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            content = obj.getString("content"),
                            tags = tags,
                            isFavorite = obj.getBoolean("isFavorite"),
                            dateString = obj.optString("dateString", "Jul 18")
                        )
                    )
                }
            } catch (e: Exception) { e.printStackTrace() }
        } else {
            // Populate defaults
            notes.addAll(
                listOf(
                    Note("n1", "OS Scheduling Algorithms", "### Summary of CPU Scheduling\n1. **First Come First Served (FCFS)**: Non-preemptive, suffers from convoy effect.\n2. **Shortest Job First (SJF)**: Optimal average waiting time.\n3. **Round Robin (RR)**: Preemptive, uses a time quantum. Good for responsiveness.\n\n### Formulae:\n* Waiting Time = Turnaround Time - Burst Time", listOf("OS", "Exam Prep"), true, "Jul 18"),
                    Note("n2", "Bio-Informatics Basics", "### What is Bio-Informatics?\nBioinformatics is an interdisciplinary field that develops methods and software tools for understanding biological data, especially when the data sets are large and complex.\n\n### Key Databases:\n- NCBI GenBank\n- UniProt for proteins", listOf("Bio", "Lab"), false, "Jul 16")
                )
            )
        }

        // Load Timetable
        val timetableJson = prefs.getString("timetable", "")
        if (!timetableJson.isNullOrEmpty()) {
            try {
                timetable.clear()
                val arr = JSONArray(timetableJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    timetable.add(
                        TimetableClass(
                            subject = obj.getString("subject"),
                            room = obj.getString("room"),
                            time = obj.getString("time"),
                            dayOfWeek = obj.getString("dayOfWeek"),
                            colorHex = obj.getString("colorHex")
                        )
                    )
                }
            } catch (e: Exception) { e.printStackTrace() }
        } else {
            // Start empty as requested
        }

        // Load Exam Plans
        val examsJson = prefs.getString("exams", "")
        if (!examsJson.isNullOrEmpty()) {
            try {
                examPlans.clear()
                val arr = JSONArray(examsJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    examPlans.add(
                        ExamPlan(
                            subject = obj.getString("subject"),
                            countdownDays = obj.getInt("countdownDays"),
                            syllabus = obj.getString("syllabus"),
                            priority = obj.getString("priority"),
                            preparationProgress = obj.getInt("preparationProgress")
                        )
                    )
                }
            } catch (e: Exception) { e.printStackTrace() }
        } else {
            // Start empty as requested
        }

        // Load Habits
        val habitsJson = prefs.getString("habits", "")
        if (!habitsJson.isNullOrEmpty()) {
            try {
                habits.clear()
                val arr = JSONArray(habitsJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    habits.add(
                        Habit(
                            name = obj.getString("name"),
                            isCompletedToday = obj.getBoolean("isCompletedToday"),
                            streak = obj.getInt("streak"),
                            icon = obj.getString("icon")
                        )
                    )
                }
            } catch (e: Exception) { e.printStackTrace() }
        } else {
            // Start empty as requested
        }

        // Load Placement List
        val placementsJson = prefs.getString("placements", "")
        if (!placementsJson.isNullOrEmpty()) {
            try {
                placements.clear()
                val arr = JSONArray(placementsJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    placements.add(
                        PlacementItem(
                            company = obj.getString("company"),
                            role = obj.getString("role"),
                            status = obj.getString("status"),
                            dateString = obj.getString("dateString"),
                            prepPercentage = obj.optInt("prepPercentage", 40)
                        )
                    )
                }
            } catch (e: Exception) { e.printStackTrace() }
        } else {
            // Start empty as requested
        }

        // Load Stats & UserStats
        val statsXp = prefs.getInt("stats_xp", 0)
        val statsLevel = prefs.getInt("stats_level", 1)
        var statsStreak = prefs.getInt("stats_streak", 0)
        if (statsStreak == 14) {
            statsStreak = 0
            prefs.edit().putInt("stats_streak", 0).apply()
        }
        val statsCgpa = prefs.getFloat("stats_cgpa", 0.0f)
        stats = UserStats(statsXp, statsLevel, statsStreak, statsCgpa)
    }

    // --- SAVE AND MUTATION HELPER METHODS ---

    fun saveAll() {
        val editor = prefs.edit()

        // Profile
        val profileObj = JSONObject().apply {
            put("name", profile.name)
            put("college", profile.college)
            put("department", profile.department)
            put("semester", profile.semester)
            put("graduationYear", profile.graduationYear)
            put("careerGoal", profile.careerGoal)
            put("dailyStudyHours", profile.dailyStudyHours)
            put("isOnboarded", profile.isOnboarded)
        }
        editor.putString("profile", profileObj.toString())

        // Study Plans
        val studyPlansArr = JSONArray().apply {
            for (p in studyPlans) {
                put(JSONObject().apply {
                    put("id", p.id)
                    put("subject", p.subject)
                    put("task", p.task)
                    put("isCompleted", p.isCompleted)
                    put("priority", p.priority)
                })
            }
        }
        editor.putString("study_plans", studyPlansArr.toString())

        // Attendance
        val attendanceArr = JSONArray().apply {
            for (a in attendanceList) {
                put(JSONObject().apply {
                    put("subject", a.subject)
                    put("faculty", a.faculty)
                    put("attended", a.attended)
                    put("total", a.total)
                    put("minRequired", a.minRequired)
                })
            }
        }
        editor.putString("attendance", attendanceArr.toString())

        // Assignments
        val assignmentsArr = JSONArray().apply {
            for (a in assignments) {
                put(JSONObject().apply {
                    put("id", a.id)
                    put("title", a.title)
                    put("subject", a.subject)
                    put("deadline", a.deadline)
                    put("priority", a.priority)
                    put("progress", a.progress)
                    put("isSubmitted", a.isSubmitted)
                })
            }
        }
        editor.putString("assignments", assignmentsArr.toString())

        // Notes
        val notesArr = JSONArray().apply {
            for (n in notes) {
                put(JSONObject().apply {
                    put("id", n.id)
                    put("title", n.title)
                    put("content", n.content)
                    put("isFavorite", n.isFavorite)
                    put("dateString", n.dateString)
                    put("tags", JSONArray().apply { n.tags.forEach { put(it) } })
                })
            }
        }
        editor.putString("notes", notesArr.toString())

        // Timetable
        val timetableArr = JSONArray().apply {
            for (t in timetable) {
                put(JSONObject().apply {
                    put("subject", t.subject)
                    put("room", t.room)
                    put("time", t.time)
                    put("dayOfWeek", t.dayOfWeek)
                    put("colorHex", t.colorHex)
                })
            }
        }
        editor.putString("timetable", timetableArr.toString())

        // Exams
        val examsArr = JSONArray().apply {
            for (e in examPlans) {
                put(JSONObject().apply {
                    put("subject", e.subject)
                    put("countdownDays", e.countdownDays)
                    put("syllabus", e.syllabus)
                    put("priority", e.priority)
                    put("preparationProgress", e.preparationProgress)
                })
            }
        }
        editor.putString("exams", examsArr.toString())

        // Habits
        val habitsArr = JSONArray().apply {
            for (h in habits) {
                put(JSONObject().apply {
                    put("name", h.name)
                    put("isCompletedToday", h.isCompletedToday)
                    put("streak", h.streak)
                    put("icon", h.icon)
                })
            }
        }
        editor.putString("habits", habitsArr.toString())

        // Placements
        val placementsArr = JSONArray().apply {
            for (p in placements) {
                put(JSONObject().apply {
                    put("company", p.company)
                    put("role", p.role)
                    put("status", p.status)
                    put("dateString", p.dateString)
                    put("prepPercentage", p.prepPercentage)
                })
            }
        }
        editor.putString("placements", placementsArr.toString())

        // User Stats
        editor.putInt("stats_xp", stats.xp)
        editor.putInt("stats_level", stats.level)
        editor.putInt("stats_streak", stats.streak)
        editor.putFloat("stats_cgpa", stats.academicScore)

        editor.apply()
    }

    // --- MUTATORS ---

    fun completeOnboarding(profile: OnboardingProfile) {
        this.profile = profile.copy(isOnboarded = true)
        saveAll()
    }

    fun addStudyPlan(plan: StudyPlan) {
        studyPlans.add(plan)
        saveAll()
    }

    fun toggleStudyPlan(id: String) {
        val index = studyPlans.indexOfFirst { it.id == id }
        if (index != -1) {
            val p = studyPlans[index]
            studyPlans[index] = p.copy(isCompleted = !p.isCompleted)
            // Add some XP on completion
            if (!p.isCompleted) {
                stats = stats.copy(xp = stats.xp + 15)
            }
            saveAll()
        }
    }

    fun updateAttendance(subject: String, attendedChange: Int, totalChange: Int) {
        val index = attendanceList.indexOfFirst { it.subject == subject }
        if (index != -1) {
            val a = attendanceList[index]
            val newAttended = (a.attended + attendedChange).coerceAtLeast(0)
            val newTotal = (a.total + totalChange).coerceAtLeast(0).coerceAtLeast(newAttended)
            attendanceList[index] = a.copy(attended = newAttended, total = newTotal)
            saveAll()
        }
    }

    fun addAttendance(attendance: Attendance) {
        attendanceList.add(attendance)
        saveAll()
    }

    fun addAssignment(assignment: Assignment) {
        assignments.add(assignment)
        saveAll()
    }

    fun toggleAssignmentSubmission(id: String) {
        val index = assignments.indexOfFirst { it.id == id }
        if (index != -1) {
            val a = assignments[index]
            assignments[index] = a.copy(isSubmitted = !a.isSubmitted, progress = if (!a.isSubmitted) 100 else 0)
            if (!a.isSubmitted) {
                stats = stats.copy(xp = stats.xp + 50)
            }
            saveAll()
        }
    }

    fun addNote(note: Note) {
        notes.add(note)
        saveAll()
    }

    fun toggleNoteFavorite(id: String) {
        val index = notes.indexOfFirst { it.id == id }
        if (index != -1) {
            val n = notes[index]
            notes[index] = n.copy(isFavorite = !n.isFavorite)
            saveAll()
        }
    }

    fun addTimetableClass(c: TimetableClass) {
        timetable.add(c)
        saveAll()
    }

    fun addExamPlan(e: ExamPlan) {
        examPlans.add(e)
        saveAll()
    }

    fun addHabit(h: Habit) {
        habits.add(h)
        saveAll()
    }

    fun toggleHabit(name: String) {
        val index = habits.indexOfFirst { it.name == name }
        if (index != -1) {
            val h = habits[index]
            val completed = !h.isCompletedToday
            val newStreak = if (completed) h.streak + 1 else (h.streak - 1).coerceAtLeast(0)
            habits[index] = h.copy(isCompletedToday = completed, streak = newStreak)
            stats = stats.copy(
                xp = stats.xp + if (completed) 10 else -10,
                streak = if (habits.any { it.isCompletedToday }) stats.streak else stats.streak // Keep streak if at least one habit completed
            )
            saveAll()
        }
    }

    fun updateCgpa(cgpa: Float) {
        stats = stats.copy(academicScore = cgpa)
        saveAll()
    }

    fun addPlacement(p: PlacementItem) {
        placements.add(p)
        saveAll()
    }

    fun updatePlacementStatus(company: String, newStatus: String) {
        val index = placements.indexOfFirst { it.company == company }
        if (index != -1) {
            placements[index] = placements[index].copy(status = newStatus)
            saveAll()
        }
    }

    fun deleteStudyPlan(id: String) {
        studyPlans.removeAll { it.id == id }
        saveAll()
    }

    fun deleteAttendance(subject: String) {
        attendanceList.removeAll { it.subject == subject }
        saveAll()
    }

    fun resetAttendance(subject: String) {
        val index = attendanceList.indexOfFirst { it.subject == subject }
        if (index != -1) {
            val a = attendanceList[index]
            attendanceList[index] = a.copy(attended = 0, total = 0)
            saveAll()
        }
    }

    fun deleteAssignment(id: String) {
        assignments.removeAll { it.id == id }
        saveAll()
    }

    fun deleteNote(id: String) {
        notes.removeAll { it.id == id }
        saveAll()
    }

    fun deleteTimetableClass(subject: String, time: String, dayOfWeek: String) {
        timetable.removeAll { it.subject == subject && it.time == time && it.dayOfWeek == dayOfWeek }
        saveAll()
    }

    fun deleteExamPlan(subject: String) {
        examPlans.removeAll { it.subject == subject }
        saveAll()
    }

    fun deleteHabit(name: String) {
        habits.removeAll { it.name == name }
        saveAll()
    }

    fun deletePlacement(company: String) {
        placements.removeAll { it.company == company }
        saveAll()
    }

    fun registerUser(email: String, pass: String): Boolean {
        if (email.isBlank() || pass.isBlank()) return false
        prefs.edit()
            .putString("auth_email", email.trim())
            .putString("auth_password", pass)
            .apply()
        return true
    }

    fun loginUser(email: String, pass: String): Boolean {
        val savedEmail = prefs.getString("auth_email", "")
        val savedPass = prefs.getString("auth_password", "")
        val emailNorm = email.trim()
        if ((emailNorm == savedEmail && pass == savedPass && savedEmail.isNotEmpty()) || 
            (emailNorm == "demo@lifeos.com" && pass == "password") ||
            (emailNorm == "demo" && pass == "demo")
        ) {
            isLoggedIn = true
            prefs.edit().putBoolean("is_logged_in", true).apply()
            return true
        }
        return false
    }

    fun logoutUser() {
        isLoggedIn = false
        prefs.edit().putBoolean("is_logged_in", false).apply()
    }

    fun loginWithGoogle(email: String): Boolean {
        val emailNorm = email.trim()
        isLoggedIn = true
        prefs.edit().putBoolean("is_logged_in", true).apply()
        if (!profile.isOnboarded) {
            val username = emailNorm.substringBefore("@")
            profile = OnboardingProfile(
                name = username.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                college = "Stanford University",
                department = "Computer Science",
                isOnboarded = true
            )
            saveAll()
        }
        return true
    }

    fun loginWithPhone(phone: String): Boolean {
        isLoggedIn = true
        prefs.edit().putBoolean("is_logged_in", true).apply()
        if (!profile.isOnboarded) {
            profile = OnboardingProfile(
                name = "User " + phone.takeLast(4),
                college = "Stanford University",
                department = "Computer Science",
                isOnboarded = true
            )
            saveAll()
        }
        return true
    }

    fun resetAllData() {
        prefs.edit().clear().apply()
        profile = OnboardingProfile(isOnboarded = false)
        studyPlans.clear()
        attendanceList.clear()
        assignments.clear()
        notes.clear()
        timetable.clear()
        examPlans.clear()
        habits.clear()
        placements.clear()
        stats = UserStats()
        loadData()
    }
}
