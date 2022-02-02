package com.ghostchu.botdefender.module;

import com.ghostchu.botdefender.BotDefender;
import com.ghostchu.botdefender.module.impl.HandshakeFlood;
import com.ghostchu.botdefender.module.impl.PingFlood;
import com.ghostchu.botdefender.module.impl.TabCompleteFlood;
import com.ghostchu.botdefender.module.impl.suspicion.BasicSuspicionProvider;
import com.ghostchu.botdefender.module.impl.suspicion.Suspicion;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class ModuleManager {
    private final Suspicion suspicion;
    private final HandshakeFlood handshakeFlood;
    private final PingFlood pingFlood;
    private final TabCompleteFlood tabCompleteFlood;
    public ModuleManager(@NotNull BotDefender plugin){
        this.suspicion = new BasicSuspicionProvider(plugin);
        this.handshakeFlood = new HandshakeFlood(plugin);
        this.pingFlood = new PingFlood(plugin);
        this.tabCompleteFlood = new TabCompleteFlood(plugin);
    }
}
