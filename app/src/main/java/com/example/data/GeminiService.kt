package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    
    // Fallback candidates to guarantee successful response
    private val MODEL_CANDIDATES = listOf("gemini-3.5-flash", "gemini-2.5-flash", "gemini-1.5-flash")
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateResponse(prompt: String, systemInstruction: String? = null): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "API Key is missing or default placeholder!")
            return@withContext "Error: Gemini API Key is missing. Please configure your GEMINI_API_KEY secret in the AI Studio Settings Panel."
        }

        var lastError = ""

        // Try model candidates in order
        for (modelName in MODEL_CANDIDATES) {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
            try {
                // Build the standard request payload
                val root = JSONObject()
                
                // Contents
                val contentsArr = JSONArray()
                val contentObj = JSONObject()
                val partsArr = JSONArray()
                val partObj = JSONObject()
                partObj.put("text", prompt)
                partsArr.put(partObj)
                contentObj.put("parts", partsArr)
                contentsArr.put(contentObj)
                root.put("contents", contentsArr)

                // System instruction if provided
                if (!systemInstruction.isNullOrEmpty()) {
                    val sysInstObj = JSONObject()
                    val sysPartsArr = JSONArray()
                    val sysPartObj = JSONObject()
                    sysPartObj.put("text", systemInstruction)
                    sysPartsArr.put(sysPartObj)
                    sysInstObj.put("parts", sysPartsArr)
                    root.put("systemInstruction", sysInstObj)
                }

                // Generation config
                val genConfig = JSONObject()
                genConfig.put("temperature", 0.7)
                root.put("generationConfig", genConfig)

                val requestBody = root.toString().toRequestBody(mediaType)
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    val bodyString = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        if (bodyString.isNotEmpty()) {
                            val responseJson = JSONObject(bodyString)
                            val candidates = responseJson.optJSONArray("candidates")
                            if (candidates != null && candidates.length() > 0) {
                                val firstCandidate = candidates.getJSONObject(0)
                                val content = firstCandidate.optJSONObject("content")
                                if (content != null) {
                                    val parts = content.optJSONArray("parts")
                                    if (parts != null && parts.length() > 0) {
                                        return@withContext parts.getJSONObject(0).optString("text", "No text generated.")
                                    }
                                }
                            }
                        }
                    } else {
                        Log.w(TAG, "Model $modelName request unsuccessful: Code ${response.code}, Body: $bodyString")
                        lastError = "Code ${response.code}: $bodyString"
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception with model $modelName", e)
                lastError = e.localizedMessage ?: "Unknown exception"
            }
        }

        return@withContext "Error: Failed to fetch AI response from Gemini. Details: $lastError"
    }
}
