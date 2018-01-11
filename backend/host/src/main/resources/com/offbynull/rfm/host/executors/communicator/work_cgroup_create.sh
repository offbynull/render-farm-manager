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
if [ $# -lt 6 ]
then
    echo Arguments missing >&2
    exit 1
fi
work_id="${0}"
work_dir="${1}"
work_user="${2}"
cpus="${3}"
cpu_quota="${4}"
cpu_period="${5}"
memory_size="${6}"



# Create cgroups
work_group=$(id -g -n "$work_user")
if [ $? != 0 ] || [ -z "$work_group" ]
then
    echo Capture group failed >&2
    exit 1
fi

cgcreate -a "$work_user:$work_group" -t "$work_user:$work_group" -g "memory,cpu,cpuset:$work_id"
if [ $? != 0 ]
then
    echo Cgroup creation failed >&2
    exit 1
fi
function cleanup_cgroup {
    cgdelete -g "memory,cpu,cpuset:$work_id"
}
function cleanup {
    cleanup_cgroup
}
trap cleanup 0

echo "$cpus" > "/sys/fs/cgroup/cpuset/$work_id/cpuset.cpus"

echo "$cpu_quota" > "/sys/fs/cgroup/cpu/$work_id/cpu.cfs_quota_us"
echo "$cpu_period" > "/sys/fs/cgroup/cpu/$work_id/cpu.cfs_period_us"

echo "$memory_size" > "/sys/fs/cgroup/memory/$work_id/memory.limit_in_bytes"
echo 0 > "/sys/fs/cgroup/memory/$work_id/memory.swappiness" # no swapping?



# Remove trap (assume everything worked successfully at this point)
trap - 0

exit 0
