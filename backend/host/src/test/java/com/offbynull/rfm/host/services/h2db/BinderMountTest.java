package com.offbynull.rfm.host.services.h2db;

import com.offbynull.rfm.host.model.partition.MountPartition;
import com.offbynull.rfm.host.model.requirement.MountRequirement;
import com.offbynull.rfm.host.model.specification.CapacityEnabledSpecification;
import com.offbynull.rfm.host.model.specification.MountSpecification;
import com.offbynull.rfm.host.parser.Parser;
import static com.offbynull.rfm.host.services.h2db.BinderTestUtils.assertMountPartition;
import static com.offbynull.rfm.host.testutils.TestUtils.loadSpec;
import static com.offbynull.rfm.host.testutils.TestUtils.pullCapacities;
import java.math.BigDecimal;
import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.EMPTY_MAP;
import java.util.IdentityHashMap;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class BinderMountTest {
    
    private final Parser parser = new Parser(EMPTY_LIST, EMPTY_LIST);
    
    @Test
    public void mustBindToMountWithMaxCapacity() throws Exception {
        MountRequirement mountReq = (MountRequirement) parser.parseReq(EMPTY_MAP, "1 mount with [100,100000] capacity");
        MountSpecification mountSpec = (MountSpecification) loadSpec("mount:100000{ s_target:/test1 }");
        
        IdentityHashMap<CapacityEnabledSpecification, BigDecimal> updatableCapacities = new IdentityHashMap<>();
        pullCapacities(mountSpec, updatableCapacities);
        updatableCapacities.replace(mountSpec, BigDecimal.valueOf(90000));
        
        MountPartition mountPartition = Binder.partitionMount(updatableCapacities, mountReq, mountSpec);
        
        assertMountPartition(mountPartition, "/test1", 90000);
    }
    
    @Test
    public void mustBindToMountWithMaxRequirement() throws Exception {
        MountRequirement mountReq = (MountRequirement) parser.parseReq(EMPTY_MAP, "1 mount with [100,100000] capacity");
        MountSpecification mountSpec = (MountSpecification) loadSpec("mount:70000{ s_target:/test1 }");
        
        IdentityHashMap<CapacityEnabledSpecification, BigDecimal> updatableCapacities = new IdentityHashMap<>();
        pullCapacities(mountSpec, updatableCapacities);
        
        MountPartition mountPartition = Binder.partitionMount(updatableCapacities, mountReq, mountSpec);
        
        assertMountPartition(mountPartition, "/test1", 70000);
    }
    
    @Test
    public void mustNotBindToBecauseCapacityBelowMinimum() throws Exception {
        MountRequirement mountReq = (MountRequirement) parser.parseReq(EMPTY_MAP, "1 mount with 100000 capacity");
        MountSpecification mountSpec = (MountSpecification) loadSpec("mount:100000{ s_target:/test1 }");
        
        IdentityHashMap<CapacityEnabledSpecification, BigDecimal> updatableCapacities = new IdentityHashMap<>();
        pullCapacities(mountSpec, updatableCapacities);
        updatableCapacities.replace(mountSpec, BigDecimal.valueOf(99999));
        
        MountPartition mountPartition = Binder.partitionMount(updatableCapacities, mountReq, mountSpec);
        
        assertNull(mountPartition);
    }
    
}
