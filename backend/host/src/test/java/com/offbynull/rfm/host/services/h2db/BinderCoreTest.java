package com.offbynull.rfm.host.services.h2db;

import com.offbynull.rfm.host.model.expression.BooleanLiteralExpression;
import com.offbynull.rfm.host.model.partition.CorePartition;
import com.offbynull.rfm.host.model.requirement.CoreRequirement;
import com.offbynull.rfm.host.model.requirement.CpuRequirement;
import com.offbynull.rfm.host.model.requirement.NumberRange;
import com.offbynull.rfm.host.model.specification.CapacityEnabledSpecification;
import com.offbynull.rfm.host.model.specification.CoreSpecification;
import com.offbynull.rfm.host.model.specification.CpuSpecification;
import java.math.BigDecimal;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class BinderCoreTest {
    
    @Test
    public void mustBindToIndividualCoreWithSingleCpuRequirement() {
        CoreRequirement coreReq = new CoreRequirement(
                new NumberRange(1, 1),
                new BooleanLiteralExpression(true),
                new CpuRequirement[] {
                    new CpuRequirement(
                        new NumberRange(1, 99999999),
                        new BooleanLiteralExpression(true),
                        new NumberRange(50000, 50000)
                    )
                }
        );
        CpuSpecification cpuSpec1 = new CpuSpecification(
                BigDecimal.valueOf(100000),
                Map.of("n_cpu_id", ZERO)
        );
        CpuSpecification cpuSpec2 = new CpuSpecification(
                BigDecimal.valueOf(100000),
                Map.of("n_cpu_id", ONE)
        );
        CoreSpecification coreSpec = new CoreSpecification(
                new CpuSpecification[] { cpuSpec1, cpuSpec2 },
                Map.of("n_core_id", ZERO)
        );
        Map<CapacityEnabledSpecification, BigDecimal> updatableCapacities = new HashMap<>();
        updatableCapacities.put(cpuSpec1, cpuSpec1.getCapacity());
        updatableCapacities.put(cpuSpec2, cpuSpec2.getCapacity());
        
        CorePartition corePartition = Binder.partitionIndividualCore(updatableCapacities, coreReq, coreSpec);
        
        assertEquals(4, corePartition.getCpuPartitions().size());
        
        assertEquals(
                50000,
                corePartition.getCpuPartitions().get(0).getCapacity().intValueExact());
        assertEquals(
                Map.of("n_cpu_id", ZERO),
                corePartition.getCpuPartitions().get(0).getSpecificationId());
        
        assertEquals(
                50000,
                corePartition.getCpuPartitions().get(1).getCapacity().intValueExact());
        assertEquals(
                Map.of("n_cpu_id", ONE),
                corePartition.getCpuPartitions().get(1).getSpecificationId());
        
        assertEquals(
                50000,
                corePartition.getCpuPartitions().get(2).getCapacity().intValueExact());
        assertEquals(
                Map.of("n_cpu_id", ZERO),
                corePartition.getCpuPartitions().get(2).getSpecificationId());
        
        assertEquals(
                50000,
                corePartition.getCpuPartitions().get(3).getCapacity().intValueExact());
        assertEquals(
                Map.of("n_cpu_id", ONE),
                corePartition.getCpuPartitions().get(3).getSpecificationId());
    }
    
    @Test
    public void mustBindToIndividualCoreWithMultipleCpuRequirements() {
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
        CoreRequirement coreReq = new CoreRequirement(
                new NumberRange(1, 1),
                new BooleanLiteralExpression(true),
                new CpuRequirement[] {
                    new CpuRequirement(
                        new NumberRange(1, 99999999),
                        new BooleanLiteralExpression(true),
                        new NumberRange(75000, 75000)
                    ),
                    new CpuRequirement(
                        new NumberRange(1, 99999999),
                        new BooleanLiteralExpression(true),
                        new NumberRange(20000, 20000)
                    )
                }
        );
        CpuSpecification cpuSpec1 = new CpuSpecification(
                BigDecimal.valueOf(100000),
                Map.of("n_cpu_id", ZERO)
        );
        CpuSpecification cpuSpec2 = new CpuSpecification(
                BigDecimal.valueOf(100000),
                Map.of("n_cpu_id", ONE)
        );
        CoreSpecification coreSpec = new CoreSpecification(
                new CpuSpecification[] { cpuSpec1, cpuSpec2 },
                Map.of("n_core_id", ZERO)
        );
        Map<CapacityEnabledSpecification, BigDecimal> updatableCapacities = new HashMap<>();
        updatableCapacities.put(cpuSpec1, cpuSpec1.getCapacity());
        updatableCapacities.put(cpuSpec2, cpuSpec2.getCapacity());
        
        CorePartition corePartition = Binder.partitionIndividualCore(updatableCapacities, coreReq, coreSpec);
        
        assertEquals(4, corePartition.getCpuPartitions().size());
        
        assertEquals(
                75000,
                corePartition.getCpuPartitions().get(0).getCapacity().intValueExact());
        assertEquals(
                Map.of("n_cpu_id", ZERO),
                corePartition.getCpuPartitions().get(0).getSpecificationId());
        
        assertEquals(
                75000,
                corePartition.getCpuPartitions().get(1).getCapacity().intValueExact());
        assertEquals(
                Map.of("n_cpu_id", ONE),
                corePartition.getCpuPartitions().get(1).getSpecificationId());
        
        assertEquals(
                20000,
                corePartition.getCpuPartitions().get(2).getCapacity().intValueExact());
        assertEquals(
                Map.of("n_cpu_id", ZERO),
                corePartition.getCpuPartitions().get(2).getSpecificationId());
        
        assertEquals(
                20000,
                corePartition.getCpuPartitions().get(3).getCapacity().intValueExact());
        assertEquals(
                Map.of("n_cpu_id", ONE),
                corePartition.getCpuPartitions().get(3).getSpecificationId());
    }
    
    @Test
    public void mustBindAcrossAllCoresWithSingleCpuRequirement() {
        CoreRequirement coreReq = new CoreRequirement(
                null,
                new BooleanLiteralExpression(true),
                new CpuRequirement[] {
                    new CpuRequirement(
                        new NumberRange(1, 99999999),
                        new BooleanLiteralExpression(true),
                        new NumberRange(50000, 50000)
                    )
                }
        );
        CpuSpecification cpuSpec1 = new CpuSpecification(
                BigDecimal.valueOf(100000),
                Map.of("n_cpu_id", ZERO)
        );
        CoreSpecification coreSpec1 = new CoreSpecification(
                new CpuSpecification[] { cpuSpec1 },
                Map.of("n_core_id", ZERO)
        );
        CpuSpecification cpuSpec2 = new CpuSpecification(
                BigDecimal.valueOf(100000),
                Map.of("n_cpu_id", ONE)
        );
        CoreSpecification coreSpec2 = new CoreSpecification(
                new CpuSpecification[] { cpuSpec2 },
                Map.of("n_core_id", ONE)
        );
        Map<CapacityEnabledSpecification, BigDecimal> updatableCapacities = new HashMap<>();
        updatableCapacities.put(cpuSpec1, cpuSpec1.getCapacity());
        updatableCapacities.put(cpuSpec2, cpuSpec2.getCapacity());
        
        List<CorePartition> corePartitions = Binder.partitionAcrossAllCores(updatableCapacities, coreReq, List.of(coreSpec1, coreSpec2));
        
        assertEquals(2, corePartitions.size());
        assertEquals(2, corePartitions.get(0).getCpuPartitions().size());
        assertEquals(2, corePartitions.get(1).getCpuPartitions().size());

        
        
        assertEquals(
                Map.of("n_core_id", ZERO),
                corePartitions.get(0).getSpecificationId());
        
        assertEquals(
                50000,
                corePartitions.get(0).getCpuPartitions().get(0).getCapacity().intValueExact());
        assertEquals(
                Map.of("n_cpu_id", ZERO),
                corePartitions.get(0).getCpuPartitions().get(0).getSpecificationId());
        
        assertEquals(
                50000,
                corePartitions.get(0).getCpuPartitions().get(1).getCapacity().intValueExact());
        assertEquals(
                Map.of("n_cpu_id", ZERO),
                corePartitions.get(0).getCpuPartitions().get(1).getSpecificationId());
        
        
        
        assertEquals(
                Map.of("n_core_id", ONE),
                corePartitions.get(1).getSpecificationId());
        
        assertEquals(
                50000,
                corePartitions.get(1).getCpuPartitions().get(0).getCapacity().intValueExact());
        assertEquals(
                Map.of("n_cpu_id", ONE),
                corePartitions.get(1).getCpuPartitions().get(0).getSpecificationId());
        
        assertEquals(
                50000,
                corePartitions.get(1).getCpuPartitions().get(1).getCapacity().intValueExact());
        assertEquals(
                Map.of("n_cpu_id", ONE),
                corePartitions.get(1).getCpuPartitions().get(1).getSpecificationId());
    }
    
    @Test
    public void mustBindAcrossAllCoresWithMultipleCpuRequirements() {
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
        CoreRequirement coreReq = new CoreRequirement(
                null,
                new BooleanLiteralExpression(true),
                new CpuRequirement[] {
                    new CpuRequirement(
                        new NumberRange(1, 99999999),
                        new BooleanLiteralExpression(true),
                        new NumberRange(75000, 75000)
                    ),
                    new CpuRequirement(
                        new NumberRange(1, 99999999),
                        new BooleanLiteralExpression(true),
                        new NumberRange(20000, 20000)
                    )
                }
        );
        CpuSpecification cpuSpec1 = new CpuSpecification(
                BigDecimal.valueOf(100000),
                Map.of("n_cpu_id", ZERO)
        );
        CoreSpecification coreSpec1 = new CoreSpecification(
                new CpuSpecification[] { cpuSpec1 },
                Map.of("n_core_id", ZERO)
        );
        CpuSpecification cpuSpec2 = new CpuSpecification(
                BigDecimal.valueOf(100000),
                Map.of("n_cpu_id", ONE)
        );
        CoreSpecification coreSpec2 = new CoreSpecification(
                new CpuSpecification[] { cpuSpec2 },
                Map.of("n_core_id", ONE)
        );
        Map<CapacityEnabledSpecification, BigDecimal> updatableCapacities = new HashMap<>();
        updatableCapacities.put(cpuSpec1, cpuSpec1.getCapacity());
        updatableCapacities.put(cpuSpec2, cpuSpec2.getCapacity());
        
        List<CorePartition> corePartitions = Binder.partitionAcrossAllCores(updatableCapacities, coreReq, List.of(coreSpec1, coreSpec2));
        
        assertEquals(2, corePartitions.size());
        assertEquals(2, corePartitions.get(0).getCpuPartitions().size());
        assertEquals(2, corePartitions.get(1).getCpuPartitions().size());
        
        
        
        assertEquals(
                Map.of("n_core_id", ZERO),
                corePartitions.get(0).getSpecificationId());
        
        assertEquals(
                75000,
                corePartitions.get(0).getCpuPartitions().get(0).getCapacity().intValueExact());
        assertEquals(
                Map.of("n_cpu_id", ZERO),
                corePartitions.get(0).getCpuPartitions().get(0).getSpecificationId());
        
        assertEquals(
                20000,
                corePartitions.get(0).getCpuPartitions().get(1).getCapacity().intValueExact());
        assertEquals(
                Map.of("n_cpu_id", ZERO),
                corePartitions.get(0).getCpuPartitions().get(1).getSpecificationId());
        
        

        assertEquals(
                Map.of("n_core_id", ONE),
                corePartitions.get(1).getSpecificationId());
        
        assertEquals(
                75000,
                corePartitions.get(1).getCpuPartitions().get(0).getCapacity().intValueExact());
        assertEquals(
                Map.of("n_cpu_id", ONE),
                corePartitions.get(1).getCpuPartitions().get(0).getSpecificationId());
        
        assertEquals(
                20000,
                corePartitions.get(1).getCpuPartitions().get(1).getCapacity().intValueExact());
        assertEquals(
                Map.of("n_cpu_id", ONE),
                corePartitions.get(1).getCpuPartitions().get(1).getSpecificationId());
    }
}
