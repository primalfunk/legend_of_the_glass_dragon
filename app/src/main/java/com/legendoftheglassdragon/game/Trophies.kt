package com.legendoftheglassdragon.game

/** Stable IDs are intentionally independent from copy shown in the cabinet. */
enum class TrophyCategory(val label: String) {
    PROGRESSION("Progression"),
    COMBAT("Combat mastery"),
    ENDURANCE("Endurance"),
    WEALTH("Wealth & equipment"),
    COMMUNITY("Simulated community"),
    DISCOVERY("Discovery"),
    DRAGON("The Glass Dragon"),
}

data class TrophyDefinition(
    val id: String,
    val number: Int,
    val name: String,
    val description: String,
    val category: TrophyCategory,
    val secret: Boolean = false,
    val progress: ((GameState, TrophyMetagameSnapshot) -> TrophyProgress?)? = null,
)

data class TrophyProgress(val current: Int, val target: Int)

data class TrophyMetagameSnapshot(
    val earnedIds: Set<String> = emptySet(),
    val dragonVictories: Int = 0,
) {
    val earnedCount: Int get() = earnedIds.size
}

data class TrophyUnlock(val trophy: TrophyDefinition, val progress: TrophyProgress? = null)

/** The complete initial catalogue. Keep this list append-only once IDs ship. */
object TrophyCatalog {
    const val NORMAL_MAX_LEVEL = 10
    const val TOP_TIER = 7

