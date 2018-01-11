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
if [ $# -lt 4 ]
then
    echo Arguments missing >&2
    exit 1
fi
check_track_script="${0}"
set_state_script="${1}"
check_state_script="${2}"
stop_process_script="${3}"
work_id="${4}"



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
check_state_output=$(bash "-c" "$check_state_script" "$work_id" "$work_dir" "STARTED")
check_state_result=$?
if [ $check_state_result != 0 ]
then
    echo Check state failed >&2
    exit $check_state_result
fi



# Execute
bash "-c" "$stop_process_script" "$work_id" "$work_dir"
if [ $? != 0 ]
then
    echo Failed to stop process >&2
    exit 1
fi

bash "-c" "$set_state_script" "$work_id" "$work_dir" "ALLOCATED"
if [ $? != 0 ]
then
    bash "-c" "$stop_process_script" "$work_id" "$work_dir"
    exit 1
fi

exit 0
