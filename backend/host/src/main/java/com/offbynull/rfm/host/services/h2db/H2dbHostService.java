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
import java.io.IOException;
import java.math.BigDecimal;
import static java.math.BigDecimal.ZERO;
import java.util.Iterator;
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
        H2dbSetupDataUtils.create(dataSource);
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
        H2dbWorkerDataUtils.updateWorker(dataSource, worker);
    }

    @Override
    public void deleteWorker(String host, int port) throws IOException {
        H2dbWorkerDataUtils.deleteWorker(dataSource, host, port);
    }

    @Override
    public StoredWorker getWorker(String host, int port) throws IOException {
        return H2dbWorkerDataUtils.getWorker(dataSource, host, port);
    }

    @Override
    public List<StoredWorker> getWorkers(String key, Direction direction, int max) throws IOException {
        return H2dbWorkerDataUtils.getWorkers(dataSource, key, direction, max);
    }



    @Override
    public void bind(String id, String host, int port) throws IOException {
        H2dbBindDataUtils.bind(dataSource, id, host, port);
    }

    @Override
    public void unbind(String id, String host, int port) throws IOException {
        H2dbBindDataUtils.unbind(dataSource, id, host, port);
    }

    @Override
    public Set<Bind> listWorkBinds(String id) throws IOException {
        return H2dbBindDataUtils.listWorkBinds(dataSource, id);
    }

    @Override
    public Set<Bind> listWorkerBinds(String host, int port) throws IOException {
        return H2dbBindDataUtils.listWorkerBinds(dataSource, host, port);
    }



    @Override
    public StoredWork findBindableWork(String host, int port) throws IOException {
        StoredWorker storedWorker = H2dbWorkerDataUtils.getWorker(dataSource, host, port);
        if (storedWorker == null) { // Worker was removed or something was wrong with it.
            return null;
        }
        
        String key = null;
        BigDecimal minPriority = ZERO;
        while (true) {
            List<StoredWork> storedWorks = H2dbWorkDataUtils.getWorksPrioritized(dataSource, key, minPriority, Direction.FORWARD, 100);
            if (storedWorks.isEmpty()) {
                return null;
            }
            
            for (StoredWork storedWork : storedWorks) {
                Worker worker = storedWorker.getWorker();
                Work work = storedWork.getWork();
                if (BindEvaluator.evaluate(work)) {
                    return storedWork;
                }
            }
            
            StoredWork last = storedWorks.get(storedWorks.size() - 1);
            key = last.getKey();
            minPriority = last.getWork().getCore().getPriority();
        }
    }
}
