package com.offbynull.rfm.host.executors.communicator;

import com.offbynull.rfm.host.executors.communicator.ProcCpuInfoParser.ProcCpuInfoEntry;
import java.util.List;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ProcCpuInfoParserTest {

    @Test
    public void mustParseMultipleCpus() {
        List<ProcCpuInfoEntry> actual = ProcCpuInfoParser.parse(""
                + "processor	: 0\n"
                + "vendor_id	: GenuineIntel\n"
                + "cpu family	: 6\n"
                + "model		: 158\n"
                + "model name	: Intel(R) Core(TM) i7-7700K CPU @ 4.20GHz\n"
                + "stepping	: 9\n"
                + "cpu MHz		: 4199.996\n"
                + "cache size	: 8192 KB\n"
                + "physical id	: 0\n"
                + "siblings	: 2\n"
                + "core id		: 0\n"
                + "cpu cores	: 2\n"
                + "apicid		: 0\n"
                + "initial apicid	: 0\n"
                + "fpu		: yes\n"
                + "fpu_exception	: yes\n"
                + "cpuid level	: 22\n"
                + "wp		: yes\n"
                + "flags		: fpu vme de\n"
                + "bugs		:\n"
                + "bogomips	: 8399.99\n"
                + "clflush size	: 64\n"
                + "cache_alignment	: 64\n"
                + "address sizes	: 39 bits physical, 48 bits virtual\n"
                + "power management:\n"
                + "\n"
                + "processor	: 1\n"
                + "vendor_id	: GenuineIntel\n"
                + "cpu family	: 6\n"
                + "model		: 158\n"
                + "model name	: Intel(R) Core(TM) i7-7700K CPU @ 4.20GHz\n"
                + "stepping	: 9\n"
                + "cpu MHz		: 4199.996\n"
                + "cache size	: 8192 KB\n"
                + "physical id	: 0\n"
                + "siblings	: 2\n"
                + "core id		: 1\n"
                + "cpu cores	: 2\n"
                + "apicid		: 1\n"
                + "initial apicid	: 1\n"
                + "fpu		: yes\n"
                + "fpu_exception	: yes\n"
                + "cpuid level	: 22\n"
                + "wp		: yes\n"
                + "flags		: 3dnowprefetch rdseed clflushopt\n"
                + "bugs		:\n"
                + "bogomips	: 8399.99\n"
                + "clflush size	: 64\n"
                + "cache_alignment	: 64\n"
                + "address sizes	: 39 bits physical, 48 bits virtual\n"
                + "power management:");
        
        assertEquals(2, actual.size());
        assertEquals(0L, actual.get(0).getProcessor());
        assertEquals(0L, actual.get(0).getCoreId());
        assertEquals(0L, actual.get(0).getPhysicalId());
        assertEquals("Intel(R) Core(TM) i7-7700K CPU @ 4.20GHz", actual.get(0).getModel());
        assertEquals(1L, actual.get(1).getProcessor());
        assertEquals(1L, actual.get(1).getCoreId());
        assertEquals(0L, actual.get(1).getPhysicalId());
        assertEquals("Intel(R) Core(TM) i7-7700K CPU @ 4.20GHz", actual.get(1).getModel());
    }

    @Test(expected=RuntimeException.class)
    public void mustFailToParseIfProcessorMissing() {
        ProcCpuInfoParser.parse(""
                + "model name	: Intel(R) Core(TM) i7-7700K CPU @ 4.20GHz\n"
                + "physical id	: 0\n"
                + "core id		: 0\n"
                + "flags		: fpu vme de\n"
                + "\n"
                + "processor	: 1\n"
                + "model name	: Intel(R) Core(TM) i7-7700K CPU @ 4.20GHz\n"
                + "physical id	: 0\n"
                + "core id		: 1\n"
                + "flags		: 3dnowprefetch rdseed clflushopt");
    }
    
    @Test(expected=RuntimeException.class)
    public void mustFailToParseIfModelNameMissing() {
        ProcCpuInfoParser.parse(""
                + "processor	: 0\n"
                + "physical id	: 0\n"
                + "core id		: 0\n"
                + "flags		: fpu vme de\n"
                + "\n"
                + "processor	: 1\n"
                + "model name	: Intel(R) Core(TM) i7-7700K CPU @ 4.20GHz\n"
                + "physical id	: 0\n"
                + "core id		: 1\n"
                + "flags		: 3dnowprefetch rdseed clflushopt");
    }

    @Test(expected=RuntimeException.class)
    public void mustFailToParseIfCoreIdMissing() {
        ProcCpuInfoParser.parse(""
                + "processor	: 0\n"
                + "model name	: Intel(R) Core(TM) i7-7700K CPU @ 4.20GHz\n"
                + "physical id	: 0\n"
                + "flags		: fpu vme de\n"
                + "\n"
                + "processor	: 1\n"
                + "model name	: Intel(R) Core(TM) i7-7700K CPU @ 4.20GHz\n"
                + "physical id	: 0\n"
                + "core id		: 1\n"
                + "flags		: 3dnowprefetch rdseed clflushopt");
    }
    
    @Test(expected=RuntimeException.class)
    public void mustFailToParseIfPhysicalIdMissing() {
        ProcCpuInfoParser.parse(""
                + "processor	: 0\n"
                + "model name	: Intel(R) Core(TM) i7-7700K CPU @ 4.20GHz\n"
                + "core id		: 0\n"
                + "flags		: fpu vme de\n"
                + "\n"
                + "processor	: 1\n"
                + "model name	: Intel(R) Core(TM) i7-7700K CPU @ 4.20GHz\n"
                + "physical id	: 0\n"
                + "core id		: 1\n"
                + "flags		: 3dnowprefetch rdseed clflushopt");
    }
    
    @Test
    public void mustParseABunchOfNewLinesToNothing() {
        List<ProcCpuInfoEntry> actual = ProcCpuInfoParser.parse("\n\n\n\n\n\n\n\n\n"); // should never happen, but just in case?

        assertEquals(0, actual.size());
    }
}
