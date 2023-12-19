package com.netflix.conductor.core.sync.simpleredis;

import com.netflix.conductor.core.config.ConductorProperties;
import com.netflix.conductor.core.dal.ExecutionDAOFacade;
import org.junit.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class SimpleRedisLockTest {

    @Test
    public void testAcquireLock() {
        ExecutionDAOFacade mockExecutionDAOFacade = mock(ExecutionDAOFacade.class);
        ConductorProperties properties = new ConductorProperties();
        properties.setLockTimeToTry(Duration.ofMillis(500));
        properties.setLockLeaseTime(Duration.ofSeconds(60));
        SimpleRedisLock lock = new SimpleRedisLock(mockExecutionDAOFacade,properties);
        String lockId = UUID.randomUUID().toString();
        when(mockExecutionDAOFacade.addLock(any(), Mockito.any(),anyInt())).thenReturn("OK");
        boolean firstLock = lock.acquireLock(lockId,properties.getLockTimeToTry().toMillis(),properties.getLockLeaseTime().toMillis(), TimeUnit.MICROSECONDS);
        assertTrue(firstLock);
        when(mockExecutionDAOFacade.addLock(any(),any(),anyInt())).thenReturn(null);
        boolean secondLock = lock.acquireLock(lockId,properties.getLockTimeToTry().toMillis(),properties.getLockLeaseTime().toMillis(), TimeUnit.MICROSECONDS);
        assertFalse(secondLock);
        when(mockExecutionDAOFacade.removeLock(lockId)).thenReturn(0L);
        lock.deleteLock(lockId);
        when(mockExecutionDAOFacade.addLock(any(),any(),anyInt())).thenReturn("OK");
        boolean afterDeleteLock = lock.acquireLock(lockId,properties.getLockTimeToTry().toMillis(),properties.getLockLeaseTime().toMillis(), TimeUnit.MICROSECONDS);
        assertTrue(afterDeleteLock);
        when(mockExecutionDAOFacade.removeLock(lockId)).thenReturn(0L);
        lock.releaseLock(lockId);
        when(mockExecutionDAOFacade.addLock(any(),any(),anyInt())).thenReturn("OK");
        boolean afterReleaseLock = lock.acquireLock(lockId,properties.getLockTimeToTry().toMillis(),properties.getLockLeaseTime().toMillis(), TimeUnit.MICROSECONDS);
        assertTrue(afterReleaseLock);
        when(mockExecutionDAOFacade.removeLock(lockId)).thenReturn(0L);
        lock.releaseLock(lockId);
        when(mockExecutionDAOFacade.addLock(any(),any(),anyInt())).thenReturn("OK");
        boolean durationLock = lock.acquireLock(lockId,properties.getLockTimeToTry().toMillis(),properties.getLockLeaseTime().toMillis(), TimeUnit.MICROSECONDS);
        lock.releaseLock(lockId);
    }
}
