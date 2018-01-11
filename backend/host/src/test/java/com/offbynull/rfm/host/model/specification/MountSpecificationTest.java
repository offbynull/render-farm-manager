package com.offbynull.rfm.host.model.specification;

import static com.offbynull.rfm.host.testutils.TestUtils.loadSpecResource;
import java.math.BigDecimal;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class MountSpecificationTest {

    @Test
    public void mustCreateSpec() throws Exception {
        MountSpecification spec = (MountSpecification) loadSpecResource("../model/worker/mount_ok");

        assertEquals("/mnt1", spec.getProperties().get("s_target"));
        assertEquals(false, spec.getProperties().get("b_rotational"));
        assertEquals("ext4", spec.getProperties().get("s_type"));
        assertEquals(BigDecimal.valueOf(123458888L).stripTrailingZeros(), ((BigDecimal) spec.getProperties().get("n_capacity")).stripTrailingZeros());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void mustFailToCreateSpecIfAvailableHigherThanTotal() throws Exception {
        loadSpecResource("../model/worker/mount_oob");
    }
}
