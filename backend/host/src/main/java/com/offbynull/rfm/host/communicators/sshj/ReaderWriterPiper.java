/*
 * Copyright (c) 2018, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.rfm.host.communicators.sshj;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ReaderWriterPiper implements Closeable {
    
    private static final Logger logger = LoggerFactory.getLogger(ReaderWriterPiper.class);
    
    private static final int BUFFER_SIZE = 16 * 1024;
    
    private final Reader reader;
    private final Writer writer;
    private final Thread thread;

    public static ReaderWriterPiper spawn(Reader reader, Writer writer, String name) {
        Validate.notNull(name);
        Validate.notNull(reader);
        Validate.notNull(writer);
        ReaderWriterPiper ret = new ReaderWriterPiper(reader, writer);
        ret.thread.setDaemon(true);
        ret.thread.setName("SSHJ ReadWrite " + name + " Pipe");
        ret.thread.start();
        return ret;
    }

    private ReaderWriterPiper(Reader reader, Writer writer) {
        Validate.notNull(reader);
        Validate.notNull(writer);
        this.reader = reader;
        this.writer = writer;
        this.thread = new Thread(() -> copy());
    }
    
    public void join() throws IOException {
        try {
            logger.debug("Joining");
            thread.join();
        } catch (InterruptedException ie) {
            throw new IOException("Interrupted", ie);
        }
    }

    private void copy() {
        // DO NOT CLOSE THE READER/WRITER IN THE FINALLY BLOCK IT WILL MESS THINGS UP
        char[] cbuf = new char[BUFFER_SIZE];
        try {
            while (true) {
                int len = reader.read(cbuf);
                if (len == -1) {
                    return;
                }
                logger.debug("Read {} chars", len);
                writer.write(cbuf, 0, len);
                logger.debug("Wrote {} chars", len);
            }
        } catch (RuntimeException | IOException e) {
            logger.warn("Exception encountered while piping", e);
        } finally {
            try {
                logger.debug("Flushing");
                writer.flush();
            } catch (IOException ioe) {
                logger.warn("Exception encountered while flushing", ioe);
            }
        }
    }

    @Override
    public void close() {
        logger.debug("Closing");
        thread.interrupt();
    }
}
