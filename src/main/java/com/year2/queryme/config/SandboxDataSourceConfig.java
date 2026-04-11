package com.year2.queryme.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class SandboxDataSourceConfig {

    @Bean
    @ConfigurationProperties(prefix = "queryme.sandbox-datasource")
    public DataSourceProperties sandboxDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "sandboxDataSource")
    public DataSource sandboxDataSource(
            @Qualifier("sandboxDataSourceProperties") DataSourceProperties sandboxDataSourceProperties) {
        return sandboxDataSourceProperties.initializeDataSourceBuilder().build();
    }

    @Bean(name = "sandboxJdbcTemplate")
    public JdbcTemplate sandboxJdbcTemplate(@Qualifier("sandboxDataSource") DataSource sandboxDataSource) {
        return new JdbcTemplate(sandboxDataSource);
    }
}
