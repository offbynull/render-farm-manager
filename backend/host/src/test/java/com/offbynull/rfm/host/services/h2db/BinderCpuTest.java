package com.offbynull.rfm.host.services.h2db;

import com.offbynull.rfm.host.model.expression.BooleanLiteralExpression;
import com.offbynull.rfm.host.model.partition.CpuPartition;
import com.offbynull.rfm.host.model.requirement.CpuRequirement;
import com.offbynull.rfm.host.model.requirement.NumberRange;
import com.offbynull.rfm.host.model.specification.CapacityEnabledSpecification;
import com.offbynull.rfm.host.model.specification.CpuSpecification;
import java.math.BigDecimal;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class BinderCpuTest {
    
    @Test
    public void mustBindToCpuWithAllCapacity() {
        CpuRequirement cpuReq = new CpuRequirement(
                new NumberRange(1, 1),
                new BooleanLiteralExpression(true),
                new NumberRange(100, 100000)
        );
        CpuSpecification cpuSpec = new CpuSpecification(
                BigDecimal.valueOf(100000),
                Map.of("n_cpu_id", ZERO)
        );
        Map<CapacityEnabledSpecification, BigDecimal> updatableCapacities = new HashMap<>();
        updatableCapacities.put(cpuSpec, cpuSpec.getCapacity());
        
        List<CpuPartition> cpuPartitions = Binder.partitionCpu(updatableCapacities, cpuReq, List.of(cpuSpec));
        
        assertEquals(1, cpuPartitions.size());
        
        assertEquals(
                100000,
                cpuPartitions.get(0).getCapacity().intValueExact());
        assertEquals(
                Map.of("n_cpu_id", ZERO),
                cpuPartitions.get(0).getSpecificationId());
    }
    
    @Test
    public void mustBindToCpuMultipleTimes() {
        CpuRequirement cpuReq = new CpuRequirement(
                new NumberRange(1, 999999),
                new BooleanLiteralExpression(true),
                new NumberRange(1, 33333)
        );
        CpuSpecification cpuSpec1 = new CpuSpecification(
                BigDecimal.valueOf(100000),
                Map.of("n_cpu_id", ZERO)
        );
        CpuSpecification cpuSpec2 = new CpuSpecification(
                BigDecimal.valueOf(50000),
                Map.of("n_cpu_id", ONE)
        );
        Map<CapacityEnabledSpecification, BigDecimal> updatableCapacities = new HashMap<>();
        updatableCapacities.put(cpuSpec1, cpuSpec1.getCapacity());
        updatableCapacities.put(cpuSpec2, cpuSpec2.getCapacity());
        
        List<CpuPartition> cpuPartitions = Binder.partitionCpu(updatableCapacities, cpuReq, List.of(cpuSpec1, cpuSpec2));
        
        assertEquals(6, cpuPartitions.size());
        
        assertEquals(
                33333,
                cpuPartitions.get(0).getCapacity().intValueExact());
        assertEquals(
                Map.of("n_cpu_id", ZERO),
                cpuPartitions.get(0).getSpecificationId());
        
        assertEquals(
                33333,
                cpuPartitions.get(1).getCapacity().intValueExact());
        assertEquals(
                Map.of("n_cpu_id", ZERO),
                cpuPartitions.get(1).getSpecificationId());
        
        assertEquals(
                33333,
                cpuPartitions.get(2).getCapacity().intValueExact());
        assertEquals(
                Map.of("n_cpu_id", ZERO),
                cpuPartitions.get(2).getSpecificationId());
        
        assertEquals(
                1,
                cpuPartitions.get(3).getCapacity().intValueExact());
        assertEquals(
                Map.of("n_cpu_id", ZERO),
                cpuPartitions.get(3).getSpecificationId());
        
        assertEquals(
                33333,
                cpuPartitions.get(4).getCapacity().intValueExact());
        assertEquals(
                Map.of("n_cpu_id", ONE),
                cpuPartitions.get(4).getSpecificationId());
        
        assertEquals(
                16667,
                cpuPartitions.get(5).getCapacity().intValueExact());
        assertEquals(
                Map.of("n_cpu_id", ONE),
                cpuPartitions.get(5).getSpecificationId());
    }
    
    @Test
    public void mustNotBindToCpuBecauseOverCapacity() {
        CpuRequirement cpuReq = new CpuRequirement(
                new NumberRange(1, 1),
                new BooleanLiteralExpression(true),
                new NumberRange(100000, 100000)
        );
        CpuSpecification cpuSpec = new CpuSpecification(
                BigDecimal.valueOf(100000),
                Map.of("n_cpu_id", ZERO)
        );
        Map<CapacityEnabledSpecification, BigDecimal> updatableCapacities = new HashMap<>();
        updatableCapacities.put(cpuSpec, BigDecimal.valueOf(99999));
        
        List<CpuPartition> cpuPartitions = Binder.partitionCpu(updatableCapacities, cpuReq, List.of(cpuSpec));
        
        assertTrue(cpuPartitions.isEmpty());
    }
    
}
