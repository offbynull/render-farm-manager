package com.offbynull.rfm.host.model.selection;

/**
 * Selection type.
 * @author Kasra Faghihi
 */
public enum SelectionType {
    /** Select n elements per parent. */
    EACH,
    /** Select n elements total. In other words... out of all parents, select a total of n elements -- doesn't matter what parent those
     * elements are bound to. */
    TOTAL
}
