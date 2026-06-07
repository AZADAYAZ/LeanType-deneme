/*
 * Copyright (C) 2026 LeanBitLab
 * SPDX-License-Identifier: GPL-3.0-only
 */
package helium314.keyboard.latin.utils

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.channels.BufferOverflow
import org.nehuatl.llamacpp.LlamaHelper
import java.io.File

/**
 * Offline proofreading service using llamacpp-kotlin with GGUF models.
 *
 * Uses LlamaHelper for on-device inference with llama.cpp backend.
 * Supports any GGUF model for text correction/generation.
 *
 * Expected model files:
 * - Any GGUF format model file
 */
class ProofreadService(private val context: Context) {

    private val sharedPrefs: SharedPreferences by lazy {
        context.prefs()
    }

    fun getPrefs(): SharedPreferences = sharedPrefs
    
    // Singleton holder for model state to prevent reloading on every request
    object ModelHolder {
        var llamaHelper: LlamaHelper? = null
        var currentModelPath: String? = null
        var isModelAvailable: Boolean = true
        var isModelLoaded: Boolean = false

        // Smart Unload Logic
        private var unloadJob: Job? = null
        private val scope = CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO)
        private const val UNLOAD_DELAY_MS = 10 * 60 * 1000L // 10 minutes

        // Flow for LLM events
        val llmFlow = MutableSharedFlow<LlamaHelper.LLMEvent>(
            extraBufferCapacity = 64,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )

        @Synchronized
        fun scheduleUnload(context: Context) {
            unloadJob?.cancel()
            
            val prefs = context.prefs()
            val keepLoaded = prefs.getBoolean(Settings.PREF_OFFLINE_KEEP_MODEL_LOADED, Defaults.PREF_OFFLINE_KEEP_MODEL_LOADED)
            
            if (keepLoaded) {
                 Log.i(TAG, "Model unload skipped (Keep Model Loaded enabled)")
                 return
            }

            unloadJob = scope.launch {
                delay(UNLOAD_DELAY_MS)
                unloadModel()
                Log.i(TAG, "Offline AI model unloaded due to inactivity")
            }
        }

        @Synchronized
        fun cancelUnload() {
            unloadJob?.cancel()
            unloadJob = null
        }

        @Synchronized
        fun unloadModel() {
            try {
                llamaHelper?.release()
            } catch (e: Exception) {
                Log.w(TAG, "Error unloading llama model", e)
            }
            llamaHelper = null
            currentModelPath = null
            isModelLoaded = false
            isModelAvailable = true
        }

        @Synchronized
        fun loadModel(
            context: Context,
            modelPath: String
        ): Boolean {
            cancelUnload()

            // Check if already loaded with same path
            if (isModelLoaded && currentModelPath == modelPath && llamaHelper != null) {
                return true
            }

            unloadModel() // Ensure clean slate if path changed

            return try {
                val contentResolver = context.contentResolver
                val helper = LlamaHelper(
                    contentResolver,
                    scope,
                    llmFlow
                )

                // Load the model with default context length
                val contextLength = 2048
                var loadSuccess = false
                helper.load(
                    path = modelPath,
                    contextLength = contextLength
                ) { _ ->
                    loadSuccess = true
                }

                // Give the model a moment to load (it's async internally)
                // We'll verify on first inference if it's truly loaded
                llamaHelper = helper
                currentModelPath = modelPath
                isModelLoaded = true
                isModelAvailable = true
                true
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to load GGUF model", e)
                isModelAvailable = false
                false
            }
        }

