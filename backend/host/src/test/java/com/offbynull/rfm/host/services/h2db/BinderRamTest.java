package com.offbynull.rfm.host.services.h2db;

import com.offbynull.rfm.host.model.expression.BooleanLiteralExpression;
import com.offbynull.rfm.host.model.partition.RamPartition;
import com.offbynull.rfm.host.model.requirement.RamRequirement;
import com.offbynull.rfm.host.model.requirement.NumberRange;
import com.offbynull.rfm.host.model.specification.CapacityEnabledSpecification;
import com.offbynull.rfm.host.model.specification.RamSpecification;
import java.math.BigDecimal;
import static java.math.BigDecimal.ZERO;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class BinderRamTest {
    
    @Test
    public void mustBindToRamWithMaxCapacity() {
        RamRequirement ramReq = new RamRequirement(
                new NumberRange(1, 1),
                new BooleanLiteralExpression(true),
                new NumberRange(100, 100000)
        );
        RamSpecification ramSpec = new RamSpecification(
                BigDecimal.valueOf(100000),
                Map.of("n_ram_id", ZERO)
        );
        Map<CapacityEnabledSpecification, BigDecimal> updatableCapacities = new HashMap<>();
        updatableCapacities.put(ramSpec, BigDecimal.valueOf(90000));
        
        RamPartition ramPartition = Binder.partitionRam(updatableCapacities, ramReq, ramSpec);
        
        assertEquals(
                90000,
                ramPartition.getCapacity().intValueExact());
        assertEquals(
                Map.of("n_ram_id", ZERO),
                ramPartition.getSpecificationId());
    }
    
    @Test
    public void mustBindToRamWithMaxRequirement() {
        RamRequirement ramReq = new RamRequirement(
                new NumberRange(1, 1),
                new BooleanLiteralExpression(true),
                new NumberRange(100, 100000)
        );
        RamSpecification ramSpec = new RamSpecification(
                BigDecimal.valueOf(70000),
                Map.of("n_ram_id", ZERO)
        );
        Map<CapacityEnabledSpecification, BigDecimal> updatableCapacities = new HashMap<>();
        updatableCapacities.put(ramSpec, ramSpec.getCapacity());
        
        RamPartition ramPartition = Binder.partitionRam(updatableCapacities, ramReq, ramSpec);
        
        assertEquals(
                70000,
                ramPartition.getCapacity().intValueExact());
        assertEquals(
                Map.of("n_ram_id", ZERO),
                ramPartition.getSpecificationId());
    }
    
    @Test
    public void mustNotBindToBecauseCapacityBelowMinimum() {
        RamRequirement ramReq = new RamRequirement(
                new NumberRange(1, 1),
                new BooleanLiteralExpression(true),
                new NumberRange(100000, 100000)
        );
        RamSpecification ramSpec = new RamSpecification(
                BigDecimal.valueOf(100000),
                Map.of("n_ram_id", ZERO)
        );
        Map<CapacityEnabledSpecification, BigDecimal> updatableCapacities = new HashMap<>();
        updatableCapacities.put(ramSpec, BigDecimal.valueOf(99999));
        
        RamPartition ramPartition = Binder.partitionRam(updatableCapacities, ramReq, ramSpec);
        
        assertNull(ramPartition);
    }
    
}
