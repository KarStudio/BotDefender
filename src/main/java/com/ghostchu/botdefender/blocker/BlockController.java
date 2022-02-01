package com.ghostchu.botdefender.blocker;

import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.time.Duration;

public interface BlockController {
    /**
     * 封锁指定的 IP 地址
     * @param address IP 地址
     * @param duration 持续时间
     */
    void block(@NotNull InetAddress address, @NotNull Duration duration);

    /**
     * 解除指定 IP 地址的封锁
     * @param address IP 地址
     */
    void unblock(@NotNull InetAddress address);
}
