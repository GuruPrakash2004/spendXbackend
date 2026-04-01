package com.example.expencetracker.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;
 
/**
 * ActiveMQ Artemis / JMS configuration.
 *
 * The key concern here is message serialization:
 *   By default JmsTemplate sends Java-serialized (binary) messages, which
 *   are opaque and tied to our JVM. Switching to JSON means any consumer
 *   (another Spring service, a Node.js app, etc.) can read the messages.
 *
 * We configure MappingJackson2MessageConverter to:
 *   - Serialize messages as JSON text (MessageType.TEXT).
 *   - Embed a __TypeId__ header so the consumer knows which class to
 *     deserialize the payload into without needing class names in the body.
 */
@Configuration
public class JmsConfig {
 
    /**
     * Registers a Jackson-based JMS message converter as a Spring bean.
     *
     * Spring Boot's JMS autoconfiguration automatically picks up any
     * MessageConverter bean in the context and wires it into JmsTemplate
     * and @JmsListener — no extra plumbing required.
     */
    @Bean
    public MessageConverter jacksonJmsMessageConverter() {
 
        // Build an ObjectMapper that handles Java 8 date/time types (LocalDate, etc.)
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        // Write dates as ISO-8601 strings, not epoch milliseconds
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
 
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
 
        // Send as a JMS TextMessage (JSON string) rather than binary ObjectMessage
        converter.setTargetType(MessageType.TEXT);
 
        // __TypeId__ header lets the @JmsListener side know which Java class
        // to deserialize the message back into without guessing
        converter.setTypeIdPropertyName("__TypeId__");
 
        converter.setObjectMapper(objectMapper);
        return converter;
    }
}
