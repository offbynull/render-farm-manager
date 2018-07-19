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
import com.offbynull.rfm.host.services.h2db.InternalUtils.DecomposedWorkerKey;
import static com.offbynull.rfm.host.services.h2db.InternalUtils.fromWorkerKey;
import static com.offbynull.rfm.host.services.h2db.InternalUtils.toWorkerKey;
import java.io.IOException;
import java.sql.Connection;
import static java.sql.Connection.TRANSACTION_READ_COMMITTED;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;
import org.apache.commons.lang3.Validate;

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
        H2dbWorkDataUtils.updateWork(dataSource, work);
    }

    @Override
    public void deleteWork(String id) throws IOException {
        H2dbWorkDataUtils.deleteWork(dataSource, id);
    }

    @Override
    public StoredWork getWork(String id) throws IOException {
        return H2dbWorkDataUtils.getWork(dataSource, id);
    }

    @Override
    public List<StoredWork> getWorks(String key, Direction direction, int max) throws IOException {
        return H2dbWorkDataUtils.getWorks(dataSource, key, direction, max);
    }



    @Override
    public void updateWorker(Worker worker) throws IOException {
        Validate.notNull(worker);
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(TRANSACTION_READ_COMMITTED);
            
            WorkerSetter.setWorker(conn, worker);
            
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
            
            WorkerDeleter.deleteWorker(conn, host, port);
            
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
            
            String key = toWorkerKey(host, port);
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
        
        if (max <= 255) {
            throw new IOException("Max too high"); // not a real restriction, but we want to avoid clobbering the db so cap at 250
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
                DecomposedWorkerKey decomposedLastKey = fromWorkerKey(nextKey);
                String nextHost = decomposedLastKey.getHost();
                int nextPort = decomposedLastKey.getPort();
                
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
