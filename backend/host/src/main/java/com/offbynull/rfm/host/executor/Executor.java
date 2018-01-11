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
package com.offbynull.rfm.host.executor;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Executes high-level Linux commands with containerization support.
 * <p>
 * Each instance of a {@link Executor} is bound to a specific boot context of a host. If it changes (e.g. the host is rebooted), a new
 * instance will be required -- method invocations on the existing instance will fail.
 * @author Kasra Faghihi
 */
public interface Executor extends Closeable {

    /**
     * Check Linux host's state.
     * @return check output
     * @throws IOException on IO error
     * @throws RebootedException if host was rebooted since this {@link Executor} was created
     */
    HostCheckResult checkHost() throws IOException;

    /**
     * Create task.
     * <p>
     * Upon successful execution, the newly created task will have a task state of {@link TaskState#CREATED}.
     * @param id task identifier
     * @param config task configuration
     * @throws IOException on I/O, resource, or internal error
     * @throws RebootedException if host was rebooted since this {@link Executor} was created
     * @throws TaskIdConflictException if task ID already exists
     * @throws NullPointerException if any argument is {@code null}
     * @throws ArithmeticException if any of the following expressions cause an overflow:
     * {@code cpuAffinity.size() * 100000L}
     * @throws IllegalArgumentException if any of the following conditions are NOT met:
     * {@code !id.isEmpty()},
     * {@code !Arrays.asList(args).contains(null)},
     * {@code args.length > 0},
     * {@code !args[0].isEmpty()}
     */
    void createTask(String id, TaskConfiguration config) throws IOException;

    /**
     * Destroy task. Resources associated with the task being destroyed, if any, will be removed as well.
     * @param id task identifier
     * @throws IOException on I/O, resource, or internal error
     * @throws RebootedException if host was rebooted since this {@link Executor} was created
     * @throws TaskIdConflictException if task ID doesn't exist
     * @throws TaskStateException if task state isn't set to {@link TaskState#CREATED}
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any of the following conditions are NOT met:
     * {@code !id.isEmpty()}
     */
    void destroyTask(String id) throws IOException;

    /**
     * Allocate resources for task.
     * <p>
     * Upon successful execution, the task's state will update to {@link TaskState#ALLOCATED}.
     * @param id task identifier
     * @param resources task resource allocation
     * @throws IOException on I/O, resource, or internal error
     * @throws RebootedException if host was rebooted since this {@link Executor} was created
     * @throws TaskIdConflictException if task ID doesn't exist
     * @throws TaskStateException if task state isn't set to {@link TaskState#CREATED}
     * @throws NullPointerException if any argument is {@code null}
     * @throws ArithmeticException if any of the following expressions cause an overflow:
     * {@code cpuAffinity.size() * 100000L}
     * @throws IllegalArgumentException if any of the following conditions are NOT met:
     * {@code !id.isEmpty()},
     * {@code !Arrays.asList(args).contains(null)},
     * {@code args.length > 0},
     * {@code !args[0].isEmpty()}
     */
    void allocateTask(String id, TaskResourceAllocation resources) throws IOException;

    /**
     * Deallocate resources for task.
     * <p>
     * Upon successful execution, the task's state will update to {@link TaskState#CREATED}.
     * @param id task identifier
     * @throws IOException on I/O, resource, or internal error
     * @throws RebootedException if host was rebooted since this {@link Executor} was created
     * @throws TaskStateException if task state isn't set to {@link TaskState#ALLOCATED}
     * @throws TaskIdConflictException if task ID doesn't exist
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any of the following conditions are NOT met:
     * {@code !id.isEmpty()}
     */
    void deallocateTask(String id) throws IOException;

    /**
     * Reallocate resources for task. Note that, unlike calling {@link #deallocateTask(java.lang.String) } followed by
     * {@link #allocateTask(java.lang.String, com.offbynull.rfm.host.executor.TaskResourceAllocation) }, this method guarantees that
     * persisted data remains in-place (e.g. disk gets resized but files on disk remain untouched).
     * @param id task identifier
     * @param resources task resource allocation
     * @throws IOException on I/O, resource, or internal error
     * @throws RebootedException if host was rebooted since this {@link Executor} was created
     * @throws TaskIdConflictException if task ID doesn't exist
     * @throws TaskStateException if task state isn't set to {@link TaskState#ALLOCATED}
     * @throws NullPointerException if any argument is {@code null}
     * @throws ArithmeticException if any of the following expressions cause an overflow:
     * {@code cpuAffinity.size() * 100000L}
     * @throws IllegalArgumentException if any of the following conditions are NOT met:
     * {@code !id.isEmpty()}
     */
    void reallocateTask(String id, TaskResourceAllocation resources) throws IOException;

