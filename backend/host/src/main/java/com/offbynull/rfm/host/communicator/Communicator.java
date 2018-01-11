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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;

/**
 * Low-level Linux host communicator. Executes arbitrary bash scripts (as root) as well as transfers files.
 * @author Kasra Faghih
 */
public interface Communicator extends Closeable {
    
    /**
     * Upload a file. If the remote file already exists, it will be truncated/removed before uploading.
     * @param timeout timeout in milliseconds
     * @param localData data to upload
     * @param remotePath remote file path
     * @throws IOException on IO error, or if {@code remotePath} exists but is not a regular file
     * @throws TimeLimitExceededException if timed out
     * @throws IllegalArgumentException if any of the following conditions are NOT met:
     * {@code timeout >= 0L},
     * {@code !remotePath.isEmpty()}
     * @throws NullPointerException if any argument is {@code null}
     */
    void upload(long timeout, byte[] localData, String remotePath) throws IOException;

    /**
     * Upload into a region of a remote file. Note that if...
     * <ul>
     * <li>the remote file doesn't exist, it will get created.</li>
     * <li>the offset within the remote file being written to doesn't exist, the remote file will get padded with 0s until the offset.</li>
     * </ul>
     * @param timeout timeout in milliseconds
     * @param localData data to upload
     * @param localOffset offset in {@code localData} to read from
     * @param remotePath remote file path
     * @param remoteOffset offset in file at {@code remotePath} to write to
     * @param len number of bytes to write
     * @throws IOException on IO error, or if {@code remotePath} exists but is not a regular file
     * @throws TimeLimitExceededException if timed out
     * @throws ArithmeticException if any of the following expressions cause an overflow:
     * {@code localOffset + len},
     * {@code remoteOffset + len}
     * @throws IllegalArgumentException if any of the following conditions are NOT met:
     * {@code timeout >= 0L},
     * {@code localOffset >= 0},
     * {@code localOffset < localData.length},
     * {@code localOffset + len <= localData.length},
     * {@code remoteOffset >= 0L},
     * {@code len >= 0},
     * {@code !remotePath.isEmpty()}
     * @throws NullPointerException if any argument is {@code null}
     */
    void upload(long timeout, byte[] localData, int localOffset, String remotePath, long remoteOffset, int len) throws IOException;

    /**
     * Upload a file. If the remote file already exists, it will be truncated/removed before uploading.
     * @param timeout timeout in milliseconds
     * @param localPath local file path
     * @param remotePath remote file path
     * @throws IOException on IO error, or if {@code remotePath} exists but is not a regular file
     * @throws TimeLimitExceededException if timed out
     * @throws IllegalArgumentException if any of the following conditions are NOT met:
     * {@code timeout >= 0L},
     * {@code !localPath.isEmpty()},
     * {@code !remotePath.isEmpty()}
     * @throws NullPointerException if any argument is {@code null}
     */
    void upload(long timeout, String localPath, String remotePath) throws IOException;

    /**
     * Pipe (download) a region of a remote file to a stream.
     * @param timeout timeout in milliseconds
     * @param remotePath remote file path
     * @param remoteOffset offset in file at {@code remotePath} to read from
     * @param os stream to write file to (will be closed by this method)
     * @param limit maximum number of bytes to read
     * @throws IOException on IO error, or if {@code remotePath} doesn't exist, or if {@code remotePath} not a regular file, or if
     * {@code remotePath} isn't the expected length
     * @throws TimeLimitExceededException if timed out
     * @throws ArithmeticException if any of the following expressions cause an overflow:
     * {@code remoteOffset + limit}
     * @throws IllegalArgumentException if any of the following conditions are NOT met:
     * {@code timeout >= 0L},
     * {@code !remotePath.isEmpty()},
     * {@code remoteOffset >= 0L},
     * {@code limit >= 0L}
     * @throws NullPointerException if any argument is {@code null}
     */
    void pipe(long timeout, String remotePath, long remoteOffset, OutputStream os, long limit) throws IOException;

