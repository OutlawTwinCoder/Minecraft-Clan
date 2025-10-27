package com.outlaw.clans.service;

import com.outlaw.clans.OutlawClansPlugin;
import com.outlaw.clans.model.Clan;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CurrencyPromptManager implements Listener {

    public enum Action { DEPOSIT, WITHDRAW, GIVE }

    private static class PromptContext {
        final UUID clanId;
        final Action action;
        final UUID target;

        PromptContext(UUID clanId, Action action, UUID target) {
            this.clanId = clanId;
            this.action = action;
            this.target = target;
        }
    }

    private final OutlawClansPlugin plugin;
    private final Map<UUID, PromptContext> prompts = new ConcurrentHashMap<>();

    public CurrencyPromptManager(OutlawClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void requestAmount(Player player, Clan clan, Action action, UUID target) {
        if (player == null || clan == null || action == null) {
            return;
        }
        prompts.put(player.getUniqueId(), new PromptContext(clan.getId(), action, target));
        String modeLabel = plugin.currency().mode() == CurrencyService.Mode.EXP
                ? ChatColor.YELLOW + "XP"
                : ChatColor.YELLOW + plugin.currency().itemType().name();
        player.sendMessage(ChatColor.AQUA + "Saisis le montant à traiter en " + modeLabel + ChatColor.AQUA + " (ou 'cancel').");
    }

    public void cancel(Player player) {
        if (player != null) {
            prompts.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        PromptContext context = prompts.remove(player.getUniqueId());
        if (context == null) {
            return;
        }
        event.setCancelled(true);
        String message = event.getMessage().trim();
        if (message.equalsIgnoreCase("cancel")) {
            player.sendMessage(ChatColor.YELLOW + "Opération annulée.");
            Bukkit.getScheduler().runTask(plugin, () -> reopenMenu(player, context));
            return;
        }
        int amount;
        try {
            amount = Integer.parseInt(message);
        } catch (NumberFormatException ex) {
            player.sendMessage(ChatColor.RED + "Montant invalide. Entrez un nombre entier.");
            prompts.put(player.getUniqueId(), context);
            return;
        }
        if (amount <= 0) {
            player.sendMessage(ChatColor.RED + "Le montant doit être positif.");
            prompts.put(player.getUniqueId(), context);
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> process(player, context, amount));
    }

    private void process(Player player, PromptContext context, int amount) {
        Clan clan = plugin.clans().getClan(context.clanId).orElse(null);
        if (clan == null || !clan.isMember(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Clan introuvable ou vous n'en faites plus partie.");
            return;
        }

        switch (context.action) {
            case DEPOSIT -> handleDeposit(player, clan, amount);
            case WITHDRAW -> handleWithdraw(player, clan, amount);
            case GIVE -> handleGive(player, clan, context.target, amount);
        }
        reopenMenu(player, context);
    }

    private void handleDeposit(Player player, Clan clan, int amount) {
        if (!plugin.currency().depositIntoClan(player, clan, amount)) {
            sendInsufficient(player);
            return;
        }
        player.sendMessage(ChatColor.GREEN + "Déposé " + plugin.currency().formatAmount(amount) + ChatColor.GREEN + " dans la banque du clan.");
    }

    private void handleWithdraw(Player player, Clan clan, int amount) {
        if (!clan.isLeader(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Seul le leader peut retirer des fonds.");
            return;
        }
        if (!plugin.currency().withdrawFromClan(player, clan, amount)) {
            player.sendMessage(ChatColor.RED + "Le clan n'a pas assez de fonds.");
            return;
        }
        player.sendMessage(ChatColor.GREEN + "Retiré " + plugin.currency().formatAmount(amount) + ChatColor.GREEN + " du clan.");
    }

    private void handleGive(Player player, Clan clan, UUID targetId, int amount) {
        if (!clan.isLeader(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Seul le leader peut distribuer des fonds.");
            return;
        }
        if (targetId == null) {
            player.sendMessage(ChatColor.RED + "Cible invalide.");
            return;
        }
        Player target = Bukkit.getPlayer(targetId);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Ce joueur doit être en ligne pour recevoir la monnaie.");
            return;
        }
        if (!clan.isMember(targetId)) {
            player.sendMessage(ChatColor.RED + "Ce joueur n'est pas dans le clan.");
            return;
        }
        if (!plugin.currency().transferToMember(clan, target, amount)) {
            player.sendMessage(ChatColor.RED + "Le clan n'a pas assez de fonds.");
            return;
        }
        player.sendMessage(ChatColor.GREEN + "Envoyé " + plugin.currency().formatAmount(amount) + ChatColor.GREEN + " à " + target.getName() + ".");
        target.sendMessage(ChatColor.AQUA + "Vous avez reçu " + plugin.currency().formatAmount(amount) + ChatColor.AQUA + " de votre clan.");
    }

    private void reopenMenu(Player player, PromptContext context) {
        Clan clan = plugin.clans().getClan(context.clanId).orElse(null);
        if (clan == null) {
            return;
        }
        plugin.menuUI().openCurrencyMenu(player, clan);
    }

    private void sendInsufficient(Player player) {
        if (plugin.currency().mode() == CurrencyService.Mode.EXP) {
            player.sendMessage(ChatColor.RED + "Vous n'avez pas assez d'XP.");
        } else {
            player.sendMessage(ChatColor.RED + "Vous n'avez pas assez de " + plugin.currency().itemType().name() + ".");
        }
    }
}
