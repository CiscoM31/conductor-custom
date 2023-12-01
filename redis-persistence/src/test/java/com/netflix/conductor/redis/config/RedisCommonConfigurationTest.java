package com.netflix.conductor.redis.config;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Map;

public class RedisCommonConfigurationTest {
    private static final Logger log = LoggerFactory.getLogger(RedisCommonConfigurationTest.class);

    @Test
    public void testGetShardName() {
        RedisCommonConfiguration config = new RedisCommonConfiguration();
        assert(config.getShardName().equals("custom"));
        updateEnv("HOSTNAME","galaxy-0");
        assert(config.getShardName().equals("0"));
        updateEnv("HOSTNAME","galaxy-0.svc.cluster.local");
        assert(config.getShardName().equals("0"));
    }


    @SuppressWarnings({ "unchecked" })
    private void updateEnv(String name, String val) {
        try {
            Map<String, String> env = System.getenv();
            Field field = env.getClass().getDeclaredField("m");
            field.setAccessible(true);
            ((Map<String, String>) field.get(env)).put(name, val);
        }
        catch(Exception ee) {
            log.error("Error while setting env variable",ee);
        }
    }
}