    val all: List<TrophyDefinition> = listOf(
        t("first_blood", 1, "FIRST BLOOD", "Defeat 25 enemies during a character's lifetime.", TrophyCategory.PROGRESSION, p = { s, _ -> count(s.trophyStats.forestBattlesWon, 25) }),
        t("seasoned_adventurer", 2, "SEASONED ADVENTURER", "Survive 10 game days.", TrophyCategory.PROGRESSION, p = { s, _ -> count(s.trophyStats.daysCompleted, 10) }),
        t("old_hand", 3, "OLD HAND", "Survive 25 game days.", TrophyCategory.PROGRESSION, p = { s, _ -> count(s.trophyStats.daysCompleted, 25) }),
        t("veteran_of_the_forest", 4, "VETERAN OF THE FOREST", "Win 100 forest battles.", TrophyCategory.PROGRESSION, p = { s, _ -> count(s.trophyStats.forestBattlesWon, 100) }),
        t("the_long_road", 5, "THE LONG ROAD", "Reach the highest normal character level.", TrophyCategory.PROGRESSION, p = { s, _ -> count(s.player.level, NORMAL_MAX_LEVEL) }),
        t("student_becomes_master", 6, "THE STUDENT BECOMES THE MASTER", "Complete the full required master/trainer progression.", TrophyCategory.PROGRESSION, p = { s, _ -> count(s.trophyStats.trainingWins, NORMAL_MAX_LEVEL - 1) }),
        t("overqualified", 7, "OVERQUALIFIED", "Advance after qualifying for advancement earlier.", TrophyCategory.PROGRESSION),
        t("master_at_arms", 8, "MASTER AT ARMS", "Obtain a top-tier weapon.", TrophyCategory.PROGRESSION),
        t("dressed_for_war", 9, "DRESSED FOR WAR", "Obtain top-tier armor.", TrophyCategory.PROGRESSION),
        t("fully_armed", 10, "FULLY ARMED", "Possess top-tier weapon and armor together.", TrophyCategory.PROGRESSION),

        t("untouchable", 11, "UNTOUCHABLE", "Win 10 consecutive forest battles.", TrophyCategory.COMBAT, p = { s, _ -> count(s.trophyStats.bestForestWinStreak, 10) }),
        t("relentless", 12, "RELENTLESS", "Win 25 consecutive forest battles.", TrophyCategory.COMBAT, secret = true, p = { s, _ -> count(s.trophyStats.bestForestWinStreak, 25) }),
        t("against_the_odds", 13, "AGAINST THE ODDS", "Defeat an enemy meaningfully stronger than you.", TrophyCategory.COMBAT),
        t("last_breath", 14, "LAST BREATH", "Win a battle below 10% maximum HP.", TrophyCategory.COMBAT, p = { s, _ -> count(s.trophyStats.lowHpForestVictories, 1) }),
        t("no_retreat", 15, "NO RETREAT", "Win three dangerous battles begun while injured.", TrophyCategory.COMBAT, p = { s, _ -> count(s.trophyStats.dangerousSequenceWins, 3) }),
        t("perfect_hunt", 16, "PERFECT HUNT", "Complete a full day's forest fighting without losing.", TrophyCategory.COMBAT, p = { s, _ -> count(s.trophyStats.perfectDays, 1) }),
        t("one_more_fight", 17, "ONE MORE FIGHT", "Win the final forest fight of a day.", TrophyCategory.COMBAT, secret = true, p = { s, _ -> count(s.trophyStats.finalFightVictories, 1) }),
        t("giant_killer", 18, "GIANT KILLER", "Defeat an opponent far above your expected strength.", TrophyCategory.COMBAT, p = { s, _ -> count(s.trophyStats.giantKills, 1) }),
        t("hard_lessons", 19, "HARD LESSONS", "Lose to enemy types and later defeat those same types.", TrophyCategory.COMBAT, secret = true, p = { s, _ -> count(s.trophyStats.defeatedAfterBeingDefeatedIds.size, 2) }),
        t("back_from_the_dead", 20, "BACK FROM THE DEAD", "Exceed your progression high-water mark after defeat.", TrophyCategory.COMBAT, secret = true),

        t("iron_constitution", 21, "IRON CONSTITUTION", "Complete five consecutive days without defeat.", TrophyCategory.ENDURANCE, p = { s, _ -> count(s.trophyStats.bestConsecutiveDaysWithoutDefeat, 5) }),
        t("unbroken", 22, "UNBROKEN", "Defeat the Glass Dragon without suffering a defeat.", TrophyCategory.ENDURANCE),
        t("scenic_route", 23, "THE SCENIC ROUTE", "Defeat the Glass Dragon after an unusually long campaign.", TrophyCategory.ENDURANCE, p = { s, _ -> count(if (s.trophyStats.dragonVictoryDay >= 35) 1 else 0, 1) }),
        t("speedrunner", 24, "SPEEDRUNNER", "Defeat the Glass Dragon in 12 days or fewer.", TrophyCategory.ENDURANCE, p = { s, _ -> count(if (s.trophyStats.dragonVictoryDay in 1..12) 1 else 0, 1) }),
        t("life_well_lived", 25, "A LIFE WELL LIVED", "Complete a successful campaign with several substantial accomplishments.", TrophyCategory.ENDURANCE),

        t("heavy_purse", 26, "HEAVY PURSE", "Accumulate 1,000 gold at one time.", TrophyCategory.WEALTH, p = { s, _ -> count(s.trophyStats.maxGoldHeld, 1_000) }),
        t("fortune_favors_the_bold", 27, "FORTUNE FAVORS THE BOLD", "Win a risky community challenge while carrying at least 250 gold.", TrophyCategory.WEALTH, secret = true),
        t("patron_of_the_realm", 28, "PATRON OF THE REALM", "Spend 3,000 gold on legitimate progression and services.", TrophyCategory.WEALTH, p = { s, _ -> count(s.trophyStats.cumulativeGoldSpent, 3_000) }),
        t("arsenal", 29, "ARSENAL", "Acquire six different weapon tiers.", TrophyCategory.WEALTH, p = { s, _ -> count(s.trophyStats.currentWeapons.size, 6) }),
        t("armorers_friend", 30, "ARMORER'S FRIEND", "Acquire six different armor tiers.", TrophyCategory.WEALTH, p = { s, _ -> count(s.trophyStats.currentArmors.size, 6) }),
        t("from_rags_to_riches", 31, "FROM RAGS TO RICHES", "Rise from near-poverty to 1,000 gold in one campaign.", TrophyCategory.WEALTH, p = { s, _ -> count(s.trophyStats.maxGoldHeld, 1_000) }),
        t("big_spender", 32, "BIG SPENDER", "Make a purchase costing at least 1,500 gold.", TrophyCategory.WEALTH, p = { s, _ -> count(s.trophyStats.largestPurchase, 1_500) }),
        t("prepared_for_anything", 33, "PREPARED FOR ANYTHING", "Reach the Dragon confrontation in exceptional material condition.", TrophyCategory.WEALTH, secret = true),

        t("everybody_knows_your_name", 34, "EVERYBODY KNOWS YOUR NAME", "Reach the top three in the simulated rankings.", TrophyCategory.COMMUNITY, p = { s, _ -> count(if (s.trophyStats.bestRank <= 3) 1 else 0, 1) }),
        t("social_climber", 35, "SOCIAL CLIMBER", "Rise from the lower rankings into the upper tier.", TrophyCategory.COMMUNITY),
        t("number_one", 36, "NUMBER ONE", "Finish a day as the highest-ranked adventurer.", TrophyCategory.COMMUNITY),
        t("living_legend", 37, "LIVING LEGEND", "Maintain the top ranking for three consecutive days.", TrophyCategory.COMMUNITY, p = { s, _ -> count(s.trophyStats.bestFirstPlaceStreak, 3) }),
        t("rivalry", 38, "RIVALRY", "Defeat the same simulated rival twice.", TrophyCategory.COMMUNITY, p = { s, _ -> count(s.trophyStats.challengeWinNpcIds.groupingBy { it }.eachCount().values.maxOrNull() ?: 0, 2) }),
        t("nemesis", 39, "NEMESIS", "Defeat a rival who has defeated you.", TrophyCategory.COMMUNITY, secret = true),
        t("local_celebrity", 40, "LOCAL CELEBRITY", "Become repeatedly notable in the realm's community record.", TrophyCategory.COMMUNITY, p = { s, _ -> count(s.trophyStats.notableCommunityEvents, 5) }),

        t("seen_it_all", 41, "SEEN IT ALL", "Encounter every normal enemy type.", TrophyCategory.DISCOVERY, secret = true, p = { s, _ -> count(s.trophyStats.encounteredEnemies.intersect(MonsterCatalog.all.map { it.id }.toSet()).size, MonsterCatalog.all.size) }),
        t("monster_scholar", 42, "MONSTER SCHOLAR", "Defeat every normal enemy type at least once.", TrophyCategory.DISCOVERY, p = { s, _ -> count(s.trophyStats.defeatedEnemies.intersect(MonsterCatalog.all.map { it.id }.toSet()).size, MonsterCatalog.all.size) }),
        t("stranger_things", 43, "STRANGER THINGS", "Discover three rare or special encounters.", TrophyCategory.DISCOVERY, secret = true, p = { s, _ -> count(s.trophyStats.rareEncounterIds.size, 3) }),
        t("against_better_judgment", 44, "AGAINST BETTER JUDGMENT", "Survive an unusually dangerous optional encounter.", TrophyCategory.DISCOVERY, secret = true),
        t("road_less_traveled", 45, "THE ROAD LESS TRAVELED", "Discover a rare path in the realm.", TrophyCategory.DISCOVERY, secret = true),

        t("dragon_slayer", 46, "DRAGON SLAYER", "Defeat the Glass Dragon for the first time.", TrophyCategory.DRAGON, p = { _, m -> count(m.dragonVictories, 1) }),
        t("dragonbane", 47, "DRAGONBANE", "Defeat the Glass Dragon three times across your metagame history.", TrophyCategory.DRAGON, p = { _, m -> count(m.dragonVictories, 3) }),
        t("legend", 48, "LEGEND", "Defeat the Glass Dragon ten times.", TrophyCategory.DRAGON, p = { _, m -> count(m.dragonVictories, 10) }),
        t("by_a_thread", 49, "BY A THREAD", "Defeat the Glass Dragon below 10% maximum HP.", TrophyCategory.DRAGON, secret = true),
        t("legend_of_the_glass_dragon", 50, "LEGEND OF THE GLASS DRAGON", "Earn at least 40 of the other 49 trophies.", TrophyCategory.DRAGON),
    )

