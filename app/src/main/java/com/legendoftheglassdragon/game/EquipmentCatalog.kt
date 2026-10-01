package com.legendoftheglassdragon.game

data class WeaponDefinition(
    val id: String,
    val displayName: String,
    val attackContribution: Int,
    val purchasePrice: Int,
    val resaleValue: Int,
    val tier: Int,
    val minimumLevel: Int,
)

data class ArmorDefinition(
    val id: String,
    val displayName: String,
    val defenseContribution: Int,
    val purchasePrice: Int,
    val resaleValue: Int,
    val tier: Int,
    val minimumLevel: Int,
)

object EquipmentCatalog {
    const val STARTER_WEAPON_ID = "rusty_sword"
    const val STARTER_ARMOR_ID = "worn_leather"

    val weapons = listOf(
        WeaponDefinition(STARTER_WEAPON_ID, "Rusty Sword", 0, 0, 0, 0, 1),
        WeaponDefinition("iron_sword", "Iron Sword", 1, 35, 15, 1, 1),
        WeaponDefinition("steel_blade", "Steel Blade", 3, 80, 35, 2, 1),
        WeaponDefinition("knights_sword", "Knight's Sword", 5, 160, 70, 3, 2),
        WeaponDefinition("broad_sword", "Broad Sword", 7, 300, 135, 4, 3),
        WeaponDefinition("steel_longsword", "Steel Longsword", 9, 550, 250, 5, 4),
        WeaponDefinition("battle_axe", "Battle Axe", 12, 900, 410, 6, 6),
        WeaponDefinition("runed_blade", "Runed Blade", 15, 1_500, 700, 7, 8),
    )

    val armors = listOf(
        ArmorDefinition(STARTER_ARMOR_ID, "Worn Leather", 0, 0, 0, 0, 1),
        ArmorDefinition("studded_leather", "Studded Leather", 1, 35, 15, 1, 1),
        ArmorDefinition("chain_mail", "Chain Mail", 3, 85, 35, 2, 1),
        ArmorDefinition("plate_armor", "Plate Armor", 5, 180, 80, 3, 2),
        ArmorDefinition("scale_armor", "Scale Armor", 7, 325, 145, 4, 3),
        ArmorDefinition("knight_plate", "Knight Plate", 9, 575, 260, 5, 4),
        ArmorDefinition("mithril_mail", "Mithril Mail", 12, 950, 430, 6, 6),
        ArmorDefinition("dragon_scale", "Dragon Scale", 15, 1_600, 750, 7, 8),
    )

    fun weapon(id: String): WeaponDefinition? = weapons.firstOrNull { it.id == id }

    fun armor(id: String): ArmorDefinition? = armors.firstOrNull { it.id == id }

    fun requireWeapon(id: String): WeaponDefinition = requireNotNull(weapon(id))

    fun requireArmor(id: String): ArmorDefinition = requireNotNull(armor(id))

    fun weaponIdForLegacyValue(value: String): String? =
        weapons.firstOrNull { it.id == value || it.displayName == value }?.id

    fun armorIdForLegacyValue(value: String): String? =
        armors.firstOrNull { it.id == value || it.displayName == value }?.id

    fun nextWeapon(currentId: String): WeaponDefinition? {
        val resolved = weaponIdForLegacyValue(currentId) ?: return null
        return weapons.getOrNull(weapons.indexOfFirst { it.id == resolved } + 1)
    }

    fun nextArmor(currentId: String): ArmorDefinition? {
        val resolved = armorIdForLegacyValue(currentId) ?: return null
        return armors.getOrNull(armors.indexOfFirst { it.id == resolved } + 1)
    }

    fun totalAttack(baseAttack: Int, weaponId: String): Int =
        safeTotal(baseAttack, requireWeapon(weaponId).attackContribution)

    fun totalDefense(baseDefense: Int, armorId: String): Int =
        safeTotal(baseDefense, requireArmor(armorId).defenseContribution)

    private fun safeTotal(base: Int, contribution: Int): Int =
        (base.toLong() + contribution).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
}
