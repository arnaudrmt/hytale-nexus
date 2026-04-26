package fr.arnaud.nexus.ui;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import fr.arnaud.nexus.tutorial.TutorialStepConfig;

import java.util.Locale;

public final class NexusHud extends CustomUIHud {

    public NexusHud(PlayerRef playerRef) {
        super(playerRef);
    }

    @Override
    public void build(UICommandBuilder cmd) {
        cmd.append("WaveBarHud.ui");
        cmd.append("TutorialHud.ui");
    }

    // ── Wave bar ─────────────────────────────────────────────────────────

    public void showWave(int currentWave, int totalWaves, int killed, int total) {
        UICommandBuilder cmd = new UICommandBuilder();
        cmd.set("#WaveBarPanel.Visible", true);
        cmd.set("#WaveLabel.Style.TextColor", "#b4c8c9");
        cmd.set("#WaveLabel.TextSpans",
            Message.raw(String.format(Locale.ROOT, "WAVE %d / %d", currentWave, totalWaves)));
        cmd.set("#WaveProgressBar.Bar", "#4A90D9");
        cmd.set("#WaveProgressBar.Value", total > 0 ? (float) killed / total : 0f);
        update(false, cmd);
    }

    public void showWaveComplete() {
        UICommandBuilder cmd = new UICommandBuilder();
        cmd.set("#WaveBarPanel.Visible", true);
        cmd.set("#WaveLabel.Style.TextColor", "#66BB6A");
        cmd.set("#WaveLabel.TextSpans", Message.raw("WAVE COMPLETE"));
        cmd.set("#WaveProgressBar.Bar", "#66BB6A");
        cmd.set("#WaveProgressBar.Value", 1f);
        update(false, cmd);
    }

    public void hideWave() {
        UICommandBuilder cmd = new UICommandBuilder();
        cmd.set("#WaveBarPanel.Visible", false);
        update(false, cmd);
    }

    // ── Tutorial ─────────────────────────────────────────────────────────

    public void showTutorialStep(TutorialStepConfig step) {
        UICommandBuilder cmd = new UICommandBuilder();
        cmd.set("#TutorialPanel.Visible", true);
        cmd.set("#TutorialTitle.TextSpans", Message.translation(step.titleKey()));
        cmd.set("#TutorialBody.TextSpans", Message.translation(step.bodyKey()));
        cmd.set("#TutorialClickHint.Visible", step.isClickBased());
        update(false, cmd);
    }

    public void hideTutorial() {
        UICommandBuilder cmd = new UICommandBuilder();
        cmd.set("#TutorialPanel.Visible", false);
        update(false, cmd);
    }
}
