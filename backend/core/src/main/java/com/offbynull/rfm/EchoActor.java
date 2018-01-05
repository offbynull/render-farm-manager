/*
 * Copyright (c) 2017, Kasra Faghihi, All rights reserved.
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
package com.offbynull.rfm;

import com.offbynull.actors.address.Address;
import com.offbynull.actors.gateways.actor.Context;
import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;

final class EchoActor implements Coroutine {

    @Override
    public void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();
        ctx.allow();
        
        cnt.suspend();

        while (true) {
            Object msg = ctx.in();
            Address from = ctx.source();
            ctx.out(from, msg);

            cnt.suspend();
        }
    }
    
}
