package com.offbynull.rfm.host.communicators.sshj;

import java.io.StringReader;
import java.io.StringWriter;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ReaderWriterPiperTest {
    private ReaderWriterPiper fixture;
    
    @After
    public void tearDown() throws Exception {
        fixture.close();
        fixture.join();
    }

    @Test
    public void mustCopyStreamUntilFinish() throws Exception {
        String largeInput = StringUtils.repeat("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ", 15000);

        try (StringWriter writer = new StringWriter();
                StringReader reader = new StringReader(largeInput)) {
            fixture = ReaderWriterPiper.spawn(reader, writer, "test");
            fixture.join();
            assertEquals(largeInput, writer.toString());
        }
    }
    
}
