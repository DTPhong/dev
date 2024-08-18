package cc.bliss.match3.service.gamemanager.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy;
import org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "cc.bliss.match3.service.gamemanager.db.match3",
        entityManagerFactoryRef = "match3EntityManagerFactory",
        transactionManagerRef = "match3TransactionManager"
)
public class Match3WriteDataSourceConfig {
    protected Map<String, Object> jpaProperties() {
        Map<String, Object> props = new HashMap<>();
        props.put("hibernate.physical_naming_strategy", SpringPhysicalNamingStrategy.class.getName());
        props.put("hibernate.implicit_naming_strategy", SpringImplicitNamingStrategy.class.getName());
        return props;
    }

    @Bean
    @Primary
    @ConfigurationProperties("app.datasource.match3")
    public DataSourceProperties match3DataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    @ConfigurationProperties("app.datasource.match3.configuration")
    public DataSource match3DataSource() {
        return match3DataSourceProperties().initializeDataSourceBuilder()
                .type(HikariDataSource.class).build();
    }

    @Primary
    @Bean(name = "match3EntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean match3EntityManagerFactory(EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(match3DataSource())
                .packages("cc.bliss.match3.service.gamemanager.ent.persistence.match3")
                .properties(jpaProperties())
                .build();
    }

    @Primary
    @Bean(name = "match3TransactionManager")
    public PlatformTransactionManager match3TransactionManager(
            final @Qualifier("match3EntityManagerFactory") LocalContainerEntityManagerFactoryBean match3EntityManagerFactory) {
        return new JpaTransactionManager(match3EntityManagerFactory.getObject());
    }
}

