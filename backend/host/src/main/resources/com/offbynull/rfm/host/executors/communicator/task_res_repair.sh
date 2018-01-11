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
if [ $# -lt 5 ]
then
    echo Arguments missing >&2
    exit 1
fi
check_track_script="${0}"
check_state_script="${1}"
create_cgroup_script="${2}"
mount_image_script="${3}"
repair_image_script="${4}"
work_id="${5}"



# Get tracking (gives back work_id work_dir in STDOUT)
check_track_output=$(bash "-c" "$check_track_script" "$work_id")
check_track_result=$?
if [ $check_track_result != 0 ]
then
    echo Check tracking failed >&2
    exit $check_track_result
fi

work_dir=$( cut -d ' ' -f 2- <<< "$check_track_output" )
if [ $? != 0 ]
then
    echo Unable to extract work directory >&2
    exit 1
fi



# Get state
check_state_output=$(bash "-c" "$check_state_script" "$work_id" "$work_dir" "ALLOCATED")
check_state_result=$?
if [ $check_state_result != 0 ]
then
    echo Check state failed >&2
    exit $check_state_result
fi



# Get user
work_user=$( cat "$work_dir/work.user" )
if [ $? != 0 ]
then
    echo Unable to extract work user >&2
    exit 1
fi



# Force allocation of resources -- recovers tasks after an unexpected reboot
ret=0
cpus=$( cat "$work_dir/work.cpus" 2>"/dev/null" || ret=$? )
cpu_quota=$( cat "$work_dir/work.cpu_quota" 2>"/dev/null" || ret=$? )
cpu_period=$( cat "$work_dir/work.cpu_period" 2>"/dev/null" || ret=$? )
memory_size=$( cat "$work_dir/work.memory_size" 2>"/dev/null" || ret=$? )
disk_size=$( cat "$work_dir/work.disk_size" 2>"/dev/null" || ret=$? )
disk_size_actual=$( stat "--printf=%s" "$work_dir/work.img" 2>"/dev/null" || ret=$? )
if [ $ret != 0 ]
then
    echo Unable to read resources >&2
    exit 1
fi

if [ "$disk_size" != "$disk_size_actual" ]
then
    echo Disk size mismatch >&2
    exit 1
fi

bash "-c" "$create_cgroup_script" "$work_id" "$work_dir" "$work_user" "$cpus" "$cpu_quota" "$cpu_period" "$memory_size"
if [ $? != 0 ]
then
    echo Unable to reallocate cgroups >&2
    exit 1
fi

bash "-c" "$repair_image_script" "$work_id" "$work_dir" "$disk_size"
if [ $? != 0 ]
then
    echo Unable to repair image >&2
    exit 1
fi

bash "-c" "$mount_image_script" "$work_id" "$work_dir"
if [ $? != 0 ]
then
    echo Unable to mount image >&2
    exit 1
fi

exit 0
