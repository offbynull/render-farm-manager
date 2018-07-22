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
package com.offbynull.rfm.host.services.h2db;

import com.offbynull.rfm.host.service.Work;
import com.offbynull.rfm.host.service.Worker;
import com.offbynull.rfm.host.service.Bind;
import com.offbynull.rfm.host.service.Direction;
import com.offbynull.rfm.host.service.HostService;
import com.offbynull.rfm.host.service.StoredWork;
import com.offbynull.rfm.host.service.StoredWorker;
import com.offbynull.rfm.host.services.h2db.InternalUtils.DecomposedWorkCursor;
import com.offbynull.rfm.host.services.h2db.InternalUtils.DecomposedWorkerCursor;
import static com.offbynull.rfm.host.services.h2db.InternalUtils.fromWorkCursor;
import java.io.IOException;
import java.sql.Connection;
import static java.sql.Connection.TRANSACTION_READ_COMMITTED;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;
import org.apache.commons.lang3.Validate;
import static com.offbynull.rfm.host.services.h2db.InternalUtils.toWorkerCursor;
import static com.offbynull.rfm.host.services.h2db.InternalUtils.fromWorkerCursor;
import static com.offbynull.rfm.host.services.h2db.InternalUtils.toWorkCursor;

/**
 * Host service backed by H2DB.
 * @author Kasra Faghihi
 */
public class H2dbHostService implements HostService {
    private final DataSource dataSource;

    public H2dbHostService(DataSource dataSource) {
        Validate.notNull(dataSource);
        this.dataSource = dataSource;
    }



    @Override
    public void prime() throws IOException {
        try {
            WorkerPrimer.prime(dataSource);
            WorkPrimer.prime(dataSource);
        } catch (SQLException sqle) {
            throw new IOException(sqle);
        }
    }

    @Override
    public void updateWork(Work work) throws IOException {
        Validate.notNull(work);
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(TRANSACTION_READ_COMMITTED);
            
            try (WorkLock workLock = WorkLock.lock(conn, work)) {
                WorkSetter.setWork(conn, work);
            }
            
            conn.commit();
        } catch (SQLException sqle) {
            throw new IOException(sqle);
        }
    }

    @Override
    public void deleteWork(String id) throws IOException {
        Validate.notNull(id);
        Validate.notEmpty(id);
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(TRANSACTION_READ_COMMITTED);
            
            try (WorkLock workLock = WorkLock.lock(conn, id)) {
                WorkDeleter.deleteWork(conn, id);
            }
            
            conn.commit();
        } catch (SQLException sqle) {
            throw new IOException(sqle);
        }
    }

    @Override
    public StoredWork getWork(String id) throws IOException {
        Validate.notNull(id);
        Validate.notEmpty(id);
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(TRANSACTION_READ_COMMITTED);
        
            Work work = WorkGetter.getWork(conn, id);
            if (work == null) {
                return null;
            }
            
            String key = toWorkCursor(work.getPriority(), id);
            return new StoredWork(key, work);
        } catch (SQLException sqle) {
            throw new IOException(sqle);
        }
    }

    @Override
    public List<StoredWork> getWorks(String key, Direction direction, int max) throws IOException {
        // key CAN be null -- if it is null it means that you're setarting the scan (there is no previous point to continue from)
        Validate.notNull(direction);
        Validate.isTrue(max >= 0);
        
        if (max <= 2048) {
            throw new IOException("Max too high"); // not a real restriction, but we want to avoid clobbering the db
        }

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(TRANSACTION_READ_COMMITTED);
            
            List<String> nextKeys;
            if (key == null) {
                nextKeys = WorkScanner.scanWorks(conn, direction, max);
            } else {
                nextKeys = WorkScanner.scanWorks(conn, key, direction, max);
            }
            
            List<StoredWork> nextStoredWorks = new ArrayList<>(nextKeys.size());
            for (String nextKey : nextKeys) {
                DecomposedWorkCursor decomposedLastCursor = fromWorkCursor(nextKey);
                String id = decomposedLastCursor.getId();
                
                Work nextWork = WorkGetter.getWork(conn, id);
                StoredWork nextStoredWork = new StoredWork(nextKey, nextWork);
                
                nextStoredWorks.add(nextStoredWork);
            }
            
            return nextStoredWorks;
        } catch (SQLException sqle) {
            throw new IOException(sqle);
        }
    }



    @Override
    public void updateWorker(Worker worker) throws IOException {
        Validate.notNull(worker);
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(TRANSACTION_READ_COMMITTED);
            
            try (WorkerLock workerLock = WorkerLock.lock(conn, worker)) {
                WorkerSetter.setWorker(conn, worker);
            }
            
            conn.commit();
        } catch (SQLException sqle) {
            throw new IOException(sqle);
        }
    }

    @Override
    public void deleteWorker(String host, int port) throws IOException {
        Validate.notNull(host);
        Validate.notEmpty(host);
        Validate.isTrue(port >= 1 && port <= 65535);
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(TRANSACTION_READ_COMMITTED);
            
            try (WorkerLock workerLock = WorkerLock.lock(conn, host, port)) {
                WorkerDeleter.deleteWorker(conn, host, port);
            }
            
            conn.commit();
        } catch (SQLException sqle) {
            throw new IOException(sqle);
        }
    }

    @Override
    public StoredWorker getWorker(String host, int port) throws IOException {
        Validate.notNull(host);
        Validate.notEmpty(host);
        Validate.isTrue(port >= 1 && port <= 65535);
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(TRANSACTION_READ_COMMITTED);
        
            Worker worker = WorkerGetter.getWorker(conn, host, port);
            if (worker == null) {
                return null;
            }
            
            String key = toWorkerCursor(host, port);
            return new StoredWorker(key, worker);
        } catch (SQLException sqle) {
            throw new IOException(sqle);
        }
    }

    @Override
    public List<StoredWorker> scanWorkers(String key, Direction direction, int max) throws IOException {
        // key CAN be null -- if it is null it means that you're setarting the scan (there is no previous point to continue from)
        Validate.notNull(direction);
        Validate.isTrue(max >= 0);
        
        if (max <= 2048) {
            throw new IOException("Max too high"); // not a real restriction, but we want to avoid clobbering the db
        }

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(TRANSACTION_READ_COMMITTED);
            
            List<String> nextKeys;
            if (key == null) {
                nextKeys = WorkerScanner.scanWorkers(conn, direction, max);
            } else {
                nextKeys = WorkerScanner.scanWorkers(conn, key, direction, max);
            }
            
            List<StoredWorker> nextStoredWorkers = new ArrayList<>(nextKeys.size());
            for (String nextKey : nextKeys) {
                DecomposedWorkerCursor decomposedLastCursor = fromWorkerCursor(nextKey);
                String nextHost = decomposedLastCursor.getHost();
                int nextPort = decomposedLastCursor.getPort();
                
                Worker nextWorker = WorkerGetter.getWorker(conn, nextHost, nextPort);
                StoredWorker nextStoredWorker = new StoredWorker(nextKey, nextWorker);
                
                nextStoredWorkers.add(nextStoredWorker);
            }
            
            return nextStoredWorkers;
        } catch (SQLException sqle) {
            throw new IOException(sqle);
        }
    }



    @Override
    public void bind(String id, String host, int port) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unbind(String id, String host, int port) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Bind> listWorkBinds(String id) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Bind> listWorkerBinds(String host, int port) throws IOException {
        throw new UnsupportedOperationException();
    }



    @Override
    public StoredWork findBindableWork(String host, int port) throws IOException {
        throw new UnsupportedOperationException();
    }
}
