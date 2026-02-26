package com.autoparts.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestConfig {
    @Bean
    public RestClient restClient(RestClient.Builder builder,
                                 @Value("${crawler.base-url:http://localhost:9001}") String crawlerBaseUrl) {
        return builder.baseUrl(crawlerBaseUrl).build();
    }
}
