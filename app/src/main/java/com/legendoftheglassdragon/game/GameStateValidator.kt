package com.legendoftheglassdragon.game

sealed interface GameStateValidationResult {
    data object Valid : GameStateValidationResult
    data class Invalid(val reason: String) : GameStateValidationResult
}

object GameStateValidator {
    fun validate(state: GameState): GameStateValidationResult {
        if (state.saveVersion != GameState.CURRENT_SAVE_VERSION) {
            return GameStateValidationResult.Invalid("Unsupported save version")
        }
        if (state.currentDay < 1 || state.playerDay.currentDay != state.currentDay) {
            return GameStateValidationResult.Invalid("Invalid current day")
        }
        if (state.revision < 0 || state.world.randomStep < 0) {
            return GameStateValidationResult.Invalid("Invalid deterministic state")
        }
        if (state.player.id.isBlank()) {
            return GameStateValidationResult.Invalid("Missing player identity")
        }
        val validName = CharacterNameValidator.validate(state.player.name)
        if (validName !is NameValidationResult.Valid || validName.name != state.player.name) {
            return GameStateValidationResult.Invalid("Invalid player name")
        }
        if (state.player.level < 1 || state.player.experience < 0) {
            return GameStateValidationResult.Invalid("Invalid progression values")
        }
        if (state.player.experience < ProgressionRules.experienceRequiredForLevel(state.player.level)) {
            return GameStateValidationResult.Invalid("Experience does not support player level")
        }
        if (state.player.maximumHp < 1 || state.player.currentHp !in 0..state.player.maximumHp) {
            return GameStateValidationResult.Invalid("Invalid hit points")
        }
        if (state.player.baseAttack < 0 || state.player.baseDefense < 0 ||
            state.player.gold < 0 || state.player.bankedGold < 0 ||
            state.player.challengeWins < 0 || state.player.challengeLosses < 0
        ) {
            return GameStateValidationResult.Invalid("Invalid player statistics")
        }
        if (EquipmentCatalog.weapon(state.player.weaponId) == null ||
            EquipmentCatalog.armor(state.player.armorId) == null
        ) {
            return GameStateValidationResult.Invalid("Unknown player equipment")
        }
        if (state.playerDay.fightsRemaining !in 0..GameBalance.FOREST_FIGHTS_PER_DAY) {
            return GameStateValidationResult.Invalid("Invalid daily state")
        }
        if (state.playerDay.wasDefeatedToday && state.playerDay.fightsRemaining != 0) {
            return GameStateValidationResult.Invalid("Defeated player still has forest fights")
        }
        if (state.playerDay.challengesRemaining !in 0..GameBalance.CHALLENGES_PER_DAY) {
            return GameStateValidationResult.Invalid("Invalid daily challenge state")
        }
        if (!state.playerDay.trainingAttemptedToday && state.playerDay.lastTrainingSucceeded != null) {
            return GameStateValidationResult.Invalid("Invalid daily training state")
        }
        val community = state.community
        if (community.lastSimulatedDay != state.currentDay - 1) {
            return GameStateValidationResult.Invalid("Community day is out of sync")
        }
        if (community.adventurers.isEmpty() || community.adventurers.size > 20) {
            return GameStateValidationResult.Invalid("Invalid community population")
        }
        if (community.adventurers.map { it.id }.distinct().size != community.adventurers.size ||
            community.adventurers.map { it.name }.distinct().size != community.adventurers.size ||
            community.adventurers.any { it.id == state.player.id }
        ) {
            return GameStateValidationResult.Invalid("Community identities are not unique")
        }
        community.adventurers.forEach { npc ->
            if (npc.id.isBlank() || npc.name.isBlank() || npc.level < 1 || npc.experience < 0 ||
                npc.maximumHp < 1 || npc.baseAttack < 0 || npc.baseDefense < 0 || npc.gold < 0 ||
                npc.experience < ProgressionRules.experienceRequiredForLevel(npc.level) ||
                EquipmentCatalog.weapon(npc.weaponId) == null ||
                EquipmentCatalog.armor(npc.armorId) == null ||
                npc.familiarity !in 0..CommunityRules.MAX_FAMILIARITY ||
                npc.rivalry !in 0..GameBalance.MAX_RIVALRY ||
                npc.playerWinsAgainst < 0 || npc.playerLossesAgainst < 0 ||
                npc.challengeWins < 0 || npc.challengeLosses < 0 ||
                npc.lastMetDay !in 0..state.currentDay ||
                npc.lastChallengeDay !in 0..state.currentDay ||
                npc.lastAdvancedDay != community.lastSimulatedDay
            ) {
                return GameStateValidationResult.Invalid("Invalid NPC state")
            }
            val lawfulRecovery = when (npc.status) {
                NpcStatus.ACTIVE -> npc.recoveryDaysRemaining == 0
                NpcStatus.RECOVERING -> npc.recoveryDaysRemaining > 0
            }
            if (!lawfulRecovery) {
                return GameStateValidationResult.Invalid("Invalid NPC recovery state")
            }
        }
        if (community.recentEvents.size > CommunityRules.MAX_RETAINED_EVENTS ||
            community.recentEvents.map { it.id }.distinct().size != community.recentEvents.size
        ) {
            return GameStateValidationResult.Invalid("Invalid realm news archive")
        }
        val knownCharacterIds = community.adventurers.mapTo(mutableSetOf()) { it.id }
            .apply { add(state.player.id) }
        community.recentEvents.forEach { event ->
            if (!isLawfulEvent(event, state.currentDay, knownCharacterIds)) {
                return GameStateValidationResult.Invalid("Invalid realm news event")
            }
        }
        if (community.chronicleHistory.size > ChronicleRules.MAX_HISTORY_DAYS ||
            community.chronicleHistory.map { it.day }.distinct().size !=
            community.chronicleHistory.size ||
            community.chronicleHistory.zipWithNext().any { (first, second) -> first.day >= second.day }
        ) {
            return GameStateValidationResult.Invalid("Invalid Chronicle history")
        }
        val chronicleEventIds = mutableSetOf<String>()
        community.chronicleHistory.forEach { record ->
            val lawfulRecordDay = record.day in 1 until state.currentDay ||
                (record.day == state.currentDay && state.season.stage == DragonStage.RESOLVED)
            if (!lawfulRecordDay ||
                record.events.size > ChronicleRules.MAX_ITEMS_PER_DAY ||
                record.events.any { event ->
                    event.day != record.day ||
                        !isLawfulEvent(event, state.currentDay, knownCharacterIds) ||
                        !chronicleEventIds.add(event.id)
                }
            ) {
                return GameStateValidationResult.Invalid("Invalid Chronicle record")
            }
        }
        state.pendingDayTransition?.let { transition ->
            if (transition.resolvedDay != state.currentDay - 1 ||
                transition.nextDay != state.currentDay ||
                community.chronicleHistory.none { it.day == transition.resolvedDay }
            ) {
                return GameStateValidationResult.Invalid("Invalid day transition checkpoint")
            }
        }
        val season = state.season
        if (season.pressure !in 0..DragonSeasonBalance.MAX_PRESSURE ||
            season.stageStartedDay !in 1..state.currentDay ||
            season.lastIncidentDay !in 0..state.currentDay ||
            season.playerLastAttemptDay !in 0..state.currentDay ||
            season.incidents.size > DragonSeasonBalance.MAX_INCIDENTS ||
            season.attempts.size > DragonSeasonBalance.MAX_ATTEMPTS ||
            season.declaredChallengerIds.distinct().size != season.declaredChallengerIds.size ||
            season.declaredChallengerIds.any { id -> community.adventurers.none { it.id == id } }
        ) {
            return GameStateValidationResult.Invalid("Invalid Dragon Season state")
        }
        if (season.incidents.map { it.id }.distinct().size != season.incidents.size ||
            season.incidents.any { incident ->
                incident.id.isBlank() || incident.day !in 1..state.currentDay ||
                    incident.stage == DragonStage.RESOLVED ||
                    (incident.residentId != null && incident.residentId !in knownCharacterIds)
            } ||
            season.incidents.zipWithNext().any { (first, second) ->
                first.day > second.day || first.stage.ordinal > second.stage.ordinal
            }
        ) {
            return GameStateValidationResult.Invalid("Invalid Dragon incident history")
        }
        if (season.attempts.map { it.id }.distinct().size != season.attempts.size ||
            season.attempts.any { attempt ->
                attempt.id.isBlank() || attempt.day !in 1..state.currentDay ||
                    attempt.challengerId !in knownCharacterIds ||
                    attempt.challengerPower < 1 || attempt.dragonPower < 1
            } || season.attempts.count { it.outcome == DragonAttemptOutcome.VICTORY } > 1
        ) {
            return GameStateValidationResult.Invalid("Invalid Dragon attempt history")
        }
        if (season.stage == DragonStage.RESOLVED) {
            if (season.victorId !in knownCharacterIds || season.resolvedDay !in 1..state.currentDay ||
                season.conclusionPhase == null ||
                season.attempts.singleOrNull { it.outcome == DragonAttemptOutcome.VICTORY }
                    ?.challengerId != season.victorId
            ) {
                return GameStateValidationResult.Invalid("Invalid resolved season")
            }
        } else if (season.victorId != null || season.resolvedDay != 0 ||
            season.conclusionPhase != null ||
            season.attempts.any { it.outcome == DragonAttemptOutcome.VICTORY }
        ) {
            return GameStateValidationResult.Invalid("Unresolved season has a final outcome")
        }
        if (listOfNotNull(state.activeCombat, state.activeChallenge, state.activeDragonCombat).size > 1) {
            return GameStateValidationResult.Invalid("Multiple active encounters")
        }
        state.activeCombat?.let { combat ->
            val monster = MonsterCatalog.find(combat.monsterId)
                ?: return GameStateValidationResult.Invalid("Unknown active monster")
            if (!combat.fightConsumed) {
                return GameStateValidationResult.Invalid("Active encounter did not consume a fight")
            }
            if (combat.recentMessages.isEmpty() ||
                combat.recentMessages.size > GameBalance.MAX_COMBAT_MESSAGES ||
                combat.recentMessages.any { it.text.isBlank() }
            ) {
                return GameStateValidationResult.Invalid("Invalid combat log")
            }
            if (combat.experienceReward < 0 || combat.goldReward < 0 || combat.goldLost < 0) {
                return GameStateValidationResult.Invalid("Invalid combat accounting")
            }
            if (state.playerDay.wasDefeatedToday && combat.status != CombatStatus.DEFEAT) {
                return GameStateValidationResult.Invalid("Defeat state conflicts with combat")
            }
            when (combat.status) {
                CombatStatus.ACTIVE -> {
                    if (combat.monsterCurrentHp !in 1..monster.maximumHp ||
                        !combat.escapeAvailable ||
                        combat.rewardsGranted ||
                        combat.experienceReward != 0 ||
                        combat.goldReward != 0 ||
                        combat.goldLost != 0
                    ) {
                        return GameStateValidationResult.Invalid("Invalid active combat")
                    }
                }
                CombatStatus.VICTORY -> {
                    if (combat.monsterCurrentHp != 0 ||
                        combat.escapeAvailable ||
                        !combat.rewardsGranted ||
                        combat.experienceReward != monster.experienceReward ||
                        combat.goldReward !in monster.minimumGoldReward..monster.maximumGoldReward ||
                        combat.goldLost != 0
                    ) {
                        return GameStateValidationResult.Invalid("Invalid victory state")
                    }
                }
                CombatStatus.ESCAPED -> {
                    if (combat.monsterCurrentHp !in 1..monster.maximumHp ||
                        combat.escapeAvailable ||
                        combat.rewardsGranted ||
                        combat.experienceReward != 0 ||
                        combat.goldReward != 0 ||
                        combat.goldLost != 0
                    ) {
                        return GameStateValidationResult.Invalid("Invalid escape state")
                    }
                }
                CombatStatus.DEFEAT -> {
                    if (combat.monsterCurrentHp !in 1..monster.maximumHp ||
                        combat.escapeAvailable ||
                        combat.rewardsGranted ||
                        combat.experienceReward != 0 ||
                        combat.goldReward != 0 ||
                        state.playerDay.fightsRemaining != 0 ||
                        !state.playerDay.wasDefeatedToday ||
                        state.player.currentHp <= 0
                    ) {
                        return GameStateValidationResult.Invalid("Invalid defeat state")
                    }
                }
            }
        }
        state.activeChallenge?.let { challenge ->
            val opponent = community.adventurers.firstOrNull { it.id == challenge.opponentId }
                ?: return GameStateValidationResult.Invalid("Unknown challenge opponent")
            if (challenge.recentMessages.isEmpty() ||
                challenge.recentMessages.size > GameBalance.MAX_COMBAT_MESSAGES ||
                challenge.recentMessages.any { it.text.isBlank() } ||
                challenge.experienceReward < 0 || challenge.goldTransferred < 0
            ) {
                return GameStateValidationResult.Invalid("Invalid challenge accounting")
            }
            when (challenge.status) {
                ChallengeStatus.ACTIVE -> {
                    if (!challenge.opportunityConsumed || challenge.rewardsGranted ||
                        challenge.opponentCurrentHp !in 1..opponent.maximumHp ||
                        challenge.experienceReward != 0 || challenge.goldTransferred != 0
                    ) {
                        return GameStateValidationResult.Invalid("Invalid active challenge")
                    }
                }
                ChallengeStatus.PLAYER_VICTORY -> {
                    if (!challenge.opportunityConsumed || !challenge.rewardsGranted ||
                        challenge.opponentCurrentHp != 0 || challenge.experienceReward <= 0
                    ) {
                        return GameStateValidationResult.Invalid("Invalid challenge victory")
                    }
                }
                ChallengeStatus.PLAYER_DEFEAT -> {
                    if (!challenge.opportunityConsumed || challenge.rewardsGranted ||
                        challenge.opponentCurrentHp !in 1..opponent.maximumHp ||
                        challenge.experienceReward != 0 || state.player.currentHp <= 0
                    ) {
                        return GameStateValidationResult.Invalid("Invalid challenge defeat")
                    }
                }
                ChallengeStatus.YIELDED -> {
                    if (!challenge.opportunityConsumed || challenge.rewardsGranted ||
                        challenge.opponentCurrentHp !in 1..opponent.maximumHp ||
                        challenge.experienceReward != 0 || challenge.goldTransferred != 0
                    ) {
                        return GameStateValidationResult.Invalid("Invalid yielded challenge")
                    }
                }
                ChallengeStatus.REFUSED -> {
                    if (challenge.opportunityConsumed || challenge.rewardsGranted ||
                        challenge.opponentCurrentHp != opponent.maximumHp ||
                        challenge.experienceReward != 0 || challenge.goldTransferred != 0
                    ) {
                        return GameStateValidationResult.Invalid("Invalid refused challenge")
                    }
                }
            }
        }
        state.activeDragonCombat?.let { combat ->
            if (combat.recentMessages.isEmpty() ||
                combat.recentMessages.size > GameBalance.MAX_COMBAT_MESSAGES ||
                combat.recentMessages.any { it.text.isBlank() } ||
                combat.turnNumber < 0 || combat.goldLost < 0 ||
                combat.dragonCurrentHp !in 0..DragonSeasonBalance.DRAGON_MAXIMUM_HP
            ) {
                return GameStateValidationResult.Invalid("Invalid Dragon combat state")
            }
            when (combat.status) {
                DragonCombatStatus.ACTIVE -> if (
                    combat.dragonCurrentHp <= 0 || combat.attemptRecorded ||
                    season.stage != DragonStage.CHALLENGE_WINDOW
                ) {
                    return GameStateValidationResult.Invalid("Invalid active Dragon combat")
                }
                DragonCombatStatus.PLAYER_VICTORY -> if (
                    combat.dragonCurrentHp != 0 || !combat.attemptRecorded ||
                    season.stage != DragonStage.RESOLVED || season.victorId != state.player.id
                ) {
                    return GameStateValidationResult.Invalid("Invalid Dragon victory")
                }
                DragonCombatStatus.PLAYER_DEFEAT,
                DragonCombatStatus.RETREATED,
                -> if (combat.dragonCurrentHp <= 0 || !combat.attemptRecorded) {
                    return GameStateValidationResult.Invalid("Invalid completed Dragon attempt")
                }
            }
        }
        return GameStateValidationResult.Valid
    }

    private fun isLawfulEvent(
        event: WorldEvent,
        currentDay: Int,
        knownCharacterIds: Set<String>,
    ): Boolean = event.id.isNotBlank() &&
        event.day in 1..currentDay &&
        event.characterId in knownCharacterIds &&
        (event.relatedCharacterId == null || event.relatedCharacterId in knownCharacterIds) &&
        event.importance in 1..3 &&
        event.value >= 0 && event.previousRank >= 0 && event.newRank >= 0 &&
        event.beforeValue >= 0 && event.afterValue >= 0 && event.relationshipDelta >= 0
}
