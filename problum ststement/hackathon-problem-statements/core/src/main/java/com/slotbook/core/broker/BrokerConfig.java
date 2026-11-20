package com.slotbook.core.broker;

import com.slotbook.contract.Topics;
import jakarta.jms.ConnectionFactory;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.settings.impl.AddressSettings;
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.jms.artemis.ArtemisConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;

/**
 * The message broker runs inside the app (Apache ActiveMQ Artemis, embedded, nothing to install).
 * A message whose listener throws is redelivered; after 5 failed deliveries it goes to the DLQ queue.
 */
@Configuration
@EnableJms
public class BrokerConfig {

    @Bean
    public ArtemisConfigurationCustomizer deadLetters() {
        return config -> config.addAddressSetting("#", new AddressSettings()
                .setMaxDeliveryAttempts(5)
                .setRedeliveryDelay(500)
                .setDeadLetterAddress(SimpleString.of("DLQ"))
                .setAutoCreateDeadLetterResources(true));
    }

    /** Topic subscriptions: each module gets its own durable copy of every message; a listener that throws gets it again. */
    @Bean(Topics.LISTENER_FACTORY)
    public DefaultJmsListenerContainerFactory slotbookTopicListener(ConnectionFactory connectionFactory,
                                                                    DefaultJmsListenerContainerFactoryConfigurer configurer) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setPubSubDomain(true);
        factory.setSubscriptionDurable(true);
        factory.setSubscriptionShared(true);
        factory.setSessionTransacted(true);
        return factory;
    }
}
