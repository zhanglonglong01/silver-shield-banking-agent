package com.fintechathon.silvershield.agent.planning;

import java.util.List;
import java.util.Set;

public record StructuredIntent(
        Command command,
        Set<BankingScene> scenes,
        boolean crossSceneRequested,
        List<String> missingSlots) {

    public StructuredIntent {
        scenes = Set.copyOf(scenes);
        missingSlots = List.copyOf(missingSlots);
    }

    public enum Command {
        START,
        RESUME,
        CANCEL
    }
}
