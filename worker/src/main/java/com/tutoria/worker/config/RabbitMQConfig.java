package com.tutoria.worker.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.perguntas-exchange}")
    private String perguntasExchange;

    @Value("${app.rabbitmq.perguntas-queue}")
    private String perguntasQueue;

    @Value("${app.rabbitmq.perguntas-routing-key}")
    private String perguntasRoutingKey;

    @Value("${app.rabbitmq.respostas-exchange}")
    private String respostasExchange;

    @Value("${app.rabbitmq.respostas-queue}")
    private String respostasQueue;

    @Value("${app.rabbitmq.respostas-routing-key}")
    private String respostasRoutingKey;

    @Bean
    public DirectExchange perguntasExchange() {
        return new DirectExchange(perguntasExchange, true, false);
    }

    @Bean
    public Queue perguntasQueue() {
        return QueueBuilder.durable(perguntasQueue).build();
    }

    @Bean
    public Binding perguntasBinding(Queue perguntasQueue, DirectExchange perguntasExchange) {
        return BindingBuilder.bind(perguntasQueue).to(perguntasExchange).with(perguntasRoutingKey);
    }

    @Bean
    public DirectExchange respostasExchange() {
        return new DirectExchange(respostasExchange, true, false);
    }

    @Bean
    public Queue respostasQueue() {
        return QueueBuilder.durable(respostasQueue).build();
    }

    @Bean
    public Binding respostasBinding(Queue respostasQueue, DirectExchange respostasExchange) {
        return BindingBuilder.bind(respostasQueue).to(respostasExchange).with(respostasRoutingKey);
    }

    @Bean
    public MessageConverter jacksonMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jacksonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jacksonMessageConverter);
        return template;
    }
}
