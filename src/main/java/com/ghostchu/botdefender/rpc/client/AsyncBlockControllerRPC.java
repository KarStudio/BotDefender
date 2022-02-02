package com.ghostchu.botdefender.rpc.client;

import com.ghostchu.botdefender.BotDefender;
import com.ghostchu.botdefender.blocker.BlockController;
import com.ghostchu.botdefender.rpc.proto.BlockControllerGrpc;
import com.ghostchu.botdefender.rpc.proto.BlockControllerProto;
import com.ghostchu.simplereloadlib.ReloadResult;
import com.ghostchu.simplereloadlib.Reloadable;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.time.Duration;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class AsyncBlockControllerRPC implements Reloadable, BlockController {
    private final BotDefender plugin;
    private String user;
    private String target;
    private ManagedChannel channel;
    private BlockControllerGrpc.BlockControllerBlockingStub blockingStub;
    private final LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();

    public AsyncBlockControllerRPC(@NotNull BotDefender plugin) {
        this.plugin = plugin;
        plugin.getReloadManager().register(this);
        Thread thread = new Thread("BlockController Async RPC IO Thread") {
            @Override
            public void run() {
                while (!isInterrupted()) {
                    try {
                        // Give thread a chance to be interrupted
                        Runnable runnable = queue.poll(3, TimeUnit.SECONDS);
                        if(runnable != null)
                            runnable.run();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        thread.setDaemon(true);
        thread.start();
        init();

    }

    public void init() {
        this.user = plugin.getConfig().getString("rpc.user");
        this.target = plugin.getConfig().getString("rpc.target");
        if (this.channel != null)
            this.channel.shutdownNow();
        this.channel = ManagedChannelBuilder.forTarget(target)
                // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
                // needing certificates.
                .usePlaintext()
                .build();
        blockingStub = BlockControllerGrpc.newBlockingStub(channel);

    }


    @Override
    public ReloadResult reloadModule() throws Exception {
        init();
        return Reloadable.super.reloadModule();
    }

    /**
     * 封锁指定的 IP 地址
     *
     * @param address  IP 地址
     * @param duration 持续时间
     */
    @Override
    public void block(@NotNull InetAddress address, @NotNull Duration duration) {
        long endTime = System.currentTimeMillis() + duration.toMillis();
        plugin.getLogger().info("[RPC] Block IPAddress: " + address.getHostAddress() + ", Until: " + new Date(endTime));
        BlockControllerProto.Address rpcAddress = BlockControllerProto.Address.newBuilder().setAddress(address.getHostAddress()).build();
        BlockControllerProto.BlockRequest rpcBlockRequest = BlockControllerProto.BlockRequest.newBuilder()
                .setAddress(rpcAddress)
                .setDuration(duration.toMillis())
                .build();
        //noinspection ResultOfMethodCallIgnored
        this.queue.offer(()-> blockingStub.blockAddress(rpcBlockRequest));

    }

    /**
     * 解除指定 IP 地址的封锁
     *
     * @param address IP 地址
     */
    @Override
    public void unblock(@NotNull InetAddress address) {
        plugin.getLogger().info("[RPC] Unblock IPAddress: " + address.getHostAddress());
        BlockControllerProto.Address rpcAddress = BlockControllerProto.Address.newBuilder().setAddress(address.getHostAddress()).build();
        //noinspection ResultOfMethodCallIgnored
        this.queue.offer(()-> blockingStub.unblockAddress(rpcAddress));
    }
}
