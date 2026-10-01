package com.legendoftheglassdragon.game

data class MonsterDefinition(
    val id: String,
    val displayName: String,
    val intendedLevel: Int,
    val maximumHp: Int,
    val attack: Int,
    val defense: Int,
    val experienceReward: Int,
    val minimumGoldReward: Int,
    val maximumGoldReward: Int,
    val encounterText: String,
)

object MonsterCatalog {
    val all: List<MonsterDefinition> = listOf(
        MonsterDefinition("giant_rat", "Giant Rat", 1, 8, 2, 0, 6, 2, 4,
            "A Giant Rat lunges from beneath the bracken!"),
        MonsterDefinition("cave_bat", "Cave Bat", 1, 9, 3, 0, 7, 2, 5,
            "A Cave Bat dives from the dark canopy!"),
        MonsterDefinition("forest_spider", "Forest Spider", 1, 10, 3, 1, 9, 3, 6,
            "A Forest Spider drops silently across your path!"),
        MonsterDefinition("goblin", "Goblin", 1, 12, 4, 1, 12, 4, 8,
            "A Goblin leaps from behind a fallen tree!"),
        MonsterDefinition("wild_boar", "Wild Boar", 2, 15, 5, 3, 16, 6, 11,
            "A Wild Boar charges through the undergrowth!"),
        MonsterDefinition("highwayman", "Highwayman", 2, 17, 6, 3, 18, 8, 14,
            "A Highwayman blocks the old forest road!"),
        MonsterDefinition("skeleton", "Skeleton", 3, 19, 6, 4, 24, 13, 19,
            "A Skeleton rises from a moss-covered grave!"),
        MonsterDefinition("orc_scout", "Orc Scout", 3, 21, 7, 4, 28, 15, 22,
            "An Orc Scout steps from the pines, blade drawn!"),
        MonsterDefinition("dire_wolf", "Dire Wolf", 4, 23, 8, 5, 36, 14, 20,
            "A Dire Wolf circles from between the black pines!"),
        MonsterDefinition("bandit_captain", "Bandit Captain", 4, 25, 9, 5, 40, 16, 22,
            "A Bandit Captain levels a nicked sword at you!"),
        MonsterDefinition("ogre_brute", "Ogre Brute", 5, 27, 9, 6, 48, 16, 23,
            "An Ogre Brute crashes through the brush!"),
        MonsterDefinition("grave_wight", "Grave Wight", 5, 29, 10, 6, 52, 18, 27,
            "A Grave Wight rises in a halo of cold mist!"),
        MonsterDefinition("bridge_troll", "Bridge Troll", 6, 31, 10, 7, 62, 18, 26,
            "A Bridge Troll lumbers down from the old stones!"),
        MonsterDefinition("dark_knight", "Dark Knight", 6, 33, 11, 7, 68, 21, 29,
            "A Dark Knight rides silently onto the forest road!"),
        MonsterDefinition("marsh_wyvern", "Marsh Wyvern", 7, 35, 11, 8, 78, 21, 29,
            "A Marsh Wyvern descends with a leathery shriek!"),
        MonsterDefinition("thorn_warlock", "Thorn Warlock", 7, 37, 12, 8, 84, 23, 32,
            "A Thorn Warlock steps through a ring of green fire!"),
        MonsterDefinition("demon_hound", "Demon Hound", 8, 39, 13, 9, 96, 24, 33,
            "A Demon Hound pads from the smoke, eyes burning!"),
        MonsterDefinition("hill_giant", "Hill Giant", 8, 41, 14, 9, 104, 26, 36,
            "A Hill Giant tears an oak sapling from the earth!"),
        MonsterDefinition("lich_knight", "Lich Knight", 9, 43, 14, 10, 116, 27, 37,
            "A Lich Knight raises a frost-rimed blade!"),
        MonsterDefinition("ancient_wyrm", "Ancient Wyrm", 9, 45, 15, 10, 124, 30, 40,
            "An Ancient Wyrm uncoils beneath the ruined watchtower!"),
        MonsterDefinition("crystal_warden", "Crystal Warden", 10, 47, 15, 11, 138, 31, 42,
            "A Crystal Warden awakens with a glass-bright chime!"),
        MonsterDefinition("dragon_disciple", "Dragon Disciple", 10, 49, 16, 11, 148, 34, 46,
            "A Dragon Disciple bars the path to the high peaks!"),
    )

    fun find(id: String): MonsterDefinition? = all.firstOrNull { it.id == id }

    fun require(id: String): MonsterDefinition =
        requireNotNull(find(id)) { "Unknown monster: $id" }
}
