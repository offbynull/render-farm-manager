package com.offbynull.rfm.host.services.h2db;

import com.offbynull.rfm.host.model.specification.HostSpecification;
import static com.offbynull.rfm.host.service.Direction.BACKWARD;
import static com.offbynull.rfm.host.service.Direction.FORWARD;
import com.offbynull.rfm.host.service.Worker;
import static com.offbynull.rfm.host.testutils.TestUtils.loadSpecResource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public final class WorkerScannerTest {
    private JdbcDataSource dataSource;
    
    private Connection conn;
    
    @Before
    public void setUp() throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setUser("sa");
        dataSource.setPassword("sa");
        dataSource.setUrl("jdbc:h2:mem:test");

        conn = dataSource.getConnection();                // Because this is an in-memory database, we need atleast 1 connection open during
                                                          // the duration of the test to keep the database intact. Once the test is finishes
                                                          // we close this.

        WorkerPrimer.prime(dataSource);
        loadWorkerIntoDatabase("basic1");
        loadWorkerIntoDatabase("basic2");
        loadWorkerIntoDatabase("basic3");
    }
    
    @After
    public void tearDown() throws SQLException {
        conn.close(); // Close the final open connection, releasing in-memory database. Contents of database discarded.
    }
    
    @Test
    public void mustScanForward() throws SQLException, ClassNotFoundException, IOException {
        List<String> workers;
        String foundKey;
        
        workers = WorkerScanner.scanWorkers(conn, FORWARD, 1);
        assertTrue(!workers.isEmpty());
        foundKey = workers.get(0);
        assertEquals("basic1:12345", foundKey);
        
        workers = WorkerScanner.scanWorkers(conn, foundKey, FORWARD, 1);
        assertTrue(!workers.isEmpty());
        foundKey = workers.get(0);
        assertEquals("basic2:12345", foundKey);
        
        workers = WorkerScanner.scanWorkers(conn, foundKey, FORWARD, 1);
        assertTrue(!workers.isEmpty());
        foundKey = workers.get(0);
        assertEquals("basic3:12345", foundKey);
        
        workers = WorkerScanner.scanWorkers(conn, foundKey, FORWARD, 1);
        assertTrue(workers.isEmpty());
    }

    @Test
    public void mustScanBackward() throws SQLException, ClassNotFoundException, IOException {
        List<String> workers;
        String foundKey;
        
        workers = WorkerScanner.scanWorkers(conn, BACKWARD, 1);
        assertTrue(!workers.isEmpty());
        foundKey = workers.get(0);
        assertEquals("basic3:12345", foundKey);
        
        workers = WorkerScanner.scanWorkers(conn, foundKey, BACKWARD, 1);
        assertTrue(!workers.isEmpty());
        foundKey = workers.get(0);
        assertEquals("basic2:12345", foundKey);
        
        workers = WorkerScanner.scanWorkers(conn, foundKey, BACKWARD, 1);
        assertTrue(!workers.isEmpty());
        foundKey = workers.get(0);
        assertEquals("basic1:12345", foundKey);
        
        workers = WorkerScanner.scanWorkers(conn, foundKey, BACKWARD, 1);
        assertTrue(workers.isEmpty());
    }

    @Test
    public void mustScanInBlocksMoreThan1() throws SQLException, ClassNotFoundException, IOException {
        List<String> workers;
        
        workers = WorkerScanner.scanWorkers(conn, FORWARD, 2);
        assertEquals("basic1:12345", workers.get(0));
        assertEquals("basic2:12345", workers.get(1));
        
        workers = WorkerScanner.scanWorkers(conn, "basic2:12345", FORWARD, 1);
        assertEquals("basic3:12345", workers.get(0));
        
        workers = WorkerScanner.scanWorkers(conn, "basic1:12345", FORWARD, 100);
        assertEquals("basic2:12345", workers.get(0));
        assertEquals("basic3:12345", workers.get(1));
    }
    
    private void loadWorkerIntoDatabase(String name) throws ClassNotFoundException, IOException, SQLException {
        HostSpecification hostSpec = (HostSpecification) loadSpecResource("/com/offbynull/rfm/host/services/h2db/" + name);
        Worker worker = new Worker(hostSpec);
        
        WorkerSetter.setWorker(conn, worker);
    }
}
