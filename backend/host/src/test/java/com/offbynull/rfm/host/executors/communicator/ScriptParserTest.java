package com.offbynull.rfm.host.executors.communicator;

import java.util.Map;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ScriptParserTest {

    @Test
    public void mustParse() {
        Map<String, String> actual = ScriptParser.parse(""
                + "!A\n"
                + "3\n"
                + "line1\n"
                + "line1 2\n"
                + "line1 2 3\n"
                + "!OUTPUT 2\n"
                + "2\n"
                + "aaa\n"
                + "bb");
        
        assertEquals(2, actual.size());
        assertEquals("line1\nline1 2\nline1 2 3", actual.get("!A"));
        assertEquals("aaa\nbb", actual.get("!OUTPUT 2"));
    }

    @Test
    public void mustParseIfEmpty() {
        Map<String, String> actual = ScriptParser.parse("");
        
        assertEquals(0, actual.size());
    }

    @Test(expected=RuntimeException.class)
    public void mustFailToParseIfLessLinesThanSpecifiedIsProvided() {
        ScriptParser.parse(""
                + "!A\n"
                + "4\n"
                + "line1\n"
                + "line1 2\n"
                + "line1 2 3");
    }

    @Test(expected=RuntimeException.class)
    public void mustFailToParseIfLineCountNotProvided() {
        ScriptParser.parse("!A");
    }
    
}
