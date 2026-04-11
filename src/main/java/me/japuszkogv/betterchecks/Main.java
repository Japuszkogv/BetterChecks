package me.japuszkogv.betterchecks;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.Arrays;

public final class Main extends JavaPlugin implements CommandExecutor, Listener {

    private static Economy econ = null;

    @Override
    public void onEnable() {
        if (!setupEconomy()) {
            getLogger().severe("Brak Vault! Wylaczam plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getCommand("withdraw").setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        econ = rsp.getProvider();
        return econ != null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (args.length != 1) {
            player.sendMessage("§cUsage: /withdraw <amount>");
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage("§cThat's not a valid number!");
            return true;
        }

        if (amount <= 0 || econ.getBalance(player) < amount) {
            player.sendMessage("§cInsufficient funds!");
            return true;
        }

        econ.withdrawPlayer(player, amount);
        player.getInventory().addItem(createCheck(amount));
        player.sendMessage("§aYou have paid a check for the amount of: §e" + amount + "$");
        return true;
    }

    public ItemStack createCheck(double amount) {
        ItemStack check = new ItemStack(Material.PAPER);
        ItemMeta meta = check.getItemMeta();
        meta.setDisplayName("§6§lBANK CHECK");
        meta.setLore(Arrays.asList("§7Value: §e" + amount + "$", "§7Right-click to redeem!"));
        meta.getPersistentDataContainer().set(new NamespacedKey(this, "value"), PersistentDataType.DOUBLE, amount);
        check.setItemMeta(meta);
        return check;
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.PAPER || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey(this, "value");
        if (!meta.getPersistentDataContainer().has(key, PersistentDataType.DOUBLE)) return;

        double amount = meta.getPersistentDataContainer().get(key, PersistentDataType.DOUBLE);
        Player player = event.getPlayer();

        item.setAmount(item.getAmount() - 1);
        econ.depositPlayer(player, amount);
        player.sendMessage("§aYou have cashed a check for the amount of: §e" + amount + "$");
        event.setCancelled(true);
    }
}
