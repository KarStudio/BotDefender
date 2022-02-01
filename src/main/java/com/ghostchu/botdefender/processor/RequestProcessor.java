package com.ghostchu.botdefender.processor;

import com.ghostchu.botdefender.BotDefender;
import com.ghostchu.botdefender.StatusMode;
import com.ghostchu.botdefender.util.TimeUtil;
import com.ghostchu.simplereloadlib.ReloadResult;
import com.ghostchu.simplereloadlib.ReloadStatus;
import com.ghostchu.simplereloadlib.Reloadable;
import de.themoep.minedown.MineDown;
import lombok.Data;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;


public class RequestProcessor implements Listener, Reloadable {
    private final BotDefender plugin;
    private Map<StatusMode, ConfigRecord> configMap;

    public RequestProcessor(BotDefender plugin) {
        this.plugin = plugin;
        init();
        plugin.getReloadManager().register(this);
        plugin.getProxy().getPluginManager().registerListener(plugin, this);
    }

    public void init() {
        Configuration config = plugin.getConfig().getSection("processor");
        Map<StatusMode, ConfigRecord> configMap = new HashMap<>();
        for (String mode : config.getKeys()) {
            configMap.put(StatusMode.valueOf(mode), new ConfigRecord(config.getSection(mode)));
        }
        this.configMap = configMap;
    }


    @EventHandler(priority = EventPriority.HIGH) // 设置为高优先级，这样可以让各类检测比登录处理器更早执行需要的操作
    public void onPlayerConnect(PreLoginEvent event) {
        if (!(event.getConnection().getSocketAddress() instanceof InetSocketAddress))
            return;
        InetSocketAddress socketAddress = (InetSocketAddress) event.getConnection().getSocketAddress();
        InetAddress address = socketAddress.getAddress();
        int score = plugin.getSuspicion().getScore(address);
        ConfigRecord ruleRecord = configMap.get(plugin.getCurrentMode());
        if(score < ruleRecord.getMinSuspicionScore()){
            // 执行操作
            switch (ruleRecord.getAction()) {
                case DISCONNECT:
                    event.setCancelled(true);
                    event.setCancelReason(MineDown.parse(ruleRecord.getMessage()));
                    break;
                case BLOCK:
                    event.setCancelled(true);
                    if(ruleRecord.getMessage() != null)
                        event.setCancelReason(MineDown.parse(ruleRecord.getMessage()));
                    plugin.getBlockController().block(address, Duration.ofMillis(ruleRecord.getDuration()));
            }
        }

    }

    @Override
    public ReloadResult reloadModule() {
        init();
        return ReloadResult.builder().status(ReloadStatus.SUCCESS).build();
    }

    @Data
    static class ConfigRecord {
        private final int minSuspicionScore;
        private final Action action;
        private String message;
        private long duration;

        public ConfigRecord(@NotNull Configuration config){
            this.minSuspicionScore = config.getInt("min_suspicion_score",50);
            this.action = Action.valueOf(config.getString("action","DISCONNECT"));
            this.message = config.getString("message","");
            this.duration = TimeUtil.convert(config.getString("duration","30m"));
        }

        enum Action {
            DISCONNECT, BLOCK
        }
    }
}
