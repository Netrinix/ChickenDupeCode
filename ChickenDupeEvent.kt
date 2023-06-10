package me.foshou.case.dupe

import org.bukkit.entity.Chicken
import org.bukkit.event.HandlerList
import org.bukkit.event.entity.EntityEvent
import org.bukkit.inventory.ItemStack


class ChickenDupeEvent(private val chicken: Chicken) : EntityEvent(chicken) {
    private val handlers = HandlerList()

    override fun getHandlers() = handlers

    fun getChicken() = chicken
    fun getItem(): ItemStack? {
        if (chicken.scoreboardTags.size >= 1) {
            try {
                val i: Array<ItemStack?> = ChickenDupeRewrite.itemStacksFromString(
                    chicken.scoreboardTags.toTypedArray()[0] as String
                )
                if (i[0]?.type.toString().contains("_SHULKER_BOX")) {
                    return i[0]
                }
            } catch (ignored: Exception) {
            }
        }
        return null
    }
}
