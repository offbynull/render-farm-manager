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
if [ $# -lt 8 ]
then
    echo Arguments missing >&2
    exit 1
fi
add_track_script="${0}"
set_state_script="${1}"
create_work_dir_script="${2}"
delete_work_dir_script="${3}"
remove_track_script="${4}"
work_id="${5}"
work_dir="${6}"
work_user="${7}"
work_cmd=( "${@:8}" )



# Add track and create work directory
bash "-c" "$add_track_script" "$work_id" "$work_dir"
add_track_result=$?
if [ $add_track_result != 0 ]
then
    echo Unable to add track >&2
    exit $add_track_result
fi

bash "-c" "$create_work_dir_script" "$work_id" "$work_dir"
if [ $? != 0 ]
then
    bash "-c" "$delete_work_dir_script" "$work_id" "$work_dir"
    bash "-c" "$remove_track_script" "$work_id"
    exit 1
fi



# Write out specifics of task
ret=0
echo "$work_user" > "$work_dir/work.user" || ret=$?
sync "$work_dir/work.user" >"/dev/null"
iter=0
for work_cmd_arg in "${work_cmd[@]}"
do
    echo "$work_cmd_arg" > "$work_dir/work.cmd$iter" || ret=$?
    sync "$work_dir/work.cmd$iter" >"/dev/null"
    iter=$((iter+1))
done

if [ $ret != 0 ]
then
    echo Unable to write out config >&2
    exit 1
fi

bash "-c" "$set_state_script" "$work_id" "$work_dir" "CREATED"
if [ $? != 0 ]
then
    bash "-c" "$delete_work_dir_script" "$work_id" "$work_dir"
    bash "-c" "$remove_track_script" "$work_id"
    exit 1
fi

exit 0
