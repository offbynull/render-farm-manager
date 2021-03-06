/*
 * Copyright (c) 2018, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.rfm.host.model.expression;

import static com.offbynull.rfm.host.model.expression.DataType.BOOLEAN;
import static com.offbynull.rfm.host.model.expression.DataType.NUMBER;
import static com.offbynull.rfm.host.model.expression.DataType.STRING;

/**
 * Built-in functions used by the parser for operators.
 * @author Kasra Faghihi
 */
public final class RequirementFunctionBuiltIns {
    private RequirementFunctionBuiltIns() {
        // do nothing
    }

    /** NOT operator function name. */
    public static final String NOT_B_B_NAME = "__b_not_b";
    /** NOT operator function. */
    public static final RequirementFunction NOT_B_B_FUNCTION = new RequirementFunction(BOOLEAN, NOT_B_B_NAME, BOOLEAN);

    
    /** DIVIDE operator function name. */
    public static final String DIVIDE_N_NN_NAME = "__n_div_nn";
    /** MULTIPLY operator function name. */
    public static final String MULTIPLY_N_NN_NAME = "__n_mul_nn";
    /** DIVIDE operator function. */
    public static final RequirementFunction DIVIDE_N_NN_FUNCTION = new RequirementFunction(NUMBER, DIVIDE_N_NN_NAME, NUMBER, NUMBER);    
    /** MULTIPLY operator function. */
    public static final RequirementFunction MULTIPLY_N_NN_FUNCTION = new RequirementFunction(NUMBER, MULTIPLY_N_NN_NAME, NUMBER, NUMBER);


    /** ADDITION operator function name. */
    public static final String ADD_N_NN_NAME = "__n_add_nn";
    /** SUBTRACTION operator function name. */
    public static final String SUB_N_NN_NAME = "__n_sub_nn";
    /** ADDITION operator function. */
    public static final RequirementFunction ADD_N_NN_FUNCTION = new RequirementFunction(NUMBER, ADD_N_NN_NAME, NUMBER, NUMBER);
    /** SUBTRACTION operator function. */
    public static final RequirementFunction SUB_N_NN_FUNCTION = new RequirementFunction(NUMBER, SUB_N_NN_NAME, NUMBER, NUMBER);

    
    /** GREATER THAN operator function name. */
    public static final String GREATER_THAN_B_NN_NAME = "__b_gt_nn";
    /** LESS THAN operator function name. */
    public static final String LESS_THAN_B_NN_NAME = "__b_lt_nn";
    /** GREATER THAN operator function. */
    public static final RequirementFunction GREATER_THAN_B_NN_FUNCTION
            = new RequirementFunction(BOOLEAN, GREATER_THAN_B_NN_NAME, NUMBER, NUMBER);
    /** LESS THAN operator function. */
    public static final RequirementFunction LESS_THAN_B_NN_FUNCTION
            = new RequirementFunction(BOOLEAN, LESS_THAN_B_NN_NAME, NUMBER, NUMBER);


    /** EQUALS number operator function name. */
    public static final String EQUAL_B_NN_NAME = "__b_eq_nn";
    /** EQUALS booleans operator function name. */
    public static final String EQUAL_B_BB_NAME = "__b_eq_bb";
    /** EQUALS string operator function name. */
    public static final String EQUAL_B_SS_NAME = "__b_eq_ss";
    /** EQUALS number operator function. */
    public static final RequirementFunction EQUAL_B_NN_FUNCTION = new RequirementFunction(BOOLEAN, EQUAL_B_NN_NAME, NUMBER, NUMBER);
    /** EQUALS booleans operator function. */
    public static final RequirementFunction EQUAL_B_BB_FUNCTION = new RequirementFunction(BOOLEAN, EQUAL_B_BB_NAME, BOOLEAN, BOOLEAN);
    /** EQUALS string operator function. */
    public static final RequirementFunction EQUAL_B_SS_FUNCTION = new RequirementFunction(BOOLEAN, EQUAL_B_SS_NAME, STRING, STRING);
    
    
    /** AND operator function name. */
    public static final String AND_B_BB_NAME = "__b_and_bb";
    /** AND operator function. */
    public static final RequirementFunction AND_B_BB_FUNCTION = new RequirementFunction(BOOLEAN, AND_B_BB_NAME, BOOLEAN, BOOLEAN);
    
    
    /** OR operator function name. */
    public static final String OR_B_BB_NAME = "__b_or_bb";
    /** OR operator function. */
    public static final RequirementFunction OR_B_BB_FUNCTION = new RequirementFunction(BOOLEAN, OR_B_BB_NAME, BOOLEAN, BOOLEAN);
}
