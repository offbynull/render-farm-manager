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
if [ $# -lt 10 ]
then
    echo Arguments missing >&2
    exit 1
fi
task_res_repair_script="${0}"
task_stop_script="${1}"
add_track_script="${2}"
check_track_script="${3}"
remove_track_script="${4}"
set_state_script="${5}"
check_state_script="${6}"
create_cgroup_script="${7}"
mount_image_script="${8}"
repair_image_script="${9}"
stop_process_script="${10}"



# Get tasks that were started at a different boot instance
if [ ! -f "/opt/rfm/tasks" ]
then
    exit 0
fi

tasks=$(cat "/opt/rfm/tasks")
if [ $? != 0 ]
then
    echo Failed to read active tasks >&2
    exit 1
fi

boottime=$(cat /proc/stat | grep btime | cut -f 2 -d ' ')
if [ $? != 0 ]
then
    echo Failed to get boot time >&2
    exit 1
fi

ret=0
while read -r line  # https://superuser.com/a/284226
do
    # Get tracked task variables (id, boottime, and directory)
    work_id=$( cut -d ' ' -f 1 <<< "$line" )
    if [ $? != 0 ]
    then
        echo Failed to cut line >&2
        exit 1
    fi

    work_boottime=$( cut -d ' ' -f 2 <<< "$line" )
    if [ $? != 0 ]
    then
        echo Failed to cut line >&2
        exit 1
    fi

    work_dir=$( cut -d ' ' -f 3- <<< "$line" )
    if [ $? != 0 ]
    then
        echo Failed to cut line >&2
        exit 1
    fi

    # If boottime is the same as the current boottime, skip to next task
    if [ $boottime == $work_boottime ]
    then
        continue
    fi

    # Remove track and add it back in again -- this is so we have a new boottime set for the task
    remove_track_output=$( bash "-c" "$remove_track_script" "$work_id" )
    if [ $? != 0 ]
    then
        echo "Remove track failed: $work_id" >&2
        continue
    fi

    work_state=$( cat "$work_dir/work.state" )
    if [ $? != 0 ]
    then
        echo "Unable to extract work state for $work_id" >&2
        rm -rf "$work_dir" >"/dev/null"
        ret=4
        continue
    fi

    add_track_output=$( bash "-c" "$add_track_script" "$work_id" "$work_dir" )
    if [ $? != 0 ]
    then
        echo "Add track failed: $work_id" >&2
        continue
    fi

    # Recreate resources for task (if required)
    case "$work_state" in
        "CREATED")
            # we don't need to do anything here
            ###
            ;;
        "ALLOCATED")
            # call repair to re-initialize resources
            bash "-c" "$task_res_repair_script" "$check_track_script" "$check_state_script" "$create_cgroup_script" "$mount_image_script" "$repair_image_script" "$work_id"
            if [ $? != 0 ]
            then
                echo "Repair failed: $work_id" >&2
                ret=4
            fi
            ;;
        "STARTED")
            # call stop to roll back from started state to allocate state
            bash "-c" "$task_stop_script" "$check_track_script" "$set_state_script" "$check_state_script" "$stop_process_script" "$work_id"
            if [ $? != 0 ]
            then
                echo "Stop failed: $work_id" >&2
                ret=4
            fi
            # call repair to re-initialize resources
            bash "-c" "$task_res_repair_script" "$check_track_script" "$check_state_script" "$create_cgroup_script" "$mount_image_script" "$repair_image_script" "$work_id"
            if [ $? != 0 ]
            then
                echo "Repair failed: $work_id" >&2
                ret=4
            fi
            ;;
        *)
            echo "Unrecognized work state: $work_id" >&2
            exit 1
            ;;
    esac
done <<< "$tasks"

exit $ret
