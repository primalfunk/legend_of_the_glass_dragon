package com.legendoftheglassdragon.game

import kotlin.math.max

/**
 * Converts authoritative state transitions into compact campaign facts. It is idempotent for
 * ordinary recomposition because callers pass the saved previous state and only persist the
 * returned state once.
 */
object TrophyStatsTracker {
    fun observe(previous: GameState?, next: GameState): GameState {
        if (previous == null || previous.revision >= next.revision) return seed(next)
        val before = previous.trophyStats
        var stats = before.copy(
            maxGoldHeld = max(before.maxGoldHeld, next.player.gold + next.player.bankedGold),
            bestExperience = max(before.bestExperience, next.player.experience),
            weaponIdsOwned = (before.weaponIdsOwned + next.player.weaponId).distinct(),
            armorIdsOwned = (before.armorIdsOwned + next.player.armorId).distinct(),
        )

        if (next.currentDay > previous.currentDay) {
            val clean = !previous.playerDay.wasDefeatedToday && previous.trophyStats.forestBattlesLost == before.forestBattlesLost
            stats = stats.copy(
                daysCompleted = before.daysCompleted + (next.currentDay - previous.currentDay),
                consecutiveDaysWithoutDefeat = if (clean) before.consecutiveDaysWithoutDefeat + 1 else 0,
                bestConsecutiveDaysWithoutDefeat = max(
                    before.bestConsecutiveDaysWithoutDefeat,
                    if (clean) before.consecutiveDaysWithoutDefeat + 1 else 0,
                ),
                perfectDays = before.perfectDays + if (
                    previous.playerDay.fightsRemaining == 0 &&
                        !previous.playerDay.wasDefeatedToday &&
                        before.forestBattlesWon > 0
                ) 1 else 0,
            )
        }

        val previousCombat = previous.activeCombat
        val nextCombat = next.activeCombat
        if (nextCombat != null && previousCombat?.status == CombatStatus.ACTIVE && nextCombat.status != CombatStatus.ACTIVE) {
            val monster = MonsterCatalog.require(nextCombat.monsterId)
            val won = nextCombat.status == CombatStatus.VICTORY
            val defeatedIds = if (won) (before.defeatedEnemyIds + monster.id).distinct() else before.defeatedEnemyIds
            val injured = next.player.currentHp * 10 < next.player.maximumHp
            val streak = if (won) before.forestWinStreak + 1 else 0
            val dangerous = previous.player.currentHp * 2 < previous.player.maximumHp && won
            stats = stats.copy(
                forestBattlesWon = before.forestBattlesWon + if (won) 1 else 0,
                forestBattlesLost = before.forestBattlesLost + if (nextCombat.status == CombatStatus.DEFEAT) 1 else 0,
                forestWinStreak = streak,
                bestForestWinStreak = max(before.bestForestWinStreak, streak),
                defeatedEnemyIds = defeatedIds,
                enemyIdsThatDefeatedPlayer = if (nextCombat.status == CombatStatus.DEFEAT) (before.enemyIdsThatDefeatedPlayer + monster.id).distinct() else before.enemyIdsThatDefeatedPlayer,
                lowHpForestVictories = before.lowHpForestVictories + if (won && injured) 1 else 0,
                dangerousBattlesWon = before.dangerousBattlesWon + if (dangerous) 1 else 0,
                dangerousSequenceWins = if (dangerous) before.dangerousSequenceWins + 1 else before.dangerousSequenceWins,
                finalFightVictories = before.finalFightVictories + if (won && previous.playerDay.fightsRemaining == 0) 1 else 0,
                meaningfulDefeats = before.meaningfulDefeats + if (nextCombat.status == CombatStatus.DEFEAT) 1 else 0,
                maxExperienceBeforeDefeat = if (nextCombat.status == CombatStatus.DEFEAT) max(before.maxExperienceBeforeDefeat, before.bestExperience) else before.maxExperienceBeforeDefeat,
                maxExperienceAfterDefeat = before.maxExperienceAfterDefeat,
                giantKills = before.giantKills + if (won && monster.intendedLevel >= previous.player.level + 2) 1 else 0,
                defeatedAfterBeingDefeatedIds = if (won && monster.id in before.enemyIdsThatDefeatedPlayer) (before.defeatedAfterBeingDefeatedIds + monster.id).distinct() else before.defeatedAfterBeingDefeatedIds,
            )
            if (nextCombat.status == CombatStatus.DEFEAT) {
                stats = stats.copy(dangerousSequenceWins = 0)
            }
        }
        if (previous.activeCombat == null && next.activeCombat?.status == CombatStatus.ACTIVE) {
            stats = stats.copy(encounteredEnemyIds = (stats.encounteredEnemyIds + next.activeCombat.monsterId).distinct())
        }
        if (next.player.experience > stats.maxExperienceAfterDefeat && stats.meaningfulDefeats > 0) {
            stats = stats.copy(maxExperienceAfterDefeat = next.player.experience)
        }

        if (previous.player.level < next.player.level && next.playerDay.lastTrainingSucceeded == true) {
            stats = stats.copy(
                trainingWins = before.trainingWins + 1,
                overqualifiedAdvancement = before.overqualifiedAdvancement || ProgressionRules.isTrainingEligible(previous.player),
            )
        }

        val beforeWealth = previous.player.gold + previous.player.bankedGold
        val afterWealth = next.player.gold + next.player.bankedGold
        val lossWasDefeat = next.activeCombat?.status == CombatStatus.DEFEAT ||
            next.activeChallenge?.status == ChallengeStatus.PLAYER_DEFEAT ||
            next.activeDragonCombat?.status == DragonCombatStatus.PLAYER_DEFEAT
        if (!lossWasDefeat && afterWealth < beforeWealth) {
            val spent = beforeWealth - afterWealth
            val changedEquipment = previous.player.weaponId != next.player.weaponId || previous.player.armorId != next.player.armorId
            stats = stats.copy(
                cumulativeGoldSpent = before.cumulativeGoldSpent + spent,
                largestPurchase = max(before.largestPurchase, if (changedEquipment) spent else 0),
            )
        }
        stats = stats.copy(
            notableCommunityEvents = max(
                before.notableCommunityEvents,
                next.community.recentEvents.count { it.characterId == next.player.id || it.relatedCharacterId == next.player.id },
            ),
        )

        val previousChallenge = previous.activeChallenge
        val nextChallenge = next.activeChallenge
        if (nextChallenge != null && previousChallenge?.status == ChallengeStatus.ACTIVE && nextChallenge.status != ChallengeStatus.ACTIVE) {
            stats = when (nextChallenge.status) {
                ChallengeStatus.PLAYER_VICTORY -> {
                    val opponent = previous.community.adventurers.firstOrNull { it.id == nextChallenge.opponentId }
                    stats.copy(
                        challengeWinNpcIds = stats.challengeWinNpcIds + nextChallenge.opponentId,
                        // Substitution for a draft optional-encounter concept: a voluntary
                        // challenge against a stronger simulated adventurer is the existing
                        // dangerous optional choice.
                        dangerousOptionalSuccess = stats.dangerousOptionalSuccess ||
                            (opponent != null && opponent.level > previous.player.level),
                    )
                }
                ChallengeStatus.PLAYER_DEFEAT -> stats.copy(
                    challengeLossNpcIds = stats.challengeLossNpcIds + nextChallenge.opponentId,
                    dangerousSequenceWins = 0,
                    meaningfulDefeats = stats.meaningfulDefeats + 1,
                )
                else -> stats
            }
        }

        val ranking = CommunityRules.rankings(next)
        val rank = ranking.indexOfFirst { it.characterId == next.player.id }.let { if (it < 0) Int.MAX_VALUE else it + 1 }
        if (rank < Int.MAX_VALUE) {
            val initialRank = if (before.rankAtCampaignStart == Int.MAX_VALUE) rank else before.rankAtCampaignStart
            stats = stats.copy(
                bestRank = minOf(before.bestRank, rank),
                rankAtCampaignStart = initialRank,
                firstPlaceStreak = if (rank == 1) before.firstPlaceStreak + 1 else 0,
                bestFirstPlaceStreak = max(before.bestFirstPlaceStreak, if (rank == 1) before.firstPlaceStreak + 1 else 0),
            )
        }

        val knownEvents = (previous.season.incidents.map { it.id } + previous.trophyStats.rareEncounterIds).toSet()
        val newRare = next.season.incidents.filter { it.id !in knownEvents && it.type.ordinal >= DragonIncidentType.GLASS_FRAGMENTS.ordinal }
            .map { it.id }
        if (newRare.isNotEmpty()) stats = stats.copy(
            rareEncounterIds = (stats.rareEncounterIds + newRare).distinct(),
            rarePathFound = stats.rarePathFound || next.season.incidents.any {
                it.type == DragonIncidentType.DRAGON_SIGHTING || it.type == DragonIncidentType.CHAMPION_CALL
            },
        )

        if (next.season.stage == DragonStage.RESOLVED && next.season.victorId == next.player.id) {
            val ready = next.player.level >= 8 && EquipmentCatalog.requireWeapon(next.player.weaponId).tier + EquipmentCatalog.requireArmor(next.player.armorId).tier >= 12
            stats = stats.copy(
                dragonVictoryInCampaign = true,
                dragonVictoryDay = next.currentDay,
                lowHpDragonVictory = stats.lowHpDragonVictory || next.player.currentHp * 10 < next.player.maximumHp,
                dragonReady = stats.dragonReady || ready,
            )
        }
        if (next.activeDragonCombat?.status == DragonCombatStatus.PLAYER_DEFEAT &&
            previous.activeDragonCombat?.status == DragonCombatStatus.ACTIVE
        ) {
            stats = stats.copy(meaningfulDefeats = stats.meaningfulDefeats + 1)
        }
        return next.copy(trophyStats = stats)
    }

    private fun seed(state: GameState): GameState = state.copy(
        trophyStats = state.trophyStats.copy(
            maxGoldHeld = max(state.trophyStats.maxGoldHeld, state.player.gold + state.player.bankedGold),
            bestExperience = max(state.trophyStats.bestExperience, state.player.experience),
            weaponIdsOwned = (state.trophyStats.weaponIdsOwned + state.player.weaponId).distinct(),
            armorIdsOwned = (state.trophyStats.armorIdsOwned + state.player.armorId).distinct(),
        ),
    )
}
