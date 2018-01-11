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

import com.offbynull.rfm.host.communicator.BootTimeChangedException;
import com.offbynull.rfm.host.communicator.StatResult;
import java.io.IOException;
import static java.lang.String.format;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.connection.channel.direct.Signal;
import net.schmizz.sshj.sftp.FileAttributes;
import net.schmizz.sshj.sftp.RemoteFile;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import org.apache.commons.lang3.Validate;
import org.apache.commons.text.RandomStringGenerator;
import com.offbynull.rfm.host.communicator.Communicator;
import com.offbynull.rfm.host.communicator.ExecuteResult;
import com.offbynull.rfm.host.communicator.InMemoryExecuteResult;
import com.offbynull.rfm.host.communicator.StreamLimitExceededException;
import com.offbynull.rfm.host.communicator.TimeLimitExceededException;
import static com.offbynull.rfm.host.communicators.sshj.InternalUtils.readLine;
import static com.offbynull.rfm.host.communicators.sshj.InternalUtils.writeLine;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.net.InetAddress;
import java.net.UnknownHostException;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Set;
import static java.util.stream.Collectors.joining;
import net.schmizz.sshj.sftp.OpenMode;
import org.apache.commons.collections4.set.UnmodifiableSet;
import static org.apache.commons.collections4.set.UnmodifiableSet.unmodifiableSet;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import static org.apache.commons.io.IOUtils.closeQuietly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Low-level Linux host communicator using SSH (via SSHJ).
 * @author Kasra Faghihi
 */
public final class SshjCommunicator implements Communicator {
    
    private static final Logger logger = LoggerFactory.getLogger(SshjCommunicator.class);

    private static final SingletonTimer singletonTimer = new SingletonTimer();

    private static final String PRIME_SCRIPT;

    private static final String ROOT_RUN_SCRIPT;
    private static final String ROOT_RUN_YES_ROOT = "YesRoot";
    private static final String ROOT_RUN_NO_ROOT = "NoRoot";
    private static final String ROOT_RUN_SUDO_PROMPT = "SudoPasswd";
    private static final String ROOT_RUN_ROOT_OK = "Ok";
    private static final String ROOT_RUN_BOOT_UPDATE = "BootUpdate";
    static {
        try {
            PRIME_SCRIPT = IOUtils.toString(SshjCommunicator.class.getResourceAsStream("prime.sh"), UTF_8);
            ROOT_RUN_SCRIPT = IOUtils.toString(SshjCommunicator.class.getResourceAsStream("root_run.sh"), UTF_8);
        } catch (IOException ioe) {
            throw new IllegalStateException(ioe);
        }
    }

    private final Object lock;
    private final Set<Closeable> activeCloseables;
    private boolean closed;

    private final RandomStringGenerator safeRandomStringGen;
    private final String safeHostname;

    private final SshHost host;
    private final SshCredential cred;

    /**
     * Constructs a {@link SshjCommunicator} instance. Equivalent to calling {@code new SshjCommunicator(new Random(), host, cred)}.
     * @param host SSH host
     * @param cred SSH credentials
     * @throws NullPointerException of my argument is {@code null}
     */
    public SshjCommunicator(SshHost host, SshCredential cred) {
        this(new Random(), host, cred);
    }

    /**
     * Constructs a {@link SshjCommunicator} instance.
     * @param random random number generator
     * @param host SSH host
     * @param cred SSH credentials
     * @throws IllegalStateException if this machine doesn't have a local hostname
     * @throws NullPointerException of my argument is {@code null}
     */
    public SshjCommunicator(Random random, SshHost host, SshCredential cred) {
        Validate.notNull(random);
        Validate.notNull(host);
        Validate.notNull(cred);

        try {
            this.lock = new Object();
            this.activeCloseables = Collections.newSetFromMap(new IdentityHashMap<>());

            this.safeRandomStringGen = createRsg(random);
            this.safeHostname = InetAddress.getLocalHost().getCanonicalHostName().replaceAll("[^a-zA-Z0-9]", "_");
            
            this.host = host;
            this.cred = cred;
        } catch (UnknownHostException uhe) {
            throw new IllegalStateException(uhe);
        }
    }
    
