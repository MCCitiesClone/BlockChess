package gg.ethereallabs.blockChess;

import gg.ethereallabs.blockChess.command.ChessCommand;
import gg.ethereallabs.blockChess.config.Config;
import gg.ethereallabs.blockChess.events.ChessPieceListener;
import gg.ethereallabs.blockChess.events.PlayerListener;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class BlockChess extends JavaPlugin {

    public static BlockChess instance;
    public static MiniMessage mm;
    public static NamespacedKey CHESS_PIECE_KEY;
    public static Map<String, Material> whitePiecesByChar;
    public static Map<String, Material> blackPiecesByChar;

    public final Map<String, String> fenToName = new HashMap<>(Map.of(
        "p", "Pawn",
        "r", "Rook",
        "n", "Knight",
        "b", "Bishop",
        "q", "Queen",
        "k", "King"
    ));

    public final Map<Material, String> whitePieces = new HashMap<>(Map.of(
        Material.BIRCH_BUTTON,          "p",
        Material.IRON_DOOR,             "r",
        Material.IRON_HORSE_ARMOR,      "n",
        Material.END_ROD,               "b",
        Material.WHITE_CANDLE,          "q",
        Material.IRON_BLOCK,            "k"
    ));

    public final Map<Material, String> blackPieces = new HashMap<>(Map.of(
        Material.POLISHED_BLACKSTONE_BUTTON, "p",
        Material.DARK_OAK_DOOR,              "r",
        Material.LEATHER_HORSE_ARMOR,        "n",
        Material.LIGHTNING_ROD,              "b",
        Material.BLACK_CANDLE,               "q",
        Material.NETHERITE_BLOCK,            "k"
    ));

    @Override
    public void onEnable() {
        instance = this;
        mm = MiniMessage.miniMessage();
        CHESS_PIECE_KEY = new NamespacedKey(this, "chess_piece");
        saveDefaultConfig();
        Config.load(this);

        whitePiecesByChar = new HashMap<>();
        for (Map.Entry<Material, String> e : whitePieces.entrySet()) {
            whitePiecesByChar.put(e.getValue(), e.getKey());
        }

        blackPiecesByChar = new HashMap<>();
        for (Map.Entry<Material, String> e : blackPieces.entrySet()) {
            blackPiecesByChar.put(e.getValue(), e.getKey());
        }

        Bukkit.getPluginManager().registerEvents(new PlayerListener(), instance);
        Bukkit.getPluginManager().registerEvents(new ChessPieceListener(), instance);

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
            new ChessCommand().register(event.registrar())
        );
    }

    @Override
    public void onDisable() {}

    public void sendMessage(String message, CommandSender... senders) {
        for (CommandSender sender : senders) {
            if (sender != null) {
                sender.sendMessage(mm.deserialize(message));
            }
        }
    }
}
