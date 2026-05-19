package com.flowboard.api_gateway.config;

import io.netty.resolver.DefaultAddressResolverGroup;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/**
 * NettyResolverConfig — forces Netty to use the JVM's system DNS resolver
 * instead of its own async DNS resolver.
 *
 * Problem: Netty's DNS resolver (DnsNameResolver) sends DNS queries directly
 * over UDP using the system's configured DNS servers but handles
 * the query asynchronously in its own event loop. In some corporate network /
 * ISP environments, IPv6 DNS servers time out for external domains like
 * www.googleapis.com, causing OAuth2 token exchange to fail with
 * "Failed to resolve 'www.googleapis.com' after 2 queries".
 *
 * Solution: Use DefaultAddressResolverGroup.INSTANCE which delegates to the
 * JVM's java.net.InetAddress resolution (i.e., OS-level DNS) and is always
 * reliable in the same environment where the browser resolves names fine.
 */
@Configuration
public class NettyResolverConfig {

    /**
     * ReactorClientHttpConnector bean configured with the JVM's default
     * address resolver. Spring Boot auto-configuration picks this up and uses
     * it for all reactive HTTP clients, including the one used by
     * Spring Security's OAuth2 token endpoint WebClient.
     */
    @Bean
    public ReactorClientHttpConnector reactorClientHttpConnector() {
        HttpClient httpClient = HttpClient.create()
                .resolver(DefaultAddressResolverGroup.INSTANCE);
        return new ReactorClientHttpConnector(httpClient);
    }
}
