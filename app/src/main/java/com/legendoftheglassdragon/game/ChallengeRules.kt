package com.legendoftheglassdragon.game

import kotlin.math.max

object ChallengeRules {
    const val EXPERIENCE_BASE = 5
    const val EXPERIENCE_PER_OPPONENT_LEVEL = 3
    const val GOLD_STAKE_BASE = 8
    const val GOLD_STAKE_PER_OPPONENT_LEVEL = 4

    fun experienceReward(opponent: NpcAdventurer): Int =
        EXPERIENCE_BASE + opponent.level * EXPERIENCE_PER_OPPONENT_LEVEL

    fun goldStake(opponent: NpcAdventurer): Int =
        minOf(opponent.gold, GOLD_STAKE_BASE + opponent.level * GOLD_STAKE_PER_OPPONENT_LEVEL)

    fun acceptancePercent(opponent: NpcAdventurer): Int = when (opponent.personality) {
        NpcPersonality.AGGRESSIVE -> 100
        NpcPersonality.CAUTIOUS -> 65
        NpcPersonality.AMBITIOUS -> 90
        NpcPersonality.GREEDY -> 80
        NpcPersonality.SOCIAL -> 85
        NpcPersonality.ERRATIC -> 75
    }
}

class ChallengeEngine(
    private val random: GameRandom = PersistedGameRandom,
) {
    fun begin(
        state: GameState,
        expectedRevision: Long,
        opponentId: String,
    ): GameActionResult {
        stale(state, expectedRevision)?.let { return it }
        if (state.activeCombat != null) return rejected(GameActionRejection.ActiveCombat)
        if (state.activeChallenge != null) return rejected(GameActionRejection.ActiveChallenge)
        if (state.activeDragonCombat != null) return rejected(GameActionRejection.ActiveDragonCombat)
        if (state.playerDay.wasDefeatedToday || state.playerDay.challengesRemaining <= 0) {
            return rejected(GameActionRejection.NoChallengesRemaining)
        }
        val opponent = state.community.adventurers.firstOrNull { it.id == opponentId }
            ?: return rejected(GameActionRejection.ChallengeOpponentUnavailable)
        if (opponent.status != NpcStatus.ACTIVE ||
            CommunityRules.innVisitors(state).none { it.id == opponentId }
        ) {
            return rejected(GameActionRejection.ChallengeOpponentUnavailable)
        }

        val acceptance = random.nextInt(state.world, 100)
        if (acceptance.value >= ChallengeRules.acceptancePercent(opponent)) {
            return applied(
                state.copy(
                    world = acceptance.world,
                    activeChallenge = ChallengeState(
                        opponentId = opponent.id,
                        opponentCurrentHp = opponent.maximumHp,
                        status = ChallengeStatus.REFUSED,
                        recentMessages = listOf(
                            CombatMessage(
                                opponent.name + " declines with a thin smile. 'Not today.'",
                                CombatMessageKind.INFO,
                            ),
                        ),
                        opportunityConsumed = false,
                        rewardsGranted = false,
                    ),
                ),
            )
        }

        return applied(
            state.copy(
                world = acceptance.world,
                playerDay = state.playerDay.copy(
                    challengesRemaining = state.playerDay.challengesRemaining - 1,
                ),
                activeChallenge = ChallengeState(
                    opponentId = opponent.id,
                    opponentCurrentHp = opponent.maximumHp,
                    status = ChallengeStatus.ACTIVE,
                    recentMessages = listOf(
                        CombatMessage(
                            opponent.name + " accepts your challenge!",
                            CombatMessageKind.ENCOUNTER,
                        ),
                    ),
                    opportunityConsumed = true,
                    rewardsGranted = false,
                ),
            ),
        )
    }

    fun attack(state: GameState, expectedRevision: Long): GameActionResult {
        stale(state, expectedRevision)?.let { return it }
        val challenge = state.activeChallenge
            ?: return rejected(GameActionRejection.NoActiveChallenge)
        if (challenge.status != ChallengeStatus.ACTIVE) {
            return rejected(GameActionRejection.ChallengeAlreadyComplete)
        }
        val opponent = state.community.adventurers.firstOrNull { it.id == challenge.opponentId }
            ?: return rejected(GameActionRejection.ChallengeOpponentUnavailable)

        val playerRoll = random.nextInt(state.world, GameBalance.DAMAGE_RANDOM_VALUES)
        val playerDamage = damage(state.player.attack, opponent.defense, playerRoll.value)
        val opponentHp = (challenge.opponentCurrentHp - playerDamage).coerceAtLeast(0)
        var messages = appendMessage(
            challenge.recentMessages,
            CombatMessage(
                "You strike " + opponent.name + " for " + playerDamage + " damage!",
                CombatMessageKind.PLAYER_ACTION,
            ),
        )
        if (opponentHp == 0) {
            return playerVictory(state, challenge, opponent, playerRoll.world, messages)
        }

        val opponentRoll = random.nextInt(playerRoll.world, GameBalance.DAMAGE_RANDOM_VALUES)
        val opponentDamage = damage(opponent.attack, state.player.defense, opponentRoll.value)
        val playerHp = (state.player.currentHp - opponentDamage).coerceAtLeast(0)
        messages = appendMessage(
            messages,
            CombatMessage(
                opponent.name + " answers for " + opponentDamage + " damage!",
                CombatMessageKind.MONSTER_ACTION,
            ),
        )
        if (playerHp == 0) {
            return playerDefeat(state, challenge, opponent, opponentHp, opponentRoll.world, messages)
        }
        return applied(
            state.copy(
                world = opponentRoll.world,
                player = state.player.copy(currentHp = playerHp),
                activeChallenge = challenge.copy(
                    opponentCurrentHp = opponentHp,
                    recentMessages = messages,
                ),
            ),
        )
    }

    fun yield(state: GameState, expectedRevision: Long): GameActionResult {
        stale(state, expectedRevision)?.let { return it }
        val challenge = state.activeChallenge
            ?: return rejected(GameActionRejection.NoActiveChallenge)
        if (challenge.status != ChallengeStatus.ACTIVE) {
            return rejected(GameActionRejection.ChallengeAlreadyComplete)
        }
        val opponent = state.community.adventurers.firstOrNull { it.id == challenge.opponentId }
            ?: return rejected(GameActionRejection.ChallengeOpponentUnavailable)
        val updatedOpponent = opponent.copy(
            familiarity = minOf(CommunityRules.MAX_FAMILIARITY, opponent.familiarity + 1),
            rivalry = minOf(GameBalance.MAX_RIVALRY, opponent.rivalry + GameBalance.CHALLENGE_RIVALRY_YIELD_GAIN),
            playerLossesAgainst = safeAdd(opponent.playerLossesAgainst, 1),
            challengeWins = safeAdd(opponent.challengeWins, 1),
            lastChallengeDay = state.currentDay,
        )
        val event = challengeEvent(
            state = state,
            winnerId = opponent.id,
            loserId = state.player.id,
            detail = "PLAYER_YIELD",
        )
        return applied(
            state.copy(
                player = state.player.copy(challengeLosses = safeAdd(state.player.challengeLosses, 1)),
                community = state.community.copy(
                    adventurers = replaceNpc(state.community.adventurers, updatedOpponent),
                    recentEvents = appendEvent(state.community.recentEvents, event),
                ),
                activeChallenge = challenge.copy(
                    status = ChallengeStatus.YIELDED,
                    recentMessages = appendMessage(
                        challenge.recentMessages,
                        CombatMessage("You yield. " + opponent.name + " accepts the victory.", CombatMessageKind.ESCAPE),
                    ),
                ),
            ),
        )
    }

    fun leave(state: GameState, expectedRevision: Long): GameActionResult {
        stale(state, expectedRevision)?.let { return it }
        val challenge = state.activeChallenge
            ?: return rejected(GameActionRejection.NoActiveChallenge)
        if (challenge.status == ChallengeStatus.ACTIVE) {
            return rejected(GameActionRejection.ActiveChallenge)
        }
        return applied(state.copy(activeChallenge = null))
    }

    private fun playerVictory(
        state: GameState,
        challenge: ChallengeState,
        opponent: NpcAdventurer,
        world: WorldState,
        messages: List<CombatMessage>,
    ): GameActionResult {
        val experience = ChallengeRules.experienceReward(opponent)
        val stake = ChallengeRules.goldStake(opponent)
        val updatedOpponent = opponent.copy(
            gold = opponent.gold - stake,
            familiarity = minOf(CommunityRules.MAX_FAMILIARITY, opponent.familiarity + 1),
            rivalry = minOf(GameBalance.MAX_RIVALRY, opponent.rivalry + GameBalance.CHALLENGE_RIVALRY_WIN_GAIN),
            playerWinsAgainst = safeAdd(opponent.playerWinsAgainst, 1),
            challengeLosses = safeAdd(opponent.challengeLosses, 1),
            lastChallengeDay = state.currentDay,
        )
        val event = challengeEvent(state, state.player.id, opponent.id, "PLAYER_WIN")
        return applied(
            state.copy(
                world = world,
                player = state.player.copy(
                    experience = safeAdd(state.player.experience, experience),
                    gold = safeAdd(state.player.gold, stake),
                    challengeWins = safeAdd(state.player.challengeWins, 1),
                ),
                community = state.community.copy(
                    adventurers = replaceNpc(state.community.adventurers, updatedOpponent),
                    recentEvents = appendEvent(state.community.recentEvents, event),
                ),
                activeChallenge = challenge.copy(
                    opponentCurrentHp = 0,
                    status = ChallengeStatus.PLAYER_VICTORY,
                    recentMessages = appendMessage(
                        messages,
                        CombatMessage("*** " + opponent.name + " yields the floor! ***", CombatMessageKind.VICTORY),
                    ),
                    rewardsGranted = true,
                    experienceReward = experience,
                    goldTransferred = stake,
                ),
            ),
        )
    }

    private fun playerDefeat(
        state: GameState,
        challenge: ChallengeState,
        opponent: NpcAdventurer,
        opponentHp: Int,
        world: WorldState,
        messages: List<CombatMessage>,
    ): GameActionResult {
        val stake = minOf(state.player.gold, ChallengeRules.GOLD_STAKE_BASE + opponent.level * 2)
        val updatedOpponent = opponent.copy(
            gold = safeAdd(opponent.gold, stake),
            familiarity = minOf(CommunityRules.MAX_FAMILIARITY, opponent.familiarity + 1),
            rivalry = minOf(GameBalance.MAX_RIVALRY, opponent.rivalry + GameBalance.CHALLENGE_RIVALRY_LOSS_GAIN),
            playerLossesAgainst = safeAdd(opponent.playerLossesAgainst, 1),
            challengeWins = safeAdd(opponent.challengeWins, 1),
            lastChallengeDay = state.currentDay,
        )
        val event = challengeEvent(state, opponent.id, state.player.id, "PLAYER_LOSS")
        return applied(
            state.copy(
                world = world,
                player = state.player.copy(
                    currentHp = max(1, state.player.maximumHp / GameBalance.CHALLENGE_DEFEAT_RECOVERY_HP_DIVISOR),
                    gold = state.player.gold - stake,
                    challengeLosses = safeAdd(state.player.challengeLosses, 1),
                ),
                community = state.community.copy(
                    adventurers = replaceNpc(state.community.adventurers, updatedOpponent),
                    recentEvents = appendEvent(state.community.recentEvents, event),
                ),
                activeChallenge = challenge.copy(
                    opponentCurrentHp = opponentHp,
                    status = ChallengeStatus.PLAYER_DEFEAT,
                    recentMessages = appendMessage(
                        messages,
                        CombatMessage(opponent.name + " wins. The Inn helps you back to your feet.", CombatMessageKind.DEFEAT),
                    ),
                    goldTransferred = stake,
                ),
            ),
        )
    }

    private fun challengeEvent(
        state: GameState,
        winnerId: String,
        loserId: String,
        detail: String,
    ): WorldEvent = WorldEvent(
        id = "day-" + state.currentDay + "-player-challenge-" + state.revision,
        day = state.currentDay,
        type = WorldEventType.CHALLENGE,
        characterId = winnerId,
        relatedCharacterId = loserId,
        detail = detail,
        importance = 3,
        visibility = WorldEventVisibility.PUBLIC,
        location = WorldEventLocation.INN,
        relatedPriorEventId = state.community.recentEvents
            .asReversed()
            .firstOrNull { prior ->
                prior.type == WorldEventType.CHALLENGE &&
                    setOf(prior.characterId, prior.relatedCharacterId) == setOf(winnerId, loserId)
            }
            ?.id,
        relationshipDelta = when (detail) {
            "PLAYER_WIN" -> GameBalance.CHALLENGE_RIVALRY_WIN_GAIN
            "PLAYER_LOSS" -> GameBalance.CHALLENGE_RIVALRY_LOSS_GAIN
            "PLAYER_YIELD" -> GameBalance.CHALLENGE_RIVALRY_YIELD_GAIN
            else -> 0
        },
        directlyAffectsPlayer = true,
    )

    private fun appendEvent(events: List<WorldEvent>, event: WorldEvent): List<WorldEvent> =
        (events + event).takeLast(CommunityRules.MAX_RETAINED_EVENTS)

    private fun replaceNpc(npcs: List<NpcAdventurer>, updated: NpcAdventurer): List<NpcAdventurer> =
        npcs.map { if (it.id == updated.id) updated else it }

    private fun appendMessage(
        messages: List<CombatMessage>,
        message: CombatMessage,
    ): List<CombatMessage> = (messages + message).takeLast(GameBalance.MAX_COMBAT_MESSAGES)

    private fun damage(strength: Int, defense: Int, variation: Int): Int =
        max(GameBalance.MINIMUM_DAMAGE, strength + variation - defense)

    private fun stale(state: GameState, expectedRevision: Long): GameActionResult.Rejected? =
        if (state.revision == expectedRevision) null else rejected(GameActionRejection.StaleState)

    private fun rejected(reason: GameActionRejection): GameActionResult.Rejected =
        GameActionResult.Rejected(reason)

    private fun applied(state: GameState): GameActionResult.Applied =
        GameActionResult.Applied(state.copy(revision = state.revision + 1))

    private fun safeAdd(value: Int, amount: Int): Int =
        (value.toLong() + amount).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
}