    /**
     * Download a region of a remote file.
     * @param timeout timeout in milliseconds
     * @param remotePath remote file path
     * @param remoteOffset offset in file at {@code remotePath} to read from
     * @param len number of bytes to read
     * @return contents of download file
     * @throws IOException on IO error, or if {@code remotePath} doesn't exist, or if {@code remotePath} not a regular file, or if
     * {@code remotePath} isn't the expected length
     * @throws TimeLimitExceededException if timed out
     * @throws ArithmeticException if any of the following expressions cause an overflow:
     * {@code remoteOffset + len}
     * @throws IllegalArgumentException if any of the following conditions are NOT met:
     * {@code timeout >= 0L},
     * {@code !remotePath.isEmpty()},
     * {@code remoteOffset >= 0L},
     * {@code len >= 0}
     * @throws NullPointerException if any argument is {@code null}
     */
    byte[] download(long timeout, String remotePath, long remoteOffset, int len) throws IOException;

    /**
     * Download a file. 
     * @param timeout timeout in milliseconds
     * @param remotePath remote file path
     * @return contents of download file
     * @throws IOException on IO error, or if {@code remotePath} doesn't exist, or if {@code remotePath} not a regular file
     * @throws TimeLimitExceededException if timed out
     * @throws IllegalArgumentException if any of the following conditions are NOT met:
     * {@code timeout >= 0L},
     * {@code !remotePath.isEmpty()}
     * @throws NullPointerException if any argument is {@code null}
     */
    byte[] download(long timeout, String remotePath) throws IOException;

    /**
     * Download a file. 
     * @param timeout timeout in milliseconds
     * @param remotePath remote file path
     * @param localPath local file path
     * @throws IOException on IO error, or if {@code remotePath} doesn't exist, or if {@code remotePath} not a regular file
     * @throws TimeLimitExceededException if timed out
     * @throws IllegalArgumentException if any of the following conditions are NOT met:
     * {@code timeout >= 0L},
     * {@code !remotePath.isEmpty()},
     * {@code !localPath.isEmpty()}
     * @throws NullPointerException if any argument is {@code null}
     */
    void download(long timeout, String remotePath, String localPath) throws IOException;

    /**
     * Get status of a Linux filesystem file (file, directory, symbolic link, etc). 
     * @param timeout timeout in milliseconds
     * @param remotePath remote path
     * @return status, or {@code null} if doesn't exist
     * @throws IOException on IO error
     * @throws TimeLimitExceededException if timed out
     * @throws IllegalArgumentException if any of the following conditions are NOT met:
     * {@code timeout >= 0L},
     * {@code !remotePath.isEmpty()}
     * @throws NullPointerException if any argument is {@code null}
     */
    StatResult stat(long timeout, String remotePath) throws IOException;
    

    /**
     * Delete a file.
     * @param timeout timeout in milliseconds
     * @param remotePath remote file path
     * @throws IOException on IO error, or if {@code remotePath} doesn't exist, or if {@code remotePath} not a regular file
     * @throws TimeLimitExceededException if timed out
     * @throws IllegalArgumentException if any of the following conditions are NOT met:
     * {@code timeout >= 0L},
     * {@code !remotePath.isEmpty()}
     * @throws NullPointerException if any argument is {@code null}
     */
    void delete(long timeout, String remotePath) throws IOException;

