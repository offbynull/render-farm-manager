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
package com.offbynull.rfm.host.communicator;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;

/**
 * Linux command result where stdout and stderr are buffered in files. Calling {@link #close() } will delete the files.
 * @author Kasra Faghihi
 */
public final class BufferedExecuteResult extends ExecuteResult implements Closeable {

    private final Path stdoutPath;
    private final Reader stdoutReader;
    
    private final Path stderrPath;
    private final Reader stderrReader;

    /**
     * Constructs a {@link BufferedExecuteResult} object.
     * @param stdoutPath path to stdout file
     * @param stdoutReader stdout file reader
     * @param stderrPath path to stderr file
     * @param stderrReader stderr file reader
     * @param exitCode command exit code
     * @param bootTime time host was booted (seconds since Unix epoch)
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any path argument is not a readable file
     */
    public BufferedExecuteResult(Path stdoutPath, Reader stdoutReader, Path stderrPath, Reader stderrReader, int exitCode, long bootTime) {
        super(exitCode, bootTime);

        Validate.notNull(stdoutPath);
        Validate.notNull(stdoutReader);
        Validate.notNull(stderrPath);
        Validate.notNull(stderrReader);
        Validate.isTrue(Files.isReadable(stdoutPath));
        Validate.isTrue(Files.isReadable(stderrPath));

        this.stdoutPath = stdoutPath;
        this.stdoutReader = stdoutReader;
        this.stderrPath = stderrPath;
        this.stderrReader = stderrReader;
    }

    /**
     * Get stdout reader.
     * @return stdout reader
     */
    public Reader getStdoutReader() {
        return stdoutReader;
    }

    /**
     * Get stderr reader.
     * @return stderr reader
     */
    public Reader getStderrReader() {
        return stderrReader;
    }

    @Override
    public String toString() {
        return "BufferedExecuteResult{" + "super=" + super.toString() + "stdoutPath=" + stdoutPath + ", stdoutReader=" + stdoutReader
                + ", stderrPath=" + stderrPath + ", stderrReader=" + stderrReader + '}';
    }

    @Override
    public void close() throws IOException {
        IOUtils.closeQuietly(stdoutReader);
        IOUtils.closeQuietly(stderrReader);
        Files.deleteIfExists(stdoutPath);
        Files.deleteIfExists(stderrPath);
    }
}
