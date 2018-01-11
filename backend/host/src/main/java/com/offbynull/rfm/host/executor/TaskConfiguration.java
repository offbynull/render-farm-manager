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
package com.offbynull.rfm.host.executor;

import java.util.ArrayList;
import static java.util.Arrays.asList;
import org.apache.commons.collections4.list.UnmodifiableList;
import static org.apache.commons.collections4.list.UnmodifiableList.unmodifiableList;
import org.apache.commons.lang3.Validate;

/**
 * Task configuration.
 * @author Kasra Faghihi
 */
public class TaskConfiguration {
    private final String workPath;
    private final String user;
    private final UnmodifiableList<String> command;

    /**
     * Constructs a {@link TaskConfiguration} instance.
     * @param workPath task directory
     * @param user task user
     * @param command command-line command
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any of the following conditions are NOT met:
     * {@code !workPath.isEmpty()},
     * {@code !user.isEmpty()},
     * {@code command.length > 0},
     * {@code Arrays.stream(command).allMatch(x -> x != null)},
     * {@code !command[0].isEmpty()},
     * {@code workPath.startsWith("/")},
     * {@code !workPath.endsWith("/")},
     * {@code !workPath.contains("/../")},
     * {@code !workPath.contains("/./")},
     * {@code !workPath.contains("\u0000")}
     */
    public TaskConfiguration(String workPath, String user, String... command) {
        Validate.notNull(workPath);
        Validate.notNull(user);
        Validate.notNull(command);
        Validate.notBlank(workPath);
        Validate.notBlank(user);
        Validate.notEmpty(command);
        Validate.noNullElements(command);
        Validate.isTrue(!command[0].isEmpty());
        Validate.isTrue(workPath.startsWith("/"), "Work path must be absolute");
        Validate.isTrue(!workPath.endsWith("/"), "Work path must not end with /");
        Validate.isTrue(!workPath.contains("/../"), "Work path must not traverse up");
        Validate.isTrue(!workPath.contains("/./"), "Work path must not self-reference");
        Validate.isTrue(!workPath.contains("\u0000"), "Work path must not contain NUL"); //https://serverfault.com/a/150744
        this.workPath = workPath;
        this.user = user;
        this.command = (UnmodifiableList<String>) unmodifiableList(new ArrayList<>(asList(command)));
    }

    /**
     * Get work path.
     * @return work path (directory)
     */
    public String getWorkPath() {
        return workPath;
    }

    /**
     * Get user.
     * @return user
     */
    public String getUser() {
        return user;
    }

    /**
     * Get command.
     * @return command
     */
    public UnmodifiableList<String> getCommand() {
        return command;
    }

    @Override
    public String toString() {
        return "TaskConfiguration{" + "workPath=" + workPath + ", user=" + user + ", command=" + command + '}';
    }
}
