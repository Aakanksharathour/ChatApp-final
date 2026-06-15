package com.chatapp.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

/**
 * ════════════════════════════════════════════════════════════════
 *  MongoConfig  —  Custom MongoDB client with Atlas-compatible TLS
 * ════════════════════════════════════════════════════════════════
 *
 *  WHY THIS FILE EXISTS:
 *  ─────────────────────
 *  Java 25 changed how TLS handshakes work by default.
 *  This causes MongoDB Atlas to reject the connection with
 *  an "internal_error" SSL alert.
 *
 *  This config manually creates the MongoClient with:
 *  1. TLS 1.2 protocol (forced — skips the Java 25 TLS 1.3 behavior)
 *  2. Trust-all certificate manager (bypasses certificate validation issues)
 *  3. Hostname verification disabled
 *
 *  This overrides Spring Boot's auto-configured MongoClient.
 */
@Configuration
public class MongoConfig {

    private static final Logger log = LoggerFactory.getLogger(MongoConfig.class);

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Bean
    public MongoClient mongoClient() throws Exception {
        String uri = "mongodb+srv://springchat:Spring2026@cluster0.95lwtsy.mongodb.net/chatapp?appName=Cluster0";
        log.info("Configuring MongoDB client — URI username: springchat");

        // ── Force TLS 1.2 at the JVM level before anything else ──────────
        // This must be done before any SSL socket is created.
        // Disabling TLS 1.3 removes Java 25's new TLS 1.3 extensions from
        // the ClientHello that Atlas's TLS terminator cannot handle.
        System.setProperty("jdk.tls.client.protocols", "TLSv1.2");
        System.setProperty("https.protocols", "TLSv1.2");

        // ── Create a TrustManager that accepts ALL certificates ───────────
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                public void checkServerTrusted(X509Certificate[] certs, String authType) {}
            }
        };

        // ── Create SSLContext with TLS 1.2 ───────────────────────────────
        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(null, trustAllCerts, new SecureRandom());

        // ── Set as the JVM-wide default SSL context ───────────────────────
        // This ensures even internal MongoDB driver code that creates
        // SSL sockets without our settings also uses TLS 1.2.
        SSLContext.setDefault(sslContext);

        // ── Build MongoClientSettings ─────────────────────────────────────
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(uri))
                .applyToSslSettings(builder -> {
                    builder.enabled(true);
                    builder.context(sslContext);
                    builder.invalidHostNameAllowed(true);
                })
                .build();

        log.info("MongoDB client configured — TLS 1.2 forced, trust-all enabled");
        return MongoClients.create(settings);
    }
}
