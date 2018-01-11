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
package com.offbynull.rfm.host.executors.communicator;

import com.offbynull.rfm.host.communicator.Communicator;
import com.offbynull.rfm.host.communicators.sshj.SshCredential;
import com.offbynull.rfm.host.communicators.sshj.SshHost;
import com.offbynull.rfm.host.communicators.sshj.SshjCommunicator;
import com.offbynull.rfm.host.executor.Executor;
import com.offbynull.rfm.host.executor.HostCheckResult;
import com.offbynull.rfm.host.executor.TaskCheckResult;
import com.offbynull.rfm.host.executor.TaskConfiguration;
import com.offbynull.rfm.host.executor.TaskResourceAllocation;
import static java.util.Arrays.asList;
import java.util.HashSet;

final class Test {
    public static void main(String[] args) throws Exception {
        try (Communicator comm = new SshjCommunicator(new SshHost("192.168.18.2", 22), new SshCredential("user", "user"));
                Executor exec = CommunicatorExecutor.create(comm)) {
//            try {
//                exec.stopTask("testTask");
//                exec.deallocateTask("testTask");
//                exec.destroyTask("testTask");
//                System.out.println("Cleared testTask");
//            } catch (TaskIdConflictException e) {
//                // do nothing
//            }
            
            exec.createTask("testTask",
                    new TaskConfiguration(
                            "/opt/rfm/testTask",
                            "user",
                            "stress", "-c" , "1", "-t", "60s" // -c = num of threads / -t = timeout in seconds
                    )
            );

            exec.allocateTask("testTask",
                    new TaskResourceAllocation(
                            new HashSet<>(asList(0L)), // cpu affinity
                            60000L, // 0.6 cpus worth of time
                            128L*1024L*1024L, // memory limit
                            16L*1024L*1024L // disk limit
                    )
            );

            exec.startTask("testTask");
            exec.stopTask("testTask");
            
            exec.reallocateTask("testTask",
                    new TaskResourceAllocation(
                            new HashSet<>(asList(1L)), // cpu affinity
                            20000L, // 0.2 cpus worth of time
                            256L*1024L*1024L, // memory limit
                            32L*1024L*1024L // disk limit
                    )
            );
            exec.startTask("testTask");
            
            while (true) {
                HostCheckResult hostStat = exec.checkHost();
                System.out.println(hostStat);

                TaskCheckResult taskStat = exec.checkTask("testTask");
                System.out.println(taskStat);

                if (taskStat.getSid() == null) {
                    break;
                }
            }

            exec.stopTask("testTask");            
            exec.deallocateTask("testTask");
            exec.destroyTask("testTask");
        }
    }
}
