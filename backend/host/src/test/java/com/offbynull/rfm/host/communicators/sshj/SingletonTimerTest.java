package com.offbynull.rfm.host.communicators.sshj;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SingletonTimerTest {

    private SingletonTimer fixture;

    @Before
    public void setUp() {
        fixture = new SingletonTimer();
    }
    
    @After
    public void tearDown() {
        for (int i = 0; i < fixture.getReferenceCount(); i++) {
            fixture.removeReference();
        }
    }

    @Test(timeout = 2000L)
    public void mustScheduleTask() throws Exception {
        fixture.addReference();

        CountDownLatch latch = new CountDownLatch(1);
        fixture.schedule(() -> latch.countDown(), 500L, TimeUnit.MILLISECONDS);
        
        latch.await();
    }

    @Test(expected = IllegalStateException.class)
    public void mustFailToScheduleTaskIfNotReferences() throws Exception {
        fixture.schedule(() -> {}, 500L, TimeUnit.MILLISECONDS);
    }
    
}