        private const val TAG = "LlamaProofreadService"
    }

    // AI Provider support (API compatibility)
    enum class AIProvider {
        GEMINI, GROQ, OPENAI
    }
    
    fun getProvider(): AIProvider = AIProvider.GROQ
    fun setProvider(provider: AIProvider) { /* No-op */ }

    suspend fun fetchAvailableModels(provider: AIProvider): List<String> = emptyList()

    // API-compatible methods
    fun getApiKey(): String? = null
    fun setApiKey(apiKey: String?) { /* No-op */ }
    fun hasApiKey(): Boolean = false
    
    // HuggingFace stubs
    fun getHuggingFaceToken(): String? = null
    fun setHuggingFaceToken(token: String?) { /* No-op */ }
    fun getHuggingFaceModel(): String = "Offline Mode"
    fun setHuggingFaceModel(model: String) { /* No-op */ }
    fun getHuggingFaceEndpoint(): String = "Offline Mode"
    fun setHuggingFaceEndpoint(endpoint: String) { /* No-op */ }

    fun getGroqToken(): String? = null
    fun setGroqToken(token: String?) { /* No-op */ }

    fun getGroqModel(): String = "Offline Mode"
    fun setGroqModel(model: String) { /* No-op */ }

    // Model management - single model path (no encoder/decoder split)
    fun getModelPath(): String? = sharedPrefs.getString(KEY_MODEL_PATH, null)
    
    fun setModelPath(path: String?) {
        sharedPrefs.edit().apply {
            if (path.isNullOrBlank()) {
                remove(KEY_MODEL_PATH)
            } else {
                putString(KEY_MODEL_PATH, path)
            }
            apply()
        }
        ModelHolder.unloadModel()
    }

    // Decoder path (kept for API compatibility, not used with llamacpp)
    fun getDecoderPath(): String? = sharedPrefs.getString(KEY_DECODER_PATH, null)
    
    fun setDecoderPath(path: String?) {
        sharedPrefs.edit().apply {
            if (path.isNullOrBlank()) {
                remove(KEY_DECODER_PATH)
            } else {
                putString(KEY_DECODER_PATH, path)
            }
            apply()
        }
    }

    // Tokenizer path (not needed with GGUF - tokenizer is embedded)
    fun getTokenizerPath(): String? = sharedPrefs.getString(KEY_TOKENIZER_PATH, null)
    
    fun setTokenizerPath(path: String?) {
        sharedPrefs.edit().apply {
            if (path.isNullOrBlank()) {
                remove(KEY_TOKENIZER_PATH)
            } else {
                putString(KEY_TOKENIZER_PATH, path)
            }
            apply()
        }
    }

    fun getSystemPrompt(): String = sharedPrefs.getString(Settings.PREF_OFFLINE_SYSTEM_PROMPT, "") ?: ""

    fun setSystemPrompt(prompt: String) {
        sharedPrefs.edit().putString(Settings.PREF_OFFLINE_SYSTEM_PROMPT, prompt).apply()
    }

    fun getModelName(): String {
        val path = getModelPath()
        if (path.isNullOrBlank()) return "No Model Selected"
        
        if (path.startsWith("content://")) {
            try {
                val uri = Uri.parse(path)
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            return cursor.getString(nameIndex)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to resolve content URI name", e)
            }
        }
        
        return File(path).name.takeIf { it.isNotEmpty() } ?: "Local Model"
    }

    fun setModelName(name: String) { /* No-op */ }
    
    fun getTargetLanguage(): String = "English"
    fun setTargetLanguage(language: String) { /* No-op */ }

    fun getTranslateModelName(): String = ""
    fun setTranslateModelName(modelName: String) { /* No-op */ }

    fun getTranslateHuggingFaceModel(): String = ""
    fun setTranslateHuggingFaceModel(modelName: String) { /* No-op */ }

    fun getTranslateGroqModel(): String = ""
    fun setTranslateGroqModel(modelName: String) { /* No-op */ }

    fun unloadModel() {
        ModelHolder.unloadModel()
    }

    /**
     * Run llamacpp inference for translation.
     */
    suspend fun translate(text: String): Result<String> {
        val target = sharedPrefs.getString(Settings.PREF_OFFLINE_TRANSLATE_TARGET_LANGUAGE, Defaults.PREF_OFFLINE_TRANSLATE_TARGET_LANGUAGE) ?: Defaults.PREF_OFFLINE_TRANSLATE_TARGET_LANGUAGE
        val prompt = "Translate the following text to $target. Output only the translation, nothing else:\n\n"
        return proofread(text, overridePrompt = prompt)
    }

    /**
     * Run llamacpp inference for proofreading/text correction.
     */
    suspend fun proofread(text: String, overridePrompt: String? = null): Result<String> = withContext(Dispatchers.IO) {
        val modelPath = getModelPath()
        if (modelPath.isNullOrBlank()) {
            return@withContext Result.failure(ProofreadException("Model not loaded. Please select a GGUF model file."))
        }

        // Load model (or get cached)
        if (!ModelHolder.loadModel(context, modelPath)) {
             Log.e(TAG, "Model load failed")
             return@withContext Result.failure(ProofreadException("Failed to load model."))
        }

        // Cancel unload timer while working
        ModelHolder.cancelUnload()

        try {
            val maxTokens = sharedPrefs.getInt(Settings.PREF_OFFLINE_MAX_TOKENS, Defaults.PREF_OFFLINE_MAX_TOKENS)
            
            // Build the prompt
            val systemPrompt = overridePrompt ?: getSystemPrompt()
            val fullPrompt = if (systemPrompt.isNotBlank()) {
                "$systemPrompt$text"
            } else {
                // Default proofreading prompt
                "Correct the grammar and spelling of the following text. Output only the corrected text, nothing else:\n\n$text"
            }
            
            // Collect generated text from the flow
            val generatedText = StringBuilder()
            val helper = ModelHolder.llamaHelper
                ?: return@withContext Result.failure(ProofreadException("Model not available"))

            // Use predict and collect tokens
            helper.predict(fullPrompt)
            
            // Collect events until done
            ModelHolder.llmFlow.collect { event ->
                when (event) {
                    is LlamaHelper.LLMEvent.Ongoing -> {
                        generatedText.append(event.word)
                    }
                    is LlamaHelper.LLMEvent.Done -> {
                        return@collect
                    }
                    is LlamaHelper.LLMEvent.Error -> {
                        throw ProofreadException(event.toString())
                    }
                    else -> { /* Ignore other events */ }
                }
            }

            // Schedule unload after work is done
            ModelHolder.scheduleUnload(context)

            val output = generatedText.toString().trim()

            // Strip prompt prefix if model echoed it back
            val cleanedOutput = if (systemPrompt.isNotBlank() && output.startsWith(systemPrompt, ignoreCase = true)) {
                output.removePrefix(systemPrompt).trimStart()
            } else {
                output
            }
            
            if (cleanedOutput.isNotBlank()) {
                Result.success(cleanedOutput)
            } else {
                Result.success(text)
            }

        } catch (e: Throwable) {
            Log.e(TAG, "Proofread failed", e)
            ModelHolder.scheduleUnload(context) // Ensure we still schedule unload on error
            Result.failure(ProofreadException(e.message ?: "Unknown error"))
        }
    }

    class ProofreadException(message: String) : Exception(message)
    class TranslateException(message: String) : Exception(message)

    companion object {
        private const val TAG = "LlamaProofreadService"
        private const val KEY_MODEL_PATH = "offline_model_path"
        private const val KEY_DECODER_PATH = "offline_decoder_path"
        private const val KEY_TOKENIZER_PATH = "offline_tokenizer_path"
        val AVAILABLE_MODELS = listOf("GGUF Model (Local)")
    }
}