    private static RandomStringGenerator createRsg(Random random) {
        return new RandomStringGenerator.Builder()
                .usingRandom(max -> random.nextInt(max))
                .withinRange(new char[][]{{'a', 'z'}, {'A', 'Z'}, {'0', '9'}})
                .build();
    }

    @Override
    public void upload(long timeout, byte[] localData, int localOffset, String remotePath, long remoteOffset, int len) throws IOException {
        Validate.notNull(localData);
        Validate.notNull(remotePath);
        Validate.isTrue(timeout >= 0L);
        Validate.isTrue(localOffset >= 0);
        Validate.isTrue(remoteOffset >= 0L);
        Validate.isTrue(len >= 0);
        Math.addExact(localOffset, len);   // check for rollover
        Math.addExact(remoteOffset, len);  // check for rollover
        Validate.isTrue(localOffset + len <= localData.length);
        Validate.notBlank(remotePath);

        try (MDC.MDCCloseable mdc1 = MDC.putCloseable("host", host.toString());
                MDC.MDCCloseable mdc2 = MDC.putCloseable("user", cred.getUser());
                MDC.MDCCloseable mdc3 = MDC.putCloseable("inst", safeRandomStringGen.generate(8))) {
            logger.debug("Upload timeout:{} localData:{} localOffset:{}, remotePath:{}, remoteOffset:{}, len:{}",
                    localData, localOffset, remotePath, remoteOffset, len);
            wrap(timeout, (ssh) -> {
                rawUpload(ssh, localData, localOffset, remotePath, remoteOffset, len);
                return null;
            });
            logger.debug("Uploaded");
        }
    }

    @Override
    public void upload(long timeout, byte[] localData, String remotePath) throws IOException {
        Validate.notNull(localData);
        Validate.notNull(remotePath);
        Validate.isTrue(timeout >= 0L);
        Validate.notBlank(remotePath);

        try (MDC.MDCCloseable mdc1 = MDC.putCloseable("host", host.toString());
                MDC.MDCCloseable mdc2 = MDC.putCloseable("user", cred.getUser());
                MDC.MDCCloseable mdc3 = MDC.putCloseable("inst", safeRandomStringGen.generate(8))) {
            logger.debug("Upload timeout:{} localData:{} remotePath:{}", timeout, localData, remotePath);
            wrap(timeout, (ssh) -> {
                rawUpload(ssh, localData, remotePath);
                return null;
            });
            logger.debug("Uploaded");
        }
    }

    @Override
    public void upload(long timeout, String localPath, String remotePath) throws IOException {
        Validate.notNull(localPath);
        Validate.notNull(remotePath);
        Validate.isTrue(timeout >= 0L);
        Validate.notBlank(localPath);
        Validate.notBlank(remotePath);

        try (MDC.MDCCloseable mdc1 = MDC.putCloseable("host", host.toString());
                MDC.MDCCloseable mdc2 = MDC.putCloseable("user", cred.getUser());
                MDC.MDCCloseable mdc3 = MDC.putCloseable("inst", safeRandomStringGen.generate(8))) {
            logger.debug("Upload timeout:{} localPath:{} remotePath:{}", timeout, localPath, remotePath);
            wrap(timeout, (ssh) -> {
                rawUpload(ssh, localPath, remotePath);
                return null;
            });
            logger.debug("Uploaded");
        }
    }

    @Override
    public void pipe(long timeout, String remotePath, long remoteOffset, OutputStream os, long limit) throws IOException {
        Validate.notNull(remotePath);
        Validate.notNull(os);
        Validate.isTrue(timeout >= 0L);
        Validate.isTrue(remoteOffset >= 0L);
        Validate.isTrue(limit >= 0L);
        Validate.notBlank(remotePath);
        Math.addExact(remoteOffset, limit);  // check for rollover

        try (MDC.MDCCloseable mdc1 = MDC.putCloseable("host", host.toString());
                MDC.MDCCloseable mdc2 = MDC.putCloseable("user", cred.getUser());
                MDC.MDCCloseable mdc3 = MDC.putCloseable("inst", safeRandomStringGen.generate(8))) {
            logger.debug("Piping timeout:{} remotePath:{} remoteOffset:{} limit:{}", timeout, remotePath, remoteOffset, limit);
            wrap(
                    timeout,
                    (ssh) -> {
                        rawPipe(ssh, remotePath, remoteOffset, os, limit);
                        return null;
                    },
                    () -> {
                        // close os on timeout, done to get the method to exit as quickly as possible (incase in middle of slow write)
                        closeQuietly(os);
                    });
            logger.debug("Piped");
        }
    }
    
