package gg.ethereallabs.blockChess.data;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import gg.ethereallabs.blockChess.BlockChess;
import gg.ethereallabs.blockChess.elo.EloManager;
import gg.ethereallabs.blockChess.utils.SyncHelper;
import org.bukkit.entity.Player;

import java.io.*;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class LocalStorage {
    private LocalStorage() {}

    private static final File dataFolder = new File(BlockChess.instance.getDataFolder(), "playerdata");

    private static class InstantAdapter implements JsonSerializer<Instant>, JsonDeserializer<Instant> {
        @Override
        public JsonElement serialize(Instant src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src != null ? src.toString() : null);
        }

        @Override
        public Instant deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            if (json == null || json.isJsonNull()) return null;
            String s = json.getAsString();
            return s != null ? Instant.parse(s) : null;
        }
    }

    public static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Instant.class, new InstantAdapter())
            .create();

    private static File getPlayerFile(UUID playerUuid) {
        return new File(dataFolder, playerUuid + ".json");
    }

    public static boolean playerHasData(Player player) {
        if (!dataFolder.exists()) dataFolder.mkdirs();
        return getPlayerFile(player.getUniqueId()).exists();
    }

    public static void loadPlayerData(Player player) {
        if (!dataFolder.exists()) dataFolder.mkdirs();
        File playerFile = getPlayerFile(player.getUniqueId());

        if (!playerFile.exists()) {
            createDefaultPlayerData(player);
        }

        CompletableFuture.runAsync(() -> {
            try (FileReader reader = new FileReader(playerFile)) {
                Type type = new TypeToken<PlayerData>() {}.getType();
                PlayerData playerData = gson.fromJson(reader, type);
                SyncHelper.runSync(() -> EloManager.players.put(player.getUniqueId(), playerData));
            } catch (Exception e) {
                BlockChess.instance.getLogger().log(Level.SEVERE, "Error loading player data for " + player.getName(), e);
                createDefaultPlayerData(player);
                SyncHelper.runSync(() -> EloManager.players.put(player.getUniqueId(), new PlayerData()));
            }
        });
    }

    public static void savePlayerData(Player player) {
        File playerFile = getPlayerFile(player.getUniqueId());
        if (!dataFolder.exists()) dataFolder.mkdirs();

        PlayerData playerData = EloManager.players.getOrDefault(player.getUniqueId(), new PlayerData());
        CompletableFuture.runAsync(() -> {
            try (FileWriter writer = new FileWriter(playerFile)) {
                gson.toJson(playerData, writer);
            } catch (Exception e) {
                BlockChess.instance.getLogger().log(Level.SEVERE, "Error saving player data for " + player.getName(), e);
            }
        });
    }

    public static void createDefaultPlayerData(Player player) {
        PlayerData playerData = new PlayerData(EloManager.defaultElo, 0);
        CompletableFuture.runAsync(() -> {
            try (FileWriter writer = new FileWriter(getPlayerFile(player.getUniqueId()))) {
                gson.toJson(playerData, writer);
            } catch (Exception e) {
                BlockChess.instance.getLogger().log(Level.SEVERE, "Error creating default player data for " + player.getName(), e);
            }
            SyncHelper.runSync(() -> EloManager.players.put(player.getUniqueId(), playerData));
        });
    }
}
