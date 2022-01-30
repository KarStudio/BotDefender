package com.ghostchu.botdefender.iputil;

import com.ejlchina.okhttps.OkHttps;
import com.ghostchu.botdefender.BotDefender;
import com.ghostchu.botdefender.util.OkHttpDownloader;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class GeoCityReader {
    private final File databaseFile;
    private final String key;
    private final BotDefender plugin;
    private DatabaseReader reader = null;

    public GeoCityReader(@NotNull BotDefender plugin, @NotNull File file, @NotNull String key){
        this.plugin = plugin;
        this.databaseFile = file;
        this.key = key;
    }

    /**
     * Update the GeoLite-Country database.
     */
    public void updateDatabase(){
        File tempFile = new File(databaseFile.getParentFile(), databaseFile.getName() + ".tmp");
        // Download the latest version of the database use okHttp
        String packUrl = "https://download.maxmind.com/app/geoip_download?edition_id=GeoLite2-City&license_key=YOUR_LICENSE_KEY&suffix=tar.gz";
        packUrl = packUrl.replace("YOUR_LICENSE_KEY", key);
        // Download the latest version of the database hash file
        String hashUrl = "https://download.maxmind.com/app/geoip_download?edition_id=GeoLite2-City&license_key=YOUR_LICENSE_KEY&suffix=tar.gz.sha256";
        hashUrl = hashUrl.replace("YOUR_LICENSE_KEY", key);
        OkHttpDownloader downloader = new OkHttpDownloader(plugin);
        downloader.download(packUrl, tempFile);
        String hash = OkHttps
                .sync(hashUrl)
                .get()
                .getBody()
                .toString();
        // Check the hash of the downloaded file
        try(InputStream stream = new FileInputStream(tempFile)) {
            if (!hash.startsWith(DigestUtils.sha256Hex(stream))) {
                plugin.getLogger().warning("The downloaded GeoLite-Country database is corrupted!");
                return;
            }
        }catch (IOException exception){
            plugin.getLogger().warning("The downloaded GeoLite-Country database is corrupted!");
            return;
        }
        try {
            Files.deleteIfExists(databaseFile.toPath());
            Files.move(tempFile.toPath(), databaseFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            this.reader = new DatabaseReader.Builder(databaseFile).build();
        } catch (IOException e) {
            plugin.getLogger().warning("The downloaded GeoLite-Country database is corrupted or unable to access!");
        }
    }

    /**
     * Get the country code of the given IP address.
     * @param ip The IP address.
     * @return The query result
     */
    @Nullable
    public QueryResult query(@NotNull String ip){
        try {
            return query(InetAddress.getByName(ip));
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get the city level query of the given IP address.
     * @param address The IP address to query.
     * @return Query result.
     */
    @Nullable
    public QueryResult query(@NotNull InetAddress address){
        if(reader == null) return null;
        try {
            CityResponse response = reader.city(address);
            return new QueryResult(response.getCountry(),response.getSubdivisions(),response.getCity(),response.getPostal(),response.getLocation());
        } catch (IOException | GeoIp2Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Query Result
     */
    @AllArgsConstructor
    @Data
    static class QueryResult{
        private final Country country;
        private final List<Subdivision> subdivisions;
        private final City city;
        private final Postal postal;
        private final Location location;
    }

}
