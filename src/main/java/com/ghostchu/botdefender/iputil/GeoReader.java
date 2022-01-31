package com.ghostchu.botdefender.iputil;

import com.ghostchu.botdefender.BotDefender;
import com.ice.tar.TarEntry;
import com.ice.tar.TarInputStream;
import com.maxmind.db.CHMCache;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.AsnResponse;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.model.CountryResponse;
import com.maxmind.geoip2.record.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;

public class GeoReader {
    private final BotDefender plugin;
    private DatabaseReader cityReader;
    private DatabaseReader asnReader;

    public GeoReader(@NotNull BotDefender defender) {
        this.plugin = defender;
    }


    /**
     * Setup and update database files.
     *
     * @param key        The MaxMind API key.
     * @param dataFolder The folder to save the database files to.
     * @throws IOException If the database files cannot be downloaded.
     */
    public void setupAndUpdateDatabase(@NotNull String key, @NotNull File dataFolder) throws IllegalStateException, IOException {
        if (!dataFolder.exists())
            //noinspection ResultOfMethodCallIgnored
            dataFolder.mkdirs();
        // Atom update - Only replace database file while new database download successfully.

        try {
            downloadDatabase("https://download.maxmind.com/app/geoip_download?edition_id=GeoLite2-Country&license_key={LICENSEKEY}&suffix=tar.gz".replace("{LICENSEKEY}", key), new File(dataFolder, "GeoIP2-Country.mmdb.tmp"));
            Files.move(new File(dataFolder, "GeoIP2-Country.mmdb.tmp").toPath(), new File(dataFolder, "GeoIP2-Country.mmdb").toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to download GeoIP-City database.", e);
        }
        try {
            downloadDatabase("https://download.maxmind.com/app/geoip_download?edition_id=GeoLite2-ASN&license_key={LICENSEKEY}&suffix=tar.gz".replace("{LICENSEKEY}", key), new File(dataFolder, "GeoIP2-ASN.mmdb.tmp"));
            Files.move(new File(dataFolder, "GeoIP2-ASN.mmdb.tmp").toPath(), new File(dataFolder, "GeoIP2-ASN.mmdb").toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to download GeoIP-ASN database.", e);
        }
        File cityFile = new File(dataFolder, "GeoIP2-City.mmdb");
        File asnFile = new File(dataFolder, "GeoIP2-ASN.mmdb");
        if (cityFile.exists()) {
            this.cityReader = new DatabaseReader.Builder(cityFile)
                    .withCache(new CHMCache())
                    .build();
        }
        if (asnFile.exists()) {
            this.asnReader = new DatabaseReader.Builder(asnFile)
                    .withCache(new CHMCache())
                    .build();
        }

        if (this.cityReader == null || this.asnReader == null) {
            throw new IOException("Failed to load GeoIP database.");
        }
    }

    /**
     * Download GeoIP database from MaxMind
     *
     * @param url          URL of the database
     * @param databaseFile The file to save the database to
     * @throws IOException If the database cannot be downloaded
     * @author EssentialsX - GeoIP
     */
    private void downloadDatabase(String url, File databaseFile) throws IOException {
        final URL downloadUrl = new URL(url);
        final URLConnection conn = downloadUrl.openConnection();
        conn.setConnectTimeout(10000);
        conn.connect();
        InputStream input = conn.getInputStream();
        final OutputStream output = new FileOutputStream(databaseFile);
        final byte[] buffer = new byte[2048];
        if (url.contains("gz")) {
            input = new GZIPInputStream(input);
            if (url.contains("tar.gz")) {
                // The new GeoIP2 uses tar.gz to pack the db file along with some other txt. So it makes things a bit complicated here.
                String filename;
                final TarInputStream tarInputStream = new TarInputStream(input);
                TarEntry entry;
                while ((entry = tarInputStream.getNextEntry()) != null) {
                    if (!entry.isDirectory()) {
                        filename = entry.getName();
                        if (filename.substring(filename.length() - 5).equalsIgnoreCase(".mmdb")) {
                            input = tarInputStream;
                            break;
                        }
                    }
                }
            }
        }
        int length = input.read(buffer);
        while (length >= 0) {
            output.write(buffer, 0, length);
            length = input.read(buffer);
        }
        output.close();
        input.close();
    }

    /**
     * Get the ASN of the given IP address.
     *
     * @param ip The IP address.
     * @return The ASN of the given IP address. Null if the IP address is invalid.
     */
    @Nullable
    public ASNData getAsn(@NotNull String ip) {
        try {
            return getAsn(InetAddress.getByName(ip));
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get the ASN of the given IP address.
     *
     * @param address The IP address.
     * @return The ASN of the given IP address. Null if the IP address is invalid.
     */
    @Nullable
    public ASNData getAsn(@NotNull InetAddress address) {
        if (asnReader == null) return null;
        try {
            AsnResponse response = asnReader.asn(address);
            return new ASNData(response.getAutonomousSystemNumber(), response.getAutonomousSystemOrganization());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * ASN Data
     */
    @AllArgsConstructor
    @Data
    static class ASNData {
        private final Long number;
        private final String organization;
    }


    /**
     * Get the country code of the given IP address.
     *
     * @param ip The IP address.
     * @return The query result
     */
    @Nullable
    public String query(@NotNull String ip) {
        try {
            return query(InetAddress.getByName(ip));
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get the city level query of the given IP address.
     *
     * @param address The IP address to query.
     * @return Query result.
     */
    @Nullable
    public String query(@NotNull InetAddress address) {
        if (cityReader == null) return null;
        try {
            CountryResponse response = cityReader.country(address);
            if(response.getRepresentedCountry() != null){
                return response.getRepresentedCountry().getIsoCode();
            }
            return response.getRegisteredCountry().getIsoCode();
        } catch (IOException | GeoIp2Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
