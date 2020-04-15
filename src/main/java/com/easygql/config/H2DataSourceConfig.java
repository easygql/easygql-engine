package com.easygql.config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

/**
 * @author guofen
 * @date 2019-10-27 16:41
 */

@Configuration
public class H2DataSourceConfig {
    /**
     *
     * @return
     */
    @Bean(name = "h2")
    @Qualifier("h2")
    @ConfigurationProperties(prefix="spring.datasource.h2")
    DataSource h2(){
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:easygql;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false");
        dataSource.setUsername("sa");
        dataSource.setPassword("test111");
        return dataSource;
    }

    /**
     *
     * @param h2
     * @return
     */
    @Bean(name = "h2JdbcTemplate")
    JdbcTemplate h2JdbcTemplate(@Autowired @Qualifier("h2") DataSource h2){
        return new JdbcTemplate(h2);
    }

    /**
     *
     * @param h2
     * @return
     */
    @Bean(name = "h2TXManager")
    DataSourceTransactionManager h2TXManager(@Autowired @Qualifier("h2") DataSource h2){
       return new DataSourceTransactionManager(h2);
    }
}