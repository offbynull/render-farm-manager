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
package com.offbynull.rfm.host.service;

import com.offbynull.rfm.host.model.work.Work;
import com.offbynull.rfm.host.model.worker.Worker;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Host service.
 * @author Kasra Faghihi
 */
public interface HostService {
    /**
     * Creates all necessary collections/indices/etc.. required for data access (if they don't already exist).
     * @throws IOException if error occurs with the backend datastore 
     */
    void prime() throws IOException;
    
    
    
    
    
    /**
     * Update work or add it if it doesn't exist.
     * @param work work
     * @throws NullPointerException if any argument is {@code null}
     * @throws IOException if error occurs with the backend datastore
     */
    void updateWork(Work work) throws IOException;
    
    /**
     * Delete work. Does nothing if work doesn't exist.
     * @param id work id
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code id} is empty
     * @throws IOException if error occurs with the backend datastore
     */
    void deleteWork(String id) throws IOException;

    /**
     * Get work.
     * @param id work id
     * @return found work, or {@code null} if no such work exists
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code id} is empty
     * @throws IOException if error occurs with the backend datastore
     */
    StoredWork getWork(String id) throws IOException;
    
    /**
     * Get work items.
     * @param key key to start before/after ({@code null} for start)
     * @param direction direction to iterate in
     * @param max maximum maximum number of results
     * @return results
     * @throws NullPointerException if any argument other than {@code key} is {@code null}
     * @throws IllegalArgumentException if any of the following conditions are NOT met:
     * {@code !name.isEmpty()},
     * {@code max >= 0}
     * @throws IOException if error occurs with the backend datastore
     */
    List<StoredWork> getWorks(String key, Direction direction, int max) throws IOException;
    
    
    
    
    
    /**
     * Update worker or add it if it doesn't exist.
     * @param worker worker
     * @throws NullPointerException if any argument is {@code null}
     * @throws IOException if error occurs with the backend datastore
     */
    void updateWorker(Worker worker) throws IOException;

    /**
     * Delete worker. Does nothing if worker doesn't exist. Will fail to delete if worker has work assigned to it.
     * @param host worker host
     * @param port worker port
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalStateException if worker is bound to work.
     * @throws IllegalArgumentException if any of the following conditions are NOT met:
     * {@code !host.isEmpty()},
     * {@code port >= 1 && port <= 65535}
     * @throws IOException if error occurs with the backend datastore
     */
    void deleteWorker(String host, int port) throws IOException;

    /**
     * Get worker.
     * @param host worker host
     * @param port worker port
     * @return found worker, or {@code null} if no such worker exists
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any of the following conditions are NOT met:
     * {@code !host.isEmpty()},
     * {@code port >= 1 && port <= 65535}
     * @throws IOException if error occurs with the backend datastore
     */
    StoredWorker getWorker(String host, int port) throws IOException;

    /**
     * Get workers.
     * @param key key to start before/after ({@code null} for start)
     * @param direction direction to iterate in
     * @param max maximum maximum number of results
     * @return results
     * @throws NullPointerException if any argument other than {@code key} is {@code null}
     * @throws IllegalArgumentException if any of the following conditions are NOT met:
     * {@code !name.isEmpty()},
     * {@code max >= 0}
     * @throws IOException if error occurs with the backend datastore
     */
    List<StoredWorker> getWorkers(String key, Direction direction, int max) throws IOException;
    
    
    
    
    
    /**
     * Bind work to worker.
     * @param id work ID
     * @param host worker host
     * @param port worker port
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any of the following conditions are NOT met:
     * {@code !workId.isEmpty()}
     * {@code !workerHost.isEmpty()}
     * {@code workerPort >= 1 && workerHost <= 65535}
     * @throws IOException if error occurs with the backend datastore
     */
    void bind(String id, String host, int port) throws IOException;
    
    /**
     * Unbind work from worker.
     * @param id work ID
     * @param host worker host
     * @param port worker port
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any of the following conditions are NOT met:
     * {@code !workId.isEmpty()}
     * {@code !workerHost.isEmpty()}
     * {@code workerPort >= 1 && workerHost <= 65535}
     * @throws IOException if error occurs with the backend datastore
     */
    void unbind(String id, String host, int port) throws IOException;
    
    /**
     * List workers assigned to work.
     * @param id work ID
     * @return work bindings
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any of the following conditions are NOT met:
     * {@code !id.isEmpty()}
     * @throws IOException if error occurs with the backend datastore
     */
    Set<Bind> listWorkBinds(String id) throws IOException;

    /**
     * List work assigned to worker.
     * @param host worker host
     * @param port worker port
     * @return work bindings
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any of the following conditions are NOT met:
     * {@code !host.isEmpty()},
     * {@code port >= 1 && port <= 65535}
     * @throws IOException if error occurs with the backend datastore
     */
    Set<Bind> listWorkerBinds(String host, int port) throws IOException;
    
    
    
    
    
    /**
     * Find waiting work item with the lowest possible priority that can run on the specified host.
     * @param host worker host
     * @param port worker port
     * @return work with the lowest priority that can on the specified host
     * @throws IOException if error occurs with the backend datastore
     */
    StoredWork findBindableWork(String host, int port) throws IOException;
}
