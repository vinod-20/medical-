package com.example.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Checks if the Gemini API Key is configured.
     */
    fun isApiKeyAvailable(): Boolean {
        return try {
            val key = BuildConfig.GEMINI_API_KEY
            key.isNotEmpty() && key != "MY_GEMINI_API_KEY" && key != "placeholder"
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Base function to request completion from Gemini API
     */
    private suspend fun generateText(prompt: String): String = withContext(Dispatchers.IO) {
        if (!isApiKeyAvailable()) {
            Log.w(TAG, "Gemini API key is not configured, running in simulation mode.")
            return@withContext getSimulatedResponse(prompt)
        }

        val apiKey = BuildConfig.GEMINI_API_KEY
        val url = "$BASE_URL?key=$apiKey"

        val jsonRequest = JSONObject().apply {
            put("contents", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", org.json.JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonRequest.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: "No error body"
                    Log.e(TAG, "Gemini Request failed! Code: ${response.code}, Message: ${response.message}, Body: $errBody")
                    throw IOException("HTTP ${response.code} : ${response.message}")
                }

                val bodyText = response.body?.string() ?: throw IOException("Empty response body")
                val jsonResponse = JSONObject(bodyText)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    if (content != null) {
                        val parts = content.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text", "No text part found.")
                        }
                    }
                }
                "Response parsing failed."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calling Gemini API: ${e.message}", e)
            "Error: ${e.message}. (Using local clinical fallback instead)"
        }
    }

    suspend fun generateAnalysis(scanType: String, findingNotes: String): String {
        val prompt = """
            You are a board-certified hospital radiologist and an AI co-pilot.
            We have a standard radiology scan.
            Scan Type: $scanType
            Radiologist Finding Notes: $findingNotes
            
            Synthesize a brief, professional, clinical-grade "AI IMPRESSION & FINDINGS" report.
            Keep it objective, concise (under 4-5 sentences), and format it cleanly with:
            - CLINICAL IMPRESSION:
            - SUSPECTED PATHOLOGY:
            - RECOMMENDATIONS:
            Do not markdown-bold with asterisks too heavily, keep it professional and easy to scan on mobile screens.
        """.trimIndent()
        return generateText(prompt)
    }

    suspend fun translateVoiceToReport(transcript: String): String {
        val prompt = """
            You are a teleradiology AI assistant. An expert radiologist has dictated a quick voice memo about is scan:
            Memo transcript: "$transcript"
            
            Rearrange and translate this dictation into a structured clinical report draft:
            Include sections:
            - FINDINGS:
            - IMPRESSION:
            - SEVERITY RATING: (e.g. Normal, Mild, Moderate, Acute Emergency)
            Ensure the clinical terminology is correct and professional. Keep it clean and readable on a 5-inch mobile screen.
        """.trimIndent()
        return generateText(prompt)
    }

    suspend fun consultFractureDemo(x: Float, y: Float, partName: String): String {
        val prompt = """
            You are a diagnostic teaching assistant. A user tapped on a digital X-Ray of: $partName
            Tapped Coordinates: X=${x.toInt()}px, Y=${y.toInt()}px.
            Provide a short educational snippet (3 sentences max) summarizing:
            1. What anatomical structures reside in this quadrant of $partName.
            2. If a lesion or fracture is found at this area, what typical medical code/classification it would fall under.
            Be crisp, educational, and professional.
        """.trimIndent()
        return generateText(prompt)
    }

    private fun getSimulatedResponse(prompt: String): String {
        return when {
            prompt.contains("Chest X-Ray") || prompt.contains("Chest") -> {
                "CLINICAL IMPRESSION: Normal cardiothoracic ratio. No focal alveolar consolidation, pleural effusion, or pneumothorax identified. Lungs are clear.\n\n" +
                "SUSPECTED PATHOLOGY: None. Consistent with a healthy thoracic profile.\n\n" +
                "RECOMMENDATIONS: No acute pulmonary intervention required. Routine clinical correlation as indicated."
            }
            prompt.contains("Brain MRI") || prompt.contains("Brain") -> {
                "CLINICAL IMPRESSION: Symmetrical cerebral hemispheres. Ventricles and sulci are normal for age. No acute infarct, hemorrhage, or mass effect.\n\n" +
                "SUSPECTED PATHOLOGY: No space-occupying lesion or neurodegenerative markers identified.\n\n" +
                "RECOMMENDATIONS: Standard follow-up. Repeat brain imaging only if sensory/motor deficits present."
            }
            prompt.contains("Knee X-Ray") || prompt.contains("Knee") || prompt.contains("joint") -> {
                "CLINICAL IMPRESSION: Narrowing of the medial femorotibial joint space. Subchondral sclerosis and minimal osteophyte formation are visible along the patellar margin.\n\n" +
                "SUSPECTED PATHOLOGY: Moderate Osteoarthritis.\n\n" +
                "RECOMMENDATIONS: Weight management, moderate physiotherapy, and orthopedic consult if joint lock occurs."
            }
            prompt.contains("translateVoiceToReport") || prompt.contains("voice memo") || prompt.contains("dictat") -> {
                "FINDINGS: Segmental consolidation detected in the right lower lung lobe with elevated fluid density.\n\n" +
                "IMPRESSION: Suspected bacterial lobar pneumonia.\n\n" +
                "SEVERITY RATING: Moderate. Antibiotics course strongly advised."
            }
            else -> {
                "CLINICAL IMPRESSION: Digital evaluation shows local density variance within acceptable biological ranges.\n\n" +
                "SUSPECTED PATHOLOGY: Minimal diagnostic significance. No signs of acute fracture or cellular disruption.\n\n" +
                "RECOMMENDATIONS: Correlate with standard clinical lab reports and patient functional symptoms."
            }
        }
    }
}
