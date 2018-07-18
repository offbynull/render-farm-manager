package com.offbynull.rfm.host.services.h2db;

import com.offbynull.rfm.host.service.Direction;
import static com.offbynull.rfm.host.service.Direction.BACKWARD;
import static com.offbynull.rfm.host.service.Direction.FORWARD;
import com.offbynull.rfm.host.service.Worker;
import static com.offbynull.rfm.host.services.h2db.WorkerGetter.getWorker;
import java.math.BigDecimal;
import java.sql.Connection;
import static java.sql.Connection.TRANSACTION_READ_COMMITTED;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.Validate;

final class WorkerScanner {
    static List<Worker> getWorkers(Connection conn, String lastHost, int lastPort, Direction direction, int max) throws SQLException {
        Validate.notNull(conn);
        // key CAN be null -- it means start at the beginning
        Validate.notNull(direction);
        Validate.notEmpty(lastHost);
        Validate.isTrue(lastPort >= 1 && lastPort <= 65535);
        Validate.isTrue(max >= 0);
        
        String selectWorkStr = "select s_host,n_port from host_spec";
        switch (direction) {
            case FORWARD:
                selectWorkStr += " where s_host>=? and n_port>? order by s_host,n_port asc";
                break;
            case BACKWARD:
                selectWorkStr += " where s_host<=? and n_port<? order by s_host,n_port desc";
                break;
            default:
                throw new IllegalStateException(); // should never happen
        }
        

        conn.setAutoCommit(false);
        conn.setTransactionIsolation(TRANSACTION_READ_COMMITTED);

        try (PreparedStatement selectWorkIdPs = conn.prepareStatement(selectWorkStr)) {
            selectWorkIdPs.setString(1, lastHost);
            selectWorkIdPs.setBigDecimal(2, BigDecimal.valueOf(lastPort));
            try (ResultSet selectWorkIdRs = selectWorkIdPs.executeQuery()) {
                List<Worker> ret = new LinkedList<>();
                while (selectWorkIdRs.next() && ret.size() < max) {
                    String host = selectWorkIdRs.getString("s_host");
                    BigDecimal port = selectWorkIdRs.getBigDecimal("n_port");

                    Worker worker = getWorker(conn, host, port.intValueExact());
                    if (worker != null) { // if worker was removed or worker had bad attributes, skip.
                        ret.add(worker);
                    }
                }
                return ret;
            }
        }
    }
}
