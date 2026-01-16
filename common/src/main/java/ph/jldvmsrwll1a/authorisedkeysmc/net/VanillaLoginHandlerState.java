package ph.jldvmsrwll1a.authorisedkeysmc.net;

public enum VanillaLoginHandlerState {
    /// Right after handshaking.
    STARTING,
    /// Key exchange.
    ENCRYPTING,
    /// Vanilla authentication.
    AUTHENTICATING,
    /// Custom query.
    IN_CUSTOM_PROCESS,
    /// Check if player can join.
    CHECKING_CAN_JOIN,
    /// Kick all duplicates.
    AWAITING_DEDUPLICATION,
    /// Go to config protocol.
    SWITCHING_PROTOCOL,
    /// Done.
    DONE,
}