    @Override
    public byte[] download(long timeout, String remotePath, long remoteOffset, int len) throws IOException {
        Validate.notNull(remotePath);
        Validate.isTrue(timeout >= 0L);
        Validate.isTrue(remoteOffset >= 0L);
        Validate.isTrue(len >= 0);
        Validate.notBlank(remotePath);
        Math.addExact(remoteOffset, len);  // check for rollover
        
        try (MDC.MDCCloseable mdc1 = MDC.putCloseable("host", host.toString());
                MDC.MDCCloseable mdc2 = MDC.putCloseable("user", cred.getUser());
                MDC.MDCCloseable mdc3 = MDC.putCloseable("inst", safeRandomStringGen.generate(8))) {
            logger.debug("Download timeout:{} remotePath:{} remoteOffset:{} len:{}", timeout, remotePath, remoteOffset, len);
            byte[] ret = wrap(timeout, (ssh) -> rawDownload(ssh, remotePath, remoteOffset, len));
            logger.debug("Downloaded:{}", ret);
            
            return ret;
        }
    }

    @Override
    public byte[] download(long timeout, String remotePath) throws IOException {
        Validate.notNull(remotePath);
        Validate.isTrue(timeout >= 0L);
        Validate.notBlank(remotePath);
        
        try (MDC.MDCCloseable mdc1 = MDC.putCloseable("host", host.toString());
                MDC.MDCCloseable mdc2 = MDC.putCloseable("user", cred.getUser());
                MDC.MDCCloseable mdc3 = MDC.putCloseable("inst", safeRandomStringGen.generate(8))) {
            logger.debug("Download timeout:{} remotePath:{}", timeout, remotePath);
            byte[] ret = wrap(timeout, (ssh) -> rawDownload(ssh, remotePath));
            logger.debug("Downloaded:{}", ret);
            
            return ret;
        }
    }

    @Override
    public void download(long timeout, String remotePath, String localPath) throws IOException {
        Validate.notNull(remotePath);
        Validate.notNull(localPath);
        Validate.isTrue(timeout >= 0L);
        Validate.notBlank(remotePath);
        Validate.notBlank(localPath);
        
        try (MDC.MDCCloseable mdc1 = MDC.putCloseable("host", host.toString());
                MDC.MDCCloseable mdc2 = MDC.putCloseable("user", cred.getUser());
                MDC.MDCCloseable mdc3 = MDC.putCloseable("inst", safeRandomStringGen.generate(8))) {
            logger.debug("Download timeout:{} remotePath:{}, localPath:{}", timeout, remotePath, localPath);
            wrap(timeout, (ssh) -> {
                rawDownload(ssh, remotePath, localPath);
                return null;
            });
            logger.debug("Downloaded");
        }
    }

    @Override
    public StatResult stat(long timeout, String remotePath) throws IOException {
        Validate.notNull(remotePath);
        Validate.isTrue(timeout >= 0L);
        Validate.notBlank(remotePath);
        
        try (MDC.MDCCloseable mdc1 = MDC.putCloseable("host", this.host.toString());
                MDC.MDCCloseable mdc2 = MDC.putCloseable("user", this.cred.getUser())) {
            logger.debug("Stat timeout:{} remotePath:{}", timeout, remotePath);
            StatResult ret = wrap(timeout, (ssh) -> rawStat(ssh, remotePath));
            logger.debug("Stat'd:{}", ret);
            
            return ret;
        }
    }

    @Override
    public void delete(long timeout, String remotePath) throws IOException {
        Validate.notNull(remotePath);
        Validate.isTrue(timeout >= 0L);
        Validate.notBlank(remotePath);
        
        try (MDC.MDCCloseable mdc1 = MDC.putCloseable("host", host.toString());
                MDC.MDCCloseable mdc2 = MDC.putCloseable("user", cred.getUser());
                MDC.MDCCloseable mdc3 = MDC.putCloseable("inst", safeRandomStringGen.generate(8))) {
            logger.debug("Delete timeout:{} remotePath:{}", timeout, remotePath);
            wrap(timeout, (ssh) -> {
                rawDelete(ssh, remotePath);
                return null;
            });
            logger.debug("Deleted");
        }
    }

