package com.offbynull.rfm.host.model.specification;

import static com.offbynull.rfm.host.testutils.TestUtils.loadSpecResource;
import static java.lang.Boolean.TRUE;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class GpuSpecificationTest {

    @Test
    public void mustCreateSpec() throws Exception {
        GpuSpecification spec = (GpuSpecification) loadSpecResource("../model/worker/gpu_ok");

        assertEquals("pci_0000_0001", spec.getProperties().get("s_gpu_id"));
        assertEquals("nvidia", spec.getProperties().get("s_vendor"));
        assertEquals("1080", spec.getProperties().get("s_model"));
        assertEquals(TRUE, spec.getProperties().get("b_available"));
    }
    
}
