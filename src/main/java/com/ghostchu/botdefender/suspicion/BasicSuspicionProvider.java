package com.ghostchu.botdefender.suspicion;

import com.ghostchu.botdefender.BotDefender;
import com.ghostchu.botdefender.StatusMode;
import com.ghostchu.botdefender.geoip.GeoReader;
import com.ghostchu.simplereloadlib.ReloadResult;
import com.ghostchu.simplereloadlib.Reloadable;
import net.md_5.bungee.config.Configuration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.*;
import java.util.stream.Collectors;

public class BasicSuspicionProvider implements Suspicion, Reloadable {
    private final BotDefender plugin;
    private List<InetAddress> exclude = new ArrayList<>();
    private int baseScore = 0;
    @Nullable
    private CountryFilter countryFilter;
    @Nullable
    private ASNFilter asnFilter;
    @Nullable
    private ProxyFilter proxyFilter;
    private Map<StatusMode, Integer> pingScores;

    public BasicSuspicionProvider(@NotNull BotDefender plugin) {
        this.plugin = plugin;
        init();
        plugin.getReloadManager().register(this);
    }

    @Override
    public ReloadResult reloadModule() throws Exception {
        init();
        return Reloadable.super.reloadModule();
    }

    public void init() {
        Configuration config = plugin.getConfig().getSection("suspicion");
        this.baseScore = config.getInt("base");
        this.exclude = config.getStringList("exclude").stream().map(str -> {
            try {
                return InetAddress.getByName(str);
            } catch (UnknownHostException e) {
                e.printStackTrace();
                return null;
            }
        }).collect(Collectors.toList());
        if (config.getBoolean("country_filter.enable", true)) {
            this.countryFilter = new CountryFilter(config.getSection("country_filter"), plugin.getGeoReader());
        } else {
            this.countryFilter = null;
        }
        if (config.getBoolean("asn_filter.enable", true)) {
            this.asnFilter = new ASNFilter(config.getSection("asn_filter"), plugin.getGeoReader());
        } else {
            this.asnFilter = null;
        }
        if (config.getBoolean("proxy_filter.enable", true)) {
            this.proxyFilter = new ProxyFilter(config);
        } else {
            this.proxyFilter = null;
        }
        Map<StatusMode, Integer> pingMap = new HashMap<>();
        for (String mode : config.getSection("ping").getKeys()) {
            pingMap.put(StatusMode.valueOf(mode), config.getInt("ping." + mode));
        }
        this.pingScores = pingMap;
    }

    /**
     * 获取指定地址的可疑度
     * 越趋于负无穷，则该玩家可疑度越低
     * 越趋于正无穷，则该玩家可疑度越高
     *
     * @param address 地址
     * @return 可疑度
     */
    @Override
    public int getScore(@NotNull InetAddress address) {
        StatusMode mode = plugin.getCurrentMode();
        int score = baseScore;
        if(this.exclude.contains(address)) {
            return score;
        }
        if (this.countryFilter != null) {
            score += this.countryFilter.calcScore(address, mode);
        }
        if (this.asnFilter != null) {
            score += this.asnFilter.calcScore(address, mode);
        }
        if (this.proxyFilter != null) {
            score += this.proxyFilter.calcScore(address, mode);
        }
        score += getPingScore(address);
        return score;
    }


    private int getPingScore(@NotNull InetAddress address) {
        if(plugin.getSpeedLimiter().hadPing(address)){
            return pingScores.get(plugin.getCurrentMode());
        }
        return 0;
    }


    static class ProxyFilter {
        private final List<InetAddress> whitelist;
        private final Map<StatusMode, Integer> scoreMapping = new HashMap<>();
        private final String apiUrl;
        private List<InetAddress> proxies = new ArrayList<>();

        public ProxyFilter(@NotNull Configuration config) {
            this.apiUrl = config.getString("api");
            this.whitelist = config.getStringList("whitelist").stream().map(str -> {
                try {
                    return InetAddress.getByName(str);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                    return null;
                }
            }).collect(Collectors.toList());

            Configuration scoreConfig = config.getSection("score");
            for (String key : scoreConfig.getKeys()) {
                StatusMode mode = StatusMode.valueOf(key);
                this.scoreMapping.put(mode, scoreConfig.getInt(key));
            }
            Timer timer = new Timer("ProxySpider", true);
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    proxies = spiderProxies();
                }
            }, 0, 1000 * 60 * 60);
        }

        @NotNull
        private List<InetAddress> spiderProxies() {
            // Read the string content
            try {
                URL url = new URL(apiUrl);
                url.openConnection();
                String content = url.getContent().toString();
                String[] ips = content.split("\n");
                return Arrays.stream(ips).map(str -> {
                    try {
                        return InetAddress.getByName(str);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                }).collect(Collectors.toList());
            } catch (Exception e) {
                e.printStackTrace();
                return Collections.emptyList();
            }
        }

        public int calcScore(@NotNull InetAddress address, @NotNull StatusMode mode) {
            if (this.whitelist.contains(address)) {
                return 0;
            }
            if (this.proxies.contains(address)) {
                return this.scoreMapping.getOrDefault(mode, 0);
            }
            return 0;
        }
    }

    static class CountryFilter {
        private final GeoReader geoReader;
        private final Map<String, Integer> notInList = new HashMap<>();
        private final Map<String, Map<StatusMode, Integer>> rules = new HashMap<>();

        public CountryFilter(@NotNull Configuration config, @NotNull GeoReader geoReader) {
            this.geoReader = geoReader;
            // 载入列表外的分值
            for (String mode : config.getSection("not_in_list").getKeys()) {
                notInList.put(mode, config.getInt("not_in_list." + mode));
            }
            // 载入特定国家的分值
            for (String country : config.getSection("country").getKeys()) {
                Map<StatusMode, Integer> modeMap = new HashMap<>();
                for (String mode : config.getSection("country." + country).getKeys()) {
                    modeMap.put(StatusMode.valueOf(mode), config.getInt("country." + country + "." + mode));
                }
                rules.put(country, modeMap);
            }
        }

        public int calcScore(@NotNull InetAddress address, @NotNull StatusMode mode) {
            String country = geoReader.query(address);
            if (country == null) return 0;
            if (rules.containsKey(country)) {
                return rules.get(country).get(mode);
            }
            return notInList.getOrDefault(mode.name(), 0);
        }
    }

    static class ASNFilter {
        private final GeoReader geoReader;
        private final Map<String, Integer> notInList = new HashMap<>();
        private final Map<String, Map<StatusMode, Integer>> rules = new HashMap<>();

        public ASNFilter(@NotNull Configuration config, @NotNull GeoReader geoReader) {
            this.geoReader = geoReader;
            // 载入列表外的分值
            for (String mode : config.getSection("not_in_list").getKeys()) {
                notInList.put(mode, config.getInt("not_in_list." + mode));
            }
            // 载入特定国家的分值
            for (String asn : config.getSection("asn").getKeys()) {
                Map<StatusMode, Integer> modeMap = new HashMap<>();
                for (String mode : config.getSection("asn." + asn).getKeys()) {
                    modeMap.put(StatusMode.valueOf(mode), config.getInt("asn." + asn + "." + mode));
                }
                rules.put(asn, modeMap);
            }
        }

        public int calcScore(@NotNull InetAddress address, @NotNull StatusMode mode) {
            String country = geoReader.query(address);
            if (country == null) return 0;
            if (rules.containsKey(country)) {
                return rules.get(country).get(mode);
            }
            return notInList.getOrDefault(mode.name(), 0);
        }
    }
}
