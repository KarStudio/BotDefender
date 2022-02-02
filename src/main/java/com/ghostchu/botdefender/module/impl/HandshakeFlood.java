package com.ghostchu.botdefender.module.impl;

import com.ghostchu.botdefender.BotDefender;
import com.ghostchu.botdefender.StatusMode;
import com.ghostchu.botdefender.util.TimeUtil;
import com.ghostchu.simplereloadlib.ReloadResult;
import com.ghostchu.simplereloadlib.Reloadable;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.SneakyThrows;
import net.md_5.bungee.api.event.ClientConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class HandshakeFlood implements Listener, Reloadable {
    private final BotDefender plugin;
    private final Map<StatusMode, Integer> limiter = new HashMap<>();
    private Cache<InetAddress, AtomicInteger> handshakeCounter;
    private long blockDuration;

    public HandshakeFlood(BotDefender plugin) {
        this.plugin = plugin;
        plugin.getReloadManager().register(this);
        init();
        plugin.getProxy().getPluginManager().registerListener(plugin, this);
    }

    public void init() {
        Configuration speedLimitConfig = plugin.getConfig().getSection("speed-limit");
        long time = TimeUtil.convert(speedLimitConfig.getString("speed-limit.time", "1m"));
        this.blockDuration = TimeUtil.convert(speedLimitConfig.getString("block-duration", "10m"));
        this.handshakeCounter = CacheBuilder
                .newBuilder()
                .expireAfterWrite(time, TimeUnit.MILLISECONDS)
                .build();
        // module - handshake
        for (String statusConfig : speedLimitConfig.getSection("handshake").getKeys()) {
            this.limiter.put(StatusMode.valueOf(statusConfig), speedLimitConfig.getInt("handshake." + statusConfig));
        }
    }


    /**
     * 处理 TCP 握手
     *
     * @param event 事件
     */
    @SneakyThrows
    @EventHandler(priority = EventPriority.LOW)
    public void onHandShake(ClientConnectEvent event) {
        SocketAddress socketAddress = event.getSocketAddress();
        if (socketAddress instanceof InetSocketAddress) {
            InetAddress address = ((InetSocketAddress) socketAddress).getAddress();
            //  if (address.isAnyLocalAddress()) return;
            AtomicInteger counter = handshakeCounter.get(address, () -> new AtomicInteger(0));
            int count = counter.incrementAndGet();
            // 握手检查
            if (count >= limiter.get(plugin.getCurrentMode())) {
                plugin.getBlockController().block(address, Duration.ofMillis(blockDuration));
                event.setCancelled(true);
                handshakeCounter.invalidate(address);
            }
        }
    }

    @Override
    public ReloadResult reloadModule() throws Exception {
        init();
        return Reloadable.super.reloadModule();
    }
}
