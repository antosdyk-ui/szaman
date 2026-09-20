package pl.shaze.szaman;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class SzamanPlugin extends JavaPlugin implements Listener, CommandExecutor {

    private final NamespacedKey odlamekKey = new NamespacedKey(this, "odlamek_szamana");
    private final Map<UUID, Map<String, Integer>> playerPerks = new HashMap<>();
    private final Map<UUID, Long> fireworkBlock = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("szaman").setExecutor(this);
        getCommand("ustawodlamek").setExecutor(this);
        getCommand("resetujperki").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Ta komenda jest tylko dla graczy!");
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("szaman")) {
            openSzamanGUI(player);
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("ustawodlamek")) {
            if (!player.hasPermission("szaman.admin")) {
                player.sendMessage(color("&cBrak uprawnień!"));
                return true;
            }
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item == null || item.getType() == Material.AIR) {
                player.sendMessage(color("&cMusisz trzymać przedmiot w ręce!"));
                return true;
            }
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(odlamekKey, PersistentDataType.BYTE, (byte) 1);
                item.setItemMeta(meta);
                player.sendMessage(color("&aOznaczono przedmiot w ręce jako Odłamek Szamana!"));
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("resetujperki")) {
            playerPerks.remove(player.getUniqueId());
            updatePlayerMaxHealth(player);
            player.sendMessage(color("&aPomyślnie zresetowano wszystkie twoje perki!"));
            return true;
        }

        return false;
    }

    public void openSzamanGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, "Prażysta");

        ItemStack glass = createItem(Material.BLUE_STAINED_GLASS_PANE, " ");
        ItemStack ironBars = createItem(Material.IRON_BARS, " ");

        int[] glassSlots = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 27, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44};
        for (int slot : glassSlots) inv.setItem(slot, glass);

        inv.setItem(18, ironBars);
        inv.setItem(26, ironBars);

        inv.setItem(20, createPerkItem(player, "zycie", Material.RED_DYE, "&cPerk zycia", 30,
                new String[]{"&7I - Dodatkowe zycie 0.5 serca", "&7II - Dodatkowe zycie 1 serca", "&7III - Dodatkowe zycie 1.5 serca", "&7IV - Dodatkowe zycie 2 serca"}));

        inv.setItem(21, createPerkItem(player, "blokada", Material.IRON_CHESTPLATE, "&cPerk blokady", 30,
                new String[]{"&7I - 1% szansy na unikniecie uderzenia", "&7II - 2% szansy na unikniecie uderzenia", "&7III - 3% szansy na unikniecie uderzenia", "&7IV - 5% szansy na unikniecie uderzenia"}));

        inv.setItem(22, createPerkItem(player, "sila", Material.IRON_SWORD, "&cPerk sily", 30,
                new String[]{"&7I - 5% dodatkowych obrazen", "&7II - 10% dodatkowych obrazen", "&7III - 15% dodatkowych obrazen", "&7IV - 20% dodatkowych obrazen"}));

        inv.setItem(23, createPerkItem(player, "uwiezienie", Material.FIREWORK_ROCKET, "&cPerk uwiezienia", 30,
                new String[]{"&7I - szansa na blokade fajerwerki na 2 sekundy", "&7II - szansa na blokade fajerwerki na 3 sekundy", "&7III - szansa na blokade fajerwerki na 4 sekundy", "&7IV - szansa na blokade fajerwerki na 5 sekundy"}));

        inv.setItem(24, createPerkItem(player, "wampiryzm", Material.REDSTONE, "&cPerk wampiryzmu", 30,
                new String[]{"&7I - 1% szansy na odzyskanie zadanego HP", "&7II - 2% szansy na odzyskanie zadanego HP", "&7III - 3% szansy na odzyskanie zadanego HP", "&7IV - 5% szansy na odzyskanie zadanego HP"}));

        inv.setItem(13, createPerkItem(player, "extrahit", Material.CAMPFIRE, "&cPerk extrahit", 50,
                new String[]{"&7I - 1% szansy na extra hit", "&7II - 2% szansy na extra hit", "&7III - 3% szansy na extra hit", "&7IV - 4% szansy na extra hit"}));

        player.openInventory(inv);
    }

    private ItemStack createPerkItem(Player player, String perkKey, Material mat, String title, int cost, String[] levels) {
        int currentLvl = getPerkLevel(player, perkKey);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color(title));

        List<String> lore = new ArrayList<>();
        if (currentLvl == 0) {
            lore.add(color("&8» &7Aktualny poziom: &cNie posiadasz zadnego poziomu tej umiejętnosci!"));
        } else {
            lore.add(color("&8» &7Aktualny poziom: &a" + currentLvl));
        }
        lore.add("");
        lore.add(color("&8» &ePoziomy"));
        for (String lvlLore : levels) {
            lore.add(color("&8» " + lvlLore));
        }
        lore.add("");
        if (currentLvl < 4) {
            lore.add(color("&8» &7Aktualny koszt ulepszenia: &d" + cost));
        } else {
            lore.add(color("&8» &aOsiągnięto maksymalny poziom!"));
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equalsIgnoreCase("Prażysta")) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        int slot = event.getSlot();

        String perkToUpgrade = switch (slot) {
            case 20 -> "zycie";
            case 21 -> "blokada";
            case 22 -> "sila";
            case 23 -> "uwiezienie";
            case 24 -> "wampiryzm";
            case 13 -> "extrahit";
            default -> null;
        };

        if (perkToUpgrade != null) {
            upgradePerk(player, perkToUpgrade, slot == 13 ? 50 : 30);
            openSzamanGUI(player);
        }
    }

    private void upgradePerk(Player player, String perkKey, int cost) {
        int currentLvl = getPerkLevel(player, perkKey);
        if (currentLvl >= 4) {
            player.sendMessage(color("&cPosiadasz już maksymalny poziom tego perku!"));
            return;
        }

        if (takeOdlamek(player, cost)) {
            setPerkLevel(player, perkKey, currentLvl + 1);
            player.sendMessage(color("&aPomyślnie ulepszono perk na poziom " + (currentLvl + 1) + "!"));
            if (perkKey.equals("zycie")) {
                updatePlayerMaxHealth(player);
            }
        } else {
            player.sendMessage(color("&cBrak wystarczającej ilości Odłamków w ekwipunku!"));
        }
    }

    private boolean takeOdlamek(Player player, int amount) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (isOdlamek(item)) {
                count += item.getAmount();
            }
        }

        if (count < amount) return false;

        int leftToTake = amount;
        for (ItemStack item : player.getInventory().getContents()) {
            if (isOdlamek(item)) {
                int stackAmount = item.getAmount();
                if (stackAmount <= leftToTake) {
                    leftToTake -= stackAmount;
                    item.setAmount(0);
                } else {
                    item.setAmount(stackAmount - leftToTake);
                    leftToTake = 0;
                }
                if (leftToTake <= 0) break;
            }
        }
        return true;
    }

    private boolean isOdlamek(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.has(odlamekKey, PersistentDataType.BYTE);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player attacker) {
            int extrahitLvl = getPerkLevel(attacker, "extrahit");
            if (extrahitLvl > 0) {
                double chance = extrahitLvl * 0.01;
                if (Math.random() < chance) {
                    event.setDamage(event.getDamage() * 1.5);
                    attacker.sendMessage(color("&6[Szaman] &eZadano Extra Hit!"));
                }
            }

            int silaLvl = getPerkLevel(attacker, "sila");
            if (silaLvl > 0) {
                double boost = switch (silaLvl) {
                    case 1 -> 0.05;
                    case 2 -> 0.10;
                    case 3 -> 0.15;
                    case 4 -> 0.20;
                    default -> 0.0;
                };
                event.setDamage(event.getDamage() * (1.0 + boost));
            }

            int wampLvl = getPerkLevel(attacker, "wampiryzm");
            if (wampLvl > 0) {
                double chance = (wampLvl == 4) ? 0.05 : wampLvl * 0.01;
                if (Math.random() < chance) {
                    double heal = event.getFinalDamage();
                    double maxHp = attacker.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                    attacker.setHealth(Math.min(maxHp, attacker.getHealth() + heal));
                }
            }

            if (event.getEntity() instanceof Player victim) {
                int uwiezienieLvl = getPerkLevel(attacker, "uwiezienie");
                if (uwiezienieLvl > 0) {
                    double chance = 0.05;
                    if (Math.random() < chance) {
                        int blockSeconds = uwiezienieLvl + 1;
                        fireworkBlock.put(victim.getUniqueId(), System.currentTimeMillis() + (blockSeconds * 1000L));
                        victim.sendMessage(color("&cTwój lot fajerwerką został zablokowany na " + blockSeconds + " sek.!"));
                    }
                }
            }
        }

        if (event.getEntity() instanceof Player victim) {
            int blokadaLvl = getPerkLevel(victim, "blokada");
            if (blokadaLvl > 0) {
                double chance = (blokadaLvl == 4) ? 0.05 : blokadaLvl * 0.01;
                if (Math.random() < chance) {
                    event.setCancelled(true);
                    victim.sendMessage(color("&aUniknięto ciosu!"));
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getItem() != null && event.getItem().getType() == Material.FIREWORK_ROCKET) {
            if (fireworkBlock.containsKey(player.getUniqueId())) {
                long blockTime = fireworkBlock.get(player.getUniqueId());
                if (System.currentTimeMillis() < blockTime) {
                    event.setCancelled(true);
                    player.sendMessage(color("&cNie możesz używać fajerwerków!"));
                } else {
                    fireworkBlock.remove(player.getUniqueId());
                }
            }
        }
    }

    private int getPerkLevel(Player player, String perkKey) {
        return playerPerks.getOrDefault(player.getUniqueId(), new HashMap<>()).getOrDefault(perkKey, 0);
    }

    private void setPerkLevel(Player player, String perkKey, int level) {
        Map<String, Integer> perks = playerPerks.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        perks.put(perkKey, level);
    }

    private void updatePlayerMaxHealth(Player player) {
        int zycieLvl = getPerkLevel(player, "zycie");
        double extraHp = switch (zycieLvl) {
            case 1 -> 1.0;
            case 2 -> 2.0;
            case 3 -> 3.0;
            case 4 -> 4.0;
            default -> 0.0;
        };
        AttributeInstance attr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr != null) {
            attr.setBaseValue(20.0 + extraHp);
        }
    }

    private String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}
