package com.ghostchu.botdefender.blocker;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public interface BlockController {
    /**
     * 检查指定的 IP 地址是否处于封锁列表中
     * @param address IP 地址
     * @return 封禁剩余时长，如果不存在，返回 null
     */
    @Nullable
    Duration isBlocked(@NotNull InetAddress address);

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

    /**
     * 获取所有封锁列表
     * @return 封锁列表 [IP,剩余时长]
     */
    @NotNull
    List<Map.Entry<InetAddress, Duration>> getBlockList();
}
