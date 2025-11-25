package org.ncsu.settleup.settlementservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AmqpConfig}.  These tests verify that the beans
 * declared in the configuration class are created with the expected
 * properties.  A mock {@link ConnectionFactory} is used when creating
 * the {@link RabbitTemplate} to avoid any real AMQP connections.
 */
class AmqpConfigTest {

    @Test
    void topicExchange_hasExpectedName() {
        AmqpConfig config = new AmqpConfig();
        TopicExchange exchange = config.topicExchange();
        assertNotNull(exchange, "Exchange should not be null");
        assertEquals(AmqpConfig.EXCHANGE_NAME, exchange.getName(), "Exchange name should match constant");
    }

    @Test
    void queue_isDurableAndHasExpectedName() {
        AmqpConfig config = new AmqpConfig();
        Queue queue = config.queue();
        assertNotNull(queue, "Queue should not be null");
        assertEquals(AmqpConfig.QUEUE_NAME, queue.getName(), "Queue name should match constant");
        assertTrue(queue.isDurable(), "Queue should be durable");
    }

    @Test
    void binding_bindsQueueToExchangeWithCorrectRoutingKey() {
        AmqpConfig config = new AmqpConfig();
        Queue queue = config.queue();
        TopicExchange exchange = config.topicExchange();
        Binding binding = config.binding(queue, exchange);
        assertNotNull(binding, "Binding should not be null");
        // The binding should be between the queue and the exchange
        assertEquals(queue.getName(), binding.getDestination(), "Binding destination should be the queue name");
        assertEquals(exchange.getName(), binding.getExchange(), "Binding exchange should be the exchange name");
        assertEquals("expense.recorded", binding.getRoutingKey(), "Routing key should be 'expense.recorded'");
    }

    @Test
    void jackson2JsonMessageConverter_returnsInstance() {
        AmqpConfig config = new AmqpConfig();
        Jackson2JsonMessageConverter converter1 = config.jackson2JsonMessageConverter();
        Jackson2JsonMessageConverter converter2 = config.jackson2JsonMessageConverter();
        assertNotNull(converter1, "Converter should not be null");
        assertNotNull(converter2, "Second converter should not be null");
        // The converter type should be Jackson2JsonMessageConverter
        assertEquals(Jackson2JsonMessageConverter.class, converter1.getClass());
        // A new instance should be returned each time (the config does not memoize)
        assertNotSame(converter1, converter2, "jackson2JsonMessageConverter should produce new instances");
    }

    @Test
    void rabbitTemplate_usesProvidedConnectionFactoryAndConverter() {
        AmqpConfig config = new AmqpConfig();
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        RabbitTemplate template = config.rabbitTemplate(connectionFactory, converter);
        assertNotNull(template, "RabbitTemplate should not be null");
        // Template should be configured with the provided connection factory and converter
        assertSame(connectionFactory, template.getConnectionFactory(), "ConnectionFactory should be the one provided");
        assertSame(converter, template.getMessageConverter(), "MessageConverter should be the one provided");
    }
}