    /**
     * Execute a bash script with root permission.
     * <p>
     * This variant of the execute method pipes stdout and stderr streams to their respective {@link Writer}s.
     * <p>
     * Script arguments are automatically escaped.
     * @param timeout timeout in milliseconds
     * @param expectedBootTime expected boot time in seconds since Unix epoch, or {@code -1L} if unknown
     * @param stdoutWriter writer to pipe stdout to
     * @param stdoutLimit number of characters to pipe from stdout must be less than this amount
     * @param stderrWriter writer to pipe stdout to
     * @param stderrLimit number of characters to pipe from stderr must be less than this amount
     * @param script script to execute
     * @param scriptArgs arguments for script
     * @return execution result
     * @throws IOException on IO error
     * @throws BootTimeChangedException if {@code expectedBootTime} is different than host's boot time
     * @throws StreamLimitExceededException if stdout/stderr limit is hit
     * @throws TimeLimitExceededException if timed out
     * @throws IllegalArgumentException if any of the following conditions are NOT met:
     * {@code timeout >= 0L},
     * {@code expectedBootTime == -1L || expectedBootTime >= 0L},
     * {@code stdoutLimit >= 0L},
     * {@code stderrLimit >= 0L}
     * @throws NullPointerException if any argument is {@code null}
     */
    ExecuteResult execute(
            long timeout, long expectedBootTime,
            Writer stdoutWriter, long stdoutLimit,
            Writer stderrWriter, long stderrLimit,
            String script, String... scriptArgs) throws IOException;