    @Override
    public ExecuteResult execute(
            long timeout, long expectedBootTime,
            Writer stdoutWriter, long stdoutLimit,
            Writer stderrWriter, long stderrLimit,
            String script, String... scriptArgs) throws IOException {
        Validate.notNull(script);
        Validate.notNull(scriptArgs);
        Validate.notNull(stdoutWriter);
        Validate.notNull(stderrWriter);
        Validate.noNullElements(scriptArgs);
        Validate.isTrue(timeout >= 0L);
        Validate.isTrue(expectedBootTime == -1L || expectedBootTime >= 0L);
        
        try (MDC.MDCCloseable mdc1 = MDC.putCloseable("host", host.toString());
                MDC.MDCCloseable mdc2 = MDC.putCloseable("user", cred.getUser());
                MDC.MDCCloseable mdc3 = MDC.putCloseable("inst", safeRandomStringGen.generate(8))) {
            logger.debug("Execute timeout:{} expectedBootTime:{} stdoutLimit:{} stderrLimit:{} script:{} scriptArgs:{}",
                    timeout, expectedBootTime, stdoutLimit, stderrLimit, script, scriptArgs);
            ExecuteResult ret = wrap(timeout, (ssh) -> rawExecute(ssh, expectedBootTime, stdoutWriter, stdoutLimit, stderrWriter,
                    stderrLimit, script, scriptArgs));
            logger.debug("Executed:{}", ret);
            
            return ret;
        }
    }

    /**
     * Prime host.
     * <p>
     * Priming a host sets up the root user with a new SSH key pair, enables SSH via key pairs for root user, and retrieves host
     * fingerprints. New hosts should always be primed.
     * <p>
     * Any SSH keys generated by previous invocations of this operation will be removed.
     * @param timeout timeout in milliseconds
     * @param expectedBootTime expected boot time in seconds since Unix epoch, or {@code -1L} if unknown
     * @return prime output
     * @throws IOException on IO error
     * @throws BootTimeChangedException if {@code expectedBootTime} is different than host's boot time
     * @throws StreamLimitExceededException if stdout and/or stderr consumes more than 32K chars
     * @throws TimeLimitExceededException if timed out
     * @throws IllegalStateException on script output parse error
     */
    public PrimeResult prime(long timeout, long expectedBootTime) throws IOException {
        logger.debug("Prime timeout:{} expectedBootTime:{}", timeout, expectedBootTime);

        InMemoryExecuteResult res = execute(timeout, -1L, 32L*1024L, 32L*1024L, PRIME_SCRIPT);
        if (res.getExitCode() != 0) {
            throw new IOException("Bad exit code\n" + res.getStderr());
        }

        try (StringReader sr = new StringReader(res.getStdout());
                BufferedReader br = new BufferedReader(sr)) {
            String[] fingerprints = null;
            String privateKey = null;
            String publicKey = null;
            String keyPassword = null;

            while (true) {
                String markerStr = br.readLine();
                if (markerStr == null) {
                    break;
                }

                if (!markerStr.startsWith("!")) {
                    throw new IllegalArgumentException();
                }

                String lengthStr = br.readLine();

                long length = Long.parseLong(lengthStr);
                StringBuilder outputStr = new StringBuilder();
                for (long i = 0; i < length; i++) {
                    String line = br.readLine();
                    outputStr.append(line);
                    if (i < length - 1) {
                        outputStr.append('\n');
                    }
                }

                switch (markerStr) {
                    case "!FINGERPRINTS":
                        Validate.isTrue(fingerprints == null, "Duplicate fingerprints entry");
                        fingerprints = outputStr.toString().split("[\n]+");
                        break;
                    case "!KEYPRIV":
                        Validate.isTrue(privateKey == null, "Duplicate private key entry");
                        privateKey = outputStr.toString();
                        break;
                    case "!KEYPUB":
                        Validate.isTrue(publicKey == null, "Duplicate public key entry");
                        publicKey = outputStr.toString();
                        break;
                    case "!KEYPASSWORD":
                        Validate.isTrue(keyPassword == null, "Duplicate private key password entry");
                        keyPassword = outputStr.toString();
                        break;
                    default:
                        logger.debug("Unrecognized marker {}", markerStr);
                        break;
                }
            }

            PrimeResult ret = new PrimeResult(Arrays.asList(fingerprints), privateKey, publicKey, keyPassword);
            logger.debug("Primed:{}", ret);
            
            return ret;
        } catch (RuntimeException re) {
            throw new IllegalStateException("Parse error", re);
        }
    }

