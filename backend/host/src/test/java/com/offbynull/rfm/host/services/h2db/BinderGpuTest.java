package com.offbynull.rfm.host.services.h2db;

import com.offbynull.rfm.host.model.expression.BooleanLiteralExpression;
import com.offbynull.rfm.host.model.partition.GpuPartition;
import com.offbynull.rfm.host.model.requirement.GpuRequirement;
import com.offbynull.rfm.host.model.requirement.NumberRange;
import com.offbynull.rfm.host.model.specification.CapacityEnabledSpecification;
import com.offbynull.rfm.host.model.specification.GpuSpecification;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class BinderGpuTest {
    
    @Test
    public void mustBindToGpu() {
        GpuRequirement gpuReq = new GpuRequirement(
                new NumberRange(1, 1),
                new BooleanLiteralExpression(true),
                new NumberRange(1, 1)
        );
        GpuSpecification gpuSpec = new GpuSpecification(
                BigDecimal.valueOf(1),
                Map.of("s_gpu_id", "pci_0000")
        );
        Map<CapacityEnabledSpecification, BigDecimal> updatableCapacities = new HashMap<>();
        updatableCapacities.put(gpuSpec, BigDecimal.valueOf(1));
        
        GpuPartition gpuPartition = Binder.partitionGpu(updatableCapacities, gpuReq, gpuSpec);
        
        assertEquals(
                1,
                gpuPartition.getCapacity().intValueExact());
        assertEquals(
                Map.of("s_gpu_id", "pci_0000"),
                gpuPartition.getSpecificationId());
    }
    
    @Test
    public void mustNotBindToBecauseGpuIsBeingUsed() {
        GpuRequirement gpuReq = new GpuRequirement(
                new NumberRange(1, 1),
                new BooleanLiteralExpression(true),
                new NumberRange(1, 1)
        );
        GpuSpecification gpuSpec = new GpuSpecification(
                BigDecimal.valueOf(1),
                Map.of("s_gpu_id", "pci_0000")
        );
        Map<CapacityEnabledSpecification, BigDecimal> updatableCapacities = new HashMap<>();
        updatableCapacities.put(gpuSpec, BigDecimal.valueOf(0));
        
        GpuPartition gpuPartition = Binder.partitionGpu(updatableCapacities, gpuReq, gpuSpec);
        
        assertNull(gpuPartition);
    }
    
}
