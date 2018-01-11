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
if [ $# -lt 12 ]
then
    echo Arguments missing >&2
    exit 1
fi
check_track_script="${0}"
check_state_script="${1}"
create_cgroup_script="${2}"
mount_image_script="${3}"
resize_image_script="${4}"
unmount_image_script="${5}"
delete_cgroup_script="${6}"
work_id="${7}"
cpus="${8}"
cpu_quota="${9}"
cpu_period="${10}"
memory_size="${11}"
disk_size="${12}"



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



# Reallocate resources
bash "-c" "$delete_cgroup_script" "$work_id" "$work_dir"
if [ $? != 0 ]
then
    echo Unable to deallocate cgroups >&2
    exit 1
fi

bash "-c" "$create_cgroup_script" "$work_id" "$work_dir" "$work_user" "$cpus" "$cpu_quota" "$cpu_period" "$memory_size"
if [ $? != 0 ]
then
    echo Unable to reallocate cgroups >&2
    exit 1
fi

bash "-c" "$unmount_image_script" "$work_id" "$work_dir"
if [ $? != 0 ]
then
    echo Unable to unmount image >&2
    exit 1
fi

bash "-c" "$resize_image_script" "$work_id" "$work_dir" "$disk_size"
if [ $? != 0 ]
then
    echo Unable to resize image >&2
    exit 1
fi

bash "-c" "$mount_image_script" "$work_id" "$work_dir"
if [ $? != 0 ]
then
    echo Unable to mount image >&2
    exit 1
fi

# If the host crashes while these commands, there's a possibility that some files will be updated but other won't. The chance of this
# happening is very low, but a workaround could be that work.state file could temporarily be set to CREATED (and sync'd), then changed back
# to ALLOCATED (and sync'd) once all the files have been updated+sync'd.
rm "$work_dir/work.cpus" >"/dev/null"
rm "$work_dir/work.cpu_quota" >"/dev/null"
rm "$work_dir/work.cpu_period" >"/dev/null"
rm "$work_dir/work.memory_size" >"/dev/null"
rm "$work_dir/work.disk_size" >"/dev/null"
ret=0
echo "$cpus" > "$work_dir/work.cpus" || ret=$?
echo "$cpu_quota" > "$work_dir/work.cpu_quota" || ret=$?
echo "$cpu_period" > "$work_dir/work.cpu_period" || ret=$?
echo "$memory_size" > "$work_dir/work.memory_size" || ret=$?
echo "$disk_size" > "$work_dir/work.disk_size" || ret=$?
sync "$work_dir/work.cpus" "$work_dir/work.cpu_quota" "$work_dir/work.cpu_period" "$work_dir/work.memory_size" "$work_dir/work.disk_size" >"/dev/null"
if [ $ret != 0 ]
then
    echo Unable to write out resources >&2
    exit 1
fi

exit 0
