package com.offbynull.rfm.host.services.h2db;

import com.offbynull.rfm.host.model.specification.HostSpecification;
import com.offbynull.rfm.host.service.Worker;
import static com.offbynull.rfm.host.testutils.TestUtils.loadSpecResource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public final class WorkerAccessTest {
    private JdbcDataSource dataSource;
    
    private Connection keepAliveConnection;
    
    @Before
    public void setUp() throws IOException, SQLException {
        dataSource = new JdbcDataSource();
        dataSource.setUser("sa");
        dataSource.setPassword("sa");
        dataSource.setUrl("jdbc:h2:mem:test");
        
        keepAliveConnection = dataSource.getConnection(); // Because this is an in-memory database, we need atleast 1 connection open during
                                                          // the duration of the test to keep the database intact. Once the test is finishes
                                                          // we close this.
    }
    
    @After
    public void tearDown() throws SQLException {
        keepAliveConnection.close(); // Close the final open connection, releasing in-memory database. Contents of database discarded.
    }
    
    @Test
    public void mustPrime() throws SQLException {
        WorkerPrimer.prime(dataSource);
    }
    
    @Test
    public void mustWriteAndRead() throws SQLException, ClassNotFoundException, IOException {
        HostSpecification hostSpec = (HostSpecification) loadSpecResource("/com/offbynull/rfm/host/services/h2db/basic");
        Worker worker = new Worker(hostSpec);
        
        WorkerPrimer.prime(dataSource);
        WorkerSetter.setWorker(dataSource, worker);
    }
}
