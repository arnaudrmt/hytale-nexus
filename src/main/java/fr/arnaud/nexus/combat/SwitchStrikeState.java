package fr.arnaud.nexus.combat;

/**
 * State machine for a Switch Strike execution sequence.
 *
 * GDD refs (§ Le Switch Strike):
 *
 *   IDLE        → No Switch Strike in progress.
 *   PHASE_A     → Player fires melee weapon ability (triggers Reality Vision on Mini-Boss).
 *   PHASE_B     → Reality Vision active (Mini-Boss only):
 *                   Camera transitions ISO → FPS. Time at 30% speed. Player at 80% speed.
 *                   2-3s window to aim at a colour rift (weak point).
 *   PHASE_C     → Secondary ranged ability auto-fires. Time returns to 100%.
 *                   Screen shake. Colour circle re-invades screen.
 *   COOLDOWN    → Brief post-execution window before IDLE is restored.
 *                   Flow refund check: if ≥3 enemies killed → +1 or +2 segments.
 *
 * LOCKED is used in co-op when the partner has already initiated their Switch Strike
 * (GDD § Infrastructure & Multijoueur / The Lock — only one active at a time).
 */
public enum SwitchStrikeState {
    IDLE,
    PHASE_A,
    PHASE_B,
    PHASE_C,
    COOLDOWN,
    LOCKED
}
