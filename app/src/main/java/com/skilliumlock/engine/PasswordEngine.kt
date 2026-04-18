package com.skilliumlock.engine

import kotlin.random.Random

/**
 * Password Engine for dynamic password challenge generation.
 *
 * Challenge types:
 * - "PASSWORD"    → Normal mode (shift = 0), use stored password as-is
 * - "PASSWORD3"   → Positive shift 3: each digit = min(9, digit + 3)
 * - "-PASSWORD2"  → Negative shift 2: each digit = max(0, digit - 2)
 *
 * Example with stored password "4567":
 *   PASSWORD8  → 9999  (each digit + 8, capped at 9)
 *   -PASSWORD3 → 1234  (each digit - 3, floored at 0)
 */
object PasswordEngine {

    data class Challenge(
        val displayText: String,  // e.g., "PASSWORD", "PASSWORD3", "-PASSWORD2"
        val shift: Int,           // 0..9
        val isPositive: Boolean   // true = add, false = subtract
    )

    /**
     * Generate a random password challenge.
     * ~20% chance of normal (shift=0), otherwise random shift 1-9 with random direction.
     */
    fun generateChallenge(): Challenge {
        val useNormal = Random.nextInt(5) == 0 // 20% chance
        if (useNormal) {
            return Challenge(
                displayText = "PASSWORD",
                shift = 0,
                isPositive = true
            )
        }

        val shift = Random.nextInt(1, 10) // 1 to 9
        val isPositive = Random.nextBoolean()

        val displayText = if (isPositive) {
            "PASSWORD$shift"
        } else {
            "-PASSWORD$shift"
        }

        return Challenge(displayText, shift, isPositive)
    }

    /**
     * Transform a stored password according to the challenge.
     *
     * @param storedPassword The original 4-digit password (e.g., "4567")
     * @param challenge The active challenge
     * @return The transformed password the user must enter
     */
    fun transformPassword(storedPassword: String, challenge: Challenge): String {
        if (challenge.shift == 0) return storedPassword

        return storedPassword.map { char ->
            val digit = char.digitToInt()
            val transformed = if (challenge.isPositive) {
                minOf(9, digit + challenge.shift)
            } else {
                maxOf(0, digit - challenge.shift)
            }
            transformed.toString()
        }.joinToString("")
    }

    /**
     * Validate user input against the stored password with the active challenge.
     *
     * @param userInput What the user typed (4 digits)
     * @param storedPassword The profile's stored password
     * @param challenge The active challenge
     * @return true if the input matches the transformed password
     */
    fun validatePassword(userInput: String, storedPassword: String, challenge: Challenge): Boolean {
        val expected = transformPassword(storedPassword, challenge)
        return userInput == expected
    }
}
