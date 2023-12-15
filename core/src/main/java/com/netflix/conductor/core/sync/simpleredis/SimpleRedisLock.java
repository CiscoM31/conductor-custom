/*
 * Copyright 2020 Netflix, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.netflix.conductor.core.sync.simpleredis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.netflix.conductor.core.config.ConductorProperties;
import com.netflix.conductor.core.dal.ExecutionDAOFacade;
import com.netflix.conductor.core.sync.Lock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.concurrent.*;

public class SimpleRedisLock implements Lock {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleRedisLock.class);

    private ObjectMapper objectMapper = new ObjectMapper();
    private ExecutionDAOFacade facade = null;
    private ConductorProperties properties = null;

    public SimpleRedisLock(ExecutionDAOFacade facade, ConductorProperties properties) {
        this.facade=facade;
        this.properties=properties;
    }

    public void acquireLock(String lockId) {
        acquireLock(lockId,properties.getLockTimeToTry().toMillis(),TimeUnit.MILLISECONDS);
    }

    public boolean acquireLock(String lockId, long timeToTry, TimeUnit unit) {
        return acquireLock(lockId,properties.getLockTimeToTry().toMillis(),properties.getLockLeaseTime().toMillis(),TimeUnit.MILLISECONDS);
    }

    public boolean acquireLock(String lockId, long timeToTry, long leaseTime, TimeUnit unit)  {
        int leaseTimeSeconds = (int) properties.getLockLeaseTime().getSeconds();
        if(unit.equals(TimeUnit.MILLISECONDS)) {
            leaseTimeSeconds = (int) leaseTime/1000;
        }
        String value = "{}";
        try {
            ObjectNode valueNode = objectMapper.createObjectNode();
            valueNode.put("time", new Date().toString());
            value = objectMapper.writeValueAsString(valueNode);
        }
        catch(Exception ee) {
            LOGGER.error("Error while serializing value node",ee);
        }

        String result = facade.addLock(lockId,value,leaseTimeSeconds);
        if("OK".equals(result)) {
            return true;
        } else {
            return false;
        }
    }


    public void releaseLock(String lockId) {
        facade.removeLock(lockId);
    }

    public void deleteLock(String lockId) {
        releaseLock(lockId);
    }
}
