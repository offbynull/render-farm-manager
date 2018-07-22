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
    public void mustBindToAllCpusInCore() {
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
        
        List<CorePartition> corePartitions = Binder.partitionAcrossCores(updatableCapacities, coreReq, List.of(coreSpec));
        
        assertEquals(1, corePartitions.size());
        assertEquals(4, corePartitions.get(0).getCpuPartitions().size());
        
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
                50000,
                corePartitions.get(0).getCpuPartitions().get(2).getCapacity().intValueExact());
        assertEquals(
                Map.of("n_cpu_id", ONE),
                corePartitions.get(0).getCpuPartitions().get(2).getSpecificationId());
        
        assertEquals(
                50000,
                corePartitions.get(0).getCpuPartitions().get(3).getCapacity().intValueExact());
        assertEquals(
                Map.of("n_cpu_id", ONE),
                corePartitions.get(0).getCpuPartitions().get(3).getSpecificationId());
    }
}
