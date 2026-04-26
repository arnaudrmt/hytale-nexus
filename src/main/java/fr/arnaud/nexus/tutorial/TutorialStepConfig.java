package fr.arnaud.nexus.tutorial;

public record TutorialStepConfig(
    String id,
    String titleKey,
    String bodyKey,
    TutorialTriggerType triggerType,
    float displaySeconds
) {
    public boolean isManual() {
        return triggerType == TutorialTriggerType.MANUAL;
    }

    public boolean isTimerBased() {
        return isManual() && displaySeconds > 0f;
    }

    public boolean isClickBased() {
        return isManual() && displaySeconds == 0f;
    }
}
