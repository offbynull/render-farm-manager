package com.offbynull.rfm.host.services.h2db;

import com.offbynull.rfm.host.model.partition.CpuPartition;
import com.offbynull.rfm.host.model.requirement.CpuRequirement;
import com.offbynull.rfm.host.model.specification.CapacityEnabledSpecification;
import com.offbynull.rfm.host.model.specification.CpuSpecification;
import com.offbynull.rfm.host.parser.Parser;
import static com.offbynull.rfm.host.services.h2db.BinderTestUtils.assertCpuPartition;
import static com.offbynull.rfm.host.services.h2db.BinderTestUtils.createCapacityMap;
import static com.offbynull.rfm.host.testutils.TestUtils.loadSpec;
import java.math.BigDecimal;
import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.EMPTY_MAP;
import java.util.IdentityHashMap;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class BinderCpuTest {
    
    private final Parser parser = new Parser(EMPTY_LIST, EMPTY_LIST);
    
    @Test
    public void mustBindToCpuWithMaxCapacity() throws Exception {
        CpuRequirement cpuReq = (CpuRequirement) parser.parseReq(EMPTY_MAP, "1 cpu with [100,100000] capacity");
        CpuSpecification cpuSpec = (CpuSpecification) loadSpec("cpu:100000{ n_cpu_id:0 }");
        
        IdentityHashMap<CapacityEnabledSpecification, BigDecimal> updatableCapacities = createCapacityMap(cpuSpec);
        updatableCapacities.put(cpuSpec, BigDecimal.valueOf(90000));
        
        CpuPartition cpuPartition = Binder.partitionCpu(updatableCapacities, cpuReq, cpuSpec);
        
        assertCpuPartition(cpuPartition, 0, 90000);
    }
    
    @Test
    public void mustBindToCpuWithMaxRequirement() throws Exception {
        CpuRequirement cpuReq = (CpuRequirement) parser.parseReq(EMPTY_MAP, "1 cpu with [100,100000] capacity");
        CpuSpecification cpuSpec = (CpuSpecification) loadSpec("cpu:70000{ n_cpu_id:0 }");
        
        IdentityHashMap<CapacityEnabledSpecification, BigDecimal> updatableCapacities = createCapacityMap(cpuSpec);
        
        CpuPartition cpuPartition = Binder.partitionCpu(updatableCapacities, cpuReq, cpuSpec);
        
        assertCpuPartition(cpuPartition, 0, 70000);
    }
    
    @Test
    public void mustNotBindToBecauseCapacityBelowMinimum() throws Exception {
        CpuRequirement cpuReq = (CpuRequirement) parser.parseReq(EMPTY_MAP, "1 cpu with 100000 capacity");
        CpuSpecification cpuSpec = (CpuSpecification) loadSpec("cpu:100000{ n_cpu_id:0 }");
        
        IdentityHashMap<CapacityEnabledSpecification, BigDecimal> updatableCapacities = createCapacityMap(cpuSpec);
        updatableCapacities.put(cpuSpec, BigDecimal.valueOf(99999));
        
        CpuPartition cpuPartition = Binder.partitionCpu(updatableCapacities, cpuReq, cpuSpec);
        
        assertNull(cpuPartition);
    }
    
}
