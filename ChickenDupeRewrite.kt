package me.foshou.case.dupe

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Chicken
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.ItemSpawnEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException


object ChickenDupeRewrite: Listener {

    abstract class ChickenDupeRunnable(val chicken: Chicken, val itemStack: ItemStack): BukkitRunnable() {
        override fun run() {
            chicken.world.dropItem(chicken.location, itemStack)
            Bukkit.getPluginManager().callEvent(ChickenDupeEvent(chicken))
        }
    }

    @EventHandler
    fun onRightClick(e: PlayerInteractEntityEvent) {
        if (e.player.hasPermission("2b2t.chicken")&&
            e.player.inventory.itemInMainHand.type.toString().contains("_SHULKER_BOX")&&
                e.rightClicked.type == EntityType.CHICKEN) {
            (e.rightClicked as Chicken).let {
                if (it.age < 0) return@onRightClick

                val itemStacks = arrayOfNulls<ItemStack>(1)
                itemStacks[0] = e.player.inventory.itemInMainHand
                val s: String? = itemStacksToString(itemStacks)
                it.scoreboardTags.clear()
                it.scoreboardTags.add(s)
                it.isCustomNameVisible = true
                it.customName = e.player.inventory.itemInMainHand.i18NDisplayName
                object : ChickenDupeRunnable(it, e.player.inventory.itemInMainHand) {
                }
            }
        }
    }

    private val runnableMap = hashMapOf<Chicken, BukkitRunnable>()

    @EventHandler
    fun onSpawn(e: ItemSpawnEvent) {
        if (e.entity.getNearbyEntities(0.5, 1.0, 0.5).size >= 1 && e.entity.itemStack
                .type == Material.EGG
        ) {
            e.entity.getNearbyEntities(0.5, 1.0, 0.5).forEach {
                if (it.type == EntityType.CHICKEN && it.scoreboardTags.isNotEmpty()) {
                    if(runnableMap.containsKey(it)) {
                        return
                    } else {
                        val i = itemStacksFromString(it.scoreboardTags.toTypedArray()[0])[0] as ItemStack
                        runnableMap[it as Chicken] = object : ChickenDupeRunnable(it, i) {
                        }
                    }
                }
            }
        }
    }


    @Throws(IllegalStateException::class)
    fun itemStacksToString(stacks: Array<ItemStack?>): String? = try {
            val outputStream = ByteArrayOutputStream()
            val outputStreamObject = BukkitObjectOutputStream(outputStream)

            // Write the size of the inventory
            outputStreamObject.writeInt(stacks.size)

            // Save every element in the list
            stacks.forEach {
                outputStreamObject.writeObject(it)
            }

            // Serialize that array
            outputStreamObject.close()
            Base64Coder.encodeLines(outputStream.toByteArray())
        } catch (e: Exception) {
            throw IllegalStateException("Unable to save item stacks.", e)
        }

    @Throws(IOException::class)
    fun itemStacksFromString(str: String?): Array<ItemStack?> = try {
            val inputStream = ByteArrayInputStream(Base64Coder.decodeLines(str))
            val dataInput = BukkitObjectInputStream(inputStream)
            val items = arrayOfNulls<ItemStack>(dataInput.readInt())

            // Read the serialized inventory
            items.indices.forEach {
                items[it] = dataInput.readObject() as ItemStack
            }
            dataInput.close()
            items
        } catch (e: ClassNotFoundException) {
            throw IOException("Unable to decode class type.", e)
        }
}