    @Override
    public void close() throws IOException {
        logger.trace("Closing");
        synchronized (lock) {
            if (closed) {
                return;
            }
            closed = true;

            activeCloseables.forEach((closeable) -> {
                try {
                    closeable.close();
                } catch (IOException ioe) {
                    logger.warn("Failed to close resource", ioe);
                }
            });
        }
    }




    private <T> T wrap(long timeout, SSHTimeoutFunction<T> runLogic) throws IOException {
        return wrap(timeout, runLogic, () -> {});
    }
    
    private <T> T wrap(long timeout, SSHTimeoutFunction<T> runLogic, Runnable additionalTimeoutLogic) throws IOException {
        AtomicBoolean timeoutFlag = new AtomicBoolean();        
        SSHClient ssh = null;
        
        singletonTimer.addReference();
        try {
            synchronized (lock) {
                if (closed) {
                    throw new IOException("Closed");
                }

                ssh = new SSHClient();
                activeCloseables.add(ssh);
            }

            initTimeout(ssh, timeout, timeoutFlag);
            connect(ssh);

            return runLogic.apply(ssh);
        } catch (RuntimeException | IOException e) {
            if (e instanceof IOException && timeoutFlag.get() == true) {
                logger.debug("Timed out");
                
                try {
                    additionalTimeoutLogic.run();
                } catch (RuntimeException re) {
                    logger.warn("Error running additional timeout logic", re);
                }
                
                throw new TimeLimitExceededException(e);
            } else {
                logger.debug("Exception", e);
                throw e;
            }
        } finally {
            logger.trace("Removing timer");
            singletonTimer.removeReference();
            logger.trace("Removed timer");

            if (ssh != null) {
                ssh.close();
                synchronized (lock) {
                    // if another thread closed obj while method was running, throw exc (regardless of success or failure), the ssh client
                    // will have already been closed in this object's close() method.
                    if (closed) {
                        throw new IOException("Closed");
                    }
                    activeCloseables.remove(ssh);
                }
            }
        }
    }
    
    private interface SSHTimeoutFunction<T> {
        T apply(SSHClient ssh) throws IOException;
    }

    private void initTimeout(SSHClient ssh, long timeout, AtomicBoolean timeoutFlag) throws IOException {
        // Set disconnect timer
        logger.trace("Adding timer:{} millis", timeout);
        singletonTimer.schedule(() -> {
            timeoutFlag.set(true);
            try {
                ssh.disconnect();
            } catch (IOException e) {
                // swallow exception -- do nothing
            }
        }, timeout, TimeUnit.MILLISECONDS);
        logger.trace("Added timer");
    }

    private void connect(SSHClient ssh) throws IOException {
        // Add host fingerprints
        if (host.getFingerprints() == null) {
            ssh.addHostKeyVerifier(new PromiscuousVerifier());
        } else {
            host.getFingerprints().forEach(f -> ssh.addHostKeyVerifier(f));
        }

        // Connect
        logger.trace("Connecting");
        ssh.connect(host.getHost(), host.getPort());
        logger.trace("Connected");

        // Authenticate
        if (cred.getKey() != null) {
            logger.trace("Authenticating using pubpriv");
            KeyProvider keyProvider = ssh.loadKeys(
                    cred.getKey().getPrivateKey(),
                    cred.getKey().getPublicKey(),
                    new CustomPasswordFinder(cred.getKey().getPassword()));
            ssh.authPublickey(cred.getUser(), keyProvider);
        } else if (cred.getPassword() != null) {
            logger.trace("Authenticating using password");
            ssh.authPassword(cred.getUser(), cred.getPassword());
        }
        logger.trace("Authenticated");
    }



    private static final UnmodifiableSet<OpenMode> CREATE_WRITE_MODE;
    static {
        Set<OpenMode> openModes = new HashSet<>();
        openModes.add(OpenMode.CREAT);
        openModes.add(OpenMode.WRITE);
        CREATE_WRITE_MODE = (UnmodifiableSet<OpenMode>) unmodifiableSet(openModes);
    }

