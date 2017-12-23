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

import java.net.InetSocketAddress;
import org.apache.commons.io.IOUtils;

final class Main {

    private Main() {
        // do nothing
    }
    
    public static void main(String[] args) throws Exception {
        RenderFarmManager renderFarmManager = RenderFarmManager.create(InetSocketAddress.createUnresolved("0.0.0.0", 8071));
        
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.out.println("Shutting down");
                IOUtils.closeQuietly(renderFarmManager);
            }
        });
    }

}
