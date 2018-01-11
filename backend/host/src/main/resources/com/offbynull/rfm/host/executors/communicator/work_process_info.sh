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

work_mnt="$work_dir/work_mnt"



# Get SID/PIDs
work_sid_file="$work_mnt/work.sid"
work_starttime_file="$work_mnt/work.starttime"
if [ -f "$work_sid_file" ] && [ -f "$work_starttime_file" ]
then
    # Load saved SID and SID starttime
    run_starttime=$(cat "$work_starttime_file")
    if [ $? != 0 ]
    then
        echo Reading starttime file failed >&2
        exit 1
    fi

    run_sid=$(cat "$work_sid_file")
    if [ $? != 0 ]
    then
        echo Reading SID file failed >&2
        exit 1
    fi

    # Get SID starttime again, if it's equal then dump out SID + PIDs for that SID... otherwise it means process has ended / the SID ended
    # and this new one is for a different process.
      # ps outputs may have prepended/appended spaces if length of output is too short, tr removes spaces
    current_starttime=$(ps -o lstart --no-headers "--pid=$run_sid" | tr -d ' ') 
    if [ "$run_starttime" == "$current_starttime" ]
    then
        echo !RUN_SID
        echo "$run_sid" | wc -l
        echo "$run_sid"

          # ps outputs may have prepended/appended spaces if length of output is too short, tr removes spaces
        run_pids=$(ps -o pid --no-headers "--sid=$run_sid" | tr -d ' ')  # returns 1 if not found, 0 if found, output will always be the same
        echo !RUN_PIDS
        echo "$run_pids" | wc -l
        echo "$run_pids"
    fi
fi



# Get exit code
work_exitcode_file="$work_mnt/work.exitcode"
if [ -f "$work_exitcode_file" ]
then
    work_exitcode=$(cat $work_exitcode_file)
    echo !EXITCODE
    echo "$work_exitcode" | wc -l
    echo "$work_exitcode"
fi



# Check disk space usage
mountpoint -q "$work_mnt"
if [ -d "$work_mnt" ] && [ $? == 0 ] # if work_mnt is a dir and a mount point
then
    df_output=$(df -B 1 -a --output="used,avail,fstype,target" "$work_mnt")
    echo !DF
    echo "$df_output" | wc -l
    echo "$df_output"
fi



# List Memory (https://serverfault.com/a/903208/461008)
cgroup_mem_usage_file="/sys/fs/cgroup/memory/$work_id/memory.usage_in_bytes"
if [ -f "$cgroup_mem_usage_file" ]
then
    cgroup_mem_usage=$(cat "$cgroup_mem_usage_file")
    echo !CGROUP_MEM_USAGE
    echo "$cgroup_mem_usage" | wc -l
    echo "$cgroup_mem_usage"
fi

cgroup_mem_stat_file="/sys/fs/cgroup/memory/$work_id/memory.stat"
if [ -f "$cgroup_mem_stat_file" ]
then
    cgroup_mem_stat=$(cat "$cgroup_mem_stat_file")
    echo !CGROUP_MEM_STAT
    echo "$cgroup_mem_stat" | wc -l
    echo "$cgroup_mem_stat"
fi

exit 0