    private void rawUpload(SSHClient ssh, byte[] localData, int localOffset, String remotePath, long remoteOffset, int len) throws IOException {
        try (SFTPClient sftp = ssh.newSFTPClient();
                RemoteFile remoteFile = sftp.open(remotePath, CREATE_WRITE_MODE)) {
            remoteFile.write(remoteOffset, localData, localOffset, len);
        }
    }

    private void rawUpload(SSHClient ssh, byte[] localData, String remotePath) throws IOException {
        try (SFTPClient sftp = ssh.newSFTPClient()) {
            String name = FilenameUtils.getName(remotePath);

            CustomInMemorySourceFile srcFile = new CustomInMemorySourceFile(name, localData);
            sftp.getFileTransfer().upload(srcFile, remotePath);
        }
    }

    private void rawUpload(SSHClient ssh, String localPath, String remotePath) throws IOException {
        try (SFTPClient sftp = ssh.newSFTPClient()) {
            sftp.getFileTransfer().upload(localPath, remotePath);
        }
    }
    
    private static final int PIPE_BLOCK_SIZE = 64*1024; // SSHJ seems to limit to this size, even if we specify larger
    private void rawPipe(SSHClient ssh, String remotePath, long remoteOffset, OutputStream os, long limit) throws IOException {
        try (SFTPClient sftp = ssh.newSFTPClient();
                RemoteFile remoteFile = sftp.open(remotePath)) {
            if (limit == 0) {
                return;
            }

            // This isn't the most efficient way of doing this, but it's the most obvious. In the future, you can have multiple buffers of
            // size PIPE_BLOCK_SIZE. One thread can read into the buffers, while the other thread can write the buffers out to the stream.
            //
            // Assuming reading and writing are happening at relatively the same speed, multiple buffers/threads would be a good solution.
            
            byte []temp = new byte[PIPE_BLOCK_SIZE];
            while (true) {
                int readLen = (int) Math.min(limit, PIPE_BLOCK_SIZE);
                int dataLen = remoteFile.read(remoteOffset, temp, 0, readLen);
                os.write(temp, 0, dataLen);
                
                remoteOffset += dataLen;
                
                limit -= dataLen;
                if (dataLen == 0L || limit == 0L) { // if nothing left to read OR limit was hit
                    break;
                }
            }
        } finally {
            closeQuietly(os);
        }
    }

    private byte[] rawDownload(SSHClient ssh, String remotePath, long remoteOffset, int len) throws IOException {
        try (SFTPClient sftp = ssh.newSFTPClient();
                RemoteFile remoteFile = sftp.open(remotePath)) {
            if (len == 0) {
                // This is a special case -- sshj will return -1 if you actually try to read 0 length, regardless of if you read in the file
                // or past the file.
                long fileLen = remoteFile.length();
                if (remoteOffset > fileLen) {
                    throw new IOException("Not enough bytes -- " + fileLen);
                }
                return new byte[0];
            } else {
                byte[] ret = new byte[len];
                int readAmount = remoteFile.read(remoteOffset, ret, 0, len);
                if (readAmount < len) {
                    throw new IOException("Not enough bytes -- " + readAmount + " read but " + len + " expected");
                }
                return ret;
            }
        }
    }

    private byte[] rawDownload(SSHClient ssh, String remotePath) throws IOException {
        try (SFTPClient sftp = ssh.newSFTPClient()) {
            CustomInMemoryDestFile dstFile = new CustomInMemoryDestFile();
            sftp.getFileTransfer().download(remotePath, dstFile);
            return dstFile.toByteArray();
        }
    }

    private void rawDownload(SSHClient ssh, String remotePath, String localFile) throws IOException {
        try (SFTPClient sftp = ssh.newSFTPClient()) {
            sftp.getFileTransfer().download(remotePath, localFile);
        }
    }

    private StatResult rawStat(SSHClient ssh, String remotePath) throws IOException {
        try (SFTPClient sftp = ssh.newSFTPClient()) {
            FileAttributes attrs = sftp.statExistence(remotePath);
            if (attrs == null){
                return null;
            } else {
                return new StatResult(attrs.getUID(), attrs.getGID(), attrs.getSize(), attrs.getMode().getMask());
            }
        }
    }

