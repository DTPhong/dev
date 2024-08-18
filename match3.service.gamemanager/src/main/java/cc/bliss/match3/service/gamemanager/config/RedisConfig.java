/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cc.bliss.match3.service.gamemanager.config;

import cc.bliss.match3.service.gamemanager.service.system.DefaultMessageDelegate;
import cc.bliss.match3.service.gamemanager.util.GsonRedisSerializer;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.*;

import java.time.Duration;

/**
 * @author Phong
 */
@Configuration
@EnableCaching
public class RedisConfig {

    @Value("${redis.host}")
    private String redisHost;

    @Value("${redis.port}")
    private int redisPort;

    @Value("${redis.password}")
    private String redisPassword;

    @Value("${redis.expire.second}")
    private int redisExpireSecond;

    private RedisCacheManager buildRedisCacheManager(RedisSerializer keySerializer, RedisSerializer valueSerializer) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(redisExpireSecond))
                .disableCachingNullValues()
                .serializeValuesWith( RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(keySerializer));

        return RedisCacheManager.builder(redisConnectionFactory())
                .cacheDefaults(config)
                .build();
    }

    @Bean
    @Primary
    public RedisCacheManager cacheManagerObject() {
        return buildRedisCacheManager(new StringRedisSerializer(), jackson2JsonRedisSerializer());
    }

    @Bean
    public RedisCacheManager cacheManagerList() {
        return buildRedisCacheManager(new StringRedisSerializer(), new GenericJackson2JsonRedisSerializer());
    }

    @Bean
    public RedissonClient redissonClient() {
        String address = "redis://" + redisHost + ":" + redisPort;
        Config config = new Config();
        config.useSingleServer().setAddress(address).setPassword(redisPassword);
        return Redisson.create(config);
    }

    @Bean
    DefaultMessageDelegate redisListener() {
        return new DefaultMessageDelegate();
    }

    @Bean
    MessageListenerAdapter messageListenerAdapter(DefaultMessageDelegate redisListener) {
        return new MessageListenerAdapter(redisListener, "handleMessage");
    }

    @Bean
    RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory, MessageListenerAdapter redisListener) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(redisListener, ChannelTopic.of("serverMaintainTopic"));
        return container;
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(redisHost, redisPort);
        configuration.setPassword(redisPassword);
        JedisClientConfiguration jedisClientConfiguration = JedisClientConfiguration.builder().usePooling().build();
        JedisConnectionFactory factory = new JedisConnectionFactory(configuration, jedisClientConfiguration);
        factory.afterPropertiesSet();
        return factory;
    }

    @Bean
    public RedisTemplate<Object, Object> redisTemplate() {
        final RedisTemplate<Object, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory());
        configureSerializers(redisTemplate);
        return redisTemplate;
    }

    @Bean
    public RedisTemplate<String, String> redisTemplateString() {
        final RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory());
        configureStringSerializers(redisTemplate);
        return redisTemplate;
    }

    @Bean
    public HashOperations<String, String, String> hashOperation() {
        return redisTemplateString().opsForHash();
    }

    private void configureStringSerializers(RedisTemplate<String, String> redisTemplate) {

        RedisSerializer stringSerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(stringSerializer);
        redisTemplate.setValueSerializer(stringSerializer);
        redisTemplate.setHashKeySerializer(stringSerializer);
        redisTemplate.setHashValueSerializer(stringSerializer);
    }

    private Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer() {
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        om.configure(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        om.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        jackson2JsonRedisSerializer.setObjectMapper(om);
        return jackson2JsonRedisSerializer;
    }

    private void configureSerializers(RedisTemplate<Object, Object> redisTemplate) {
        RedisSerializer<String> serializerKey = new StringRedisSerializer();
        redisTemplate.setKeySerializer(serializerKey);

        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = jackson2JsonRedisSerializer();

        redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);
        redisTemplate.setHashKeySerializer(jackson2JsonRedisSerializer);
        redisTemplate.setHashValueSerializer(jackson2JsonRedisSerializer);
    }
}
