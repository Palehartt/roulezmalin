package com.roulezmalin.backend.config;

import com.roulezmalin.backend.service.RedisListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;

@Configuration
public class RedisConfig {

    @Autowired
    private RedisListener redisListener;
    
    private String nomCanal = "reponses-trajets";

    @Bean
    public MessageListenerAdapter listenerAdapter() {
        // On crée l'adaptateur en lui donnant notre écouteur
        return new MessageListenerAdapter(redisListener);
    }

    @Bean
    public RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory, 
                                                    MessageListenerAdapter listenerAdapter) {
        System.out.println("DEBUG: Démarrage du Container Redis sur le canal: " + nomCanal);
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        
        // On branche l'écouteur sur le bon canal
        container.addMessageListener(listenerAdapter, new PatternTopic(nomCanal));
        
        return container;
    }
}
