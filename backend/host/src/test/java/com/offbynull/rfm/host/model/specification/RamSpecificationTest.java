package com.offbynull.rfm.host.model.specification;

import static com.offbynull.rfm.host.testutils.TestUtils.loadSpecResource;
import java.math.BigDecimal;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class RamSpecificationTest {

    @Test
    public void mustCreateSpec() throws Exception {
        RamSpecification spec = (RamSpecification) loadSpecResource("../model/worker/ram_ok");

        assertEquals(BigDecimal.valueOf(0L), spec.getProperties().get("n_ram_id"));
        assertEquals(BigDecimal.valueOf(1111111111110L).stripTrailingZeros(), ((BigDecimal) spec.getProperties().get("n_capacity")).stripTrailingZeros());
    }

    @Test(expected = IllegalArgumentException.class)
    public void mustFailToCreateSpecIfAvailableHigherThanTotal() throws Exception {
        loadSpecResource("../model/worker/ram_oob");
    }
    
}
