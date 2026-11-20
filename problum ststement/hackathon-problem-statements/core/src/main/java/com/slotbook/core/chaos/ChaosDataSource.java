package com.slotbook.core.chaos;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Simulated database failures, for the chaos runner. Off unless slotbook.chaos.db-failure-chance is above 0.
 * When on, a statement whose SQL mentions slotbook.chaos.db-failure-tables fails with an SQLException at that
 * chance, as if the database had dropped the connection. Starts only after the app is up, so migrations run clean.
 */
@Component
public class ChaosDataSource implements BeanPostProcessor {

    private final double chance;
    private final String tables;
    private final AtomicBoolean active = new AtomicBoolean(false);

    public ChaosDataSource(@Value("${slotbook.chaos.db-failure-chance:0}") double chance,
                           @Value("${slotbook.chaos.db-failure-tables:settlement_}") String tables) {
        this.chance = chance;
        this.tables = tables;
    }

    @EventListener(ApplicationReadyEvent.class)
    void start() {
        active.set(chance > 0);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (chance <= 0 || !(bean instanceof DataSource ds)) {
            return bean;
        }
        return proxy(DataSource.class, ds, (method, args) -> {
            Object result = method.invoke(ds, args);
            return result instanceof Connection c ? connection(c) : result;
        });
    }

    private Connection connection(Connection c) {
        return proxy(Connection.class, c, (method, args) -> {
            String name = method.getName();
            if ((name.equals("prepareStatement") || name.equals("prepareCall")) && args != null && args[0] instanceof String sql) {
                maybeFail(sql);
            }
            Object result = method.invoke(c, args);
            return result instanceof Statement s && !(result instanceof java.sql.PreparedStatement) ? statement(s) : result;
        });
    }

    private Statement statement(Statement s) {
        return proxy(Statement.class, s, (method, args) -> {
            if (method.getName().startsWith("execute") && args != null && args.length > 0 && args[0] instanceof String sql) {
                maybeFail(sql);
            }
            return method.invoke(s, args);
        });
    }

    private void maybeFail(String sql) throws SQLException {
        if (active.get() && sql.toLowerCase().contains(tables) && ThreadLocalRandom.current().nextDouble() < chance) {
            throw new SQLException("chaos: simulated database failure", "08006");
        }
    }

    private interface Handler {
        Object handle(java.lang.reflect.Method method, Object[] args) throws Throwable;
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, T target, Handler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (p, method, args) -> {
            try {
                return handler.handle(method, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        });
    }
}
