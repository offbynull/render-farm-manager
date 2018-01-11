package com.offbynull.rfm.host.model.specification;

import static com.offbynull.rfm.host.testutils.TestUtils.loadSpecResource;
import java.math.BigDecimal;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class CoreSpecificationTest {

    @Test
    public void mustCreateSpec() throws Exception {
        CoreSpecification spec = (CoreSpecification) loadSpecResource("../model/worker/core_ok");

        assertEquals(BigDecimal.valueOf(0L).stripTrailingZeros(), ((BigDecimal) spec.getProperties().get("n_core_id")).stripTrailingZeros());
        assertEquals(BigDecimal.valueOf(4200).stripTrailingZeros(), ((BigDecimal) spec.getProperties().get("n_speed")).stripTrailingZeros());
        assertEquals(2, spec.getCpuSpecifications().size());
        assertEquals("intel", spec.getProperties().get("s_vendor"));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void mustFailToCreateSpecIfDuplicateCpuIds() throws Exception {
        loadSpecResource("../model/worker/core_dupe_cpu");
    }
    
}
