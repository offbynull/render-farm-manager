package com.offbynull.rfm.host.executors.communicator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class CGroupCpuSetParserTest {

    @Test
    public void mustParseSingle() {
        Set<Long> actual = CGroupCpuSetParser.parse("0");
        Set<Long> expected = new HashSet<>(Arrays.asList(0L));
        
        assertEquals(expected, actual);
    }

    @Test
    public void mustParseMultipleDistinct() {
        Set<Long> actual = CGroupCpuSetParser.parse("0,1,3,5,10,1000");
        Set<Long> expected = new HashSet<>(Arrays.asList(0L, 1L, 3L, 5L, 10L, 1000L));
        
        assertEquals(expected, actual);
    }

    @Test
    public void mustParseRange() {
        Set<Long> actual = CGroupCpuSetParser.parse("0-5");
        Set<Long> expected = new HashSet<>(Arrays.asList(0L, 1L, 2L, 3L, 4L, 5L));
        
        assertEquals(expected, actual);
    }

    @Test
    public void mustParseComplex() {
        Set<Long> actual = CGroupCpuSetParser.parse("0-2,10,15-17,25");
        Set<Long> expected = new HashSet<>(Arrays.asList(0L, 1L, 2L, 10L, 15L, 16L, 17L, 25L));
        
        assertEquals(expected, actual);
    }

    @Test
    public void mustParseDuplicates() {
        Set<Long> actual = CGroupCpuSetParser.parse("0-2,1,0-2");
        Set<Long> expected = new HashSet<>(Arrays.asList(0L, 1L, 2L));
        
        assertEquals(expected, actual);
    }

    @Test(expected=RuntimeException.class)
    public void mustParseEmpty() {
        CGroupCpuSetParser.parse("");
    }

    @Test(expected=RuntimeException.class)
    public void mustFailToParseNegative() {
        CGroupCpuSetParser.parse("-100");
    }

    @Test(expected=RuntimeException.class)
    public void mustFailToParseBackwardsRange() {
        CGroupCpuSetParser.parse("5-0");
    }

    @Test(expected=RuntimeException.class)
    public void mustFailToParseObscenelyLargeRange() {
        CGroupCpuSetParser.parse("0-10000");
    }
    
}
