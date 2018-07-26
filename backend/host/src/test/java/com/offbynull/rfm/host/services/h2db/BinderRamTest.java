package com.offbynull.rfm.host.services.h2db;

import com.offbynull.rfm.host.model.partition.RamPartition;
import com.offbynull.rfm.host.model.requirement.RamRequirement;
import com.offbynull.rfm.host.model.specification.CapacityEnabledSpecification;
import com.offbynull.rfm.host.model.specification.RamSpecification;
import com.offbynull.rfm.host.parser.Parser;
import static com.offbynull.rfm.host.services.h2db.BinderTestUtils.assertRamPartition;
import static com.offbynull.rfm.host.testutils.TestUtils.loadSpec;
import static com.offbynull.rfm.host.testutils.TestUtils.pullCapacities;
import java.math.BigDecimal;
import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.EMPTY_MAP;
import java.util.IdentityHashMap;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class BinderRamTest {
    
    private final Parser parser = new Parser(EMPTY_LIST, EMPTY_LIST);
    
    @Test
    public void mustBindToRamWithMaxCapacity() throws Exception {
        RamRequirement ramReq = (RamRequirement) parser.parseReq(EMPTY_MAP, "1 ram with [100,100000] capacity");
        RamSpecification ramSpec = (RamSpecification) loadSpec("ram:100000{ n_ram_id:0 }");
        
        IdentityHashMap<CapacityEnabledSpecification, BigDecimal> updatableCapacities = new IdentityHashMap<>();
        pullCapacities(ramSpec, updatableCapacities);
        updatableCapacities.put(ramSpec, BigDecimal.valueOf(90000));
        
        RamPartition ramPartition = Binder.partitionRam(updatableCapacities, ramReq, ramSpec);
        
        assertRamPartition(ramPartition, 0, 90000);
    }
    
    @Test
    public void mustBindToRamWithMaxRequirement() throws Exception {
        RamRequirement ramReq = (RamRequirement) parser.parseReq(EMPTY_MAP, "1 ram with [100,100000] capacity");
        RamSpecification ramSpec = (RamSpecification) loadSpec("ram:70000{ n_ram_id:0 }");
        
        IdentityHashMap<CapacityEnabledSpecification, BigDecimal> updatableCapacities = new IdentityHashMap<>();
        pullCapacities(ramSpec, updatableCapacities);
        
        RamPartition ramPartition = Binder.partitionRam(updatableCapacities, ramReq, ramSpec);
        
        assertRamPartition(ramPartition, 0, 70000);
    }
    
    @Test
    public void mustNotBindToBecauseCapacityBelowMinimum() throws Exception {
        RamRequirement ramReq = (RamRequirement) parser.parseReq(EMPTY_MAP, "1 ram with 100000 capacity");
        RamSpecification ramSpec = (RamSpecification) loadSpec("ram:100000{ n_ram_id:0 }");
        
        IdentityHashMap<CapacityEnabledSpecification, BigDecimal> updatableCapacities = new IdentityHashMap<>();
        pullCapacities(ramSpec, updatableCapacities);
        updatableCapacities.put(ramSpec, BigDecimal.valueOf(99999));
        
        RamPartition ramPartition = Binder.partitionRam(updatableCapacities, ramReq, ramSpec);
        
        assertNull(ramPartition);
    }
}
