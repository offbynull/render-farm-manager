package com.offbynull.rfm.host.executors.communicator;

import com.offbynull.rfm.host.executors.communicator.DfParser.DfEntry;
import java.util.List;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class DfParserTest {

    @Test
    public void mustParseEmptyList() {
        List<DfEntry> actual = DfParser.parse("        Used         Avail Type            Mounted on");
        
        assertEquals(0L, actual.size());
    }

    @Test
    public void mustParseList() {
        List<DfEntry> actual = DfParser.parse(""
                + "        Used         Avail Type            Mounted on\n"
                + "           0             0 sysfs           /sys\n"
                + "  6344970240    1528664064 ext4            /\n"
                + "           -             - -               /proc/sys/fs/binfmt_misc\n"
                + "      147456      15257600 ext2            /tmp/test/work_mnt");
        
        assertEquals(4L, actual.size());

        assertEquals(0L, (long) actual.get(0).getUsed());
        assertEquals(0L, (long) actual.get(0).getAvailable());
        assertEquals("sysfs", actual.get(0).getType());
        assertEquals("/sys", actual.get(0).getTarget());

        assertEquals(6344970240L, (long) actual.get(1).getUsed());
        assertEquals(1528664064L, (long) actual.get(1).getAvailable());
        assertEquals("ext4", actual.get(1).getType());
        assertEquals("/", actual.get(1).getTarget());

        assertEquals(null, actual.get(2).getUsed());
        assertEquals(null, actual.get(2).getAvailable());
        assertEquals("-", actual.get(2).getType());
        assertEquals("/proc/sys/fs/binfmt_misc", actual.get(2).getTarget());

        assertEquals(147456L, (long) actual.get(3).getUsed());
        assertEquals(15257600L, (long) actual.get(3).getAvailable());
        assertEquals("ext2", actual.get(3).getType());
        assertEquals("/tmp/test/work_mnt", actual.get(3).getTarget());
    }

    @Test
    public void mustParseOnExtraNewLines() { // should never happen, but hte parser accounts for this
        List<DfEntry> actual = DfParser.parse(""
                + "        Used         Avail Type            Mounted on\n\n\n\n\n\n\n\n\n\n"
                + "           0             0 sysfs           /sys\n\n\n\n");
        
        assertEquals(1L, actual.size());

        assertEquals(0L, (long) actual.get(0).getUsed());
        assertEquals(0L, (long) actual.get(0).getAvailable());
        assertEquals("sysfs", actual.get(0).getType());
        assertEquals("/sys", actual.get(0).getTarget());
    }

    @Test(expected=RuntimeException.class)
    public void mustFailToParsePartialList() {
        DfParser.parse(""
                + "        Used         Avail Type            Mounted on\n"
                + "           0             0 sysfs           /sys\n"
                + "  6344970240    1528664064 ex");
    }


}
