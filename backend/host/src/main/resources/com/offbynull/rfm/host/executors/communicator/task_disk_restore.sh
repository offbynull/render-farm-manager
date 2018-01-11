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
check_state_script="${1}"
image_recover_script="${2}"
work_id="${3}"
backup_dir="${4}"



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



# Restore image and update the stored disk size
bash "-c" "$image_recover_script" "$work_id" "$work_dir" "$backup_dir"
if [ $? != 0 ]
then
    echo Unable to recover image >&2
    exit 1
fi

disk_size=$(stat -c%s "$work_dir/work.img")
if [ $? != 0 ]
then
    echo Unable to get new disk limit >&2
    exit 1
fi

echo "$disk_size" > "$work_dir/work.disk_size"
if [ $? != 0 ]
then
    echo Unable to write new disk limit >&2
    exit 1
fi

sync "$work_dir/work.disk_size" >"/dev/null"

exit 0
