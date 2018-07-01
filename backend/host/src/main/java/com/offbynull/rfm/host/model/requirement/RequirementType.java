package com.offbynull.rfm.host.model.requirement;

/**
 * Requirement type.
 * @author Kasra Faghihi
 */
public enum RequirementType {
    /** Require n elements per parent. */
    EACH,
    /** Require n elements total. In other words... out of all parents, select a total of n elements -- doesn't matter what parent those
     * elements are bound to. */
    TOTAL
}
