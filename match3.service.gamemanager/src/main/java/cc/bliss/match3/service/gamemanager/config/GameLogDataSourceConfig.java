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
@EnableJpaRepositories(basePackages = "cc.bliss.match3.service.gamemanager.db.game_log",
        entityManagerFactoryRef = "logEntityManagerFactory",
        transactionManagerRef = "logTransactionManager"
)
public class GameLogDataSourceConfig {
    protected Map<String, Object> jpaProperties() {
        Map<String, Object> props = new HashMap<>();
        props.put("hibernate.physical_naming_strategy", SpringPhysicalNamingStrategy.class.getName());
        props.put("hibernate.implicit_naming_strategy", SpringImplicitNamingStrategy.class.getName());
        return props;
    }

    @Bean
    @ConfigurationProperties("app.datasource.log")
    public DataSourceProperties gameLogDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("app.datasource.log.configuration")
    public DataSource gameLogDataSource() {
        return gameLogDataSourceProperties().initializeDataSourceBuilder()
                .type(HikariDataSource.class).build();
    }

    @Bean(name = "logEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean gameLogEntityManagerFactory(EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(gameLogDataSource())
                .packages("cc.bliss.match3.service.gamemanager.ent.persistence.game_log")
                .properties(jpaProperties())
                .build();
    }

    @Bean(name = "logTransactionManager")
    public PlatformTransactionManager gameLogTransactionManager(
            final @Qualifier("logEntityManagerFactory") LocalContainerEntityManagerFactoryBean gameLogEntityManagerFactory) {
        return new JpaTransactionManager(gameLogEntityManagerFactory.getObject());
    }
}
