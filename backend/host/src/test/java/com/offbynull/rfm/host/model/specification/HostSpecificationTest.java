package com.offbynull.rfm.host.model.specification;

import static com.offbynull.rfm.host.testutils.TestUtils.loadSpecResource;
import java.math.BigDecimal;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class HostSpecificationTest {

    @Test
    public void mustCreateSpec() throws Exception {
        HostSpecification spec = (HostSpecification) loadSpecResource("../model/worker/host_ok");

        assertEquals("localhost", spec.getProperties().get("s_host"));
        assertEquals(BigDecimal.valueOf(12345).stripTrailingZeros(), ((BigDecimal) spec.getProperties().get("n_port")).stripTrailingZeros());
        assertEquals(BigDecimal.valueOf(5).stripTrailingZeros(), ((BigDecimal) spec.getProperties().get("n_host_class")).stripTrailingZeros());
        assertEquals("US-EAST", spec.getProperties().get("s_facility"));
        assertEquals(2, spec.getSocketSpecifications().size());
        assertEquals(2, spec.getGpuSpecifications().size());
        assertEquals(2, spec.getMountSpecifications().size());
        assertEquals(1, spec.getRamSpecifications().size());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void mustFailToCreateSpecIfDuplicateSocketIds() throws Exception {
        loadSpecResource("../model/worker/host_dupe_socket");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void mustFailToCreateSpecIfDuplicateGpuIds() throws Exception {
        loadSpecResource("../model/worker/host_dupe_gpu");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void mustFailToCreateSpecIfDuplicateMountIds() throws Exception {
        loadSpecResource("../model/worker/host_dupe_mount");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void mustFailToCreateSpecIfMultipleRamIds() throws Exception {
        loadSpecResource("../model/worker/host_multi_ram");
    }
    
}
