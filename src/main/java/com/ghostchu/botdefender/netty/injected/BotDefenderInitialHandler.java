package com.ghostchu.botdefender.netty.injected;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.protocol.PacketWrapper;

public class BotDefenderInitialHandler extends InitialHandler {
    public BotDefenderInitialHandler(BungeeCord bungee, ListenerInfo listener) {
        super(bungee, listener);
    }

    @Override
    public boolean shouldHandle(PacketWrapper packet) throws Exception {
        return super.shouldHandle(packet);
    }
}
