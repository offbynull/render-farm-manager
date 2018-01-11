package com.offbynull.rfm.host.executors.communicator;

import com.offbynull.rfm.host.executors.communicator.ProcMemInfoParser.ProcMemInfo;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ProcMemInfoParserTest {

    @Test
    public void mustParse() {
        ProcMemInfo actual = ProcMemInfoParser.parse(""
                + "MemTotal:        2047768 kB\n"
                + "MemFree:         1065740 kB\n"
                + "MemAvailable:    1398280 kB\n"
                + "Buffers:           80832 kB\n"
                + "Cached:           366464 kB\n"
                + "SwapCached:            0 kB\n"
                + "Active:           596856 kB\n"
                + "Inactive:         229628 kB\n"
                + "Active(anon):     380060 kB\n"
                + "Inactive(anon):    20596 kB\n"
                + "Active(file):     216796 kB\n"
                + "Inactive(file):   209032 kB\n"
                + "Unevictable:          32 kB\n"
                + "Mlocked:              32 kB\n"
                + "SwapTotal:       2095100 kB\n"
                + "SwapFree:        209510 kB\n"
                + "Dirty:                 0 kB\n"
                + "Writeback:             0 kB\n"
                + "AnonPages:        373220 kB\n"
                + "Mapped:           146108 kB\n"
                + "Shmem:             21472 kB\n"
                + "Slab:             101112 kB\n"
                + "SReclaimable:      76712 kB\n"
                + "SUnreclaim:        24400 kB\n"
                + "KernelStack:        6432 kB\n"
                + "PageTables:        23192 kB\n"
                + "NFS_Unstable:          0 kB\n"
                + "Bounce:                0 kB\n"
                + "WritebackTmp:          0 kB\n"
                + "CommitLimit:     3118984 kB\n"
                + "Committed_AS:    2300244 kB\n"
                + "VmallocTotal:   34359738367 kB\n"
                + "VmallocUsed:           0 kB\n"
                + "VmallocChunk:          0 kB\n"
                + "HardwareCorrupted:     0 kB\n"
                + "AnonHugePages:    120832 kB\n"
                + "ShmemHugePages:        0 kB\n"
                + "ShmemPmdMapped:        0 kB\n"
                + "CmaTotal:              0 kB\n"
                + "CmaFree:               0 kB\n"
                + "HugePages_Total:       0\n"
                + "HugePages_Free:        0\n"
                + "HugePages_Rsvd:        0\n"
                + "HugePages_Surp:        0\n"
                + "Hugepagesize:       2048 kB\n"
                + "DirectMap4k:       81856 kB\n"
                + "DirectMap2M:     2015232 kB");
        
        assertEquals(1398280L, actual.getMemAvailable());
        assertEquals(2047768L, actual.getMemTotal());
        assertEquals(209510L, actual.getSwapFree());
        assertEquals(2095100L, actual.getSwapTotal());
    }
    
    @Test(expected=RuntimeException.class)
    public void mustFailToParseIfMemTotalMissing() {
        ProcMemInfoParser.parse(""
                + "MemAvailable:    1398280 kB\n"
                + "SwapTotal:       2095100 kB\n"
                + "SwapFree:        209510 kB");
    }
    
    @Test(expected=RuntimeException.class)
    public void mustFailToParseIfMemAvailableMissing() {
        ProcMemInfoParser.parse(""
                + "MemTotal:        2047768 kB\n"
                + "SwapTotal:       2095100 kB\n"
                + "SwapFree:        209510 kB");
    }
    
    @Test(expected=RuntimeException.class)
    public void mustFailToParseIfSwapTotalMissing() {
        ProcMemInfoParser.parse(""
                + "MemTotal:        2047768 kB\n"
                + "MemAvailable:    1398280 kB\n"
                + "SwapFree:        209510 kB");
    }
    
    @Test(expected=RuntimeException.class)
    public void mustFailToParseIfSwapFreeMissing() {
        ProcMemInfoParser.parse(""
                + "MemTotal:        2047768 kB\n"
                + "MemAvailable:    1398280 kB\n"
                + "SwapTotal:       2095100 kB");
    }

}
