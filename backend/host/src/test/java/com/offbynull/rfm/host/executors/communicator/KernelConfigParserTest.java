package com.offbynull.rfm.host.executors.communicator;

import com.offbynull.rfm.host.executors.communicator.KernelConfigParser.KernelConfigEntry;
import java.util.List;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class KernelConfigParserTest {

    @Test
    public void mustParseEntries() {
        List<KernelConfigEntry> actual = KernelConfigParser.parse(""
                + "#\n"
                + "# Automatically generated file; DO NOT EDIT.\n"
                + "# Linux/x86_64 4.8.0-53-generic Kernel Configuration\n"
                + "#\n"
                + "CONFIG_64BIT=y\n"
                + "CONFIG_X86_64=y\n"
                + "CONFIG_X86=y\n"
                + "CONFIG_MEMCG_SWAP_ENABLED=y #   THIS IS A COMMENT  ###\n\n\n\n\n"
                + "CONFIG_WITH_COMMENT=y #this is a comment");
        
        assertEquals(5, actual.size());
        assertEquals("CONFIG_64BIT", actual.get(0).getKey());
        assertEquals("y", actual.get(0).getValue());
        assertEquals("CONFIG_X86_64", actual.get(1).getKey());
        assertEquals("y", actual.get(1).getValue());
        assertEquals("CONFIG_X86", actual.get(2).getKey());
        assertEquals("y", actual.get(2).getValue());
        assertEquals("CONFIG_MEMCG_SWAP_ENABLED", actual.get(3).getKey());
        assertEquals("y", actual.get(3).getValue());
        assertEquals("CONFIG_WITH_COMMENT", actual.get(4).getKey());
        assertEquals("y", actual.get(4).getValue());
    }

    @Test
    public void mustParseNoEntries() {
        List<KernelConfigEntry> actual = KernelConfigParser.parse("");
        
        assertEquals(0, actual.size());
    }

}
