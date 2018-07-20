package com.offbynull.rfm.host.services.h2db;

import com.offbynull.rfm.host.model.specification.HostSpecification;
import com.offbynull.rfm.host.service.Worker;
import static com.offbynull.rfm.host.testutils.TestUtils.loadSpecResource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

public final class WorkerSetterTest {
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
    }
    
    @After
    public void tearDown() throws SQLException {
        conn.close(); // Close the final open connection, releasing in-memory database. Contents of database discarded.
    }
    
    @Test
    public void mustSetWorker() throws SQLException, ClassNotFoundException, IOException {
        Worker actualWorker = loadWorkerIntoDatabase("worker1");
        Worker expectedWorker = WorkerGetter.getWorker(conn, "worker1", 12345);
        
        assertEquals(expectedWorker, actualWorker);
    }
    
    @Test
    public void mustUpdateWithLessChildren() throws SQLException, ClassNotFoundException, IOException {
        loadWorkerIntoDatabase("worker1");
        Worker actualWorker = loadWorkerIntoDatabase("worker1_less_children");
        Worker expectedWorker = WorkerGetter.getWorker(conn, "worker1", 12345);
        
        assertEquals(expectedWorker, actualWorker);
    }
    
    @Test
    public void mustUpdateWithMoreChildren() throws SQLException, ClassNotFoundException, IOException {
        loadWorkerIntoDatabase("worker1");
        loadWorkerIntoDatabase("worker1_less_children");
        Worker actualWorker = loadWorkerIntoDatabase("worker1_more_children");
        Worker expectedWorker = WorkerGetter.getWorker(conn, "worker1", 12345);
        
        assertEquals(expectedWorker, actualWorker);
    }
    
    private Worker loadWorkerIntoDatabase(String name) throws ClassNotFoundException, IOException, SQLException {
        HostSpecification hostSpec = (HostSpecification) loadSpecResource("/com/offbynull/rfm/host/services/h2db/" + name);
        Worker worker = new Worker(hostSpec);
        
        WorkerSetter.setWorker(conn, worker);
        
        return worker;
    }
}
