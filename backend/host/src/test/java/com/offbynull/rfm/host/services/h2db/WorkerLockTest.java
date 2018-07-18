package com.offbynull.rfm.host.services.h2db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import static java.util.Locale.ENGLISH;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;

public class WorkerLockTest {

    private JdbcDataSource dataSource;
    private Connection keepAliveConnection;
    
    @Before
    public void before() throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:test");
        dataSource.setUser("sa");
        dataSource.setPassword("sa");
        
        keepAliveConnection = dataSource.getConnection();
        
        try (Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement()) {
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            conn.setAutoCommit(false);
                    
            stmt.execute(""
                    + "CREATE TABLE worker_lock ("
                    + "  s_host VARCHAR(256) NOT NULL,"
                    + "  n_port NUMBER(38,10) NOT NULL,"
                    + "  PRIMARY KEY(s_host, n_port)"
                    + ")"
            );
            stmt.execute(""
                    + "CREATE TABLE worker ("
                    + "  s_host VARCHAR(256) NOT NULL,"
                    + "  n_port NUMBER(38,10) NOT NULL,"
                    + "  val INT,"
                    + "  PRIMARY KEY(s_host, n_port)"
                    + ")"
            );
        }
    }
    
    @After
    public void after() throws Exception {
        keepAliveConnection.close();
    }
    
    @Test
    public void mustNotDeadlock() throws Exception {
        try (Connection conn1 = dataSource.getConnection();
                Connection conn2 = dataSource.getConnection()) {
            conn1.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            conn1.setAutoCommit(false);
            conn2.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            conn2.setAutoCommit(false);
            
            try (WorkerLock lock1 = WorkerLock.lock(conn1, "localhost1", 12345);
                    WorkerLock lock2 = WorkerLock.lock(conn2, "localhost2", 12345)) {
                // do nothing
            }
        }
    }
    
    @Test
    public void mustDeadlockAndTimeoutIfLockRowIsBeingInserted() throws Exception {
        try (Connection conn1 = dataSource.getConnection();
                Connection conn2 = dataSource.getConnection()) {
            conn1.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            conn1.setAutoCommit(false);
            conn2.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            conn2.setAutoCommit(false);
            
            try (WorkerLock lock1 = WorkerLock.lock(conn1, "localhost", 12345)) {
                try {
                    WorkerLock lock2 = WorkerLock.lock(conn2, "localhost", 12345);
                } catch (SQLException sqle) {
                    assertTrue(sqle.getMessage().toLowerCase(ENGLISH).contains("timeout"));
                    return;
                }
            }
            fail();
        }
    }
    
    @Test
    public void mustDeadlockAndTimeoutIfLockRowIsBeingUpdated() throws Exception {
        try (Connection conn1 = dataSource.getConnection();
                Connection conn2 = dataSource.getConnection();
                Statement stmt1 = conn1.createStatement()) {
            conn1.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            conn1.setAutoCommit(false);
            conn2.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            conn2.setAutoCommit(false);
                    
            stmt1.execute("INSERT INTO worker VALUES('localhost', 12345, 0)");
            conn1.commit();
            
            try (WorkerLock lock1 = WorkerLock.lock(conn1, "localhost", 12345)) {
                try {
                    WorkerLock lock2 = WorkerLock.lock(conn2, "localhost", 12345);
                } catch (SQLException sqle) {
                    assertTrue(sqle.getMessage().toLowerCase(ENGLISH).contains("timeout"));
                    return;
                }
            }
            fail();
        }
    }
    
    @Test
    public void mustCreateAndDeleteLockRowIfNoWorkerRowExists() throws Exception {
        try (Connection conn1 = dataSource.getConnection();
                Statement stmt1 = conn1.createStatement()) {
            conn1.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            conn1.setAutoCommit(false);
            
            try (ResultSet rs = stmt1.executeQuery("SELECT * FROM worker_lock")) {
                assertFalse(rs.next());
            }
            
            try (WorkerLock lock1 = WorkerLock.lock(conn1, "localhost", 12345)) {
                try (ResultSet rs = stmt1.executeQuery("SELECT * FROM worker_lock")) {
                    assertTrue(rs.next());
                }
            }

            try (ResultSet rs = stmt1.executeQuery("SELECT * FROM worker_lock")) {
                assertFalse(rs.next());
            }
        }
    }
    
    @Test
    public void mustNotDeleteLockRowIfWorkerRowExists() throws Exception {
        try (Connection conn1 = dataSource.getConnection();
                Statement stmt1 = conn1.createStatement()) {
            conn1.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            conn1.setAutoCommit(false);
            
            stmt1.execute("INSERT INTO worker_lock VALUES('localhost', 12345)");
            stmt1.execute("INSERT INTO worker VALUES('localhost', 12345, 0)");
            conn1.commit();
            
            try (ResultSet rs = stmt1.executeQuery("SELECT * FROM worker_lock")) {
                assertTrue(rs.next());
            }
            
            try (WorkerLock lock1 = WorkerLock.lock(conn1, "localhost", 12345)) {
                try (ResultSet rs = stmt1.executeQuery("SELECT * FROM worker_lock")) {
                    assertTrue(rs.next());
                }
            }
            
            try (ResultSet rs = stmt1.executeQuery("SELECT * FROM worker_lock")) {
                assertTrue(rs.next());
            }
        }
    }
    
    @Test
    public void mustDeleteLockRowIfWorkerRowRemoved() throws Exception {
        try (Connection conn1 = dataSource.getConnection();
                Statement stmt1 = conn1.createStatement()) {
            conn1.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            conn1.setAutoCommit(false);
            
            stmt1.execute("INSERT INTO worker_lock VALUES('localhost', 12345)");
            stmt1.execute("INSERT INTO worker VALUES('localhost', 12345, 0)");
            conn1.commit();
            
            try (ResultSet rs = stmt1.executeQuery("SELECT * FROM worker_lock")) {
                assertTrue(rs.next());
            }
            
            try (WorkerLock lock1 = WorkerLock.lock(conn1, "localhost", 12345)) {
                try (ResultSet rs = stmt1.executeQuery("SELECT * FROM worker_lock")) {
                    assertTrue(rs.next());
                }
            }
            
            try (ResultSet rs = stmt1.executeQuery("SELECT * FROM worker_lock")) {
                assertTrue(rs.next());
            }
        }
    }
}
