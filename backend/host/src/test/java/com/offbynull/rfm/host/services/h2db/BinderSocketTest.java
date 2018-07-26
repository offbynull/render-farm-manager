package com.offbynull.rfm.host.services.h2db;

import com.offbynull.rfm.host.model.partition.SocketPartition;
import com.offbynull.rfm.host.model.requirement.SocketRequirement;
import com.offbynull.rfm.host.model.specification.CapacityEnabledSpecification;
import com.offbynull.rfm.host.model.specification.SocketSpecification;
import com.offbynull.rfm.host.parser.Parser;
import static com.offbynull.rfm.host.services.h2db.BinderTestUtils.assertSocketPartition;
import static com.offbynull.rfm.host.services.h2db.BinderTestUtils.assertCorePartition;
import static com.offbynull.rfm.host.services.h2db.BinderTestUtils.assertCpuPartition;
import static com.offbynull.rfm.host.services.h2db.BinderTestUtils.createCapacityMap;
import com.offbynull.rfm.host.testutils.TestUtils;
import java.math.BigDecimal;
import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.EMPTY_MAP;
import java.util.IdentityHashMap;
import org.junit.Test;

public class BinderSocketTest {
    
    private final Parser parser = new Parser(EMPTY_LIST, EMPTY_LIST);
    
    @Test
    public void mustBindToIndividualSocketWithSingleCoreRequirement() throws Exception {
        SocketRequirement socketReq = (SocketRequirement) parser.parseReq(
                EMPTY_MAP,
                ""
                + "1 socket {"
                + "    2 core {"
                + "        2 cpu with 100000 capacity"
                + "    }"
                + "}"
        );
        SocketSpecification socketSpec = (SocketSpecification) TestUtils.loadSpec(
                ""
                + "socket{\n"
                + "  core{\n"
                + "     cpu:100000{ n_cpu_id:0 }\n"
                + "     cpu:100000{ n_cpu_id:1 }\n"
                + "     n_core_id:0\n"
                + "  }\n"
                + "  core{\n"
                + "    cpu:100000{ n_cpu_id:0 }\n"
                + "    cpu:100000{ n_cpu_id:1 }\n"
                + "    n_core_id:1\n"
                + "  }\n"
                + "  core{\n"
                + "    cpu:100000{ n_cpu_id:0 }\n"
                + "    cpu:100000{ n_cpu_id:1 }\n"
                + "    n_core_id:2\n"
                + "  }\n"
                + "  n_socket_id:0\n"
                + "}"
        );

        IdentityHashMap<CapacityEnabledSpecification, BigDecimal> updatableCapacities = createCapacityMap(socketSpec);
        
        SocketPartition socketPartition_0 = Binder.partitionIndividualSocket(updatableCapacities, socketReq, socketSpec);
        assertSocketPartition(socketPartition_0, 0,
                corePartition -> assertCorePartition(corePartition, 0,
                        cpuPartition -> assertCpuPartition(cpuPartition, 0, 100000),
                        cpuPartition -> assertCpuPartition(cpuPartition, 1, 100000)
                ),
                corePartition -> assertCorePartition(corePartition, 1,
                        cpuPartition -> assertCpuPartition(cpuPartition, 0, 100000),
                        cpuPartition -> assertCpuPartition(cpuPartition, 1, 100000)
                )
        );
    }
    
    @Test
    public void mustBindToIndividualSocketWithMultiCoreRequirements() throws Exception {
        SocketRequirement socketReq = (SocketRequirement) parser.parseReq(
                EMPTY_MAP,
                ""
                + "1 socket {"
                + "    2 core {"
                + "        2 cpu with 75000 capacity"
                + "    }"
                + "    1 core {"
                + "        1 cpu with 20000 capacity"
                + "    }"
                + "}"
        );
        SocketSpecification socketSpec = (SocketSpecification) TestUtils.loadSpec(
                ""
                + "socket{\n"
                + "  core{\n"
                + "     cpu:100000{ n_cpu_id:0 }\n"
                + "     cpu:100000{ n_cpu_id:1 }\n"
                + "     n_core_id:0\n"
                + "  }\n"
                + "  core{\n"
                + "    cpu:100000{ n_cpu_id:0 }\n"
                + "    cpu:100000{ n_cpu_id:1 }\n"
                + "    n_core_id:1\n"
                + "  }\n"
                + "  n_socket_id:0\n"
                + "}"
        );

        IdentityHashMap<CapacityEnabledSpecification, BigDecimal> updatableCapacities = createCapacityMap(socketSpec);
        
        SocketPartition socketPartition_0 = Binder.partitionIndividualSocket(updatableCapacities, socketReq, socketSpec);
        assertSocketPartition(socketPartition_0, 0,
                corePartition -> assertCorePartition(corePartition, 0,
                        cpuPartition -> assertCpuPartition(cpuPartition, 0, 75000),
                        cpuPartition -> assertCpuPartition(cpuPartition, 1, 75000)
                ),
                corePartition -> assertCorePartition(corePartition, 1,
                        cpuPartition -> assertCpuPartition(cpuPartition, 0, 75000),
                        cpuPartition -> assertCpuPartition(cpuPartition, 1, 75000)
                ),
                corePartition -> assertCorePartition(corePartition, 0,
                        cpuPartition -> assertCpuPartition(cpuPartition, 0, 20000)
                )
        );
    }
    
    @Test
    public void mustBindToIndividualSocketWithUnboundedCoreRequirement() {
        // have 1 bounded core and 1 unbounded core
    }
    
    @Test
    public void mustBindToIndividualCoreWithMultipleCpuRequirements() {

    }
    
    @Test
    public void mustBindAcrossAllCoresWithSingleCpuRequirement() {

    }
    
    @Test
    public void mustBindAcrossAllCoresWithMultipleCpuRequirements() {

    }
}
