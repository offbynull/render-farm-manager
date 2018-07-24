package com.offbynull.rfm.host.services.h2db;

import com.offbynull.rfm.host.model.expression.BooleanLiteralExpression;
import com.offbynull.rfm.host.model.partition.CpuPartition;
import com.offbynull.rfm.host.model.requirement.CpuRequirement;
import com.offbynull.rfm.host.model.requirement.NumberRange;
import com.offbynull.rfm.host.model.specification.CapacityEnabledSpecification;
import com.offbynull.rfm.host.model.specification.CpuSpecification;
import java.math.BigDecimal;
import static java.math.BigDecimal.ZERO;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class BinderCpuTest {
    
    @Test
    public void mustBindToCpuWithMaxCapacity() {
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
        updatableCapacities.put(cpuSpec, BigDecimal.valueOf(90000));
        
        CpuPartition cpuPartition = Binder.partitionCpu(updatableCapacities, cpuReq, cpuSpec);
        
        assertEquals(
                90000,
                cpuPartition.getCapacity().intValueExact());
        assertEquals(
                Map.of("n_cpu_id", ZERO),
                cpuPartition.getSpecificationId());
    }
    
    @Test
    public void mustBindToCpuWithMaxRequirement() {
        CpuRequirement cpuReq = new CpuRequirement(
                new NumberRange(1, 1),
                new BooleanLiteralExpression(true),
                new NumberRange(100, 100000)
        );
        CpuSpecification cpuSpec = new CpuSpecification(
                BigDecimal.valueOf(70000),
                Map.of("n_cpu_id", ZERO)
        );
        Map<CapacityEnabledSpecification, BigDecimal> updatableCapacities = new HashMap<>();
        updatableCapacities.put(cpuSpec, cpuSpec.getCapacity());
        
        CpuPartition cpuPartition = Binder.partitionCpu(updatableCapacities, cpuReq, cpuSpec);
        
        assertEquals(
                70000,
                cpuPartition.getCapacity().intValueExact());
        assertEquals(
                Map.of("n_cpu_id", ZERO),
                cpuPartition.getSpecificationId());
    }
    
    @Test
    public void mustNotBindToBecauseCapacityBelowMinimum() {
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
        
        CpuPartition cpuPartition = Binder.partitionCpu(updatableCapacities, cpuReq, cpuSpec);
        
        assertNull(cpuPartition);
    }
    
}
