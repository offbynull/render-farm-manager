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
if [ $# -lt 2 ]
then
    echo Arguments missing >&2
    exit 1
fi
check_track_script="${0}"
info_process_script="${1}"
work_id="${2}"



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



# Output configuration
echo "!WORKDIR"
echo "$work_dir" | wc -l
echo "$work_dir"

work_user=$( cat "$work_dir/work.user" )
if [ $? != 0 ]
then
    echo Unable to extract work user >&2
    exit 1
fi
echo "!WORKUSER"
echo "$work_user" | wc -l
echo "$work_user"

arg_count=0
while true
do
    if [ ! -f "$work_dir/work.cmd$arg_count" ]
    then
        break
    fi
    arg_count=$((arg_count+1))
done

work_cmd=()
iter=0
while [ $iter -lt $arg_count ]
do
    arg=$(cat "$work_dir/work.cmd$iter")
    if [ $? != 0 ]
    then
        echo Failed to read argument $iter >&2
        exit 1
    fi
    echo "!WORKCMD$iter"
    echo "$arg" | wc -l
    echo "$arg"
    iter=$((iter+1))
done



# Output state
state=$( cat "$work_dir/work.state" )
echo !STATE
echo "$state" | wc -l
echo "$state"



# Output resource allocation
cpus=$( cat "$work_dir/work.cpus" )
echo !CPUS
echo "$cpus" | wc -l
echo "$cpus"

cpu_quota=$( cat "$work_dir/work.cpu_quota" )
echo !CPU_QUOTA
echo "$cpu_quota" | wc -l
echo "$cpu_quota"

cpu_period=$( cat "$work_dir/work.cpu_period" )
echo !CPU_PERIOD
echo "$cpu_period" | wc -l
echo "$cpu_period"

memory_size=$( cat "$work_dir/work.memory_size" )
echo !MEMORY_SIZE
echo "$memory_size" | wc -l
echo "$memory_size"

disk_size=$( cat "$work_dir/work.disk_size" )
echo !DISK_SIZE
echo "$disk_size" | wc -l
echo "$disk_size"



# Check task
bash "-c" "$info_process_script" "$work_id" "$work_dir"
exit $?
