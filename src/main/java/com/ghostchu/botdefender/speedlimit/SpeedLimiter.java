package com.ghostchu.botdefender.speedlimit;

import com.ghostchu.botdefender.BotDefender;
import com.ghostchu.botdefender.StatusMode;
import com.ghostchu.botdefender.util.TimeUtil;
import com.ghostchu.simplereloadlib.ReloadResult;
import com.ghostchu.simplereloadlib.Reloadable;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.SneakyThrows;
import net.md_5.bungee.api.Favicon;
import net.md_5.bungee.api.event.ClientConnectEvent;
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

public class SpeedLimiter implements Listener, Reloadable {
    private final BotDefender plugin;
    private Cache<InetAddress, AtomicInteger> handshakeCounter;
    private Cache<InetAddress, AtomicInteger> pingCounter;
    private Cache<InetAddress, AtomicInteger> faviconOverrideCounter;
    private final Map<String, Map<StatusMode, Integer>> limiter = new HashMap<>();
    private long blockDuration;

    public SpeedLimiter(BotDefender plugin) {
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
        this.pingCounter = CacheBuilder
                .newBuilder()
                .expireAfterWrite(time, TimeUnit.MILLISECONDS)
                .build();
        this.faviconOverrideCounter = CacheBuilder
                .newBuilder()
                .expireAfterWrite(time, TimeUnit.MILLISECONDS)
                .build();
        // module - handshake
        Map<StatusMode, Integer> handshake = new HashMap<>();
        for (String statusConfig : speedLimitConfig.getSection("handshake").getKeys()) {
            handshake.put(StatusMode.valueOf(statusConfig), speedLimitConfig.getInt("handshake." + statusConfig));
        }
        Map<StatusMode, Integer> ping = new HashMap<>();
        for (String statusConfig : speedLimitConfig.getSection("ping").getKeys()) {
            handshake.put(StatusMode.valueOf(statusConfig), speedLimitConfig.getInt("ping." + statusConfig));
        }
        Map<StatusMode, Integer> favicon = new HashMap<>();
        for (String statusConfig : speedLimitConfig.getSection("favicon").getKeys()) {
            handshake.put(StatusMode.valueOf(statusConfig), speedLimitConfig.getInt("favicon." + statusConfig));
        }
        this.limiter.put("handshake", handshake);
        this.limiter.put("ping", ping);
        this.limiter.put("favicon", favicon);
    }

    public boolean hadPing(@NotNull InetAddress address) {
        return this.pingCounter.getIfPresent(address) != null;
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
            if (count >= limiter.get("handshake").get(plugin.getCurrentMode())) {
                plugin.getBlockController().block(address, Duration.ofMillis(blockDuration));
                event.setCancelled(true);
            }
        }
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
            if (count >= limiter.get("ping").get(plugin.getCurrentMode())) {
                plugin.getBlockController().block(address, Duration.ofMillis(blockDuration));
                event.getConnection().disconnect("Ping limit reached");
                return;
            }
            // Favicon 限流
            counter = faviconOverrideCounter.get(address, () -> new AtomicInteger(0));
            count = counter.incrementAndGet();
            if (count >= limiter.get("favicon").get(StatusMode.NORMAL)) {
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
