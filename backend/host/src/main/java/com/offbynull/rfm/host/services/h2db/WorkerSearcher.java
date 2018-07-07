package com.offbynull.rfm.host.services.h2db;

import com.offbynull.rfm.host.model.requirement.CapacityEnabledRequirement;
import com.offbynull.rfm.host.model.requirement.HostRequirement;
import com.offbynull.rfm.host.model.requirement.Requirement;
import com.offbynull.rfm.host.parser.Parser;
import static com.offbynull.rfm.host.services.h2db.InternalUtils.getRequirementChildren;
import static com.offbynull.rfm.host.services.h2db.InternalUtils.getRequirementName;
import java.math.BigDecimal;
import static java.math.BigDecimal.ONE;
import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.EMPTY_MAP;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.lang3.Validate;

final class WorkerSearcher {
    private WorkerSearcher() {
        // do nothing
    }
    
    public static void main(String[] args) {
        Parser parser = new Parser(EMPTY_LIST, EMPTY_LIST);
        HostRequirement hr = parser.parseScriptReqs(EMPTY_MAP, ""
                + "[1,5] host {"
                + "  1 socket where socket.s_brand==\"intel\" {"
                + "    2 core where !core.b_hyperthread {"
                + "      [2,999999] cpu 100000 capacity where cpu.b_avx==true && socket.s_model==\"xeon\" { }"
                + "      1 cpu 5000 capacity where socket.s_model==\"haswell\" { }"
                + "    }"
                + "  }"
                + "  1 gpu with 1 capacity { }"
                + "  1 mount with 256gb capacity where b_rotational==true" // for saving work
                + "  1 mount with 32gb capacity where b_rotational=false" // for temp storage
                + "  1 ram with 64gb capacity where ram.n_speed>=3000 && ram.s_brand==\"samsung\" { }"
                + "}");
        
        Map<String, BigDecimal> minCounts = new HashMap<>();
        Map<String, BigDecimal> minCapacities = new HashMap<>();
        calculateTotals(hr, minCounts, minCapacities);
        
        QueryTracker qt = new QueryTracker();
        String val;
        val = WorkerSearcherSql.filterCondition(requirementChain, minCounts, minCapacities)
        
        System.out.println(val);
    }
    
    
    // Sums up the TOTAL start counts and TOTAL minimum capacities, NOT INCLUDING THE HOST. Why not include the host? Because we're 
    // searching by individual hosts -- it doesn't make sense to include the host's count. The workflow sums up what each individual host
    // should have in terms of counts/capacites, and then searches for x hosts that match that (where x is the host's count). For example...
    //   2 host {
    //     [3,4] socket {
    //       ? core {
    //         [10,999999] cpu with [50000,100000] capacity
    //         1 cpu with 1000 capacity
    //       }
    //     }
    //     mount with capacity [25gb,50gb]
    //     mount with capacity [100gb,120gb]
    //   }
    // will result in...
    // count_socket   = 3                         = 3              = 3
    // count_core     = 3*1                       = 3              = 3
    // count_cpu      = 3*1*10 + 3*1*1            = 30 + 3         = 33
    // capacity_cpu   = 3*1*10*50000 + 3*1*1*1000 = 1500000 + 3000 = 1503000
    // count_mount    = 1 + 1                     = 1 + 1          = 2
    // capacity_mount = 1*25gb + 1*100gb          = 25gb + 100gb   = 125gb
    //
    // Then we grab 2 hosts that match those counts.
    private static void calculateTotals(
            HostRequirement hostRequirement,
            Map<String, BigDecimal> minCounts,
            Map<String, BigDecimal> minCapacities) {
        Validate.validState(hostRequirement.getCount() != null, "Top-level host must have a count"); // sanity check -- should never happen
        
        MultiValuedMap<String, Requirement> requirementChildren = getRequirementChildren(hostRequirement);
        for (Requirement childRequirement : requirementChildren.values()) {
            recursiveCalculateTotals(childRequirement, ONE, minCounts, minCapacities);
        }
    }

    // Sums up the TOTAL start counts and TOTAL minimum capacities. For example...
    //   [2,4] socket {
    //     ? core {
    //       [10,999999] cpu with [50000,100000] capacity
    //       1 cpu with 1000 capacity
    //     }
    //   }
    // will result in...
    // count_socket   = 2                         = 2              = 2
    // count_core     = 2*1                       = 22             = 2
    // count_cpu      = 2*1*10 + 2*1*1            = 20 + 2         = 22
    // capacity_cpu   = 2*1*10*50000 + 2*1*1*1000 = 1000000 + 2000 = 1002000
    private static void recursiveCalculateTotals(
            Requirement requirement,
            BigDecimal countMultiplier,
            Map<String, BigDecimal> minCounts,
            Map<String, BigDecimal> minCapacities) {
        String name = getRequirementName(requirement);
        
        // remember count of null means '?' was used when specifying the req
        BigDecimal countStart = requirement.getCount() == null ? ONE : requirement.getCount().getStart();
        BigDecimal countStartMultipliedByCount = countMultiplier.multiply(countStart);
        
        BigDecimal minCount = minCounts.getOrDefault(name, BigDecimal.ZERO).add(countStartMultipliedByCount);
        minCounts.put(name, minCount);
            
        // If requirement has capacity, add min capacity in minCapacities map.
        if (requirement instanceof CapacityEnabledRequirement) {
            BigDecimal capacityStart = ((CapacityEnabledRequirement) requirement).getCapacityRange().getStart();
            BigDecimal capacityStartMultipliedByCount = countStart.multiply(capacityStart);
            
            BigDecimal minCapacity = minCapacities.getOrDefault(name, BigDecimal.ZERO).add(capacityStartMultipliedByCount);
            minCapacities.put(name, minCapacity);
        }
        
        // Get children -- for each child, recurse. After this method returns the minCapacities map should contain the total minimum
        // capacity required by a requirement type. For example...
        //   1 host {
        //     2 socket {
        //       [3,4] cpu with [50000,100000] capacity
        //       1 cpu with 1000 capacity 
        //     }
        //   }
        // ... the minCapacities map should give back 151000 (50000 * 3) + (1 * 1000)
        MultiValuedMap<String, Requirement> requirementChildren = getRequirementChildren(requirement);
        for (Requirement childRequirement : requirementChildren.values()) {
            recursiveCalculateTotals(childRequirement, minCount, minCounts, minCapacities);
        }
    }
}
