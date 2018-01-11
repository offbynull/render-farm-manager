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
package com.offbynull.rfm.gateways.ssh;

import com.offbynull.actors.address.Address;
import com.offbynull.actors.gateways.threadpool.ThreadPoolProcessor;
import com.offbynull.actors.shuttle.Message;
import com.offbynull.actors.shuttle.Shuttle;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.xfer.InMemorySourceFile;
import net.schmizz.sshj.xfer.scp.SCPFileTransfer;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SshProcessor implements ThreadPoolProcessor {
    
    private static final Logger LOG = LoggerFactory.getLogger(SshProcessor.class);
    
    private static final String HOSTNAME;
    private static final Random RANDOM;
    static {
        String hostname;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ex) {
            hostname = "nohost";
            LOG.warn("Failed to get hostname", ex);
        }
        HOSTNAME = hostname;
        
        Random random;
        try {
            random = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException ex) {
            random = new Random();
            LOG.warn("Failed to get secure random", ex);
        }
        RANDOM = random;
    }
    
    @Override
    public void process(Message request, ConcurrentHashMap<String, Shuttle> outShuttles) throws Exception {
        Validate.notNull(request);
        Validate.notNull(outShuttles);

        SshRequestMessage payload = (SshRequestMessage) request.getMessage();
        Address srcAddr = request.getSourceAddress();
        Address dstAddr = request.getDestinationAddress();
        String srcPrefix = srcAddr.getElement(0);
        
        Message response;
        
        try (SSHClient ssh = new SSHClient()) {
            String host = payload.getAddress();
            int port = payload.getPort();
            String username = payload.getUsername();
            String password = payload.getPassword();
            String script = payload.getScript();
            long timeout = payload.getTimeout();
            
            
            
            ssh.addHostKeyVerifier((h, p, k) -> true);
            
            ssh.connect(host, port);
            ssh.authPassword(username, password);
            
            LOG.debug("Connected to {}:{}", host, port);
            
            String dstFile = "/tmp/" + HOSTNAME + "_" + System.currentTimeMillis() + "_" + RANDOM.nextLong();
            SCPFileTransfer transfer = ssh.newSCPFileTransfer();
            transfer.upload(new ScriptInMemorySourceFile(script), dstFile);
            LOG.debug("Uploaded {} to {}:{}", dstFile, host, port);
            
            Session session = ssh.startSession();
            Command cmd = session.exec(dstFile);
            LOG.debug("Executing {} on {}:{}", dstFile, host, port);
            
            String stdout = IOUtils.toString(cmd.getInputStream(), UTF_8);
            String stderr = IOUtils.toString(cmd.getErrorStream(), UTF_8);
            cmd.join(timeout, TimeUnit.SECONDS);
            cmd.close(); // must call close before getExitStatus?
            Integer exit = cmd.getExitStatus();
            if (exit == null) {
                throw new IllegalStateException("No exit code returned");
            }

            LOG.debug("Finished {} on {}:{} with exit code {}", dstFile, host, port, exit);
            
            
            response = new Message(dstAddr, srcAddr, new SshResponseMessage(stdout, stderr, exit));
        } catch (IOException | RuntimeException e) {
            LOG.error("Failed to run ssh script", e);
            response = new Message(dstAddr, srcAddr, new SshErrorMessage(e.toString()));
        }
        
        
        Shuttle outShuttle = outShuttles.get(srcPrefix);
        if (outShuttle != null) {
            outShuttle.send(response);
        }
    }
    
    private static final class ScriptInMemorySourceFile extends InMemorySourceFile {
        
        private final byte[] data;
        
        ScriptInMemorySourceFile(String script) {
            Validate.notNull(script);
            data = script.getBytes(UTF_8);
        }

        @Override
        public String getName() {
            return "script";
        }

        @Override
        public long getLength() {
            return data.length;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(data);
        }

        @Override
        public int getPermissions() throws IOException {
            return 0744;
        }
    }
}
