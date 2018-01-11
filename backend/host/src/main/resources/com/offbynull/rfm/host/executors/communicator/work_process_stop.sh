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
work_img="$work_dir/work.img"



# Kill all PIDs in session
run_sid=$(cat "$work_mnt/work.sid")
if [ $? != 0 ]
then
    echo Reading SID file failed >&2
elif [ ! -z "$run_sid" ]
then
    pkill -9 -s "$run_sid" >"/dev/null"
fi



# Remove files
work_script_file="$work_mnt/work.sh"
work_stdout_file="$work_mnt/work.stdout"
work_stderr_file="$work_mnt/work.stderr"
work_exitcode_file="$work_mnt/work.exitcode"
work_sid_file="$work_mnt/work.sid"
work_starttime_file="$work_mnt/work.starttime"

rm "$work_script_file" >"/dev/null"
rm "$work_stdout_file" >"/dev/null"
rm "$work_stderr_file" >"/dev/null"
rm "$work_exitcode_file" >"/dev/null"
rm "$work_sid_file" >"/dev/null"
rm "$work_starttime_file" >"/dev/null"

exit 0
