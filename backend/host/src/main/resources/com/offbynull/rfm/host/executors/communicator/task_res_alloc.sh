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
if [ $# -lt 14 ]
then
    echo Arguments missing >&2
    exit 1
fi
check_track_script="${0}"
set_state_script="${1}"
check_state_script="${2}"
create_cgroup_script="${3}"
create_image_script="${4}"
mount_image_script="${5}"
unmount_image_script="${6}"
delete_image_script="${7}"
delete_cgroup_script="${8}"
work_id="${9}"
cpus="${10}"
cpu_quota="${11}"
cpu_period="${12}"
memory_size="${13}"
disk_size="${14}"



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
check_state_output=$(bash "-c" "$check_state_script" "$work_id" "$work_dir" "CREATED")
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



# Deallocate any existing resources (just incase)
bash "-c" "$unmount_image_script" "$work_id" "$work_dir"
bash "-c" "$delete_image_script" "$work_id" "$work_dir"
bash "-c" "$delete_cgroup_script" "$work_id" "$work_dir"



# Allocate resources
bash "-c" "$create_cgroup_script" "$work_id" "$work_dir" "$work_user" "$cpus" "$cpu_quota" "$cpu_period" "$memory_size"
if [ $? != 0 ]
then
    bash "-c" "$delete_cgroup_script" "$work_id" "$work_dir"
    exit 1
fi

bash "-c" "$create_image_script" "$work_id" "$work_dir" "$disk_size"
if [ $? != 0 ]
then
    bash "-c" "$delete_image_script" "$work_id" "$work_dir"
    bash "-c" "$delete_cgroup_script" "$work_id" "$work_dir"
    exit 1
fi

bash "-c" "$mount_image_script" "$work_id" "$work_dir"
if [ $? != 0 ]
then
    bash "-c" "$unmount_image_script" "$work_id" "$work_dir"
    bash "-c" "$delete_image_script" "$work_id" "$work_dir"
    bash "-c" "$delete_cgroup_script" "$work_id" "$work_dir"
    exit 1
fi



# Write out allocation specs in files
ret=0
echo "$cpus" > "$work_dir/work.cpus" || ret=$?
echo "$cpu_quota" > "$work_dir/work.cpu_quota" || ret=$?
echo "$cpu_period" > "$work_dir/work.cpu_period" || ret=$?
echo "$memory_size" > "$work_dir/work.memory_size" || ret=$?
echo "$disk_size" > "$work_dir/work.disk_size" || ret=$?
sync "$work_dir/work.cpus" "$work_dir/work.cpu_quota" "$work_dir/work.cpu_period" "$work_dir/work.memory_size" "$work_dir/work.disk_size" >"/dev/null"
if [ $ret != 0 ]
then
    rm "$work_dir/work.cpus" >"/dev/null"
    rm "$work_dir/work.cpu_quota" >"/dev/null"
    rm "$work_dir/work.cpu_period" >"/dev/null"
    rm "$work_dir/work.memory_size" >"/dev/null"
    rm "$work_dir/work.disk_size" >"/dev/null"
    echo Unable to write out resources >&2
    exit 1
fi



# Update state
bash "-c" "$set_state_script" "$work_id" "$work_dir" "ALLOCATED"
if [ $? != 0 ]
then
    bash "-c" "$unmount_image_script" "$work_id" "$work_dir"
    bash "-c" "$delete_image_script" "$work_id" "$work_dir"
    bash "-c" "$delete_cgroup_script" "$work_id" "$work_dir"
    rm "$work_dir/work.cpus" >"/dev/null"
    rm "$work_dir/work.cpu_quota" >"/dev/null"
    rm "$work_dir/work.cpu_period" >"/dev/null"
    rm "$work_dir/work.memory_size" >"/dev/null"
    rm "$work_dir/work.disk_size" >"/dev/null"
    exit 1
fi

exit 0
