package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

sealed class TabScreen {
    object Home : TabScreen()
    object Study : TabScreen()
    object Planner : TabScreen()
    object AI : TabScreen()
    object ProfileTools : TabScreen()
}

data class ChatMessage(
    val content: String,
    val isUser: Boolean,
    val timestamp: String = "10:30 AM"
)

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LifeOSRepository(application)

    // --- Tab state ---
    private val _currentTab = MutableStateFlow<TabScreen>(TabScreen.Home)
    val currentTab: StateFlow<TabScreen> = _currentTab.asStateFlow()

    // --- Settings Dialog state ---
    private val _showSettings = MutableStateFlow(false)
    val showSettings = _showSettings.asStateFlow()

    fun setShowSettings(show: Boolean) {
        _showSettings.value = show
    }

    // --- Reactive UI States ---
    private val _isLoggedIn = MutableStateFlow(repository.isLoggedIn)
    val isLoggedIn = _isLoggedIn.asStateFlow()

    private val _profile = MutableStateFlow(repository.profile)
    val profile = _profile.asStateFlow()

    private val _studyPlans = MutableStateFlow<List<StudyPlan>>(repository.studyPlans.toList())
    val studyPlans = _studyPlans.asStateFlow()

    private val _attendanceList = MutableStateFlow<List<Attendance>>(repository.attendanceList.toList())
    val attendanceList = _attendanceList.asStateFlow()

    private val _assignments = MutableStateFlow<List<Assignment>>(repository.assignments.toList())
    val assignments = _assignments.asStateFlow()

    private val _notes = MutableStateFlow<List<Note>>(repository.notes.toList())
    val notes = _notes.asStateFlow()

    private val _timetable = MutableStateFlow<List<TimetableClass>>(repository.timetable.toList())
    val timetable = _timetable.asStateFlow()

    private val _examPlans = MutableStateFlow<List<ExamPlan>>(repository.examPlans.toList())
    val examPlans = _examPlans.asStateFlow()

    private val _habits = MutableStateFlow<List<Habit>>(repository.habits.toList())
    val habits = _habits.asStateFlow()

    private val _codingStats = MutableStateFlow(repository.codingStats)
    val codingStats = _codingStats.asStateFlow()

    private val _placements = MutableStateFlow<List<PlacementItem>>(repository.placements.toList())
    val placements = _placements.asStateFlow()

    private val _stats = MutableStateFlow(repository.stats)
    val stats = _stats.asStateFlow()

    // --- Pomodoro State ---
    private val _pomodoroTotalSeconds = MutableStateFlow(25 * 60)
    val pomodoroTotalSeconds = _pomodoroTotalSeconds.asStateFlow()

    private val _pomodoroSeconds = MutableStateFlow(25 * 60)
    val pomodoroSeconds = _pomodoroSeconds.asStateFlow()

    private val _pomodoroActive = MutableStateFlow(false)
    val pomodoroActive = _pomodoroActive.asStateFlow()

    private val _pomodoroIsWork = MutableStateFlow(true)
    val pomodoroIsWork = _pomodoroIsWork.asStateFlow()

    private var pomodoroJob: Job? = null

    // --- AI Chat State ---
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage("Hi! I'm your LifeOS AI Assistant, powered by Gemini. Ask me anything about your studies, code, or schedule. I can also generate quizzes, flashcards, or study plans for you! ✦", false)
        )
    )
    val chatMessages = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading = _isChatLoading.asStateFlow()

    // --- Active Note for Summarization / Quizzing ---
    private val _selectedNote = MutableStateFlow<Note?>(null)
    val selectedNote = _selectedNote.asStateFlow()

    private val _activeQuizQuestion = MutableStateFlow<String>("")
    val activeQuizQuestion = _activeQuizQuestion.asStateFlow()

    private val _activeQuizAnswers = MutableStateFlow<List<String>>(emptyList())
    val activeQuizAnswers = _activeQuizAnswers.asStateFlow()

    private val _activeQuizCorrectIndex = MutableStateFlow(-1)
    val activeQuizCorrectIndex = _activeQuizCorrectIndex.asStateFlow()

    private val _activeQuizFeedback = MutableStateFlow("")
    val activeQuizFeedback = _activeQuizFeedback.asStateFlow()

    init {
        // Refresh values from repository
        refreshAll()
    }

    private fun refreshAll() {
        _isLoggedIn.value = repository.isLoggedIn
        _profile.value = repository.profile
        _studyPlans.value = repository.studyPlans.toList()
        _attendanceList.value = repository.attendanceList.toList()
        _assignments.value = repository.assignments.toList()
        _notes.value = repository.notes.toList()
        _timetable.value = repository.timetable.toList()
        _examPlans.value = repository.examPlans.toList()
        _habits.value = repository.habits.toList()
        _codingStats.value = repository.codingStats
        _placements.value = repository.placements.toList()
        _stats.value = repository.stats
    }

    fun setTab(tab: TabScreen) {
        _currentTab.value = tab
    }

    // --- Onboarding ---
    fun completeOnboarding(name: String, college: String, dept: String, sem: Int, grad: Int, goal: String, hours: Int) {
        val onboarding = OnboardingProfile(
            name = name,
            college = college,
            department = dept,
            semester = sem,
            graduationYear = grad,
            careerGoal = goal,
            dailyStudyHours = hours,
            isOnboarded = true
        )
        repository.completeOnboarding(onboarding)
        _profile.value = onboarding
        
        // Give some starting XP!
        gainXP(100)
    }

    fun updateProfile(name: String, college: String, dept: String, sem: Int, goal: String) {
        val current = _profile.value
        val updated = current.copy(
            name = name,
            college = college,
            department = dept,
            semester = sem,
            careerGoal = goal
        )
        repository.completeOnboarding(updated)
        _profile.value = updated
    }

    // --- Study Plans ---
    fun addManualStudyPlan(subject: String, task: String, priority: String) {
        val id = UUID.randomUUID().toString()
        val plan = StudyPlan(id, subject, task, false, priority)
        repository.addStudyPlan(plan)
        _studyPlans.value = repository.studyPlans.toList()
        gainXP(10)
    }

    fun toggleStudyPlan(id: String) {
        repository.toggleStudyPlan(id)
        _studyPlans.value = repository.studyPlans.toList()
        _stats.value = repository.stats
    }

    // --- Attendance ---
    fun changeAttendance(subject: String, attChange: Int, totChange: Int) {
        repository.updateAttendance(subject, attChange, totChange)
        _attendanceList.value = repository.attendanceList.toList()
    }

    fun addNewSubject(subject: String, faculty: String) {
        val newAttendance = Attendance(subject, faculty, 0, 0)
        repository.addAttendance(newAttendance)
        _attendanceList.value = repository.attendanceList.toList()
    }

    // --- Timetable ---
    fun addTimetableClass(subject: String, room: String, time: String, dayOfWeek: String, colorHex: String) {
        val c = TimetableClass(subject, room, time, dayOfWeek, colorHex)
        repository.addTimetableClass(c)
        _timetable.value = repository.timetable.toList()
    }

    // --- Exam Plans ---
    fun addExamPlan(subject: String, countdownDays: Int, syllabus: String, priority: String, progress: Int) {
        val e = ExamPlan(subject, countdownDays, syllabus, priority, progress)
        repository.addExamPlan(e)
        _examPlans.value = repository.examPlans.toList()
    }

    // --- Assignments ---
    fun addAssignment(title: String, subject: String, deadline: String, priority: String) {
        val id = UUID.randomUUID().toString()
        val a = Assignment(id, title, subject, deadline, priority, 0, false)
        repository.addAssignment(a)
        _assignments.value = repository.assignments.toList()
        gainXP(15)
    }

    fun toggleAssignment(id: String) {
        repository.toggleAssignmentSubmission(id)
        _assignments.value = repository.assignments.toList()
        _stats.value = repository.stats
    }

    // --- Notes ---
    fun createNote(title: String, content: String, tags: List<String>) {
        val id = UUID.randomUUID().toString()
        val note = Note(id, title, content, tags)
        repository.addNote(note)
        _notes.value = repository.notes.toList()
        gainXP(20)
    }

    fun toggleNoteFav(id: String) {
        repository.toggleNoteFavorite(id)
        _notes.value = repository.notes.toList()
    }

    // --- Habits ---
    fun toggleHabit(name: String) {
        repository.toggleHabit(name)
        _habits.value = repository.habits.toList()
        _stats.value = repository.stats
    }

    fun addHabit(name: String, icon: String) {
        val h = Habit(name, false, 0, icon)
        repository.addHabit(h)
        _habits.value = repository.habits.toList()
        _stats.value = repository.stats
    }

    // --- CGPA Simulation ---
    fun setCgpa(cgpa: Float) {
        repository.updateCgpa(cgpa)
        _stats.value = repository.stats
    }

    // --- Placement ---
    fun addPlacement(company: String, role: String, status: String, date: String) {
        val p = PlacementItem(company, role, status, date)
        repository.addPlacement(p)
        _placements.value = repository.placements.toList()
        gainXP(25)
    }

    fun changePlacementStatus(company: String, status: String) {
        repository.updatePlacementStatus(company, status)
        _placements.value = repository.placements.toList()
    }

    // --- Gamification Helpers ---
    private fun gainXP(amount: Int) {
        val currentStats = repository.stats
        var newXp = currentStats.xp + amount
        var newLevel = currentStats.level
        
        // Simple levelling: every 200 XP is a level
        val requiredXp = 200
        while (newXp >= requiredXp * newLevel) {
            newLevel++
        }
        
        repository.saveAll() // Ensure state is synchronized
        _stats.value = currentStats.copy(xp = newXp, level = newLevel)
        // Update in-memory stats in repo directly
        java.lang.reflect.Field::class.java // Just dummy ref
        val statsField = repository::class.java.getDeclaredField("stats")
        statsField.isAccessible = true
        statsField.set(repository, _stats.value)
        repository.saveAll()
    }

    // --- Pomodoro Control ---
    fun setPomodoroDuration(minutes: Int) {
        pausePomodoro()
        _pomodoroIsWork.value = true
        _pomodoroTotalSeconds.value = minutes * 60
        _pomodoroSeconds.value = minutes * 60
    }

    fun startPomodoro() {
        if (_pomodoroActive.value) return
        _pomodoroActive.value = true
        pomodoroJob = viewModelScope.launch {
            while (_pomodoroSeconds.value > 0 && _pomodoroActive.value) {
                delay(1000)
                _pomodoroSeconds.value -= 1
            }
            if (_pomodoroSeconds.value == 0) {
                // Completed session!
                gainXP(if (_pomodoroIsWork.value) 50 else 10)
                _pomodoroIsWork.value = !_pomodoroIsWork.value
                _pomodoroSeconds.value = if (_pomodoroIsWork.value) _pomodoroTotalSeconds.value else 5 * 60
                _pomodoroActive.value = false
            }
        }
    }

    fun pausePomodoro() {
        _pomodoroActive.value = false
        pomodoroJob?.cancel()
    }

    fun resetPomodoro() {
        pausePomodoro()
        _pomodoroIsWork.value = true
        _pomodoroSeconds.value = _pomodoroTotalSeconds.value
    }

    // --- AI Integration (Gemini) ---
    fun sendChatMessage(text: String) {
        if (text.trim().isEmpty()) return
        
        val userMsg = ChatMessage(text, true)
        _chatMessages.value = _chatMessages.value + userMsg
        
        viewModelScope.launch {
            _isChatLoading.value = true
            
            // Build the system instructions for Gemini custom personality
            val sysInstruction = """
                You are LifeOS, an elite AI-powered Student Operating System assistant.
                You are a combination of a Harvard-level academic advisor, a professional coding tutor (expert in LeetCode/DSA), a career mentor, and a friendly, supportive college student coach.
                Your response style is premium, direct, highly encouraging, and filled with crisp markdown tables, emojis, and visual rhythm.
                
                Keep answers action-oriented. Support student ${_profile.value.name} from ${_profile.value.college}, studying ${_profile.value.department}.
                Address the user by their actual name (${_profile.value.name}) and greet them warmly using this name.
                When answering, suggest concrete habits or study plans they can create.
            """.trimIndent()

            val aiResponse = GeminiService.generateResponse(text, sysInstruction)
            
            _chatMessages.value = _chatMessages.value + ChatMessage(aiResponse, false)
            _isChatLoading.value = false
            
            // Gain XP for interacting with AI!
            gainXP(5)
        }
    }

    fun selectNoteForAI(note: Note) {
        _selectedNote.value = note
    }

    fun summarizeSelectedNote() {
        val note = _selectedNote.value ?: return
        viewModelScope.launch {
            _isChatLoading.value = true
            setTab(TabScreen.AI)
            val prompt = """
                Analyze the following note and provide:
                1. A high-impact executive summary (2-3 sentences)
                2. 3 crucial key takeaways as bullet points
                3. A fast revision checklist
                
                Note Title: ${note.title}
                Note Content:
                ${note.content}
            """.trimIndent()
            
            _chatMessages.value = _chatMessages.value + ChatMessage("Summarizing my note '${note.title}'...", true)
            val summary = GeminiService.generateResponse(prompt, "You are a professional note summarizer.")
            _chatMessages.value = _chatMessages.value + ChatMessage(summary, false)
            _isChatLoading.value = false
            gainXP(15)
        }
    }

    fun generateQuizFromNote() {
        val note = _selectedNote.value ?: return
        viewModelScope.launch {
            _isChatLoading.value = true
            setTab(TabScreen.AI)
            val prompt = """
                Based on this study note, generate exactly ONE multiple choice question (MCQ) for testing comprehension.
                Format your response strictly as a JSON object, with no other markdown wrappers or explanation.
                JSON Schema:
                {
                  "question": "What is...",
                  "options": ["A", "B", "C", "D"],
                  "correctIndex": 1
                }
                
                Note Title: ${note.title}
                Note Content:
                ${note.content}
            """.trimIndent()
            
            _chatMessages.value = _chatMessages.value + ChatMessage("Generating a test quiz from note '${note.title}'...", true)
            val quizRaw = GeminiService.generateResponse(prompt, "You are a quiz generation engine that returns ONLY pure, parseable raw JSON.")
            
            try {
                // Remove potential markdown block wrappers from LLM response
                val cleanJson = quizRaw.replace("```json", "").replace("```", "").trim()
                val obj = JSONObject(cleanJson)
                _activeQuizQuestion.value = obj.getString("question")
                val optArr = obj.getJSONArray("options")
                val list = mutableListOf<String>()
                for (i in 0 until optArr.length()) {
                    list.add(optArr.getString(i))
                }
                _activeQuizAnswers.value = list
                _activeQuizCorrectIndex.value = obj.getInt("correctIndex")
                _activeQuizFeedback.value = ""
                
                _chatMessages.value = _chatMessages.value + ChatMessage("I've generated a quiz question for you below! Select your answer to check your score.", false)
            } catch (e: Exception) {
                _chatMessages.value = _chatMessages.value + ChatMessage("Sorry, I had trouble parsing the quiz structure. Here is a generated question text instead:\n$quizRaw", false)
            }
            
            _isChatLoading.value = false
            gainXP(15)
        }
    }

    fun answerQuiz(selectedIndex: Int) {
        if (selectedIndex == _activeQuizCorrectIndex.value) {
            _activeQuizFeedback.value = "Correct! 🎉 Well done! +20 XP awarded."
            gainXP(20)
        } else {
            val correctText = _activeQuizAnswers.value.getOrNull(_activeQuizCorrectIndex.value) ?: ""
            _activeQuizFeedback.value = "Incorrect. ❌ The correct answer is: $correctText. Keep revising!"
        }
    }

    fun generateStudyPlanWithAI(subject: String, targetDays: Int) {
        if (subject.trim().isEmpty()) return
        viewModelScope.launch {
            _isChatLoading.value = true
            setTab(TabScreen.AI)
            val prompt = """
                Create a high-impact $targetDays-day revision study plan for the subject '$subject'.
                Format your output with daily goals, recommended resources, and mock milestones.
                Make it incredibly professional, and structure it so I can finish on track.
            """.trimIndent()
            
            _chatMessages.value = _chatMessages.value + ChatMessage("Generate an AI study plan for $subject over $targetDays days.", true)
            val planText = GeminiService.generateResponse(prompt, "You are an elite academic curriculum planner.")
            _chatMessages.value = _chatMessages.value + ChatMessage(planText, false)
            
            // Auto add a study plan task in our app for this!
            addManualStudyPlan(subject, "Revise AI Generated Syllabus", "High")
            
            _isChatLoading.value = false
            gainXP(25)
        }
    }

    fun optimizeResume() {
        viewModelScope.launch {
            _isChatLoading.value = true
            setTab(TabScreen.AI)
            val prompt = """
                Review my resume for a '${_profile.value.careerGoal}' position.
                Give me an ATS Score estimation (out of 100) and list:
                1. 3 highly optimized action verbs to add.
                2. Crucial keywords tailored to this career path.
                3. Structure suggestions to pass automated scanners.
            """.trimIndent()
            
            _chatMessages.value = _chatMessages.value + ChatMessage("Run an AI Resume ATS Optimization check.", true)
            val review = GeminiService.generateResponse(prompt, "You are an expert tech recruiter and ATS resume scanner.")
            _chatMessages.value = _chatMessages.value + ChatMessage(review, false)
            _isChatLoading.value = false
            gainXP(20)
        }
    }

    fun register(email: String, pass: String): Boolean {
        return repository.registerUser(email, pass)
    }

    fun login(email: String, pass: String): Boolean {
        val success = repository.loginUser(email, pass)
        if (success) {
            _isLoggedIn.value = true
        }
        return success
    }

    fun logout() {
        repository.logoutUser()
        _isLoggedIn.value = false
    }

    fun loginWithGoogle(email: String): Boolean {
        val success = repository.loginWithGoogle(email)
        if (success) {
            _isLoggedIn.value = true
            refreshAll()
        }
        return success
    }

    fun loginWithPhone(phone: String): Boolean {
        val success = repository.loginWithPhone(phone)
        if (success) {
            _isLoggedIn.value = true
            refreshAll()
        }
        return success
    }

    fun clearChatHistory() {
        _chatMessages.value = listOf(
            ChatMessage("Hi! I'm your LifeOS AI Assistant, powered by Gemini. Ask me anything about your studies, code, or schedule. I can also generate quizzes, flashcards, or study plans for you! ✦", false)
        )
    }

    fun deleteStudyPlan(id: String) {
        repository.deleteStudyPlan(id)
        _studyPlans.value = repository.studyPlans.toList()
    }

    fun deleteAttendance(subject: String) {
        repository.deleteAttendance(subject)
        _attendanceList.value = repository.attendanceList.toList()
    }

    fun resetAttendance(subject: String) {
        repository.resetAttendance(subject)
        _attendanceList.value = repository.attendanceList.toList()
    }

    fun deleteAssignment(id: String) {
        repository.deleteAssignment(id)
        _assignments.value = repository.assignments.toList()
    }

    fun deleteNote(id: String) {
        repository.deleteNote(id)
        _notes.value = repository.notes.toList()
    }

    fun deleteTimetableClass(subject: String, time: String, dayOfWeek: String) {
        repository.deleteTimetableClass(subject, time, dayOfWeek)
        _timetable.value = repository.timetable.toList()
    }

    fun deleteExamPlan(subject: String) {
        repository.deleteExamPlan(subject)
        _examPlans.value = repository.examPlans.toList()
    }

    fun deleteHabit(name: String) {
        repository.deleteHabit(name)
        _habits.value = repository.habits.toList()
    }

    fun deletePlacement(company: String) {
        repository.deletePlacement(company)
        _placements.value = repository.placements.toList()
    }

    fun resetAllData() {
        repository.resetAllData()
        refreshAll()
        _currentTab.value = TabScreen.Home
        _chatMessages.value = listOf(
            ChatMessage("Hi! I'm your LifeOS AI Assistant, powered by Gemini. Ask me anything about your studies, code, or schedule. I can also generate quizzes, flashcards, or study plans for you! ✦", false)
        )
        _activeQuizQuestion.value = ""
        _activeQuizAnswers.value = emptyList()
        _activeQuizCorrectIndex.value = -1
        _activeQuizFeedback.value = ""
    }
}
