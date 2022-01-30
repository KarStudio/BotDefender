package com.ghostchu.botdefender.util;

import com.ejlchina.okhttps.OkHttps;
import com.ghostchu.botdefender.BotDefender;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.CountDownLatch;

@AllArgsConstructor
public class OkHttpDownloader {
    private final BotDefender plugin;

    @SneakyThrows
    public void download(String url, File file) {
        // 文件大小
        long totalLength = OkHttps.sync(url).head().getContentLength();
        // 每块最多下载 30 Mb
        long size = 30 * 1024 * 1024;
        // 需要下载的块数
        int count = new BigDecimal(totalLength).divide(new BigDecimal(size), RoundingMode.UP).intValue();
        CountDownLatch latch = new CountDownLatch(count);
        // 启动 count 个线程并行下载
        for (int i = 0; i < count; i++) {
            int index = i;
            long start = index * size;
            new Thread(() -> {
                OkHttps.sync(url)
                        .setRange(start, start + size)
                        .get()
                        .getBody()
                        .stepRate(0.1)
                        .setOnProcess(p -> plugin.getLogger().info("[Downloader] " + file.getName() + " 下载进度 [" + index + "]: " + p))
                        .toFile(file)
                        .setOnSuccess((f) -> latch.countDown())
                        .setOnFailure((e) -> {
                            plugin.getLogger().info("[Downloader] " + file.getName() + " 第 "+ index +" 块下载失败: " + e.getException().getMessage());
                            latch.countDown();
                        });
                plugin.getLogger().info("[Downloader] " + file.getName() + " 已开始下载第 " + index + "块");
            }).start();
        }
        // 等待下载结束
        latch.await();
    }
}
