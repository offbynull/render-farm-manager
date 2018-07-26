package com.offbynull.rfm.host.services.h2db;

import com.offbynull.rfm.host.model.partition.GpuPartition;
import com.offbynull.rfm.host.model.requirement.GpuRequirement;
import com.offbynull.rfm.host.model.specification.CapacityEnabledSpecification;
import com.offbynull.rfm.host.model.specification.GpuSpecification;
import com.offbynull.rfm.host.parser.Parser;
import static com.offbynull.rfm.host.services.h2db.BinderTestUtils.assertGpuPartition;
import static com.offbynull.rfm.host.services.h2db.BinderTestUtils.createCapacityMap;
import static com.offbynull.rfm.host.testutils.TestUtils.loadSpec;
import java.math.BigDecimal;
import static java.math.BigDecimal.ZERO;
import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.EMPTY_MAP;
import java.util.IdentityHashMap;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class BinderGpuTest {
    
    private final Parser parser = new Parser(EMPTY_LIST, EMPTY_LIST);
    
    @Test
    public void mustBindToGpu() throws Exception {
        GpuRequirement gpuReq = (GpuRequirement) parser.parseReq(EMPTY_MAP, "1 gpu with 1 capacity");
        GpuSpecification gpuSpec = (GpuSpecification) loadSpec("gpu:1{ s_gpu_id:pci_0000 }");
        
        IdentityHashMap<CapacityEnabledSpecification, BigDecimal> updatableCapacities = createCapacityMap(gpuSpec);
        
        GpuPartition gpuPartition = Binder.partitionGpu(updatableCapacities, gpuReq, gpuSpec);
        
        assertGpuPartition(gpuPartition, "pci_0000", 1);
    }
    
    @Test
    public void mustNotBindToBecauseGpuIsBeingUsed() throws Exception {
        GpuRequirement gpuReq = (GpuRequirement) parser.parseReq(EMPTY_MAP, "1 gpu with 1 capacity");
        GpuSpecification gpuSpec = (GpuSpecification) loadSpec("gpu:1{ s_gpu_id:pci_0000 }");
        
        IdentityHashMap<CapacityEnabledSpecification, BigDecimal> updatableCapacities = createCapacityMap(gpuSpec);
        updatableCapacities.put(gpuSpec, ZERO);
        
        GpuPartition gpuPartition = Binder.partitionGpu(updatableCapacities, gpuReq, gpuSpec);
        
        assertNull(gpuPartition);
    }
    
}