    /**
     * Execute a bash script with root permission.
     * <p>
     * This variant of the execute method doesn't limit output streams. This is because stdout and stderr get buffered to a file which
     * can be read back out using this method's return value. Remember to call {@link BufferedExecuteResult#close()} to release/delete
     * buffer files when you finish reading.
     * <p>
     * Script arguments are automatically escaped.
     * @param timeout timeout in milliseconds
     * @param expectedBootTime expected boot time in seconds since Unix epoch, or {@code -1L} if unknown
     * @param script script to execute
     * @param scriptArgs arguments for script
     * @return execution result
     * @throws IOException on IO error
     * @throws BootTimeChangedException if {@code expectedBootTime} is different than host's boot time
     * @throws TimeLimitExceededException if timed out
     * @throws IllegalArgumentException if any of the following conditions are NOT met:
     * {@code timeout >= 0L},
     * {@code expectedBootTime == -1L || expectedBootTime >= 0L}
     * @throws NullPointerException if any argument is {@code null}
     */
    default BufferedExecuteResult executeBuffered(
            long timeout, long expectedBootTime,
            String script, String... scriptArgs) throws IOException {
        Validate.notNull(script);
        Validate.notNull(scriptArgs);
        Validate.noNullElements(scriptArgs);
        Validate.isTrue(timeout >= 0L);
        Validate.isTrue(expectedBootTime == -1L || expectedBootTime >= 0L);

        Path stdoutPath = Files.createTempFile("comm", "stdout");
        Path stderrPath = Files.createTempFile("comm", "stderr");

        FileChannel stdoutFc = null;
        FileChannel stderrFc = null;
        try {
            // create stdout/stderr buffer files
            stdoutFc = FileChannel.open(stdoutPath, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
            stderrFc = FileChannel.open(stderrPath, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
            stdoutFc.truncate(0L); // just incase
            stderrFc.truncate(0L); // just incase
            
            // create writers linked to stdout/stderr file buffers
            OutputStream stdoutOs = Channels.newOutputStream(stdoutFc);
            OutputStreamWriter stdoutWriter = new OutputStreamWriter(stdoutOs, UTF_8);
            
            OutputStream stderrOs = Channels.newOutputStream(stderrFc);
            OutputStreamWriter stderrWriter = new OutputStreamWriter(stderrOs, UTF_8);

            // execute script and dump stdout/stderr to file buffers
            ExecuteResult execRes = execute(
                    timeout, expectedBootTime,
                    stdoutWriter, Long.MAX_VALUE,
                    stderrWriter, Long.MAX_VALUE,
                    script, scriptArgs);
            
            // flush writers to make sure disk has everything
            stdoutWriter.flush();
            stdoutFc.force(true);
            stderrWriter.flush();
            stderrFc.force(true);
            
            // set stdout/stderr buffer file pointers to 0 (for reading from beginning)
            stdoutFc.position(0L);
            stderrFc.position(0L);

            // create readers linked to stdout/stderr file buffers
            InputStream stdoutIs = Channels.newInputStream(stdoutFc);
            InputStreamReader stdoutReader = new InputStreamReader(stdoutIs, UTF_8);

            InputStream stderrIs = Channels.newInputStream(stderrFc);
            InputStreamReader stderrReader = new InputStreamReader(stderrIs, UTF_8);

            return new BufferedExecuteResult(
                    stdoutPath, stdoutReader,
                    stderrPath, stderrReader,
                    execRes.getExitCode(), execRes.getBootTime());
        } catch (RuntimeException | IOException e) {
            // once the channels are closed, any streams on those channels will be implictly closed as well
            IOUtils.closeQuietly(stdoutFc);
            IOUtils.closeQuietly(stderrFc);
            throw e;
        }

    }

    /**
     * Execute a bash script with root permission.
     * <p>
     * This variant of the execute method buffers streams in memory.
     * <p>
     * Script arguments are automatically escaped.
     * @param timeout timeout in milliseconds
     * @param expectedBootTime expected boot time in seconds since Unix epoch, or {@code -1L} if unknown
     * @param stdoutLimit number of characters to pipe from stdout must be less than this amount
     * @param stderrLimit number of characters to pipe from stderr must be less than this amount
     * @param script script to execute
     * @param scriptArgs arguments for script
     * @return execution result
     * @throws IOException on IO error
     * @throws BootTimeChangedException if {@code expectedBootTime} is different than host's boot time
     * @throws StreamLimitExceededException if stdout/stderr limit is hit
     * @throws TimeLimitExceededException if timed out
     * @throws IllegalArgumentException if any of the following conditions are NOT met:
     * {@code !Arrays.asList(scriptArgs).contains(null)},
     * {@code timeout >= 0L},
     * {@code expectedBootTime == -1L || expectedBootTime >= 0L},
     * {@code stdoutLimit >= 0L},
     * {@code stderrLimit >= 0L}
     * @throws NullPointerException if any argument is {@code null}
     */
    default InMemoryExecuteResult execute(
            long timeout, long expectedBootTime,
            long stdoutLimit, long stderrLimit,
            String script, String... scriptArgs) throws IOException {
        Validate.notNull(script);
        Validate.notNull(scriptArgs);
        Validate.noNullElements(scriptArgs);
        Validate.isTrue(timeout >= 0L);
        Validate.isTrue(expectedBootTime == -1L || expectedBootTime >= 0L);
        Validate.isTrue(stdoutLimit >= 0L);
        Validate.isTrue(stderrLimit >= 0L);

        try (StringWriter stdoutWriter = new StringWriter();
                StringWriter stderrWriter = new StringWriter()) {
            ExecuteResult execRes = execute(
                    timeout, expectedBootTime,
                    stdoutWriter, stdoutLimit,
                    stderrWriter, stderrLimit,
                    script, scriptArgs);
            return new InMemoryExecuteResult(
                    stdoutWriter.toString(),
                    stderrWriter.toString(),
                    execRes.getExitCode(),
                    execRes.getBootTime());
        }
    }

    /**
     * Unsafely execute a bash script with root permission.
     * <p>
     * This variant of the execute method buffers streams in memory with no (practical) limit on stream size. It also has no (practical)
     * timeout and doesn't test the host's boot time. Equivalent to calling
     * {@code execute(Long.MAX_VALUE, -1L, Long.MAX_VALUE, Long.MAX_VALUE, script, scriptArgs)}.
     * <p>
     * Script arguments are automatically escaped.
     * @param script script to execute
     * @param scriptArgs arguments for script
     * @return execution result
     * @throws IOException on IO error
     * @throws IllegalArgumentException if any of the following conditions are NOT met:
     * {@code !Arrays.asList(scriptArgs).contains(null)}
     * @throws NullPointerException if any argument is {@code null}
     */
    default InMemoryExecuteResult executeUnsafe(String script, String... scriptArgs) throws IOException {
        return execute(Long.MAX_VALUE, -1L, Long.MAX_VALUE, Long.MAX_VALUE, script, scriptArgs);
    }
}
