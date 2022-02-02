package com.ghostchu.botdefender.module;

import com.ghostchu.botdefender.BotDefender;
import com.ghostchu.botdefender.module.impl.HandshakeFlood;
import com.ghostchu.botdefender.module.impl.PingFlood;
import com.ghostchu.botdefender.module.impl.botsuspicion.BasicSuspicionProvider;
import com.ghostchu.botdefender.module.impl.botsuspicion.Suspicion;

public class ModuleManager {
    private final Suspicion botSuspicion;
    private final BotDefender plugin;
    private final HandshakeFlood handshakeFlood;
    private final PingFlood pingFlood;
    public ModuleManager (BotDefender plugin){
        this.plugin = plugin;
        this.handshakeFlood = new HandshakeFlood(plugin);
        this.pingFlood = new PingFlood(plugin);
        this.botSuspicion = new BasicSuspicionProvider(plugin);
    }

}
