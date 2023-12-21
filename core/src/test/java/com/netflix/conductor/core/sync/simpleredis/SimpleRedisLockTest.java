/*
 * Copyright 2023 Netflix, Inc.
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

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mockito.Mockito;

import com.netflix.conductor.core.config.ConductorProperties;
import com.netflix.conductor.core.dal.ExecutionDAOFacade;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class SimpleRedisLockTest {

    private ExecutorService executorService = Executors.newFixedThreadPool(10);

    @Test
    public void testAcquireLock() {
        ExecutionDAOFacade mockExecutionDAOFacade = mock(ExecutionDAOFacade.class);
        ConductorProperties properties = new ConductorProperties();
        properties.setLockTimeToTry(Duration.ofMillis(500));
        properties.setLockLeaseTime(Duration.ofSeconds(60));
        SimpleRedisLock lock = new SimpleRedisLock(mockExecutionDAOFacade, properties);
        String lockId = UUID.randomUUID().toString();
        when(mockExecutionDAOFacade.addLock(any(), Mockito.any(), anyInt())).thenReturn("OK");
        boolean firstLock =
                lock.acquireLock(
                        lockId,
                        properties.getLockTimeToTry().toMillis(),
                        properties.getLockLeaseTime().toMillis(),
                        TimeUnit.MICROSECONDS);
        assertTrue(firstLock);
        boolean secondLock =
                lock.acquireLock(
                        lockId,
                        properties.getLockTimeToTry().toMillis(),
                        properties.getLockLeaseTime().toMillis(),
                        TimeUnit.MICROSECONDS);
        assertTrue(secondLock);

        executorService.submit(
                () -> {
                    when(mockExecutionDAOFacade.addLock(any(), any(), anyInt())).thenReturn(null);
                    boolean anotherThreadLock =
                            lock.acquireLock(
                                    lockId,
                                    properties.getLockTimeToTry().toMillis(),
                                    properties.getLockLeaseTime().toMillis(),
                                    TimeUnit.MICROSECONDS);
                    assertFalse(anotherThreadLock);
                });

        waitObject(5000);

        when(mockExecutionDAOFacade.removeLock(lockId)).thenReturn(0L);
        lock.deleteLock(lockId);

        when(mockExecutionDAOFacade.addLock(any(), any(), anyInt())).thenReturn("OK");
        boolean afterDeleteLock =
                lock.acquireLock(
                        lockId,
                        properties.getLockTimeToTry().toMillis(),
                        properties.getLockLeaseTime().toMillis(),
                        TimeUnit.MICROSECONDS);
        assertTrue(afterDeleteLock);
        when(mockExecutionDAOFacade.removeLock(lockId)).thenReturn(0L);
        lock.releaseLock(lockId);

        when(mockExecutionDAOFacade.addLock(any(), any(), anyInt())).thenReturn("OK");
        boolean afterReleaseLock =
                lock.acquireLock(
                        lockId,
                        properties.getLockTimeToTry().toMillis(),
                        properties.getLockLeaseTime().toMillis(),
                        TimeUnit.MICROSECONDS);
        assertTrue(afterReleaseLock);
        when(mockExecutionDAOFacade.removeLock(lockId)).thenReturn(0L);
        lock.releaseLock(lockId);
        when(mockExecutionDAOFacade.addLock(any(), any(), anyInt())).thenReturn("OK");
        boolean durationLock =
                lock.acquireLock(
                        lockId,
                        properties.getLockTimeToTry().toMillis(),
                        properties.getLockLeaseTime().toMillis(),
                        TimeUnit.MICROSECONDS);
        lock.releaseLock(lockId);
    }

    @Test
    public void testReleaseLock() {
        ExecutionDAOFacade mockExecutionDAOFacade = mock(ExecutionDAOFacade.class);
        String lockId = UUID.randomUUID().toString();
        ConductorProperties properties = new ConductorProperties();
        properties.setLockTimeToTry(Duration.ofMillis(500));
        properties.setLockLeaseTime(Duration.ofSeconds(60));
        SimpleRedisLock lock = new SimpleRedisLock(mockExecutionDAOFacade, properties);
        when(mockExecutionDAOFacade.addLock(any(), Mockito.any(), anyInt())).thenReturn("OK");
        boolean firstLock =
                lock.acquireLock(
                        lockId,
                        properties.getLockTimeToTry().toMillis(),
                        properties.getLockLeaseTime().toMillis(),
                        TimeUnit.MICROSECONDS);
        assertTrue(firstLock);

        boolean secondLock =
                lock.acquireLock(
                        lockId,
                        properties.getLockTimeToTry().toMillis(),
                        properties.getLockLeaseTime().toMillis(),
                        TimeUnit.MICROSECONDS);
        assertTrue(secondLock);

        boolean thirdLock =
                lock.acquireLock(
                        lockId,
                        properties.getLockTimeToTry().toMillis(),
                        properties.getLockLeaseTime().toMillis(),
                        TimeUnit.MICROSECONDS);
        assertTrue(thirdLock);

        when(mockExecutionDAOFacade.removeLock(any())).thenReturn(0L);

        lock.releaseLock(lockId);

        verify(mockExecutionDAOFacade, times(0)).removeLock(any());

        lock.releaseLock(lockId);

        verify(mockExecutionDAOFacade, times(0)).removeLock(any());

        lock.releaseLock(lockId);

        verify(mockExecutionDAOFacade, times(1)).removeLock(any());
    }

    private void waitObject(long waitMilliseconds) {
        try {
            Integer waitObject = Integer.valueOf(1);
            synchronized (waitObject) {
                waitObject.wait(waitMilliseconds);
            }
        } catch (Exception ee) {
            ee.printStackTrace();
        }
    }
}
