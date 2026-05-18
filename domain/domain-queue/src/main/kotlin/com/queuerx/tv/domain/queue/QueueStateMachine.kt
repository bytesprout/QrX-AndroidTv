package com.queuerx.tv.domain.queue

/**
 * Enforces valid token state transitions.
 *
 * Prevents illegal state jumps (e.g. WAITING → COLLECTED) that could corrupt
 * the queue display. All state changes in the system must pass through this machine.
 *
 * Spec Part 5 — Token States:
 * WAITING → CALLED → ACTIVE → COMPLETED
 *                             ↓ (pharmacy referral)
 *                           REFERRED → PREPARING → READY → COLLECTED
 * Any state → EMERGENCY (override)
 * Any state → NO_SHOW
 */
class QueueStateMachine {

    private val transitions: Map<TokenState, Set<TokenState>> = mapOf(
        TokenState.WAITING to setOf(
            TokenState.CALLED,
            TokenState.NO_SHOW,
            TokenState.EMERGENCY
        ),
        TokenState.CALLED to setOf(
            TokenState.ACTIVE,
            TokenState.NO_SHOW,
            TokenState.EMERGENCY
        ),
        TokenState.ACTIVE to setOf(
            TokenState.COMPLETED,
            TokenState.REFERRED,
            TokenState.EMERGENCY
        ),
        TokenState.COMPLETED to setOf(
            TokenState.REFERRED
        ),
        TokenState.REFERRED to setOf(
            TokenState.PREPARING,
            TokenState.EMERGENCY
        ),
        TokenState.PREPARING to setOf(
            TokenState.READY,
            TokenState.EMERGENCY
        ),
        TokenState.READY to setOf(
            TokenState.COLLECTED
        ),
        TokenState.COLLECTED to emptySet(),
        TokenState.NO_SHOW to emptySet(),
        TokenState.EMERGENCY to setOf(
            TokenState.ACTIVE,
            TokenState.COMPLETED
        )
    )

    /**
     * Validates and executes a state transition.
     *
     * @return The new state [to] on success.
     * @throws IllegalArgumentException if the transition is not permitted.
     */
    fun transition(from: TokenState, to: TokenState): TokenState {
        val allowed = transitions[from] ?: emptySet()
        require(to in allowed) {
            "Invalid queue transition: $from → $to. Allowed: $allowed"
        }
        return to
    }

    /** Returns true if the [from] → [to] transition is permitted. */
    fun canTransition(from: TokenState, to: TokenState): Boolean {
        return to in (transitions[from] ?: emptySet())
    }
}

// ── Legacy compatibility shim ─────────────────────────────────────────────────
// The original QueueTokenState enum is kept so existing tests continue to compile.

@Deprecated("Use TokenState instead", replaceWith = ReplaceWith("TokenState"))
enum class QueueTokenState {
    WAITING,
    CALLED,
    ACTIVE,
    COMPLETED,
    REFERRED_TO_PHARMACY,
    PREPARING,
    READY,
    COLLECTED
}

