package com.legendoftheglassdragon.game

object CommunityFactory {
    const val POPULATION_SIZE = 16

    private val names = listOf(
        "Raven",
        "Cedric",
        "Thorn",
        "Mira",
        "Blackwood",
        "Elowen",
        "Garrick",
        "Willow",
        "Rook",
        "Selene",
        "Bram",
        "Isolde",
        "Flint",
        "Lyra",
        "Corwin",
        "Vesper",
    )

    fun create(
        worldSeed: Long,
        lastSimulatedDay: Int = 0,
    ): CommunityState {
        val offset = Math.floorMod(worldSeed, names.size.toLong()).toInt()
        val personalities = NpcPersonality.entries
        val adventurers = List(POPULATION_SIZE) { index ->
            val name = names[(index + offset) % names.size]
            val variation = Math.floorMod(worldSeed + index * 17L, 3L).toInt()
            NpcAdventurer(
                id = "npc-" + index.toString().padStart(2, '0') + "-" + name.lowercase(),
                name = name,
                level = 1,
                experience = variation * 3,
                maximumHp = 19 + variation,
                baseAttack = 4 + if (variation == 2) 1 else 0,
                baseDefense = 2 + if (variation == 1) 1 else 0,
                gold = 20 + variation * 7 + index % 5,
                weaponId = EquipmentCatalog.STARTER_WEAPON_ID,
                armorId = EquipmentCatalog.STARTER_ARMOR_ID,
                status = NpcStatus.ACTIVE,
                recoveryDaysRemaining = 0,
                personality = personalities[(index + offset) % personalities.size],
                familiarity = 0,
                lastMetDay = 0,
                lastAdvancedDay = lastSimulatedDay,
            )
        }
        return CommunityState(
            adventurers = adventurers,
            recentEvents = emptyList(),
            chronicleHistory = emptyList(),
            lastSimulatedDay = lastSimulatedDay,
        )
    }
}
