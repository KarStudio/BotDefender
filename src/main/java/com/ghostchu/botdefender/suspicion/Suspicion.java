package com.ghostchu.botdefender.suspicion;

import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;

public interface Suspicion {
    /**
     * 获取指定地址的可疑度
     * 越趋于负无穷，则该玩家可疑度越低
     * 越趋于正无穷，则该玩家可疑度越高
     * @param address 地址
     * @return 可疑度
     */
    int getScore(@NotNull InetAddress address);
}
