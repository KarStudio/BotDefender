package com.ghostchu.botdefender;

import com.ghostchu.botdefender.blocker.BlockController;
import com.ghostchu.botdefender.geoip.GeoReader;
import com.ghostchu.botdefender.speedlimit.SpeedLimiter;
import com.ghostchu.simplereloadlib.ReloadManager;
import jdk.internal.org.jline.utils.Status;
import lombok.Getter;
import lombok.SneakyThrows;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Level;

public final class BotDefender extends Plugin {
    private final GeoReader geoReader = new GeoReader(this);
    private Configuration config;
    @Getter
    private final ReloadManager reloadManager = new ReloadManager();
    @Getter
    private BlockController blockController;
    @Getter
    private StatusMode currentMode = StatusMode.NORMAL;
    @Getter
    private SpeedLimiter speedLimiter;


    @Override
    public void onEnable() {
        // Plugin startup logic
        getDataFolder().mkdirs();
        try {
            geoReader.setupAndUpdateDatabase("", getDataFolder());
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "无法初始化 BotDefender 所需要的数据库，退出...");
            return;
        }
        this.speedLimiter = new SpeedLimiter(this);

        getLogger().info("BotDefender by KarNetwork has been initialized.");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @SneakyThrows
    private void loadConfig() {
        this.config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "config.yml"));
    }

    @NotNull
    public Configuration getConfig() {
        if (this.config == null)
            loadConfig();
        return this.config;
    }

    @SneakyThrows
    public void saveConfig() {
        ConfigurationProvider.getProvider(YamlConfiguration.class).save(this.config, new File(getDataFolder(), "config.yml"));
    }

    public void saveDefaultConfig() {
        if (!getDataFolder().exists())
            getDataFolder().mkdir();
        File file = new File(getDataFolder(), "config.yml");
        if (!file.exists()) {
            try (InputStream in = getResourceAsStream("config.yml")) {
                Files.copy(in, file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
