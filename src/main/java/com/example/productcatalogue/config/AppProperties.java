package com.example.productcatalogue.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Seed seed, Ussd ussd) {

    public AppProperties {
        seed = seed == null ? new Seed(false) : seed;
        ussd = ussd == null ? new Ussd(180) : ussd;
    }

    public record Seed(boolean enabled) {}

    public record Ussd(@DefaultValue("180") long sessionTtlSeconds) {}
}
