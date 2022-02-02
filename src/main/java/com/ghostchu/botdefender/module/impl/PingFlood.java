package com.ghostchu.botdefender.module.impl;

import com.ghostchu.botdefender.BotDefender;
import com.ghostchu.botdefender.StatusMode;
import com.ghostchu.botdefender.util.TimeUtil;
import com.ghostchu.simplereloadlib.ReloadResult;
import com.ghostchu.simplereloadlib.Reloadable;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.SneakyThrows;
import net.md_5.bungee.api.Favicon;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class PingFlood implements Listener, Reloadable {
    private final BotDefender plugin;
    private final Map<StatusMode, Integer> pingLimiter = new HashMap<>();
    private final Map<StatusMode, Integer> faviconLimiter = new HashMap<>();
    private Cache<InetAddress, AtomicInteger> pingCounter;
    private Cache<InetAddress, AtomicInteger> faviconOverrideCounter;
    private long blockDuration;

    public PingFlood(BotDefender plugin) {
        this.plugin = plugin;
        plugin.getReloadManager().register(this);
        init();
        plugin.getProxy().getPluginManager().registerListener(plugin, this);
    }

    public void init() {
        Configuration speedLimitConfig = plugin.getConfig().getSection("speed-limit");
        long time = TimeUtil.convert(speedLimitConfig.getString("speed-limit.time", "1m"));
        this.blockDuration = TimeUtil.convert(speedLimitConfig.getString("block-duration", "10m"));
        this.pingCounter = CacheBuilder
                .newBuilder()
                .expireAfterWrite(time, TimeUnit.MILLISECONDS)
                .build();
        this.faviconOverrideCounter = CacheBuilder
                .newBuilder()
                .expireAfterWrite(time, TimeUnit.MILLISECONDS)
                .build();
        for (String statusConfig : speedLimitConfig.getSection("ping").getKeys()) {
            this.pingLimiter.put(StatusMode.valueOf(statusConfig), speedLimitConfig.getInt("ping." + statusConfig));
        }
        for (String statusConfig : speedLimitConfig.getSection("favicon").getKeys()) {
            this.faviconLimiter.put(StatusMode.valueOf(statusConfig), speedLimitConfig.getInt("favicon." + statusConfig));
        }
    }

    public boolean hadPing(@NotNull InetAddress address) {
        return this.pingCounter.getIfPresent(address) != null;
    }


    /**
     * 处理 Ping 和 Favicon 请求
     *
     * @param event 事件
     */
    @SneakyThrows
    @EventHandler(priority = EventPriority.LOW)
    public void onPing(ProxyPingEvent event) {
        // Update counter
        SocketAddress socketAddress = event.getConnection().getSocketAddress();
        if (socketAddress instanceof InetSocketAddress) {
            InetAddress address = ((InetSocketAddress) socketAddress).getAddress();
            if (address.isAnyLocalAddress()) return;
            // Ping 检查
            AtomicInteger counter = pingCounter.get(address, () -> new AtomicInteger(0));
            int count = counter.incrementAndGet();
            if (count >= pingLimiter.get(plugin.getCurrentMode())) {
                plugin.getBlockController().block(address, Duration.ofMillis(blockDuration));
                event.getConnection().disconnect("Ping limit reached");
                pingCounter.invalidate(address);
                return;
            }
            // Favicon 限流
            counter = faviconOverrideCounter.get(address, () -> new AtomicInteger(0));
            count = counter.incrementAndGet();
            if (count >= faviconLimiter.get(StatusMode.NORMAL)) {
                event.getResponse().setFavicon((Favicon) null);
            }
        }
    }


    @Override
    public ReloadResult reloadModule() throws Exception {
        init();
        return Reloadable.super.reloadModule();
    }
}
