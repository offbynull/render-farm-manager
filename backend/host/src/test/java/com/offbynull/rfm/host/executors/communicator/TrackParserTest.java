package com.offbynull.rfm.host.executors.communicator;

import com.offbynull.rfm.host.executors.communicator.TrackParser.TrackEntry;
import java.util.List;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class TrackParserTest {

    @Test
    public void mustParse() {
        List<TrackEntry> actual = TrackParser.parse(""
                + "id1 123 /tmp/folder a/folder b\n"
                + "id2 456 /tmp/b\n"
                + "id3 789 /tmp/d");
        
        assertEquals(3, actual.size());
        
        assertEquals("id1", actual.get(0).getId());
        assertEquals(123L, actual.get(0).getBootTime());
        assertEquals("/tmp/folder a/folder b", actual.get(0).getDirectory());
        
        assertEquals("id2", actual.get(1).getId());
        assertEquals(456L, actual.get(1).getBootTime());
        assertEquals("/tmp/b", actual.get(1).getDirectory());
        
        assertEquals("id3", actual.get(2).getId());
        assertEquals(789L, actual.get(2).getBootTime());
        assertEquals("/tmp/d", actual.get(2).getDirectory());
    }

    @Test
    public void mustParseIfEmpty() {
        List<TrackEntry> actual = TrackParser.parse("");
        
        assertEquals(0, actual.size());
    }

    @Test
    public void mustIgnoreExtraNewLinesWhenParsing() {
        List<TrackEntry> actual = TrackParser.parse(""
                + "id1 123 /tmp/folder a/folder b\n\n\n\n"
                + "id2 456 /tmp/b\n\n\n"
                + "id3 789 /tmp/d\n");
        
        assertEquals(3, actual.size());
        
        assertEquals("id1", actual.get(0).getId());
        assertEquals(123L, actual.get(0).getBootTime());
        assertEquals("/tmp/folder a/folder b", actual.get(0).getDirectory());
        
        assertEquals("id2", actual.get(1).getId());
        assertEquals(456L, actual.get(1).getBootTime());
        assertEquals("/tmp/b", actual.get(1).getDirectory());
        
        assertEquals("id3", actual.get(2).getId());
        assertEquals(789L, actual.get(2).getBootTime());
        assertEquals("/tmp/d", actual.get(2).getDirectory());
    }

    @Test(expected=RuntimeException.class)
    public void mustFailToParseIfTruncated() {
        TrackParser.parse(""
                + "id1 123 /tmp/folder a/folder b\n"
                + "id2 45");
    }
    
}
