package com.offbynull.rfm.host.communicators.sshj;

import com.offbynull.rfm.host.communicator.StreamLimitExceededException;
import java.io.Closeable;
import java.io.StringWriter;
import org.apache.commons.lang3.mutable.MutableBoolean;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

public class LimitedWriterTest {

    @Test
    public void mustPassIfLimitNotExceeded() throws Exception {
        MutableBoolean closed = new MutableBoolean();
        Closeable otherCloseable = () -> closed.setTrue();
        
        StringWriter stringWriter = new StringWriter();
        LimitedWriter limitedWriter = new LimitedWriter(stringWriter, 16, otherCloseable);

        limitedWriter.append('a');
        limitedWriter.append("bb");
        limitedWriter.append("ccc");
        limitedWriter.append("dddd");
        limitedWriter.append("eeeee");
        
        assertEquals("abbcccddddeeeee", stringWriter.toString());
    }

    @Test
    public void mustFailIfLimitExceeded() throws Exception {
        MutableBoolean closed = new MutableBoolean();
        Closeable otherCloseable = () -> closed.setTrue();
        
        StringWriter stringWriter = new StringWriter();
        LimitedWriter limitedWriter = new LimitedWriter(stringWriter, 16, otherCloseable);

        limitedWriter.append('a');
        limitedWriter.append("bb");
        limitedWriter.append("ccc");
        limitedWriter.append("dddd");
        limitedWriter.append("eeeee");
        
        try {
            limitedWriter.append("ffffff", 0, 1);
        } catch (StreamLimitExceededException slee) {
            assertTrue(closed.booleanValue());
            return;
        }
        
        fail("Exception missing");
    }
    
}
