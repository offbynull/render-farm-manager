package com.offbynull.rfm.host.services.h2db;

import com.offbynull.rfm.host.service.Direction;
import com.offbynull.rfm.host.service.StoredWorker;
import java.io.IOException;
import java.util.Iterator;
import javax.sql.DataSource;

final class WorkerIterator {
    private final DataSource dataSource;
    private String key;
    private Iterator<StoredWorker> storedWorkersIt;

    public WorkerIterator(DataSource dataSource) throws IOException {
        this.dataSource = dataSource;
        key = null;
        storedWorkersIt = H2dbWorkerDataUtils.getWorkers(dataSource, key, Direction.FORWARD, 100).iterator();
    }

    public boolean hasNext() throws IOException {
        if (!storedWorkersIt.hasNext()) {
            storedWorkersIt = H2dbWorkerDataUtils.getWorkers(dataSource, key, Direction.FORWARD, 100).iterator();
        }

        return storedWorkersIt.hasNext();
    }

    public StoredWorker next() throws IOException {
        if (!storedWorkersIt.hasNext()) {
            throw new IOException();
        }
        StoredWorker ret = storedWorkersIt.next();
        key = ret.getKey();
        return ret;
    }
}
