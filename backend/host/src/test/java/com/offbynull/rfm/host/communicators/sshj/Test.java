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

import com.offbynull.rfm.host.communicator.BufferedExecuteResult;
import java.io.IOException;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.nio.file.Paths;
import org.apache.commons.io.IOUtils;
import com.offbynull.rfm.host.communicator.Communicator;
import java.io.Reader;

final class Test {
    public static void main(String[] args) throws Exception {
        try (Communicator comm = new SshjCommunicator(new SshHost("192.168.18.2", 22), new SshCredential("user", "user"))) {
            invoke(comm, "/com/offbynull/rfm/host/executors/communicator/host_prime.sh", "bash /tmp/host_prime.sh");
        }

//        invoke(ssh, "/work_init.sh", "bash /tmp/work_init.sh kasras_work user /opt/rfm/work/kasras_work 0 250000 1000000 134217728 134217728");
//        invoke(ssh, "/work_run.sh", "bash /tmp/work_run.sh kasras_work user /opt/rfm/work/kasras_work stress --cpu 5 --vm 5 --vm-bytes 5M --timeout 20");
//        for (int i = 0; i < 30; i++) {
//            invoke(ssh, "/work_info.sh", "bash /tmp/work_info.sh kasras_work user /opt/rfm/work/kasras_work");
//        }
//        invoke(ssh, "/work_deinit.sh", "bash /tmp/work_deinit.sh kasras_work user /opt/rfm/work/kasras_work /opt/rfm/work/backups");
    }
    
    private static void invoke(Communicator ssh, String script, String command) throws IOException {
        String scriptData = IOUtils.toString(Test.class.getResourceAsStream(script), UTF_8);
        String scriptName = Paths.get(script).getFileName().toString();
        String scriptPath = "/tmp/" + scriptName;
        if (ssh.stat(5000L, scriptPath) != null) {
            ssh.delete(5000L, scriptPath);
        }

        ssh.upload(5000L, scriptData.getBytes(UTF_8), scriptPath);
        
        try (BufferedExecuteResult res = ssh.executeBuffered(50000000L, -1L, command);
                Reader stdoutReader = res.getStdoutReader();
                Reader stderrReader = res.getStderrReader()) {
            System.out.println("CMD:" + command);
            System.out.println("OUT:" + IOUtils.toString(stdoutReader));
            System.out.println("ERR:" + IOUtils.toString(stderrReader));
            System.out.println("EXITCODE:" + res.getExitCode());
            System.out.println("BOOTTIME:" + res.getBootTime());
        }
    }
}
