package com.offbynull.rfm.host.executors.communicator;

import com.offbynull.rfm.host.executors.communicator.ProcStatParser.ProcStatCpuEntry;
import java.util.List;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ProcStatParserTest {

    @Test
    public void mustParse() {
        List<ProcStatCpuEntry> actual = ProcStatParser.parse(""
                + "cpu  3315 5 2701 7337830 301 0 133 0 0 0\n"
                + "cpu0 1064 5 1078 3674346 118 0 70 0 0 0\n"
                + "cpu1 2250 0 1623 3663484 182 0 62 0 0 0\n"
                + "intr 5111480 32 533 0 0 0 0 0 0 0 0 0 0 292 0 0 36662 40530 0 0 23167 311578 22709 29 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0\n"
                + "ctxt 23810464\n"
                + "btime 1523969552\n"
                + "processes 20127\n"
                + "procs_running 2\n"
                + "procs_blocked 0\n"
                + "softirq 2317543 2 821546 6938 64099 38987 0 796 815612 0 569563");

        assertEquals(2, actual.size());
        
        assertEquals(0L, actual.get(0).getProcessor());
        assertEquals(1064L, actual.get(0).getUserTime());
        assertEquals(5L, actual.get(0).getNiceTime());
        assertEquals(1078L, actual.get(0).getSystemTime());
        assertEquals(3674346L, actual.get(0).getIdleTime());
        
        assertEquals(1L, actual.get(1).getProcessor());
        assertEquals(2250L, actual.get(1).getUserTime());
        assertEquals(0L, actual.get(1).getNiceTime());
        assertEquals(1623L, actual.get(1).getSystemTime());
        assertEquals(3663484L, actual.get(1).getIdleTime());
    }

    @Test
    public void mustParseEvenIfExtraNewlines() {
        List<ProcStatCpuEntry> actual = ProcStatParser.parse(""
                + "cpu  3315 5 2701 7337830 301 0 133 0 0 0\n\n\n\n\n\n"
                + "cpu0 1064 5 1078 3674346 118 0 70 0 0 0\n\n\n"
                + "cpu1 2250 0 1623 3663484 182 0 62 0 0 0\n\n\n"
                + "intr 5111480 32 533 0 0 0 0 0 0 0 0 0 0 292 0 0 36662 40530 0 0 23167 311578 22709 29 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0\n"
                + "ctxt 23810464\n\n\n"
                + "btime 1523969552\n\n"
                + "processes 20127\n\n\n"
                + "procs_running 2\n\n"
                + "procs_blocked 0\n"
                + "softirq 2317543 2 821546 6938 64099 38987 0 796 815612 0 569563\n\n\n\n\n");

        assertEquals(2, actual.size());
        
        assertEquals(0L, actual.get(0).getProcessor());
        assertEquals(1064L, actual.get(0).getUserTime());
        assertEquals(5L, actual.get(0).getNiceTime());
        assertEquals(1078L, actual.get(0).getSystemTime());
        assertEquals(3674346L, actual.get(0).getIdleTime());
        
        assertEquals(1L, actual.get(1).getProcessor());
        assertEquals(2250L, actual.get(1).getUserTime());
        assertEquals(0L, actual.get(1).getNiceTime());
        assertEquals(1623L, actual.get(1).getSystemTime());
        assertEquals(3663484L, actual.get(1).getIdleTime());
    }

    @Test
    public void mustParseWithSomeCpusMissing() {
        List<ProcStatCpuEntry> actual = ProcStatParser.parse("cpu5 2250 0 1623 3663484 182 0 62 0 0 0");

        assertEquals(1, actual.size());
        
        assertEquals(5L, actual.get(0).getProcessor());
        assertEquals(2250L, actual.get(0).getUserTime());
        assertEquals(0L, actual.get(0).getNiceTime());
        assertEquals(1623L, actual.get(0).getSystemTime());
        assertEquals(3663484L, actual.get(0).getIdleTime());
    }

    @Test(expected=RuntimeException.class)
    public void mustFailToParseIfSomeCpuFieldsMissing() {
        ProcStatParser.parse("cpu5 2250 0");
    }

}