    private fun t(id: String, number: Int, name: String, description: String, category: TrophyCategory, secret: Boolean = false, p: ((GameState, TrophyMetagameSnapshot) -> TrophyProgress?)? = null) = TrophyDefinition(id, number, name, description, category, secret, p)
    private fun count(current: Int, target: Int) = TrophyProgress(current.coerceAtMost(target), target)
    private fun count(current: Boolean, target: Int) = count(if (current) 1 else 0, target)

    fun require(id: String): TrophyDefinition = all.first { it.id == id }
}

object TrophyEngine {
    fun unlocked(state: GameState, snapshot: TrophyMetagameSnapshot = TrophyMetagameSnapshot()): List<TrophyUnlock> {
        val stats = state.trophyStats
        val dragonVictory = stats.dragonVictoryInCampaign && state.season.victorId == state.player.id
        val losses = stats.challengeLossNpcIds.toSet()
        val wins = stats.challengeWinNpcIds.toSet()
        val basic = buildSet {
            if (stats.overqualifiedAdvancement) add("overqualified")
            if (stats.giantKills > 0) add("against_the_odds")
            if (stats.currentWeapons.any { EquipmentCatalog.requireWeapon(it).tier >= TrophyCatalog.TOP_TIER }) add("master_at_arms")
            if (stats.currentArmors.any { EquipmentCatalog.requireArmor(it).tier >= TrophyCatalog.TOP_TIER }) add("dressed_for_war")
            if (stats.currentWeapons.any { EquipmentCatalog.requireWeapon(it).tier >= TrophyCatalog.TOP_TIER } && stats.currentArmors.any { EquipmentCatalog.requireArmor(it).tier >= TrophyCatalog.TOP_TIER }) add("fully_armed")
            if (stats.meaningfulDefeats > 0 && stats.maxExperienceAfterDefeat > stats.maxExperienceBeforeDefeat) add("back_from_the_dead")
            if (stats.dragonVictoryInCampaign && stats.meaningfulDefeats == 0) add("unbroken")
            if (dragonVictory && stats.dragonVictoryDay >= 1 && stats.bestRank <= 3 && stats.forestBattlesWon >= 25 && stats.currentWeapons.size >= 3) add("life_well_lived")
            if (stats.challengeWinNpcIds.any { it in losses }) add("nemesis")
            if (stats.rankAtCampaignStart > 3 && stats.bestRank <= 3) add("social_climber")
            if (stats.bestRank == 1) add("number_one")
            if (stats.dangerousOptionalSuccess) add("against_better_judgment")
            if (stats.rarePathFound) add("road_less_traveled")
            if (stats.maxGoldHeld >= 1_000 && stats.campaignStartGold <= 25) add("from_rags_to_riches")
            if (stats.challengeWinNpcIds.isNotEmpty() && stats.maxGoldHeld >= 250) add("fortune_favors_the_bold")
            if (stats.dragonReady) add("prepared_for_anything")
            if (stats.lowHpDragonVictory) add("by_a_thread")
        }
        val catalogUnlocks = TrophyCatalog.all.filter { it.id !in snapshot.earnedIds && it.id != "legend_of_the_glass_dragon" }
            .filter { definition ->
                definition.progress?.invoke(state, snapshot)?.let { progress ->
                    progress.current >= progress.target
                } == true
            }
            .map { TrophyUnlock(it, it.progress?.invoke(state, snapshot)) }
            .filter { it.trophy.id !in snapshot.earnedIds }
        val directUnlocks = basic.mapNotNull { id -> TrophyCatalog.all.firstOrNull { it.id == id } }
            .filter { it.id !in snapshot.earnedIds }
            .map { TrophyUnlock(it, it.progress?.invoke(state, snapshot)) }
        val possibleOthers = snapshot.earnedCount + catalogUnlocks.size + directUnlocks.size
        val capstone = TrophyCatalog.require("legend_of_the_glass_dragon")
        return (catalogUnlocks + directUnlocks).distinctBy { it.trophy.id } +
            if (possibleOthers >= 40 && capstone.id !in snapshot.earnedIds) listOf(TrophyUnlock(capstone)) else emptyList()
    }
}
