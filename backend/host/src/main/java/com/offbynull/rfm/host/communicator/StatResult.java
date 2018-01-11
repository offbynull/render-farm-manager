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
package com.offbynull.rfm.host.communicator;

import org.apache.commons.lang3.Validate;

/**
 * Linux file stat result.
 * @author Kasra Faghihi
 */
public final class StatResult {

    private final int userId;
    private final int groupId;
    private final long length;
    private final int rawMode; // file type and permission (https://unix.stackexchange.com/q/193465), equiv to running stat -c "%f %n" *

    /**
     * Constructs a {@link StatResult} instance.
     * @param userId user id
     * @param groupId group id
     * @param length file length
     * @param rawMode file type and permission mode (https://unix.stackexchange.com/q/193465)
     */
    public StatResult(int userId, int groupId, long length, int rawMode) {
        Validate.isTrue(length >= 0);
        this.userId = userId;
        this.groupId = groupId;
        this.length = length;
        this.rawMode = rawMode;
    }

    /**
     * Get user ID.
     * @return user ID
     */
    public int getUserId() {
        return userId;
    }

    /**
     * Get group ID.
     * @return group ID
     */
    public int getGroupId() {
        return groupId;
    }

    /**
     * Get length.
     * @return length
     */
    public long getLength() {
        return length;
    }

    /**
     * Get file permissions.
     * @return Linux file permissions
     */
    public int getPermissions() {
        return rawMode & 07777;
    }
//
//    /**
//     * Return {@code true} if regular file, {@code false} otherwise.
//     * @return {@code true} if regular file, {@code false} otherwise
//     */
//    public boolean isFile() {
//        return (rawMode & 0100000) == 0100000;
//    }
//
//    /**
//     * Return {@code true} if regular directory, {@code false} otherwise.
//     * @return {@code true} if regular directory, {@code false} otherwise
//     */
//    public boolean isDirectory() {
//        return (rawMode & 0040000) == 0040000;
//    }
//
//    /**
//     * Return {@code true} if symlink, {@code false} otherwise.
//     * @return {@code true} if symlink, {@code false} otherwise
//     */
//    public boolean isSymlink() {
//        return (rawMode & 0120000) == 0120000;
//    }

    @Override
    public String toString() {
        return "StatResult{" + "userId=" + userId + ", groupId=" + groupId + ", length=" + length + ", rawMode=" + rawMode + '}';
    }
    
}