    private void rawDelete(SSHClient ssh, String remotePath) throws IOException {
        try (SFTPClient sftp = ssh.newSFTPClient()) {
            sftp.rm(remotePath);
        }
    }

    private ExecuteResult rawExecute(SSHClient ssh,
            long expectedBootTime,
            Writer stdoutWriter, long stdoutLimit,
            Writer stderrWriter, long stderrLimit,
            String script, String... scriptArgs) throws IOException {
        Validate.notNull(script);
        Validate.notNull(scriptArgs);
        Validate.notNull(stdoutWriter);
        Validate.notNull(stderrWriter);
        Validate.noNullElements(scriptArgs);
        Validate.isTrue(expectedBootTime == -1L || expectedBootTime >= 0L);
        Validate.isTrue(stdoutLimit >= 0L);
        Validate.isTrue(stderrLimit >= 0L);

        String rootRunDstPath = "/tmp/" + safeHostname + safeRandomStringGen.generate(8);
        String scriptDstPath = "/tmp/" + safeHostname + safeRandomStringGen.generate(8);
        String scriptRunDstPath = "/tmp/" + safeHostname + safeRandomStringGen.generate(8);

        // Send scripts
        logger.trace("Uploading root run script to {}", rootRunDstPath);
        rawUpload(ssh, ROOT_RUN_SCRIPT.getBytes(UTF_8), rootRunDstPath);
        
        logger.trace("Uploading final script to {}", scriptDstPath);
        rawUpload(ssh, script.getBytes(UTF_8), scriptDstPath);
        
        String scriptRun = "/bin/bash " + scriptDstPath + " "
                + Arrays.stream(scriptArgs).map(a -> "'" + a.replace("'", "'\\''") + "'").collect(joining(" "));
        logger.trace("Uploading script run script to {} -- {}", scriptRunDstPath, scriptRun);
        rawUpload(ssh, scriptRun.getBytes(UTF_8), scriptRunDstPath);

        // Run script as root
        String commandStr = "/bin/bash " + rootRunDstPath + " " + expectedBootTime + " /bin/bash " + scriptRunDstPath;
        ReaderWriterPiper stdoutPiper = null;
        ReaderWriterPiper stderrPiper = null;
        LimitedWriter stdoutLimitedWriter = null;
        LimitedWriter stderrLimitedWriter = null;
        
        logger.trace("Executing command {}", commandStr);
        try (Command command = ssh.startSession().exec(commandStr);
                OutputStream stdinStream = command.getOutputStream();
                InputStream stdoutStream = command.getInputStream();
                InputStream stderrStream = command.getErrorStream();
                Writer stdinWriter = new OutputStreamWriter(stdinStream, command.getRemoteCharset());
                Reader stdoutReader = new InputStreamReader(stdoutStream, command.getRemoteCharset());
                Reader stderrReader = new InputStreamReader(stderrStream, command.getRemoteCharset())) {
            stdoutLimitedWriter = new LimitedWriter(stdoutWriter, stdoutLimit, stdinStream, stdoutStream, stderrStream, command);
            stderrLimitedWriter = new LimitedWriter(stderrWriter, stderrLimit, stdinStream, stdoutStream, stderrStream, command);

            logger.trace("Launching stdout piper");
            stdoutPiper = ReaderWriterPiper.spawn(stdoutReader, stdoutLimitedWriter, "stdout");  // we only interact with stderr to
                                                                                                 // determine if we need to sudo, so start
                                                                                                 // piping stdout immediately... just incase
                                                                                                 // the host starts sending stuff to stdout
                                                                                                 // before we expect it it will be piped
                                                                                                 // rather than being queued up on the host.

            logger.trace("Running root/sudo sequence");
            long newBootTime;
            String line = readLine(stderrReader);
            switch (line) {
                case ROOT_RUN_NO_ROOT: {
                    logger.trace("Not running as root");
                    
                    logger.trace("Reading sudo prompt");
                    line = readLine(stderrReader);
                    if (!ROOT_RUN_SUDO_PROMPT.equals(line)) {
                        throw new IOException(format("Sudo prompt missing: %s", line));
                    }

                    if (cred.getPassword() == null) {
                        throw new IOException(format("Password missing for sudo prompt: %s", line));
                    }

                    logger.trace("Sending root password to sudo");
                    writeLine(stdinWriter, cred.getPassword());
                    
                    line = readLine(stderrReader);
                    if (!ROOT_RUN_YES_ROOT.equals(line)) {
                        throw new IOException(format("Password verification failed: %s", line));
                    }
                    
                    newBootTime = processRootRunResponse(expectedBootTime, stderrReader);
                    break;
                }
                case ROOT_RUN_YES_ROOT: {
                    logger.trace("Already running as root");
                    
                    newBootTime = processRootRunResponse(expectedBootTime, stderrReader);                    
                    break;
                }
                default:
                    throw new IOException(format("Root identifier missing: %s", line));
            }

            logger.trace("Launching stderr piper");
            stderrPiper = ReaderWriterPiper.spawn(stderrReader, stderrLimitedWriter, "stderr");  // we're done with interacting with stderr
                                                                                                 // so start piping that as well now

            logger.trace("Waiting for pipers and command to finish");
            stdoutPiper.join();   // wait until stdout finishes (should finish if now that command finished)
            stderrPiper.join();   // wait until stderr finishes (should finish if now that command finished)
            command.join();       // wait until command finishes

            // Process finished/terminated -- collect data
            Signal signal = command.getExitSignal();
            if (signal != null) {
                throw new IOException(format("Signal termination: %s", signal));
            }

            Integer exitCode = command.getExitStatus();
            if (exitCode == null) {
                throw new IOException(format("Exit code missing"));
            }

            logger.trace("Exit code is {}", exitCode);
            return new ExecuteResult(exitCode, newBootTime);
        } finally {
            // Delete temporary files
            logger.trace("Deleting {}", scriptRunDstPath);
            try {
                rawDelete(ssh, scriptRunDstPath);
            } catch (RuntimeException | IOException ioe) {
                // do nothing
            }
            
            logger.trace("Deleting {}", scriptDstPath);
            try {
                rawDelete(ssh, scriptDstPath);
            } catch (RuntimeException | IOException ioe) {
                // do nothing
            }
            
            logger.trace("Deleting {}", rootRunDstPath);
            try {
                rawDelete(ssh, rootRunDstPath);
            } catch (RuntimeException | IOException ioe) {
                // do nothing
            }
            
            // Close closeable that aren't handled by try-catch-with-resources
              // DO NOT CLOSE THE LIMITEDWRITERS, BECAUSE THAT WILL CLOSE THEIR BACKING WRITERS WHICH THIS OBJECT DOESNT OWN
            IOUtils.closeQuietly(stdoutPiper, stderrPiper);
            
            // Throw exception if limit was hit
            if (stdoutLimitedWriter != null && stdoutLimitedWriter.isLimitHit()) {
                throw new StreamLimitExceededException();
            }
            if (stderrLimitedWriter != null && stderrLimitedWriter.isLimitHit()) {
                throw new StreamLimitExceededException();
            }
        }
    }
    