    /**
     * Backup task disk.
     * @param id task identifier
     * @param backupPath directory to backup to
     * @throws IOException on I/O, resource, or internal error
     * @throws RebootedException if host was rebooted since this {@link Executor} was created
     * @throws TaskIdConflictException if task ID doesn't exist
     * @throws TaskStateException if task state isn't set to {@link TaskState#ALLOCATED}
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any of the following conditions are NOT met:
     * {@code !id.isEmpty()},
     * {@code !backupPath.isEmpty()},
     * {@code backupPath.startsWith("/")},
     * {@code !backupPath.endsWith("/")},
     * {@code !backupPath.contains("/../")},
     * {@code !backupPath.contains("/./")},
     * {@code !backupPath.contains("\u0000")}
     */
    void backupTaskDisk(String id, String backupPath) throws IOException;
    
    /**
     * Restore task disk.
     * @param id task identifier
     * @param backupPath directory to restore from
     * @throws IOException on I/O, resource, or internal error
     * @throws RebootedException if host was rebooted since this {@link Executor} was created
     * @throws TaskIdConflictException if task ID doesn't exist
     * @throws TaskStateException if task state isn't set to {@link TaskState#ALLOCATED}
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any of the following conditions are NOT met:
     * {@code !id.isEmpty()},
     * {@code !backupPath.isEmpty()},
     * {@code backupPath.startsWith("/")},
     * {@code !backupPath.endsWith("/")},
     * {@code !backupPath.contains("/../")},
     * {@code !backupPath.contains("/./")},
     * {@code !backupPath.contains("\u0000")}
     */
    void restoreTaskDisk(String id, String backupPath) throws IOException;

    /**
     * Download a task file.
     * @param id task identifier
     * @param path task file path (in task disk)
     * @param offset offset in file at {@code path} to read from
     * @param os stream to write file to (will be closed by this method)
     * @param limit maximum number of bytes to read
     * @param timeout timeout in milliseconds
     * @throws IOException on I/O error, resource error, timeout, or internal error
     * @throws RebootedException if host was rebooted since this {@link Executor} was created
     * @throws TaskIdConflictException if task ID doesn't exist
     * @throws NullPointerException if any argument is {@code null}
     * @throws ArithmeticException if any of the following expressions cause an overflow:
     * {@code remoteOffset + limit}
     * @throws IllegalArgumentException if any of the following conditions are NOT met:
     * {@code !id.isEmpty()},
     * {@code !path.isEmpty()},
     * {@code path.startsWith("/")},
     * {@code !path.endsWith("/")},
     * {@code !path.contains("/../")},
     * {@code !path.contains("/./")},
     * {@code !path.contains("\u0000")},
     * {@code offset >= 0L},
     * {@code limit >= 0L},
     * {@code timeout >= 0L}
     */
    void downloadTaskFile(String id, String path, long offset, OutputStream os, long limit, long timeout) throws IOException;

    /**
     * Start task.
     * <p>
     * Upon successful execution, the task's state will update to {@link TaskState#STARTED}. Note that the task state does not automatically
     * revert to {@link TaskState#ALLOCATED} upon process completion -- you need to explicitly call {@link #stopTask(java.lang.String) }.
     * @param id task identifier
     * @throws IOException on I/O, resource, or internal error
     * @throws RebootedException if host was rebooted since this {@link Executor} was created
     * @throws TaskIdConflictException if task ID doesn't exist
     * @throws TaskStateException if task state isn't set to {@link TaskState#ALLOCATED}
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any of the following conditions are NOT met:
     * {@code !id.isEmpty()}
     */
    void startTask(String id) throws IOException;

    /**
     * Stop task.
     * <p>
     * Upon successful execution, the task's state will update to {@link TaskState#STARTED}.
     * @param id task identifier
     * @throws IOException on I/O, resource, or internal error
     * @throws RebootedException if host was rebooted since this {@link Executor} was created
     * @throws TaskIdConflictException if task ID doesn't exist
     * @throws TaskStateException if task state isn't set to {@link TaskState#STARTED}
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any of the following conditions are NOT met:
     * {@code !id.isEmpty()}
     */
    void stopTask(String id) throws IOException;

    /**
     * Check task status.
     * @param id task identifier
     * @return work status
     * @throws IOException on I/O, resource, or internal error
     * @throws RebootedException if host was rebooted since this {@link Executor} was created
     * @throws TaskIdConflictException if task ID doesn't exist
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any of the following conditions are NOT met:
     * {@code !id.isEmpty()}
     */
    TaskCheckResult checkTask(String id) throws IOException;
}
