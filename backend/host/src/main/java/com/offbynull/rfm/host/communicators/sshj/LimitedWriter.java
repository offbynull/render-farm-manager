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

import com.offbynull.rfm.host.communicator.StreamLimitExceededException;
import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class LimitedWriter extends Writer {
    
    private static final Logger logger = LoggerFactory.getLogger(LimitedWriter.class);
    
    private final Closeable[] otherCloseables;
    private final Writer writer;
    
    private final AtomicBoolean limitHit;
    private final AtomicLong count;
    private final long limit;

    public LimitedWriter(Writer writer, long limit, Closeable... otherCloseables) {
        Validate.notNull(otherCloseables);
        Validate.notNull(writer);
        Validate.noNullElements(otherCloseables);
        Validate.isTrue(limit >= 0L);
        this.otherCloseables = Arrays.copyOf(otherCloseables, otherCloseables.length);
        this.writer = writer;
        this.limitHit = new AtomicBoolean();
        this.count = new AtomicLong();
        this.limit = limit;
    }

    @Override
    public void write(int c) throws IOException {
        addCount(1L);
        writer.write(c);
    }

    @Override
    public void write(char[] cbuf) throws IOException {
        addCount(cbuf.length);
        writer.write(cbuf);
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        addCount(len);
        writer.write(cbuf, off, len);
    }

    @Override
    public void write(String str) throws IOException {
        addCount(str.length());
        writer.write(str);
    }

    @Override
    public void write(String str, int off, int len) throws IOException {
        addCount(len);
        writer.write(str, off, len);
    }

    @Override
    public Writer append(CharSequence csq) throws IOException {
        addCount(csq.length());
        return writer.append(csq);
    }

    @Override
    public Writer append(CharSequence csq, int start, int end) throws IOException {
        addCount(end-start);
        return writer.append(csq, start, end);
    }

    @Override
    public Writer append(char c) throws IOException {
        addCount(1L);
        return writer.append(c);
    }

    @Override
    public void flush() throws IOException {
        writer.flush();
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }

    private void addCount(long len) throws IOException {
        Validate.isTrue(len >= 0L); // may be negative -- see append()

        if (count.get() >= limit) {
            logger.warn("Limit hit {}", limit);

            limitHit.set(true);
            IOUtils.closeQuietly(otherCloseables);
            writer.close();
            throw new StreamLimitExceededException();
        }

        long newCount = count.addAndGet(len);
        if (newCount >= limit) {
            logger.warn("Limit hit {} vs {}", newCount, limit);

            limitHit.set(true);
            IOUtils.closeQuietly(otherCloseables);
            writer.close();
            throw new StreamLimitExceededException();
        }
        
        logger.trace("Count updated to {} vs {}", newCount, limit);
    }
    
    public boolean isLimitHit() {
        return limitHit.get();
    }
}
