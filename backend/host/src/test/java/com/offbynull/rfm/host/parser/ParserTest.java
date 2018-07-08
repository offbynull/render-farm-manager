package com.offbynull.rfm.host.parser;

import com.offbynull.rfm.host.model.expression.RequirementFunction;
import com.offbynull.rfm.host.model.expression.RequirementFunctionBuiltIns;
import com.offbynull.rfm.host.model.expression.DataType;
import com.offbynull.rfm.host.model.expression.VariableExpression;
import com.offbynull.rfm.host.model.expression.NumberLiteralExpression;
import com.offbynull.rfm.host.model.expression.BooleanLiteralExpression;
import com.offbynull.rfm.host.model.expression.StringLiteralExpression;
import com.offbynull.rfm.host.model.expression.InvocationExpression;
import com.offbynull.rfm.host.model.requirement.NumberRange;
import com.offbynull.rfm.host.model.requirement.CoreRequirement;
import com.offbynull.rfm.host.model.requirement.CpuRequirement;
import com.offbynull.rfm.host.model.requirement.SocketRequirement;
import com.offbynull.rfm.host.model.requirement.MountRequirement;
import com.offbynull.rfm.host.model.requirement.GpuRequirement;
import com.offbynull.rfm.host.model.requirement.HostRequirement;
import com.offbynull.rfm.host.model.requirement.RamRequirement;
import com.offbynull.rfm.host.service.Work;
import java.math.BigDecimal;
import java.util.ArrayList;
import static java.util.Arrays.asList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ParserTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    
    private Parser fixture;
    
    @Before
    public void before() {
        List<RequirementFunction> reqFunctions = new ArrayList<>();
        reqFunctions.add(new RequirementFunction(DataType.STRING, "rtestfunc", DataType.NUMBER));
        
        List<TagFunction> tagFunctions = new ArrayList<>();
        tagFunctions.add(new TagFunction("abs", args -> ((BigDecimal) args.get(0)).abs()));
        tagFunctions.add(new TagFunction("to_str", args -> ((BigDecimal) args.get(0)).toString()));
        
        fixture = new Parser(reqFunctions, tagFunctions);
    }



    @Test
    public void mustParseAll() throws Exception {
        Work work = fixture.parseScript(""
                // ID, followed by pickup chance, followed by dependency IDs
                + "id\n"
                + "0.5\n"
                + "dep_id1\n"
                + "dep_id2\n"
                + "dep_id3\n"
                + "\n"
                // Tags are used to allow better searching of work. For example, if your work is for a specific show, you may add the show
                // name under the tag s_show. That way, you can find all work for a given show much easier. Note that tags can be
                // referenced in other tags + they can also be referenced in requirement where clauses.
                + "b_tag_test1=true && false && false\n"
                + "n_tag_test2=800\n"
                + "s_gpu_vendor=\"nvidia\"\n"
                + "\n"
                // There is a hierarchy of requirements here. Specifying things as a hierarchy will become more useful when studios have
                // more complex hardware setups. Check out https://www.open-mpi.org/projects/hwloc/lstopo/ for examples of complex hardware
                // architecture where NUMA, multiple PCI buses, and PCI buses being bound to different CPU sockets is a thing. These
                // complex examples are cases where you want to explicitly select certain resources by heirarchy rather than just any
                // resources. For example, your code may run faster if you were to ...
                //    1. only select GPUs that are on the same PCI bus as the CPU sockets you selected.
                //    2. only assign memoy from the NUMA nodes that the CPUs you selected have direct access to (minimize hops).
                + "[1,20] host {\n"
                       // Remember that GPU is a generic identifier for any GPU and that its properties aren't set in stone. For example, if
                       // you wanted AMD gpus running on OpenCL 2.2, the where condition might be...
                       //    (gpu.s_vendor="amd" && gpu.n_opencl_version="2.2") && host.n_free_mem >= cuda.n_opencl_total_mem
                       // Obviously, AMD GPUs wouldn't contain the CUDA properties seen below (e.g. no gpu.n_cuda_major_version).
                + "    [1,5] gpu with 1 capacity where (gpu.s_vendor==s_gpu_vendor && gpu.n_cuda_major_version>=7) && host.n_free_mem >= gpu.n_cuda_total_mem\n"
                       // Remember that, just like GPU, CPU is a generic identifier for any CPU and that its properties aren't set in stone.
                       // AMD CPUs might have properties missing from Intel CPUs and vice-versa.
                       //
                       // Also, remember that CPU hierarchy isn't nessecarily needed. For example, if you have the core requirement directly
                       // under host (instead of under socket), the system won't nessecarily give you cores from the same socket.
                + "    [3,40] socket where socket.s_vendor==\"intel\" && socket.s_family==\"xeon\" {\n"
                + "        [1,9999] core where core.n_siblings>=2 {\n"
                + "            2 cpus with 100000 capacity\n"
                + "        }\n"
                + "    }\n"
                       // In the future, you may be able to declare RAM requirements directly under the CPU/core/socket in order to lock
                       // to the NUMA node(s) of that CPU/core/socket.
                + "    1 ram with [4gb,9gb] capacity where ram.n_mhz>=2666 && ram.s_type==\"ddr4\"\n"
                + "    1 mount with [10gb,40gb] capacity where !mount.b_rotational"
                + "}");

        assertEquals("id", work.getId());
        assertEquals("0.5", work.getPriority().toPlainString());
        assertEquals(new HashSet<>(asList("dep_id1", "dep_id2", "dep_id3")), work.getParents());
        
        Map<String, Object> tags = work.getTags();
        assertEquals(false, tags.get("b_tag_test1"));
        assertTrue(new BigDecimal("800").compareTo((BigDecimal) tags.get("n_tag_test2")) == 0);
        assertEquals("nvidia", tags.get("s_gpu_vendor"));
        
        
        
        String reqsScript = work.getRequirementsScript();
        HostRequirement req = fixture.parseScriptReqs(tags, reqsScript);
        
        List<SocketRequirement> socketReqs = req.getSocketRequirements();
        assertEquals(1, socketReqs.size());
        assertRange(3L, 40L, socketReqs.get(0).getCount());
        assertEquals(InvocationExpression.class, socketReqs.get(0).getWhereCondition().getClass());
        List<CoreRequirement> coreReqs = socketReqs.get(0).getCoreRequirements();
        assertEquals(1, coreReqs.size());
        assertRange(1L, 9999L, coreReqs.get(0).getCount());
        assertEquals(InvocationExpression.class, coreReqs.get(0).getWhereCondition().getClass());
        List<CpuRequirement> cpuReqs = coreReqs.get(0).getCpuRequirements();
        assertEquals(1, cpuReqs.size());
        assertRange(2L, 2L, cpuReqs.get(0).getCount());
        assertEquals(BooleanLiteralExpression.class, cpuReqs.get(0).getWhereCondition().getClass());
        assertRange(100000L, 100000L, cpuReqs.get(0).getCapacityRange());
        
        List<GpuRequirement> gpuReqs = req.getGpuRequirements();
        assertEquals(1, gpuReqs.size());
        assertRange(1L, 5L, gpuReqs.get(0).getCount());
        assertEquals(InvocationExpression.class, gpuReqs.get(0).getWhereCondition().getClass());
        
        List<RamRequirement> ramReqs = req.getRamRequirements();
        assertEquals(1, ramReqs.size());
        assertRange(1L, 1L, ramReqs.get(0).getCount());
        assertEquals(InvocationExpression.class, ramReqs.get(0).getWhereCondition().getClass());
        assertRange(4294967296L, 9663676416L, ramReqs.get(0).getCapacityRange());
        
        List<MountRequirement> mountReqs = req.getMountRequirements();
        assertEquals(1, mountReqs.size());
        assertRange(1L, 1L, mountReqs.get(0).getCount());
        assertEquals(InvocationExpression.class, mountReqs.get(0).getWhereCondition().getClass());
        assertRange(10737418240L, 42949672960L, mountReqs.get(0).getCapacityRange());
    }

    @Test
    public void mustParseWhereExpression() throws Exception {
        Work work = fixture.parseScript(""
                + "id\n"
                + "0.5\n"
                + ""
                + "\n"
                + "[1,20] host {\n"
                + "    1 gpu with 1 capacity where gpu.n_cuda_major_version==7 || gpu.n_cuda_sm_cores>=12\n"
                + "    1 cpu with 100000 capacity\n"
                + "    1 ram with 4gb capacity\n"
                + "    1 mount with 10gb capacity\n"
                + "}");
        HostRequirement hostReq = fixture.parseScriptReqs(work.getTags(), work.getRequirementsScript());
        
        InvocationExpression expr = (InvocationExpression) hostReq.getGpuRequirements().get(0).getWhereCondition();
        assertEquals(RequirementFunctionBuiltIns.OR_B_BB_NAME, expr.getFunction().getName());
        
        InvocationExpression     exprL = (InvocationExpression) expr.getArguments().get(0);               // cuda.n_major_version==7
        VariableExpression       exprLL = (VariableExpression) exprL.getArguments().get(0);                   // cuda.n_major_version
        NumberLiteralExpression  exprLR = (NumberLiteralExpression) exprL.getArguments().get(1);              // 7
        assertEquals(RequirementFunctionBuiltIns.EQUAL_B_NN_NAME, exprL.getFunction().getName());
        assertEquals("n_cuda_major_version", exprLL.getName());
        assertEquals("gpu", exprLL.getScope());
        assertEquals(7L, exprLR.getValue().longValueExact());
        
        InvocationExpression     exprR = (InvocationExpression) expr.getArguments().get(1);               // cuda.n_sm_cores>=12 (the op >= maps to > OR ==)
        InvocationExpression     exprRL = (InvocationExpression) exprR.getArguments().get(0);                 // cuda.n_sm_cores>12
        VariableExpression       exprRLL = (VariableExpression) exprRL.getArguments().get(0);                     // cuda.n_sm_cores
        NumberLiteralExpression  exprRLR = (NumberLiteralExpression) exprRL.getArguments().get(1);                // 12
        InvocationExpression     exprRR = (InvocationExpression) exprR.getArguments().get(1);             // cuda.n_sm_cores==12
        VariableExpression       exprRRL = (VariableExpression) exprRR.getArguments().get(0);                     // cuda.n_sm_cores
        NumberLiteralExpression  exprRRR = (NumberLiteralExpression) exprRR.getArguments().get(1);                // 12
        assertEquals(RequirementFunctionBuiltIns.OR_B_BB_NAME, exprR.getFunction().getName());
        assertEquals(RequirementFunctionBuiltIns.GREATER_THAN_B_NN_NAME, exprRL.getFunction().getName());
        assertEquals("n_cuda_sm_cores", exprRLL.getName());
        assertEquals("gpu", exprRLL.getScope());
        assertEquals(12L, exprRLR.getValue().longValueExact());
        assertEquals(RequirementFunctionBuiltIns.EQUAL_B_NN_NAME, exprRR.getFunction().getName());
        assertEquals("n_cuda_sm_cores", exprRRL.getName());
        assertEquals("gpu", exprRRL.getScope());
        assertEquals(12L, exprRRR.getValue().longValueExact());
    }

    @Test
    public void mustParseWithoutTags() throws Exception {
        Work work = fixture.parseScript(""
                + "id\n"
                + "0.5\n"
                + "dep_id1\n"
                + "dep_id2\n"
                + "dep_id3\n"
                + "\n"
                + "[1,20] host {\n"
                + "    1 gpu with 1 capacity\n"
                + "    1 cpu with 100000 capacity\n"
                + "    1 ram with 4gb capacity\n"
                + "    1 mount with 10gb capacity\n"
                + "}");

        assertTrue(work.getTags().isEmpty());
    }

    @Test
    public void mustParseWithoutDeps() throws Exception {
        Work work = fixture.parseScript(""
                + "id\n"
                + "0.5\n"
                + "\n"
                + "b_tag_test1=true && false && false\n"
                + "n_tag_test2=800\n"
                + "s_tag_test3=\"aaabbb\"\n"
                + "\n"
                + "[1,20] host {\n"
                + "    1 gpu with 1 capacity\n"
                + "    1 cpu with 100000 capacity\n"
                + "    1 ram with 4gb capacity\n"
                + "    1 mount with 10gb capacity\n"
                + "}");

        assertTrue(work.getParents().isEmpty());
    }

    @Test
    public void mustFailToParseWhenChanceOutOfRange() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        fixture.parseScript(""
                + "id\n"
                + "1.01\n"
                + "\n"
                + "b_tag_test1=true && false && false\n"
                + "n_tag_test2=800\n"
                + "s_tag_test3=\"aaabbb\"\n"
                + "\n"
                + "[1,20] host {\n"
                + "    1 gpu with 1 capacity\n"
                + "    1 cpu\n"
                + "    1 ram with 4gb capacity\n"
                + "    1 mount with 10gb capacity\n"
                + "}");
    }

    @Test
    public void mustFailToParseWithNoChance() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        fixture.parseScript(""
                + "id\n"
                + "\n"
                + "b_tag_test1=true && false && false\n"
                + "n_tag_test2=800\n"
                + "s_tag_test3=\"aaabbb\"\n"
                + "\n"
                + "[1,20] host {\n"
                + "    1 gpu with 1 capacity\n"
                + "    1 cpu\n"
                + "    1 ram with 4gb capacity\n"
                + "    1 mount with 10gb capacity\n"
                + "}");
    }

    @Test
    public void mustFailToParseIfIdInDeps() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        fixture.parseScript(""
                + "id\n"
                + "0.5\n"
                + "dep_id1\n"
                + "id\n"
                + "dep_id3\n"
                + "\n"
                + "[1,20] host {\n"
                + "    1 gpu with 1 capacity\n"
                + "    1 cpu\n"
                + "    1 ram with 4gb capacity\n"
                + "    1 mount with 10gb capacity\n"
                + "}");
    }

    
    
    
    
    
    @Test
    public void mustParseCpuSocket() throws Exception {
        Work work = fixture.parseScript(""
                + "id\n"
                + "0.5\n"
                + "\n"
                + "[1,20] host {\n"
                + "    [1,9999] socket {\n"
                + "        [1,9999] core{\n"
                + "            [1,9999] cpu with 100000 capacity\n"
                + "        }\n"
                + "    }\n"
                + "    1 ram with 4gb capacity\n"
                + "    1 mount with 10gb capacity\n"
                + "}");
        HostRequirement hostReq = fixture.parseScriptReqs(work.getTags(), work.getRequirementsScript());
        
        assertEquals(1,  hostReq.getSocketRequirements().size());
        SocketRequirement socketReq = hostReq.getSocketRequirements().get(0);
        assertRange(1, 9999, socketReq.getCount());

        assertEquals(1, socketReq.getCoreRequirements().size());
        CoreRequirement coreReq = socketReq.getCoreRequirements().get(0);
        assertRange(1, 9999, coreReq.getCount());

        assertEquals(1, coreReq.getCpuRequirements().size());
        CpuRequirement cpuReq = coreReq.getCpuRequirements().get(0);
        assertRange(1, 9999, cpuReq.getCount());

        assertRange(100000, 100000, cpuReq.getCapacityRange());
    }
    
    
    
    
    
    
    @Test
    public void mustParseDiskMount() {
        Work work = fixture.parseScript(""
                + "id\n"
                + "0.5\n"
                + ""
                + "\n"
                + "[1,20] host {\n"
                + "    1 socket {\n"
                + "        1 core{\n"
                + "            1 cpu with 100000 capacity\n"
                + "        }\n"
                + "    }\n"
                + "    1 ram with 4gb capacity\n"
                + "    2 mount with 10gb capacity\n"
                + "    }\n"
                + "}");
        HostRequirement hostReq = fixture.parseScriptReqs(work.getTags(), work.getRequirementsScript());

        assertEquals(1, hostReq.getMountRequirements().size());
        MountRequirement mountReq = hostReq.getMountRequirements().get(0);
        assertRange(2, 2, mountReq.getCount());

        assertRange(10737418240L, 10737418240L, mountReq.getCapacityRange());
    }
    




    
    
    @Test
    public void mustParseIfNonEssentialRequirementsMissing() throws Exception {
        Work work = fixture.parseScript(""
                + "id\n"
                + "0.5\n"
                + "\n"
                + "[1,20] host {\n"
                + "    1 socket {\n"
                + "        1 core{\n"
                + "            1 cpu with 100000 capacity\n"
                + "        }\n"
                + "    }\n"
                + "    1 ram with 4gb capacity\n"
                + "    1 mount with 10gb capacity\n"
                + "}");
        fixture.parseScriptReqs(work.getTags(), work.getRequirementsScript());
    }

    @Test
    public void mustNotFailParseIfMountRequirementMissing() throws Exception {
        fixture.parseScript(""
                + "id\n"
                + "0.5\n"
                + "\n"
                + "[1,20] host {\n"
                + "    1 socket {\n"
                + "        1 core{\n"
                + "            1 cpu with 100000 capacity\n"
                + "        }\n"
                + "    }\n"
                + "    1 ram with 4gb capacity\n"
                + "}");
    }

    @Test
    public void mustNotFailParseIfRamRequirementMissing() throws Exception {
        fixture.parseScript(""
                + "id\n"
                + "0.5\n"
                + ""
                + "\n"
                + "[1,20] host {\n"
                + "    1 socket {\n"
                + "        1 core{\n"
                + "            1 cpu with 100000 capacity\n"
                + "        }\n"
                + "    }\n"
                + "    1 mount with 4gb capacity\n"
                + "}");
    }

    @Test
    public void mustNotFailParseIfMultipleRamSelections() throws Exception {
        fixture.parseScript(""
                + "id\n"
                + "0.5\n"
                + "\n"
                + "[1,20] host {\n"
                + "    1 socket {\n"
                + "        1 core{\n"
                + "            1 cpu with 100000 capacity\n"
                + "        }\n"
                + "    }\n"
                + "    1 ram with 4gb capacity\n"
                + "    1 ram with 4gb capacity\n"
                + "    1 mount with 10gb capacity\n"
                + "}");
    }

    @Test
    public void mustNotFailParseIfMoreThan1RamSelections() throws Exception {
        fixture.parseScript(""
                + "id\n"
                + "0.5\n"
                + "\n"
                + "[1,20] host {\n"
                + "    1 socket {\n"
                + "        1 core{\n"
                + "            1 cpu with 100000 capacity\n"
                + "        }\n"
                + "    }\n"
                + "    2 ram with 4gb capacity\n"
                + "    1 mount with 10gb capacity\n"
                + "}");
    }

    @Test
    public void mustNotFailParseIfMissingCpuSelection() throws Exception {
        fixture.parseScript(""
                + "id\n"
                + "0.5\n"
                + "\n"
                + "[1,20] host {\n"
                + "    1 ram with 4gb capacity\n"
                + "    1 mount with 4gb capacity\n"
                + "}");
    }

    @Test
    public void mustFailParseIfMissingCpuCapacitySelection() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        fixture.parseScript(""
                + "id\n"
                + "0.5\n"
                + "\n"
                + "[1,20] host {\n"
                + "    1 socket {\n"
                + "        1 core{\n"
                + "            1 cpu\n"
                + "        }\n"
                + "    }\n"
                + "    2 ram with 4gb capacity\n"
                + "    1 mount with 10gb capacity\n"
                + "}");
    }

    @Test
    public void mustFailParseIfMissingRamCapacitySelection() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        fixture.parseScript(""
                + "id\n"
                + "0.5\n"
                + "\n"
                + "[1,20] host {\n"
                + "    1 socket {\n"
                + "        1 core{\n"
                + "            1 cpu with 100000 capacity\n"
                + "        }\n"
                + "    }\n"
                + "    2 ram\n"
                + "    1 mount with 10gb capacity\n"
                + "}");
    }
    
    @Test
    public void mustFailParseIfMissingMountCapacitySelection() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        fixture.parseScript(""
                + "id\n"
                + "0.5\n"
                + "\n"
                + "[1,20] host {\n"
                + "    1 socket {\n"
                + "        1 core{\n"
                + "            1 cpu with 100000 capacity\n"
                + "        }\n"
                + "    }\n"
                + "    2 ram with 4gb capacity\n"
                + "    1 mount\n"
                + "}");
    }
    
    
    
    
    
    
    @Test
    public void mustCallTagFunctionsWhenParsingTag() throws Exception {
        Work work = fixture.parseScript(""
                + "id\n"
                + "0.5\n"
                + "\n"
                + "s_test_tag=to_str(abs(-5))"
                + "\n"
                + "1 host {\n"
                + "    1 cpu with 100000 capacity\n"
                + "    1 ram with 4gb capacity\n"
                + "    1 mount with 10gb capacity\n"
                + "}");
        
        assertEquals("5", work.getTags().get("s_test_tag"));
    }
    
    @Test
    public void mustCallRequirementFunctionsWhenParsingWhereCondition() throws Exception {
        Work work = fixture.parseScript(""
                + "id\n"
                + "0.5\n"
                + "\n"
                + "s_test_tag=to_str(abs(-5))"
                + "\n"
                + "1 host where rtestfunc(1234)==\"6\" {\n"
                + "    1 cpu with 100000 capacity\n"
                + "    1 ram with 4gb capacity\n"
                + "    1 mount with 10gb capacity\n"
                + "}");
        HostRequirement hostReq = fixture.parseScriptReqs(work.getTags(), work.getRequirementsScript());
        
        InvocationExpression expr = (InvocationExpression) hostReq.getWhereCondition();
        assertEquals(RequirementFunctionBuiltIns.EQUAL_B_SS_NAME, expr.getFunction().getName());
        InvocationExpression exprL = (InvocationExpression) expr.getArguments().get(0);
        assertEquals("rtestfunc", exprL.getFunction().getName());
        assertEquals(asList(DataType.NUMBER), exprL.getFunction().getParameterTypes());
        assertEquals(DataType.STRING, exprL.getFunction().getReturnType());
        assertEquals(1, exprL.getArguments().size());
        assertEquals(1234, ((NumberLiteralExpression) exprL.getArguments().get(0)).getValue().intValue());
        StringLiteralExpression exprR = (StringLiteralExpression) expr.getArguments().get(1);
        assertEquals("6", exprR.getValue());
    }
    
    
    
    
    
    
    @Test
    public void mustProperlyReferenceTagsWhenParsing() throws Exception {
        Work work = fixture.parseScript(""
                + "id\n"
                + "0.5\n"
                + "\n"
                + "n_tag_a=-5\n"
                + "n_tag_b=abs(n_tag_a)\n"
                + "\n"
                + "1 host where n_tag_a==n_tag_b {\n"
                + "    1 cpu with 100000 capacity\n"
                + "    1 ram with 4gb capacity\n"
                + "    1 mount with 10gb capacity\n"
                + "}");
        
        
        Map<String, Object> tags = work.getTags();
        assertEquals(-5, ((BigDecimal) tags.get("n_tag_a")).intValueExact());
        assertEquals(5, ((BigDecimal) tags.get("n_tag_b")).intValueExact());
        
        
        HostRequirement hostReq = fixture.parseScriptReqs(work.getTags(), work.getRequirementsScript());
        
        InvocationExpression expr = (InvocationExpression) hostReq.getWhereCondition();
        assertEquals(RequirementFunctionBuiltIns.EQUAL_B_NN_NAME, expr.getFunction().getName());
        NumberLiteralExpression exprL = (NumberLiteralExpression) expr.getArguments().get(0);
        assertEquals(-5, exprL.getValue().intValueExact());
        NumberLiteralExpression exprR = (NumberLiteralExpression) expr.getArguments().get(1);
        assertEquals(5, exprR.getValue().intValueExact());
    }
    
    
    
    
    
    
    private static void assertRange(long start, long end, NumberRange range) {
        assertEquals(start, range.getStart().longValueExact());
        assertEquals(end, range.getEnd().longValueExact());
    }
}
