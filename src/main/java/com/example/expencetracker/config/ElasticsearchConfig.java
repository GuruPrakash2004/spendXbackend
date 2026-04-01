package com.example.expencetracker.config;



import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContextBuilder;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;

/**
 * Elasticsearch Java client configuration (ES 8.x).
 *
 * This setup handles three things the default autoconfiguration does not:
 *   1. HTTPS — connects over TLS instead of plain HTTP.
 *   2. Basic auth — sends username + password on every request.
 *   3. SSL verification disabled — trusts all certs (acceptable for internal
 *      environments; re-enable certificate validation in production).
 *
 * Client stack (bottom → top):
 *   RestClient  (Apache HttpClient — manages HTTPS + auth)
 *     └─ RestClientTransport  (HTTP ↔ ES protocol bridge)
 *         └─ ElasticsearchClient  (type-safe API used in services)
 */
@Configuration
public class ElasticsearchConfig {

    // Injected from application.yml — elasticsearch.*
    @Value("${elasticsearch.uris}")   private String uri;
    @Value("${elasticsearch.username}") private String username;
    @Value("${elasticsearch.password}") private String password;

    /**
     * Low-level Apache HTTP client configured with:
     *   - HTTPS scheme parsed from the URI
     *   - Basic auth credentials for every request
     *   - SSL context that trusts all certificates (verification-mode: none)
     */
    @Bean
    public RestClient restClient() throws Exception {

        // ── Basic auth ────────────────────────────────────
        // CredentialsProvider tells Apache HttpClient to attach
        // an Authorization header to every outbound request.
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
                AuthScope.ANY,
                new UsernamePasswordCredentials(username, password)
        );

        // ── SSL context — trust all certs ─────────────────
        // loadTrustMaterial(null, (chain, authType) -> true) accepts any certificate.
        // This matches ssl.verification-mode: none in the YAML.
        SSLContext sslContext = SSLContextBuilder.create()
                .loadTrustMaterial(null, (chain, authType) -> true)
                .build();

        // Parse host and port from the URI string (e.g. "https://localhost:9200")
        java.net.URI parsedUri = java.net.URI.create(uri);
        String  scheme = parsedUri.getScheme();   // "https"
        String  host   = parsedUri.getHost();     // "localhost"
        int     port   = parsedUri.getPort();     // 9200

        return RestClient.builder(new HttpHost(host, port, scheme))
                .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                        .setDefaultCredentialsProvider(credentialsProvider)
                        .setSSLContext(sslContext)
                )
                .build();
    }

    /**
     * Transport layer — bridges the Java API client and the REST client.
     * JacksonJsonpMapper handles JSON serialization of ES documents.
     * JavaTimeModule ensures LocalDate / LocalDateTime serialize correctly.
     */
    @Bean
    public ElasticsearchTransport elasticsearchTransport(RestClient restClient) {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        return new RestClientTransport(restClient, new JacksonJsonpMapper(mapper));
    }

    /**
     * The high-level, type-safe Elasticsearch client.
     * Inject this wherever you need to index, search, or delete documents.
     */
    @Bean
    public ElasticsearchClient elasticsearchClient(ElasticsearchTransport transport) {
        return new ElasticsearchClient(transport);
    }
}