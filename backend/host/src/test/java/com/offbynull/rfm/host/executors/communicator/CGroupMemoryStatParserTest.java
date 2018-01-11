package com.offbynull.rfm.host.executors.communicator;

import com.offbynull.rfm.host.executors.communicator.CGroupMemoryStatParser.CGroupMemoryStat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class CGroupMemoryStatParserTest {

    @Test
    public void mustParseOnExtraNewLines() {
        CGroupMemoryStat actual = CGroupMemoryStatParser.parse(""
                + "cache 486121472\n\n"
                + "rss 438042624\n\n\n\n\n\n"
                + "rss_huge 174063616\n\n"
                + "total_cache 500195328\n\n\n"
                + "total_rss 448004096\n\n"
                + "total_rss_huge 174063616\n\n\n\n\n\n\n\n\n\n\n");
        
        assertEquals(448004096L, actual.getRss());
        assertEquals(500195328L, actual.getCache());
        assertNull(actual.getSwap());
    }

    @Test
    public void mustParseWhenTotalSwapMissing() {
        CGroupMemoryStat actual = CGroupMemoryStatParser.parse(""
                + "cache 486121472\n"
                + "rss 438042624\n"
                + "rss_huge 174063616\n"
                + "total_cache 500195328\n"
                + "total_rss 448004096\n"
                + "total_rss_huge 174063616");
        
        assertEquals(448004096L, actual.getRss());
        assertEquals(500195328L, actual.getCache());
        assertNull(actual.getSwap());
    }

    @Test
    public void mustParseWhenTotalSwapIsAvailable() {
        CGroupMemoryStat actual = CGroupMemoryStatParser.parse(""
                + "cache 486121472\n"
                + "rss 438042624\n"
                + "rss_huge 174063616\n"
                + "total_swap 9999\n"
                + "total_cache 500195328\n"
                + "total_rss 448004096\n"
                + "total_rss_huge 174063616");
        
        assertEquals(448004096L, actual.getRss());
        assertEquals(500195328L, actual.getCache());
        assertEquals(9999L, (long) actual.getSwap());
    }

    @Test(expected=RuntimeException.class)
    public void mustFailToParseWhenTotalCacheIsMissing() {
        CGroupMemoryStatParser.parse(""
                + "cache 486121472\n"
                + "rss 438042624\n"
                + "rss_huge 174063616\n"
                + "total_swap 9999\n"
                + "total_rss 448004096\n"
                + "total_rss_huge 174063616");
    }

    @Test(expected=RuntimeException.class)
    public void mustFailToParseWhenTotalRssIsMissing() {
        CGroupMemoryStatParser.parse(""
                + "cache 486121472\n"
                + "rss 438042624\n"
                + "rss_huge 174063616\n"
                + "total_swap 9999\n"
                + "total_cache 500195328\n"
                + "total_rss_huge 174063616");
    }

}
