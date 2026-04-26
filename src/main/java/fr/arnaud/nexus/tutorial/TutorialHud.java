package fr.arnaud.nexus.tutorial;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public final class TutorialHud extends CustomUIHud {

    private static final String HUD_FILE = "TutorialHud.ui";

    public TutorialHud(PlayerRef playerRef) {
        super(playerRef);
    }

    @Override
    public void build(UICommandBuilder cmd) {
        cmd.append(HUD_FILE);
    }

    public void showStep(TutorialStepConfig step) {
        UICommandBuilder cmd = new UICommandBuilder();
        cmd.set("#TutorialPanel.Visible", true);
        cmd.set("#TutorialTitle.TextSpans", Message.translation(step.titleKey()));
        cmd.set("#TutorialBody.TextSpans", Message.translation(step.bodyKey()));
        cmd.set("#TutorialClickHint.Visible", step.isClickBased());
        update(false, cmd);
    }

    public void hide() {
        UICommandBuilder cmd = new UICommandBuilder();
        cmd.set("#TutorialPanel.Visible", false);
        cmd.set("#TutorialClickHint.Visible", false);
        update(false, cmd);
    }
}
