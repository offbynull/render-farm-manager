package com.offbynull.rfm.host.services.h2db;

import com.offbynull.rfm.host.parser.Parser;
import com.offbynull.rfm.host.service.Work;
import java.io.IOException;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.sql.Connection;
import java.sql.SQLException;
import static java.util.Collections.EMPTY_LIST;
import org.apache.commons.io.IOUtils;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

public class WorkGetterTest {
    
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

        WorkPrimer.prime(dataSource);
    }
    
    @After
    public void tearDown() throws SQLException {
        conn.close(); // Close the final open connection, releasing in-memory database. Contents of database discarded.
    }
    
    @Test
    public void mustGetWorker() throws SQLException, ClassNotFoundException, IOException {
        Work expectedWork1 = loadWorkIntoDatabase("work1");
        Work expectedWork2 = loadWorkIntoDatabase("work2");
        Work expectedWork3 = loadWorkIntoDatabase("work3");
        
        Work actualWork3 = WorkGetter.getWork(conn, "work3");
        Work actualWork2 = WorkGetter.getWork(conn, "work2");
        Work actualWork1 = WorkGetter.getWork(conn, "work1");
        
        assertEquals(expectedWork1, actualWork1);
        assertEquals(expectedWork2, actualWork2);
        assertEquals(expectedWork3, actualWork3);
    }
    
    private Work loadWorkIntoDatabase(String name) throws ClassNotFoundException, IOException, SQLException {
        String res = IOUtils.resourceToString("/com/offbynull/rfm/host/services/h2db/" + name, UTF_8);
        Work work = new Parser(EMPTY_LIST, EMPTY_LIST).parseScript(res);
        
        WorkSetter.setWork(conn, work);
        
        return work;
    }
}
