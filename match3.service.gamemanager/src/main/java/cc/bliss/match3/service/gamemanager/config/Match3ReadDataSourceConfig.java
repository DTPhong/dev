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
@EnableJpaRepositories(basePackages = "cc.bliss.match3.service.gamemanager.db.match3_read",
        entityManagerFactoryRef = "match3ReadEntityManagerFactory",
        transactionManagerRef = "match3ReadTransactionManager"
)
public class Match3ReadDataSourceConfig {

    protected Map<String, Object> jpaProperties() {
        Map<String, Object> props = new HashMap<>();
        props.put("hibernate.physical_naming_strategy", SpringPhysicalNamingStrategy.class.getName());
        props.put("hibernate.implicit_naming_strategy", SpringImplicitNamingStrategy.class.getName());
        return props;
    }

    @Bean
    @ConfigurationProperties("app.datasource.match3-read")
    public DataSourceProperties match3ReadDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("app.datasource.match3-read.configuration")
    public DataSource match3ReadDataSource() {
        return match3ReadDataSourceProperties().initializeDataSourceBuilder()
                .type(HikariDataSource.class).build();
    }

    @Bean(name = "match3ReadEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean match3EntityManagerFactory(EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(match3ReadDataSource())
                .packages("cc.bliss.match3.service.gamemanager.ent.persistence.match3")
                .properties(jpaProperties())
                .build();
    }

    @Bean(name = "match3ReadTransactionManager")
    public PlatformTransactionManager match3ReadTransactionManager(
            final @Qualifier("match3ReadEntityManagerFactory") LocalContainerEntityManagerFactoryBean match3EntityManagerFactory) {
        return new JpaTransactionManager(match3EntityManagerFactory.getObject());
    }
}