package gg.ethereallabs.blockChess.game;

import java.util.UUID;

public class Invitation {
    public final UUID inviter;
    public final UUID invitee;
    public final long createdAt;
    public final long expiresAt;

    public Invitation(UUID inviter, UUID invitee) {
        this.inviter = inviter;
        this.invitee = invitee;
        this.createdAt = System.currentTimeMillis();
        this.expiresAt = this.createdAt + 60_000L;
    }
}
