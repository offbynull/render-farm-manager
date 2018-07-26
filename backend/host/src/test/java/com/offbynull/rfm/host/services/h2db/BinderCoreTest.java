package com.offbynull.rfm.host.services.h2db;

import com.offbynull.rfm.host.model.partition.CorePartition;
import com.offbynull.rfm.host.model.requirement.CoreRequirement;
import com.offbynull.rfm.host.model.specification.CapacityEnabledSpecification;
import com.offbynull.rfm.host.model.specification.CoreSpecification;
import com.offbynull.rfm.host.parser.Parser;
import static com.offbynull.rfm.host.services.h2db.BinderTestUtils.assertCorePartition;
import static com.offbynull.rfm.host.services.h2db.BinderTestUtils.assertCpuPartition;
import static com.offbynull.rfm.host.testutils.TestUtils.loadSpec;
import static com.offbynull.rfm.host.testutils.TestUtils.pullCapacities;
import java.math.BigDecimal;
import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.EMPTY_MAP;
import java.util.IdentityHashMap;
import java.util.List;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class BinderCoreTest {
    
    private final Parser parser = new Parser(EMPTY_LIST, EMPTY_LIST);
    
    @Test
    public void mustBindToIndividualCoreWithSingleCpuRequirement() throws Exception {
        CoreRequirement coreReq = (CoreRequirement) parser.parseReq(
                EMPTY_MAP,
                ""
                + "1 core {\n"
                + "    [1,99999999] cpu with 50000 capacity\n"
                + "}"
        );
        CoreSpecification coreSpec = (CoreSpecification) loadSpec(
                ""
                + "core{\n"
                + "   cpu:100000{ n_cpu_id:0 }\n"
                + "   cpu:100000{ n_cpu_id:1 }\n"
                + "   n_core_id:0\n"
                + "}"
        );
        IdentityHashMap<CapacityEnabledSpecification, BigDecimal> updatableCapacities = new IdentityHashMap<>();
        pullCapacities(coreSpec, updatableCapacities);
        
        CorePartition corePartition = Binder.partitionIndividualCore(updatableCapacities, coreReq, coreSpec);
        
        assertCorePartition(corePartition, 0,
                cpuPartition -> assertCpuPartition(cpuPartition, 0, 50000),
                cpuPartition -> assertCpuPartition(cpuPartition, 1, 50000),
                cpuPartition -> assertCpuPartition(cpuPartition, 0, 50000),
                cpuPartition -> assertCpuPartition(cpuPartition, 1, 50000)
        );
    }
    
    @Test
    public void mustBindToIndividualCoreWithMultipleCpuRequirements() throws Exception {
        // note that this core requirement has 2 cpu requirements... you want as many 75000 slices of as many cpus as possible, followed by
        // 20000 slices of as many cpus as possible
        //
        // we have 1 core with 2 cpus,
        //   the 1st requirement should grab...
        //      75000 from cpu1
        //      75000 from cpu2
        //   the 2nd requirement should grab...
        //      20000 from cpu1
        //      20000 from cpu2
        CoreRequirement coreReq = (CoreRequirement) parser.parseReq(
                EMPTY_MAP,
                ""
                + "1 core {\n"
                + "    [1,99999999] cpu with 75000 capacity\n"
                + "    [1,99999999] cpu with 20000 capacity\n"
                + "}"
        );
        CoreSpecification coreSpec = (CoreSpecification) loadSpec(
                ""
                + "core{\n"
                + "   cpu:100000{ n_cpu_id:0 }\n"
                + "   cpu:100000{ n_cpu_id:1 }\n"
                + "   n_core_id:0\n"
                + "}"
        );
        
        IdentityHashMap<CapacityEnabledSpecification, BigDecimal> updatableCapacities = new IdentityHashMap<>();
        pullCapacities(coreSpec, updatableCapacities);
        
        CorePartition corePartition = Binder.partitionIndividualCore(updatableCapacities, coreReq, coreSpec);
        
        assertCorePartition(corePartition, 0,
                cpuPartition -> assertCpuPartition(cpuPartition, 0, 75000),
                cpuPartition -> assertCpuPartition(cpuPartition, 1, 75000),
                cpuPartition -> assertCpuPartition(cpuPartition, 0, 20000),
                cpuPartition -> assertCpuPartition(cpuPartition, 1, 20000)
        );
    }
    
    @Test
    public void mustBindAcrossAllCoresWithSingleCpuRequirement() throws Exception {
        CoreRequirement coreReq = (CoreRequirement) parser.parseReq(
                EMPTY_MAP,
                ""
                + "? core {\n"
                + "    [1,99999999] cpu with 50000 capacity\n"
                + "}"
        );
        CoreSpecification coreSpec1 = (CoreSpecification) loadSpec(
                ""
                + "core{\n"
                + "   cpu:100000{ n_cpu_id:0 }\n"
                + "   n_core_id:0\n"
                + "}\n"
        );
        CoreSpecification coreSpec2 = (CoreSpecification) loadSpec(
                ""
                + "core{\n"
                + "   cpu:100000{ n_cpu_id:1 }\n"
                + "   n_core_id:1\n"
                + "}\n"
        );
        
        IdentityHashMap<CapacityEnabledSpecification, BigDecimal> updatableCapacities = new IdentityHashMap<>();
        pullCapacities(coreSpec1, updatableCapacities);
        pullCapacities(coreSpec2, updatableCapacities);
        
        List<CorePartition> corePartitions = Binder.partitionAcrossAllCores(updatableCapacities, coreReq, List.of(coreSpec1, coreSpec2));

        CorePartition corePartition_0 = corePartitions.get(0);
        assertCorePartition(corePartition_0, 0,
                cpuPartition -> assertCpuPartition(cpuPartition, 0, 50000),
                cpuPartition -> assertCpuPartition(cpuPartition, 0, 50000)
        );
        
        CorePartition corePartition_1 = corePartitions.get(1);
        assertCorePartition(corePartition_1, 1,
                cpuPartition -> assertCpuPartition(cpuPartition, 1, 50000),
                cpuPartition -> assertCpuPartition(cpuPartition, 1, 50000)
        );
    }
    
    @Test
    public void mustBindAcrossAllCoresWithMultipleCpuRequirements() throws Exception {
        // note that this core requirement has 2 cpu requirements... you want as many 75000 slices of as many cpus as possible, followed by
        // 20000 slices of as many cpus as possible
        //
        // we have 2 core each with 1 cpu,
        //   the 1st requirement should grab...
        //      75000 from cpu1 of core1
        //      75000 from cpu1 of core2
        //   the 2nd requirement should grab...
        //      20000 from cpu1 of core1
        //      20000 from cpu1 of core2
        CoreRequirement coreReq = (CoreRequirement) parser.parseReq(
                EMPTY_MAP,
                ""
                + "? core {\n"
                + "    [1,99999999] cpu with 75000 capacity\n"
                + "    [1,99999999] cpu with 20000 capacity\n"
                + "}"
        );
        CoreSpecification coreSpec1 = (CoreSpecification) loadSpec(
                ""
                + "core{\n"
                + "   cpu:100000{ n_cpu_id:0 }\n"
                + "   n_core_id:0\n"
                + "}\n"
        );
        CoreSpecification coreSpec2 = (CoreSpecification) loadSpec(
                ""
                + "core{\n"
                + "   cpu:100000{ n_cpu_id:1 }\n"
                + "   n_core_id:1\n"
                + "}\n"
        );
        
        IdentityHashMap<CapacityEnabledSpecification, BigDecimal> updatableCapacities = new IdentityHashMap<>();
        pullCapacities(coreSpec1, updatableCapacities);
        pullCapacities(coreSpec2, updatableCapacities);
        
        List<CorePartition> corePartitions = Binder.partitionAcrossAllCores(updatableCapacities, coreReq, List.of(coreSpec1, coreSpec2));
        
        assertEquals(2, corePartitions.size());
        assertEquals(2, corePartitions.get(0).getCpuPartitions().size());
        assertEquals(2, corePartitions.get(1).getCpuPartitions().size());
        
        
        
        CorePartition corePartition_0 = corePartitions.get(0);
        assertCorePartition(corePartition_0, 0,
                cpuPartition -> assertCpuPartition(cpuPartition, 0, 75000),
                cpuPartition -> assertCpuPartition(cpuPartition, 0, 20000)
        );
        
        CorePartition corePartition_1 = corePartitions.get(1);
        assertCorePartition(corePartition_1, 1,
                cpuPartition -> assertCpuPartition(cpuPartition, 1, 75000),
                cpuPartition -> assertCpuPartition(cpuPartition, 1, 20000)
        );
    }
}
