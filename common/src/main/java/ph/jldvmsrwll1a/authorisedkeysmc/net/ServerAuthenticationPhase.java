package ph.jldvmsrwll1a.authorisedkeysmc.net;

public enum ServerAuthenticationPhase {
    PROLOGUE,
    SEND_SERVER_KEY,
    WAIT_FOR_CLIENT_CHALLENGE,
    WAIT_FOR_CLIENT_KEY,
    SUCCESSFUL,
    BYPASSED,
    EPILOGUE;

    public boolean shouldHandleTick() {
        return !equals(PROLOGUE) && !equals(EPILOGUE);
    }
}
