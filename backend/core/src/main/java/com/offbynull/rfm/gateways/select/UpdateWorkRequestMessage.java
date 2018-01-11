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
package com.offbynull.rfm.gateways.select;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.apache.commons.lang3.Validate;

public final class UpdateWorkRequestMessage {
    private final String id;
    private final int priority;
    private final Expression expression;

    public UpdateWorkRequestMessage(String id, int priority, Expression expression) {
        Validate.notNull(id);
        Validate.notNull(expression);
        this.id = id;
        this.priority = priority;
        this.expression = expression;
    }

    public String getId() {
        return id;
    }

    public int getPriority() {
        return priority;
    }

    public Expression getExpression() {
        return expression;
    }
    
    public static final class Expression {
        private final String operator;
        private final UnmodifiableList<Expression> operands;

        public Expression(String operator, Expression ... operands) {
            this(operator, Arrays.asList(operands));
        }

        public Expression(String operator, List<Expression> operands) {
            Validate.notNull(operator);
            Validate.notNull(operands);
            Validate.noNullElements(operands);

            this.operator = operator;
            this.operands = (UnmodifiableList<Expression>) UnmodifiableList.unmodifiableList(new ArrayList<>(operands));
        }

        public String getOperator() {
            return operator;
        }

        public UnmodifiableList<Expression> getOperands() {
            return operands;
        }
    }
}
