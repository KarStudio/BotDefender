package com.ghostchu.botdefender.netty.injected;

import net.md_5.bungee.UserConnection;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.connection.UpstreamBridge;
import net.md_5.bungee.protocol.PacketWrapper;

public class BotDefenderUpstreamBridge extends UpstreamBridge {
    public BotDefenderUpstreamBridge(ProxyServer bungee, UserConnection con) {
        super(bungee, con);
    }

    @Override
    public boolean shouldHandle(PacketWrapper packet) throws Exception {
        return super.shouldHandle(packet);
    }
}
