package demo.bug;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class ItemFrameBugDemo extends JavaPlugin implements Listener {
    private final NamespacedKey DATA_KEY = new NamespacedKey(this, "data");

    private void setPersistentData(Entity entity, String value) {
        entity.getPersistentDataContainer().set(DATA_KEY, PersistentDataType.STRING, value);
    }

    private void printItemFrameNBT(Player player, Block blockWithItemFrames, BlockFace clickedFace) {
        boolean printAllFrames = player.isSneaking();

        if (!printAllFrames) {
            ItemFrame targetFrame = getItemFramesAttachedTo(blockWithItemFrames).stream()
                .filter(itemFrame -> itemFrame.getAttachedFace() == clickedFace.getOppositeFace())
                .findFirst().orElse(null);

            if (targetFrame == null) {
                player.sendMessage("No item frame on this side (hold SHIFT to see all attached frames)");
                return;
            }

            player.sendMessage("Item frame attached to " + clickedFace + " face: ");
            player.sendMessage(getEntityInfo(targetFrame));
            return;
        }

        List<ItemFrame> itemFrames = getItemFramesAttachedTo(blockWithItemFrames);
        if (itemFrames.size() == 0) {
            player.sendMessage("No item frames attached to this block");
            return;
        }

        player.sendMessage(itemFrames.size() + " item frames attached to this block:");

        for (ItemFrame itemFrame: itemFrames) {
            player.sendMessage("");
            player.sendMessage(getEntityInfo(itemFrame));
        }
    }

    private String getEntityInfo(Entity entity) {
        NBTEditor.NBTCompound entityTag = NBTEditor.getNBTCompound(entity);
        if (entityTag == null) {
            return "null";
        }

        return entityTag.toJson();
    }

    private void spawnItemFrame(Player player, Block targetBlock, BlockFace attachToSide) {
        boolean spawnWithFacing = player.isSneaking();

        if (spawnWithFacing) {
            player.sendMessage("Spawning the item frame " + ChatColor.RED + "with" + ChatColor.RESET + " setting the attached face");
            player.sendMessage("If the bug is still present, the spawned item frame should have no custom metadata");
        }
        else {
            player.sendMessage("Spawning the item frame " + ChatColor.GREEN + "without" + ChatColor.RESET + " setting the attached face");
        }

        World world = targetBlock.getWorld();
        Location relativeLocation = targetBlock.getRelative(attachToSide).getLocation();
        ItemFrame itemFrame = null;
        try {
            itemFrame = (ItemFrame) world.spawnEntity(relativeLocation, EntityType.ITEM_FRAME);
        }
        catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Can't spawn item frame: " + e.getMessage());
            return;
        }

        setPersistentData(itemFrame, "before_facing");

        if (spawnWithFacing) {
            itemFrame.setFacingDirection(attachToSide);
        }

        itemFrame.setInvulnerable(false);
        itemFrame.setFixed(false);
        setPersistentData(itemFrame, "after_facing");
        itemFrame.setItem(new ItemStack(Material.GLISTERING_MELON_SLICE));
    }

    private static final double HANGING_RADIUS = 1.5d;
    private List<ItemFrame> getItemFramesAttachedTo(Block block) {
        List<ItemFrame> itemFrames = new ArrayList<>(6);

        for (Entity nearbyEntity: block.getWorld().getNearbyEntities(block.getLocation(), HANGING_RADIUS, HANGING_RADIUS, HANGING_RADIUS)) {
            if (!(nearbyEntity instanceof ItemFrame)) {
                continue;
            }

            Block frameBlock = nearbyEntity.getLocation().getBlock();
            Block attachedTo = frameBlock.getRelative(((ItemFrame) nearbyEntity).getAttachedFace());
            if (!block.equals(attachedTo)) {
                continue;
            }

            itemFrames.add(((ItemFrame) nearbyEntity));
        }

        return itemFrames;
    }

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.sendMessage("This server is running a plugin that demonstrates an issue with spawning item frames and setting their facing direction.");
        player.sendMessage("As of git-Paper-229 1.16.3-R0.1-SNAPSHOT, if a facing direction is changed on a spawned item frame entity, it wipes all its custom metadata");
        player.sendMessage("Left Click: Print NBT tags of the item frame attached to the clicked side");
        player.sendMessage("Shift + Left Click: Print NBT tags of all item frames attached to target block");
        player.sendMessage("Right Click: Spawn an item frame and let it pick the face automatically");
        player.sendMessage("Shift + Left Click: Spawn an item frame and set its facing");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            printItemFrameNBT(event.getPlayer(), event.getClickedBlock(), event.getBlockFace());
        }
        else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            spawnItemFrame(event.getPlayer(), event.getClickedBlock(), event.getBlockFace());
        }
    }
}
