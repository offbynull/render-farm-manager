package com.offbynull.rfm.host.model.specification;

import static com.offbynull.rfm.host.testutils.TestUtils.loadSpecResource;
import java.math.BigDecimal;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class SocketSpecificationTest {

    @Test
    public void mustCreateSpec() throws Exception {
        SocketSpecification spec = (SocketSpecification) loadSpecResource("../model/worker/socket_ok");

        assertEquals(BigDecimal.valueOf(2L).stripTrailingZeros(), ((BigDecimal) spec.getProperties().get("n_socket_id")).stripTrailingZeros());
        assertEquals(BigDecimal.valueOf(4200).stripTrailingZeros(), ((BigDecimal) spec.getProperties().get("n_speed")).stripTrailingZeros());
        assertEquals(2, spec.getCoreSpecifications().size());
        assertEquals("intel", spec.getProperties().get("s_vendor"));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void mustFailToCreateSpecIfDuplicateCoreIds() throws Exception {
        loadSpecResource("../model/worker/socket_dupe_core");
    }
    
}
