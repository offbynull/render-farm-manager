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

import com.offbynull.rfm.host.communicator.BootTimeChangedException;
import com.offbynull.rfm.host.executor.GlobalMemory;
import com.offbynull.rfm.host.executor.TaskCheckResult;
import com.offbynull.rfm.host.executor.Processor;
import com.offbynull.rfm.host.executor.Mount;
import com.offbynull.rfm.host.executor.HostCheckResult;
import com.offbynull.rfm.host.executor.Executor;
import com.offbynull.rfm.host.communicator.Communicator;
import com.offbynull.rfm.host.communicator.InMemoryExecuteResult;
import com.offbynull.rfm.host.communicator.StreamLimitExceededException;
import com.offbynull.rfm.host.communicator.TimeLimitExceededException;
import com.offbynull.rfm.host.executor.RebootedException;
import com.offbynull.rfm.host.executor.TaskConfiguration;
import com.offbynull.rfm.host.executor.TaskIdConflictException;
import com.offbynull.rfm.host.executor.TaskResourceAllocation;
import static com.offbynull.rfm.host.executor.TaskResourceAllocation.CFS_PERIOD;
import com.offbynull.rfm.host.executor.TaskMemory;
import com.offbynull.rfm.host.executor.TaskState;
import com.offbynull.rfm.host.executor.TaskStateException;
import com.offbynull.rfm.host.executors.communicator.CGroupMemoryStatParser.CGroupMemoryStat;
import com.offbynull.rfm.host.executors.communicator.DfParser.DfEntry;
import com.offbynull.rfm.host.executors.communicator.ProcMemInfoParser.ProcMemInfo;
import com.offbynull.rfm.host.executors.communicator.ProcStatParser.ProcStatCpuEntry;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import static org.apache.commons.lang3.StringUtils.removeEnd;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CommunicatorExecutor implements Executor {

    private static final Logger logger = LoggerFactory.getLogger(CommunicatorExecutor.class);
    
    //
    // Taxonomy of bash scripts
    //
    // base_lock.sh --> All scripts are run through this script. It ensures only 1 script runs on the host at one time (via file lock).
    // task_*.sh    --> High-level methods in this class map to task_*.sh files.
    // work_*.sh    --> Code that's used in multiple places is de-duplicated into work_*.sh files.
    //
    private static final String BASE_LOCK_SCRIPT;    

    private static final String HOST_BOOT_RESTORE_SCRIPT;
    private static final String HOST_STATE_CHECK_SCRIPT;

    private static final String TASK_CREATE_SCRIPT;
    private static final String TASK_DESTROY_SCRIPT;
    private static final String TASK_RES_ALLOC_SCRIPT;
    private static final String TASK_RES_DEALLOC_SCRIPT;
    private static final String TASK_RES_REALLOC_SCRIPT;
    private static final String TASK_RES_REPAIR_SCRIPT;
    private static final String TASK_DISK_BACKUP_SCRIPT;
    private static final String TASK_DISK_RESTORE_SCRIPT;
    private static final String TASK_START_SCRIPT;
    private static final String TASK_STOP_SCRIPT;
    private static final String TASK_CHECK_SCRIPT;

    private static final String WORK_ADD_TRACK_SCRIPT;
    private static final String WORK_CHECK_TRACK_SCRIPT;
    private static final String WORK_REMOVE_TRACK_SCRIPT;
    private static final String WORK_SET_STATE_SCRIPT;
    private static final String WORK_CHECK_STATE_SCRIPT;
    private static final String WORK_CREATE_WORKDIR_SCRIPT;
    private static final String WORK_DELETE_WORKDIR_SCRIPT;
    private static final String WORK_CREATE_CGROUP_SCRIPT;
    private static final String WORK_DELETE_CGROUP_SCRIPT;
    private static final String WORK_CREATE_IMAGE_SCRIPT;
    private static final String WORK_BACKUP_IMAGE_SCRIPT;
    private static final String WORK_MOUNT_IMAGE_SCRIPT;
    private static final String WORK_RECOVER_IMAGE_SCRIPT;
    private static final String WORK_RESIZE_IMAGE_SCRIPT;
    private static final String WORK_REPAIR_IMAGE_SCRIPT;
    private static final String WORK_SHRINK_IMAGE_SCRIPT;
    private static final String WORK_UNMOUNT_IMAGE_SCRIPT;
    private static final String WORK_DELETE_IMAGE_SCRIPT;
    private static final String WORK_START_PROCESS_SCRIPT;
    private static final String WORK_CHECK_PROCESS_SCRIPT;
    private static final String WORK_STOP_PROCESS_SCRIPT;
    static {
        try {
            BASE_LOCK_SCRIPT = IOUtils.toString(CommunicatorExecutor.class.getResourceAsStream("base_lock.sh"), UTF_8);

            HOST_BOOT_RESTORE_SCRIPT = IOUtils.toString(CommunicatorExecutor.class.getResourceAsStream("host_boot_restore.sh"), UTF_8);
            HOST_STATE_CHECK_SCRIPT = IOUtils.toString(CommunicatorExecutor.class.getResourceAsStream("host_state_info.sh"), UTF_8);
            
            TASK_CREATE_SCRIPT = IOUtils.toString(CommunicatorExecutor.class.getResourceAsStream("task_create.sh"), UTF_8);
            TASK_DESTROY_SCRIPT = IOUtils.toString(CommunicatorExecutor.class.getResourceAsStream("task_destroy.sh"), UTF_8);
            TASK_RES_ALLOC_SCRIPT = IOUtils.toString(CommunicatorExecutor.class.getResourceAsStream("task_res_alloc.sh"), UTF_8);
            TASK_RES_DEALLOC_SCRIPT = IOUtils.toString(CommunicatorExecutor.class.getResourceAsStream("task_res_dealloc.sh"), UTF_8);
            TASK_RES_REALLOC_SCRIPT = IOUtils.toString(CommunicatorExecutor.class.getResourceAsStream("task_res_realloc.sh"), UTF_8);
            TASK_RES_REPAIR_SCRIPT = IOUtils.toString(CommunicatorExecutor.class.getResourceAsStream("task_res_repair.sh"), UTF_8);
            TASK_DISK_BACKUP_SCRIPT = IOUtils.toString(CommunicatorExecutor.class.getResourceAsStream("task_disk_backup.sh"), UTF_8);
            TASK_DISK_RESTORE_SCRIPT = IOUtils.toString(CommunicatorExecutor.class.getResourceAsStream("task_disk_restore.sh"), UTF_8);
            TASK_START_SCRIPT = IOUtils.toString(CommunicatorExecutor.class.getResourceAsStream("task_start.sh"), UTF_8);
            TASK_STOP_SCRIPT = IOUtils.toString(CommunicatorExecutor.class.getResourceAsStream("task_stop.sh"), UTF_8);
            TASK_CHECK_SCRIPT = IOUtils.toString(CommunicatorExecutor.class.getResourceAsStream("task_check.sh"), UTF_8);

            WORK_ADD_TRACK_SCRIPT = IOUtils.toString(CommunicatorExecutor.class.getResourceAsStream("work_track_add.sh"), UTF_8);
            WORK_CHECK_TRACK_SCRIPT = IOUtils.toString(CommunicatorExecutor.class.getResourceAsStream("work_track_check.sh"), UTF_8);
            WORK_REMOVE_TRACK_SCRIPT = IOUtils.toString(CommunicatorExecutor.class.getResourceAsStream("work_track_remove.sh"), UTF_8);
            WORK_SET_STATE_SCRIPT = IOUtils.toString(CommunicatorExecutor.class.getResourceAsStream("work_state_set.sh"), UTF_8);
            WORK_CHECK_STATE_SCRIPT = IOUtils.toString(CommunicatorExecutor.class.getResourceAsStream("work_state_check.sh"), UTF_8);
            WORK_CREATE_WORKDIR_SCRIPT = IOUtils.toString(CommunicatorExecutor.class.getResourceAsStream("work_workdir_create.sh"), UTF_8);
            WORK_DELETE_WORKDIR_SCRIPT = IOUtils.toString(CommunicatorExecutor.class.getResourceAsStream("work_workdir_delete.sh"), UTF_8);
            WORK_CREATE_CGROUP_SCRIPT = IOUtils.toString(CommunicatorExecutor.class.getResourceAsStream("work_cgroup_create.sh"), UTF_8);
            WORK_DELETE_CGROUP_SCRIPT = IOUtils.toString(CommunicatorExecutor.class.getResourceAsStream("work_cgroup_delete.sh"), UTF_8);
            WORK_CREATE_IMAGE_SCRIPT = IOUtils.toString(CommunicatorExecutor.class.getResourceAsStream("work_image_create.sh"), UTF_8);
            WORK_BACKUP_IMAGE_SCRIPT = IOUtils.toString(CommunicatorExecutor.class.getResourceAsStream("work_image_backup.sh"), UTF_8);
            WORK_MOUNT_IMAGE_SCRIPT = IOUtils.toString(CommunicatorExecutor.class.getResourceAsStream("work_image_mount.sh"), UTF_8);
            WORK_RECOVER_IMAGE_SCRIPT = IOUtils.toString(CommunicatorExecutor.class.getResourceAsStream("work_image_recover.sh"), UTF_8);
            WORK_RESIZE_IMAGE_SCRIPT = IOUtils.toString(CommunicatorExecutor.class.getResourceAsStream("work_image_resize.sh"), UTF_8);
            WORK_REPAIR_IMAGE_SCRIPT = IOUtils.toString(CommunicatorExecutor.class.getResourceAsStream("work_image_repair.sh"), UTF_8);
            WORK_SHRINK_IMAGE_SCRIPT = IOUtils.toString(CommunicatorExecutor.class.getResourceAsStream("work_image_shrink.sh"), UTF_8);
            WORK_UNMOUNT_IMAGE_SCRIPT = IOUtils.toString(CommunicatorExecutor.class.getResourceAsStream("work_image_unmount.sh"), UTF_8);
            WORK_DELETE_IMAGE_SCRIPT = IOUtils.toString(CommunicatorExecutor.class.getResourceAsStream("work_image_delete.sh"), UTF_8);
            WORK_START_PROCESS_SCRIPT = IOUtils.toString(CommunicatorExecutor.class.getResourceAsStream("work_process_start.sh"), UTF_8);
            WORK_CHECK_PROCESS_SCRIPT = IOUtils.toString(CommunicatorExecutor.class.getResourceAsStream("work_process_info.sh"), UTF_8);
            WORK_STOP_PROCESS_SCRIPT = IOUtils.toString(CommunicatorExecutor.class.getResourceAsStream("work_process_stop.sh"), UTF_8);
        } catch (IOException ioe) {
            throw new IllegalStateException(ioe);
        }
    }

    private static final int EXIT_CODE_OK = 0;
    private static final int EXIT_CODE_CRITICAL = 1;
    private static final int EXIT_CODE_BAD_TASK_ID = 2;
    private static final int EXIT_CODE_BAD_TASK_STATE = 3;
    private static final int EXIT_CODE_RECOVERY_ERROR = 4;
    
    private static final long BUFFER_LIMIT = 1024L * 1024L;

    private final Communicator communicator;
    private final long bootTime;

    public static CommunicatorExecutor create(Communicator communicator) throws IOException {
        // HOST_BOOT_RESTORE_SCRIPT will reallocate resources (e.g. cgroups) for any jobs that were running during previous boot instances.
        // It's up to the caller to actually re-start the tasks.
        InMemoryExecuteResult res = communicator.execute(60000L, -1L, BUFFER_LIMIT, BUFFER_LIMIT,
                BASE_LOCK_SCRIPT,
                HOST_BOOT_RESTORE_SCRIPT,
                TASK_RES_REPAIR_SCRIPT,
                TASK_STOP_SCRIPT,
                WORK_ADD_TRACK_SCRIPT,
                WORK_CHECK_TRACK_SCRIPT,
                WORK_REMOVE_TRACK_SCRIPT,
                WORK_SET_STATE_SCRIPT,
                WORK_CHECK_STATE_SCRIPT,
                WORK_CREATE_CGROUP_SCRIPT,
                WORK_MOUNT_IMAGE_SCRIPT,
                WORK_REPAIR_IMAGE_SCRIPT,
                WORK_STOP_PROCESS_SCRIPT);
        
        switch (res.getExitCode()) {
            case EXIT_CODE_OK:
                break;
            case EXIT_CODE_CRITICAL:
                throw new IOException("Critical error\n" + res.getStderr());
            case EXIT_CODE_RECOVERY_ERROR:
                logger.warn("Problems recovering one or more tasks: {}", communicator);
                break;
            default:
                throw new IOException("Unrecognized error\n" + res.getStderr());
        }
        return new CommunicatorExecutor(communicator, res.getBootTime());
    }

    public static CommunicatorExecutor create(Communicator communicator, long bootTime) {
        return new CommunicatorExecutor(communicator, bootTime);
    }

    private CommunicatorExecutor(Communicator communicator, long bootTime) {
        Validate.notNull(communicator);
        Validate.isTrue(bootTime >= 0L); // No -1L boottime (-1L means dont care) -- executor ops must be for the same boot of the host

        this.communicator = communicator;
        this.bootTime = bootTime;
    }

    @Override
    public HostCheckResult checkHost() throws IOException {
        String stdout = simpleRunWithOutput(false, false, HOST_STATE_CHECK_SCRIPT);

        try {
            Map<String, String> output = ScriptParser.parse(stdout);

            String uname = output.get("!UNAME");

            boolean swapEnabled = KernelConfigParser.parse(output.get("!KERNEL_CONFIGS")).stream()
                    .anyMatch(x -> "CONFIG_MEMCG_SWAP_ENABLED".equals(x.getKey()) && "y".equals(x.getValue()));

            List<Mount> mounts = DfParser.parse(output.get("!DF")).stream()
                    .filter(x -> x.getAvailable() != null)
                    .filter(x -> x.getUsed() != null)
                    .map(x -> new Mount(x.getTarget(), x.getUsed(), x.getAvailable()))
                    .collect(toList());
            
            Set<Long> cpus = CGroupCpuSetParser.parse(output.get("!CGROUP_CPUSET"));
            
            List<ProcStatCpuEntry> procStat1 = ProcStatParser.parse(output.get("!PROCSTAT1"));
            List<ProcStatCpuEntry> procStat2 = ProcStatParser.parse(output.get("!PROCSTAT2"));
            List<Processor> processors = ProcCpuInfoParser.parse(output.get("!CPUINFO")).stream()
                    .filter(x -> cpus.contains(x.getProcessor()))
                    .map(x -> {
                        // procstat1 and procstat2 are taken 1 second apart
                        ProcStatCpuEntry stat1 = procStat1.stream().filter(a -> a.getProcessor() == x.getProcessor()).findAny().get();
                        ProcStatCpuEntry stat2 = procStat2.stream().filter(a -> a.getProcessor() == x.getProcessor()).findAny().get();

                        long stat1Used = stat1.getUserTime() + stat1.getNiceTime() + stat1.getSystemTime();
                        long stat1Idle = stat1.getIdleTime();
                        long stat2Used = stat2.getUserTime() + stat2.getNiceTime() + stat2.getSystemTime();
                        long stat2Idle = stat2.getIdleTime();

                        long statUsedDiff = stat2Used - stat1Used;
                        long statIdleDiff = stat2Idle - stat1Idle;

                        double usage = ((double) statUsedDiff) / ((double) (statUsedDiff + statIdleDiff));

                        return new Processor(
                                x.getPhysicalId(),
                                x.getCoreId(),
                                x.getProcessor(),
                                x.getModel(),
                                x.getFlags(),
                                usage
                        );
                    })
                    .collect(toList());
            
//            CGroupMemoryStat memStat = CGroupMemoryStatParser.parse(output.get("!CGROUP_MEM_STAT"));
            ProcMemInfo memInfo = ProcMemInfoParser.parse(output.get("!MEMINFO"));
            GlobalMemory memory = new GlobalMemory( // https://unix.stackexchange.com/q/263881
                    memInfo.getMemTotal() * 1024L,
                    memInfo.getMemAvailable() * 1024L,
                    memInfo.getSwapTotal() * 1024L,
                    memInfo.getSwapFree() * 1024L);

            List<String> tasks = TrackParser.parse(output.get("!TASKS")).stream()
                    .map(x -> {
                        try {
                            return URLDecoder.decode(x.getId(), "UTF-8");
                        } catch (IOException ioe) {
                            throw new RuntimeException(ioe);
                        }
                    })
                    .collect(toList());

            return new HostCheckResult(uname, swapEnabled, mounts, processors, memory, tasks);
        } catch (RuntimeException re) {
            throw new IOException("Parse error", re);
        }
    }

    @Override
    public void createTask(String id, TaskConfiguration config) throws IOException {
        Validate.notNull(id);
        Validate.notNull(config);
        Validate.notBlank(id);
        
        String safeId = URLEncoder.encode(id, "UTF-8"); // encode because tasks file cannot have spaces

        String[] lockArgs = ArrayUtils.insert(0, config.getCommand().toArray(new String[0]),
                TASK_CREATE_SCRIPT,
                WORK_ADD_TRACK_SCRIPT,
                WORK_SET_STATE_SCRIPT,
                WORK_CREATE_WORKDIR_SCRIPT,
                WORK_DELETE_WORKDIR_SCRIPT,
                WORK_REMOVE_TRACK_SCRIPT,
                safeId,
                config.getWorkPath(),
                config.getUser());
        simpleRun(true, false, BASE_LOCK_SCRIPT, lockArgs);
    }

    @Override
    public void destroyTask(String id) throws IOException {
        Validate.notNull(id);
        Validate.notBlank(id);
        
        String safeId = URLEncoder.encode(id, "UTF-8"); // encode because tasks file cannot have spaces

        simpleRun(true, true, BASE_LOCK_SCRIPT,
                TASK_DESTROY_SCRIPT,
                WORK_REMOVE_TRACK_SCRIPT,
                WORK_CHECK_STATE_SCRIPT,
                WORK_STOP_PROCESS_SCRIPT,
                WORK_UNMOUNT_IMAGE_SCRIPT,
                WORK_DELETE_IMAGE_SCRIPT,
                WORK_DELETE_CGROUP_SCRIPT,
                WORK_DELETE_WORKDIR_SCRIPT,
                safeId);
    }

    @Override
    public void allocateTask(String id, TaskResourceAllocation resources) throws IOException {
        Validate.notNull(id);
        Validate.notNull(resources);
        Validate.notBlank(id);

        String safeId = URLEncoder.encode(id, "UTF-8"); // encode because tasks file cannot have spaces
        
        simpleRun(true, true, BASE_LOCK_SCRIPT,
                TASK_RES_ALLOC_SCRIPT,
                WORK_CHECK_TRACK_SCRIPT,
                WORK_SET_STATE_SCRIPT,
                WORK_CHECK_STATE_SCRIPT,
                WORK_CREATE_CGROUP_SCRIPT,
                WORK_CREATE_IMAGE_SCRIPT,
                WORK_MOUNT_IMAGE_SCRIPT,
                WORK_UNMOUNT_IMAGE_SCRIPT,
                WORK_DELETE_IMAGE_SCRIPT,
                WORK_DELETE_CGROUP_SCRIPT,
                safeId,
                resources.getCpuAffinity().stream().map(p -> p.toString()).collect(joining(",")),
                Long.toString(resources.getSchedulerSlice()),
                Long.toString(CFS_PERIOD),
                Long.toString(resources.getMemoryLimit()),
                Long.toString(resources.getDiskLimit()));
    }

    @Override
    public void deallocateTask(String id) throws IOException {
        Validate.notNull(id);
        Validate.notBlank(id);

        String safeId = URLEncoder.encode(id, "UTF-8"); // encode because tasks file cannot have spaces
        
        simpleRun(true, true, BASE_LOCK_SCRIPT,
                TASK_RES_DEALLOC_SCRIPT,
                WORK_CHECK_TRACK_SCRIPT,
                WORK_SET_STATE_SCRIPT,
                WORK_CHECK_STATE_SCRIPT,
                WORK_UNMOUNT_IMAGE_SCRIPT,
                WORK_DELETE_IMAGE_SCRIPT,
                WORK_DELETE_CGROUP_SCRIPT,
                safeId);
    }

    @Override
    public void reallocateTask(String id, TaskResourceAllocation resources) throws IOException {
        Validate.notNull(id);
        Validate.notNull(resources);
        Validate.notBlank(id);

        String safeId = URLEncoder.encode(id, "UTF-8"); // encode because tasks file cannot have spaces
        
        simpleRun(true, true, BASE_LOCK_SCRIPT,
                TASK_RES_REALLOC_SCRIPT,
                WORK_CHECK_TRACK_SCRIPT,
                WORK_CHECK_STATE_SCRIPT,
                WORK_CREATE_CGROUP_SCRIPT,
                WORK_MOUNT_IMAGE_SCRIPT,
                WORK_RESIZE_IMAGE_SCRIPT,
                WORK_UNMOUNT_IMAGE_SCRIPT,
                WORK_DELETE_CGROUP_SCRIPT,
                safeId,
                resources.getCpuAffinity().stream().map(p -> p.toString()).collect(joining(",")),
                Long.toString(resources.getSchedulerSlice()),
                Long.toString(CFS_PERIOD),
                Long.toString(resources.getMemoryLimit()),
                Long.toString(resources.getDiskLimit()));
    }

    @Override
    public void backupTaskDisk(String id, String backupPath) throws IOException {
        Validate.notNull(id);
        Validate.notNull(backupPath);
        Validate.notBlank(id);
        InternalUtils.validatePath(backupPath);
        
        String safeId = URLEncoder.encode(id, "UTF-8"); // encode because tasks file cannot have spaces
        
        simpleRun(true, true, BASE_LOCK_SCRIPT,
                TASK_DISK_BACKUP_SCRIPT,
                WORK_CHECK_TRACK_SCRIPT,
                WORK_CHECK_STATE_SCRIPT,
                WORK_BACKUP_IMAGE_SCRIPT,
                safeId,
                backupPath);
    }
    
    @Override
    public void restoreTaskDisk(String id, String backupPath) throws IOException {
        Validate.notNull(id);
        Validate.notNull(backupPath);
        Validate.notBlank(id);
        InternalUtils.validatePath(backupPath);
        
        String safeId = URLEncoder.encode(id, "UTF-8"); // encode because tasks file cannot have spaces
        
        simpleRun(true, true, BASE_LOCK_SCRIPT,
                TASK_DISK_RESTORE_SCRIPT,
                WORK_CHECK_TRACK_SCRIPT,
                WORK_CHECK_STATE_SCRIPT,
                WORK_RECOVER_IMAGE_SCRIPT,
                safeId,
                backupPath);
    }

    @Override
    public void downloadTaskFile(String id, String path, long offset, OutputStream os, long limit, long timeout) throws IOException {
        Validate.notNull(id);
        Validate.notNull(path);
        Validate.notNull(os);
        Validate.notBlank(id);
        InternalUtils.validatePath(path);
        Validate.isTrue(offset >= 0L);
        Validate.isTrue(limit >= 0L);
        Validate.isTrue(timeout >= 0L);
        Math.addExact(offset, limit);  // check for rollover
        
        TaskCheckResult checkRes = checkTask(id);
        
        String mountDir = checkRes.getDisk().getTarget();
        mountDir = removeEnd(mountDir, "/"); // remove / at the end if it's there -- shouldn't be there but just incase
        
        String finalPath = mountDir + path; // append path to dir -- mountDir not starts with /, path starts with / and not contains /../

        try {
            communicator.pipe(
                    timeout,
                    finalPath,
                    offset,
                    os,
                    limit);
        } catch (TimeLimitExceededException tlee) { // don't need to check for any exc other than this one -- above checks handle it
            throw new IOException(tlee);
        }
    }

    @Override
    public void startTask(String id) throws IOException {
        Validate.notNull(id);
        Validate.notBlank(id);

        String safeId = URLEncoder.encode(id, "UTF-8"); // encode because tasks file cannot have spaces
        
        simpleRun(true, true, BASE_LOCK_SCRIPT,
                TASK_START_SCRIPT,
                WORK_CHECK_TRACK_SCRIPT,
                WORK_SET_STATE_SCRIPT,
                WORK_CHECK_STATE_SCRIPT,
                WORK_START_PROCESS_SCRIPT,
                WORK_STOP_PROCESS_SCRIPT,
                safeId);
    }

    @Override
    public void stopTask(String id) throws IOException {
        Validate.notNull(id);
        Validate.notBlank(id);

        String safeId = URLEncoder.encode(id, "UTF-8"); // encode because tasks file cannot have spaces
        
        simpleRun(true, true, BASE_LOCK_SCRIPT,
                TASK_STOP_SCRIPT,
                WORK_CHECK_TRACK_SCRIPT,
                WORK_SET_STATE_SCRIPT,
                WORK_CHECK_STATE_SCRIPT,
                WORK_STOP_PROCESS_SCRIPT,
                safeId);
    }

    @Override
    public TaskCheckResult checkTask(String id) throws IOException {
        Validate.notNull(id);
        Validate.notBlank(id);

        String safeId = URLEncoder.encode(id, "UTF-8"); // encode because tasks file cannot have spaces
        
        try {
            String checkOutput = simpleRunWithOutput(true, false, BASE_LOCK_SCRIPT,
                    TASK_CHECK_SCRIPT,
                    WORK_CHECK_TRACK_SCRIPT,
                    WORK_CHECK_PROCESS_SCRIPT,
                    safeId);

            Map<String, String> output = ScriptParser.parse(checkOutput);
            
            String sidStr = output.get("!RUN_SID");
            Integer sid = null;
            if (sidStr != null) {
                sid = Integer.parseInt(sidStr);
            }

            String pidsStr = output.get("!RUN_PIDS");
            List<Integer> pids = null;
            if (pidsStr != null && !pidsStr.isEmpty()) {
                pids = Arrays.stream(pidsStr.split("\\n"))
                    .map(x -> Integer.valueOf(x.trim()))
                    .collect(toList());
            }

            String exitCodeStr = output.get("!EXITCODE");
            Integer exitCode = null;
            if (exitCodeStr != null) {
                exitCode = Integer.parseInt(exitCodeStr);
            }
            
            String dfStr = output.get("!DF");
            Mount mount = null;
            if (dfStr != null) {
                List<DfEntry> dfEntries = DfParser.parse(dfStr);
                mount = new Mount(
                        dfEntries.get(0).getTarget(),
                        dfEntries.get(0).getUsed(),
                        dfEntries.get(0).getAvailable());
            }

            CGroupMemoryStat memStat = CGroupMemoryStatParser.parse(output.get("!CGROUP_MEM_STAT"));
            TaskMemory memory = new TaskMemory(
                    memStat.getRss(),
                    memStat.getCache(),
                    memStat.getSwap() == null ? 0L : memStat.getSwap());
            
            String workPath = output.get("!WORKDIR");
            String user = output.get("!WORKUSER");
            int cmdCounter = 0;
            String cmdArg;
            List<String> command = new ArrayList<>();
            while ((cmdArg = output.get("!WORKCMD" + cmdCounter)) != null) {
                command.add(cmdArg);
                cmdCounter++;
            }
            TaskConfiguration config = new TaskConfiguration(workPath, user, command.toArray(new String[0]));

            String stateStr = output.get("!STATE");
            TaskState state = TaskState.valueOf(stateStr);
            
            TaskResourceAllocation res = null;
            if (state != TaskState.CREATED) {
                String cpuAffinityStr = output.get("!CPUS");
                Set<Long> cpuAffinity = Arrays.stream(cpuAffinityStr.split(",")).map(x -> Long.parseLong(x)).collect(toSet());
                long schedulerQuota = Long.parseLong(output.get("!CPU_QUOTA"));
                long schedulerPeriod = Long.parseLong(output.get("!CPU_PERIOD"));
                long memoryLimit = Long.parseLong(output.get("!MEMORY_SIZE"));
                long diskLimit = Long.parseLong(output.get("!DISK_SIZE"));
                Validate.isTrue(schedulerPeriod == CFS_PERIOD);
                res = new TaskResourceAllocation(cpuAffinity, schedulerQuota, memoryLimit, diskLimit);
            }

            return new TaskCheckResult(state, config, res, sid, pids, exitCode, mount, memory);
        } catch (RuntimeException re) {
            throw new IOException("Parse error", re);
        }
    }

    private String simpleRunWithOutput(boolean badIdCheck, boolean badStateCheck, String script, long timeout, String... args)
            throws IOException {
        try {
            InMemoryExecuteResult res = communicator.execute(timeout, bootTime, BUFFER_LIMIT, BUFFER_LIMIT, script, args);
            switch (res.getExitCode()) {
                case EXIT_CODE_OK:
                    break;
                case EXIT_CODE_CRITICAL:
                    throw new IOException("Critical error\n" + res.getStderr());
                case EXIT_CODE_BAD_TASK_ID:
                    throw badIdCheck ? new TaskIdConflictException() : new IOException("Unrecognized error\n" + res.getStderr());
                case EXIT_CODE_BAD_TASK_STATE:
                    throw badStateCheck ? new TaskStateException() : new IOException("Unrecognized error\n" + res.getStderr());
                default:
                    throw new IOException("Unrecognized error\n" + res.getStderr());
            }
            return res.getStdout();
        } catch (StreamLimitExceededException | TimeLimitExceededException e) {
            throw new IOException(e);
        } catch (BootTimeChangedException btce) {
            throw new RebootedException(btce);
        }
    }

    private String simpleRunWithOutput(boolean badIdCheck, boolean badStateCheck, String script, String... args) throws IOException {
        return simpleRunWithOutput(badIdCheck, badStateCheck, script, 60000L, args);
    }

    private void simpleRun(boolean badIdCheck, boolean badStateCheck, String script, String... args) throws IOException {
        simpleRunWithOutput(badIdCheck, badStateCheck, script, args);
    }

    @Override
    public void close() throws IOException {
        communicator.close();
    }
    
}
