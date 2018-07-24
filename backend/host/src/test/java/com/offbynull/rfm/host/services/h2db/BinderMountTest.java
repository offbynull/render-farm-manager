package com.offbynull.rfm.host.services.h2db;

import com.offbynull.rfm.host.model.expression.BooleanLiteralExpression;
import com.offbynull.rfm.host.model.partition.MountPartition;
import com.offbynull.rfm.host.model.requirement.MountRequirement;
import com.offbynull.rfm.host.model.requirement.NumberRange;
import com.offbynull.rfm.host.model.specification.CapacityEnabledSpecification;
import com.offbynull.rfm.host.model.specification.MountSpecification;
import java.math.BigDecimal;
import static java.math.BigDecimal.ZERO;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class BinderMountTest {
    
    @Test
    public void mustBindToMountWithMaxCapacity() {
        MountRequirement mountReq = new MountRequirement(
                new NumberRange(1, 1),
                new BooleanLiteralExpression(true),
                new NumberRange(100, 100000)
        );
        MountSpecification mountSpec = new MountSpecification(
                BigDecimal.valueOf(100000),
                Map.of("s_target", "/test1")
        );
        Map<CapacityEnabledSpecification, BigDecimal> updatableCapacities = new HashMap<>();
        updatableCapacities.put(mountSpec, BigDecimal.valueOf(90000));
        
        MountPartition mountPartition = Binder.partitionMount(updatableCapacities, mountReq, mountSpec);
        
        assertEquals(
                90000,
                mountPartition.getCapacity().intValueExact());
        assertEquals(
                Map.of("s_target", "/test1"),
                mountPartition.getSpecificationId());
    }
    
    @Test
    public void mustBindToMountWithMaxRequirement() {
        MountRequirement mountReq = new MountRequirement(
                new NumberRange(1, 1),
                new BooleanLiteralExpression(true),
                new NumberRange(100, 100000)
        );
        MountSpecification mountSpec = new MountSpecification(
                BigDecimal.valueOf(70000),
                Map.of("s_target", "/test1")
        );
        Map<CapacityEnabledSpecification, BigDecimal> updatableCapacities = new HashMap<>();
        updatableCapacities.put(mountSpec, mountSpec.getCapacity());
        
        MountPartition mountPartition = Binder.partitionMount(updatableCapacities, mountReq, mountSpec);
        
        assertEquals(
                70000,
                mountPartition.getCapacity().intValueExact());
        assertEquals(
                Map.of("s_target", "/test1"),
                mountPartition.getSpecificationId());
    }
    
    @Test
    public void mustNotBindToBecauseCapacityBelowMinimum() {
        MountRequirement mountReq = new MountRequirement(
                new NumberRange(1, 1),
                new BooleanLiteralExpression(true),
                new NumberRange(100000, 100000)
        );
        MountSpecification mountSpec = new MountSpecification(
                BigDecimal.valueOf(100000),
                Map.of("s_target", "/test1")
        );
        Map<CapacityEnabledSpecification, BigDecimal> updatableCapacities = new HashMap<>();
        updatableCapacities.put(mountSpec, BigDecimal.valueOf(99999));
        
        MountPartition mountPartition = Binder.partitionMount(updatableCapacities, mountReq, mountSpec);
        
        assertNull(mountPartition);
    }
    
}
