package com.clickapps.crispify.engine

import com.knuddels.jtokkit.Encodings
import com.knuddels.jtokkit.api.Encoding

/**
 * TokenCounter interface for counting tokens using a specific tokenizer.
 * Default implementation uses JTokkit with CL100K_BASE encoding.
 */
interface TokenCounter {
    fun count(text: String): Int

    companion object {
        // PRD limit: ~1200 tokens for user input only
        const val LIMIT_TOKENS: Int = 1200
    }
}

class JTokkitTokenCounter : TokenCounter {
    private val encoding: Encoding by lazy {
        Encodings.newLazyEncodingRegistry().getEncoding(com.knuddels.jtokkit.api.EncodingType.CL100K_BASE)
    }

    override fun count(text: String): Int {
        if (text.isEmpty()) return 0
        // Only count tokens for the user input (not including prompt/system)
        return encoding.countTokens(text)
    }
}

