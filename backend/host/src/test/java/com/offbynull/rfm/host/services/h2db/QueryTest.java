package com.offbynull.rfm.host.services.h2db;

import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class QueryTest {

    @Test
    public void mustGenerateTokens() {
        QueryTracker qt = new QueryTracker();

        assertEquals(":::q_name_0:::", qt.alias("name"));
        assertEquals(":::q__1:::", qt.alias());
        
        assertEquals(":::p_name_0:::", qt.param("name", "FAKEVALUE1"));
        assertEquals(":::p__1:::", qt.param("FAKEVALUE2"));
        
        assertEquals(
                Map.of(":::p_name_0:::", "FAKEVAL1", ":::p__1:::", "FAKEVAL2"),
                qt.params()
        );
    }

    @Test
    public void mustCompose() {
        Query qr1 = new Query(""
                + "SELECT *"
                + " FROM"
                + " (table1) :::q_name_0:::"
                + " INNER JOIN"
                + " (table2) :::q__0_0_0:::"
                + " ON :::p_name_0:::.VALUE=:::p__1:::.VALUE AND :::p_name_0:::.VALUE=:::p__2:::.VALUE",
                Map.of(":::p_name_0:::", "FAKEVAL", ":::p__1:::", "FAKEVAL", ":::p__2:::", "FAKEVAL"));
        
        QueryTracker qt = new QueryTracker();
        assertEquals(
                "SELECT * FROM (table1) :::q_name_0_0::: INNER JOIN (table2) :::q__0_1::: ON :::p_name_0_0:::.VALUE=:::p__0_1:::.VALUE AND :::p_name_0_0:::.VALUE=:::p__0_2:::.VALUE",
                qr1.compose(qt)
        );
        assertEquals(
                Map.of(":::p_name_0_0:::", "FAKEVAL", ":::p__0_1:::", "FAKEVAL", ":::p__0_2:::", "FAKEVAL"),
                qt.params()
        );
    }

    @Test
    public void mustConvertToJdbcPreparedStatement() {
        Query qr1 = new Query(""
                + "SELECT *"
                + " FROM"
                + " (table1) :::q_name_0:::"
                + " INNER JOIN"
                + " (table2) :::q__0_0_0:::"
                + " ON :::q_name_0:::.VALUE=:::p__1::: AND :::q__0_0_0:::.VALUE=:::p__2::: AND :::p__1:::=:::p__2:::",
                Map.of(":::p__1:::", "FAKEVAL1", ":::p__2:::", "FAKEVAL2"));
        
        assertEquals(
                "SELECT * FROM (table1) q0 INNER JOIN (table2) q1 ON q0.VALUE=? AND q1.VALUE=? AND ?=?",
                qr1.toJdbcQuery()
        );
        assertEquals(
                List.of("FAKEVAL1", "FAKEVAL2", "FAKEVAL1", "FAKEVAL2"),
                qr1.toJdbcParameters()
        );
    }
}
