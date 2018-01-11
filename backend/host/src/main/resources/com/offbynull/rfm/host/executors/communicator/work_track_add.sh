#!/bin/bash

# Copyright (c) 2018, Kasra Faghihi, All rights reserved.
# 
# This library is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3.0 of the License, or (at your option) any later version.
# 
# This library is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
# 
# You should have received a copy of the GNU Lesser General Public
# License along with this library.



# Check is root
if [ $UID != "0" ]
then
    echo Root required >&2
    exit 1
fi



# Setup variables
if [ $# -lt 1 ]
then
    echo Arguments missing >&2
    exit 1
fi
work_id="${0}"
work_dir="${1}"



# Ensure folder exists
mkdir -p "/opt/rfm"
if [ $? != 0 ]
then
    echo Base directory creation failed >&2
    exit 1
fi



# Add task to task list
active_tasks=""
if [ -f "/opt/rfm/tasks" ]
then
    active_tasks=$(cat "/opt/rfm/tasks")
    if [ $? != 0 ]
    then
        echo Failed to read active tasks >&2
        exit 1
    fi
fi

boottime=$(cat /proc/stat | grep btime | cut -f 2 -d ' ')
if [ $? != 0 ]
then
    echo Failed to get boot time >&2
    exit 1
fi

while read -r line  # https://superuser.com/a/284226
do
    if [[ "$line" == "$work_id "* ]]  # https://stackoverflow.com/a/19723789
    then
        echo Task ID already exists >&2
        exit 2
    fi
done <<< "$active_tasks"

echo "$work_id $boottime $work_dir" >>"/opt/rfm/tasks"
if [ $? != 0 ]
then
    echo Failed to add task to active tasks >&2
    exit 1
fi

sync "/opt/rfm/tasks" >"/dev/null"

exit 0