    private long processRootRunResponse(long expectedBootTime, Reader stderrReader) throws IOException {
        boolean bootTimeUpdateBail = false;
        String line = readLine(stderrReader);
        switch (line) {
            case ROOT_RUN_BOOT_UPDATE: { // bad boot time
                logger.trace("Sudo okay but bad boot time reported");
                bootTimeUpdateBail = true;
                break;
            }
            case ROOT_RUN_ROOT_OK: { // already root
                logger.trace("Sudo okay");
                break;
            }
            default:
                throw new IOException(format("Unrecognized root run response: %s", line));
        }

        long newBootTime;
        line = readLine(stderrReader);
        try {
            newBootTime = Long.parseLong(line.trim());
        } catch (NumberFormatException nfe) {
            throw new IOException("Failed to interpret boot time: " + line, nfe);
        }

        logger.trace("New boot time:{}", newBootTime);
        
        if (bootTimeUpdateBail) {
            throw new BootTimeChangedException(newBootTime);
        } else {
            if (expectedBootTime != -1 && expectedBootTime != newBootTime) {
                // should never happen
                throw new IOException("Passed but reported boottime different from original: " + newBootTime + " vs " + expectedBootTime);
            }
        }

        return newBootTime;
    }

    @Override
    public String toString() {
        return "SshjCommunicator{" + "host=" + host + ", user=" + cred.getUser() + '}';
    }
    
}
