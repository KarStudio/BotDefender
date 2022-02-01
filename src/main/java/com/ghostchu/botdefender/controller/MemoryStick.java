package com.ghostchu.botdefender.controller;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

public class MemoryStick {
    private Cache<InetAddress, Memory> cache = CacheBuilder
            .newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();

    @NotNull
    public Memory getMemory(@NotNull InetAddress address){
        Memory memory = cache.getIfPresent(address);
        if(memory != null) return memory;
        memory = new Memory();
        cache.put(address, memory);
        return memory;
    }
    @AllArgsConstructor
    @RequiredArgsConstructor
    @NoArgsConstructor
    @Data
    static class Memory{
        private int memory;
        private boolean motd = false;
    }
}


