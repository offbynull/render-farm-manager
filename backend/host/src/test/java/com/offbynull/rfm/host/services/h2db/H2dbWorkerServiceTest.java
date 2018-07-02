package com.offbynull.rfm.host.services.h2db;

import com.offbynull.rfm.host.model.specification.HostSpecification;
import com.offbynull.rfm.host.service.Worker;
import com.offbynull.rfm.host.service.StoredWorker;
import static com.offbynull.rfm.host.testutils.TestUtils.loadSpecResource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Before;
import org.junit.Test;

public class H2dbWorkerServiceTest {
    
    private JdbcDataSource dataSource;
    private H2dbHostService fixture;
    
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

        fixture = new H2dbHostService(dataSource);
        fixture.prime();
    }
    
    @After
    public void tearDown() throws SQLException {
        keepAliveConnection.close(); // Close the final open connection, releasing in-memory database. Contents of database discarded.
    }

    @Test
    public void mustCreateWorker() throws IOException, ClassNotFoundException {
        HostSpecification hostSpec = (HostSpecification) loadSpecResource("../services/h2db/basic");
        Worker expectedWorker = new Worker(hostSpec);
        fixture.updateWorker(expectedWorker);

        StoredWorker actualWorker = fixture.getWorker("localhost", 12345);

        assertEquals(expectedWorker, actualWorker.getWorker());
    }

    @Test
    public void mustUpdateWorkerToContainMoreChildren() throws IOException, ClassNotFoundException {
        HostSpecification originalHostSpec = (HostSpecification) loadSpecResource("../services/h2db/basic");
        Worker originalWorker = new Worker(originalHostSpec);
        fixture.updateWorker(originalWorker);

        HostSpecification updatedHostSpec = (HostSpecification) loadSpecResource("../services/h2db/basic_more_children");
        Worker updatedWorker = new Worker(updatedHostSpec);
        fixture.updateWorker(updatedWorker);

        StoredWorker actualWorker = fixture.getWorker("localhost", 12345);

        assertEquals(updatedWorker, actualWorker.getWorker());
    }

    @Test
    public void mustUpdateWorkerToContainLessChildren() throws IOException, ClassNotFoundException {
        HostSpecification originalHostSpec = (HostSpecification) loadSpecResource("../services/h2db/basic");
        Worker originalWorker = new Worker(originalHostSpec);
        fixture.updateWorker(originalWorker);

        HostSpecification updatedHostSpec = (HostSpecification) loadSpecResource("../services/h2db/basic_less_children");
        Worker updatedWorker = new Worker(updatedHostSpec);
        fixture.updateWorker(updatedWorker);
        
        StoredWorker actualWorker = fixture.getWorker("localhost", 12345);

        assertEquals(updatedWorker, actualWorker.getWorker());
    }

    @Test
    public void mustNotGetNonExistantWorker() throws IOException {
        StoredWorker worker = fixture.getWorker("fake", 12345);
        assertNull(worker);
    }
    
}
