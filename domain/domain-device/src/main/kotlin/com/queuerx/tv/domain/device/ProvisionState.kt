package com.queuerx.tv.domain.device

/**
 * Provisioning state persisted between sessions.
 *
 * Progression:
 *   NOT_PROVISIONED → ACTIVATING → REGISTERING → SYNCING_CONFIG → PROVISIONED
 *
 * If the device is factory-reset, the state reverts to NOT_PROVISIONED.
 * The app detects this on startup and re-enters the provisioning flow.
 */
enum class ProvisionState {
    /** No activation has been attempted. Shows provisioning screen. */
    NOT_PROVISIONED,

    /** Activation request submitted; waiting for backend response. */
    ACTIVATING,

    /** Activation token validated; device registration in progress. */
    REGISTERING,

    /** Device registered; downloading and applying remote configuration. */
    SYNCING_CONFIG,

    /**
     * Fully provisioned. The app launches the display immediately.
     * The device context (IDs, tokens) has been persisted in encrypted storage.
     */
    PROVISIONED
}
