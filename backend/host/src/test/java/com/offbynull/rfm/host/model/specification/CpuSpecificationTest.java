package com.offbynull.rfm.host.model.specification;

import static com.offbynull.rfm.host.testutils.TestUtils.loadSpecResource;
import java.math.BigDecimal;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class CpuSpecificationTest {

    @Test
    public void mustCreateSpec() throws Exception {
        CpuSpecification spec = (CpuSpecification) loadSpecResource("../model/worker/cpu_ok");

        assertEquals(BigDecimal.valueOf(0L), spec.getProperties().get("n_cpu_id"));
        assertEquals(BigDecimal.valueOf(100000).stripTrailingZeros(), ((BigDecimal) spec.getProperties().get("n_capacity")).stripTrailingZeros());
        assertEquals(BigDecimal.valueOf(4200).stripTrailingZeros(), ((BigDecimal) spec.getProperties().get("n_speed")).stripTrailingZeros());
        assertEquals("intel", spec.getProperties().get("s_vendor"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void mustFailToCreateSpecIfAvailableHigherThanTotal() throws Exception {
        loadSpecResource("../model/worker/cpu_oob");
    }
    
}
