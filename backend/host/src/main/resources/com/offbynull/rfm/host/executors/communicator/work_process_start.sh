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
work_id="${0}"
work_dir="${1}"
work_user="${2}"
work_cmd="${@:3}"

work_mnt="$work_dir/work_mnt"



# Setup
if [ ! -d "$work_mnt" ]
then
    echo Mount directory missing >&2
    exit 1
fi

work_group=$(id -g -n "$work_user")    # get group to user for chown
if [ $? != 0 ] || [ -z "$work_group" ]
then
    echo Capture group failed >&2
    exit 1
fi

chown -R "$work_user:$work_group" "$work_mnt" >"/dev/null"
if [ $? != 0 ]
then
    echo Mount ownership change failed >&2
    exit 1
fi



# Run application
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
  # ps outputs may have prepended/appended spaces if length of output is too short, tr removes spaces
echo "ps -o sid --no-headers \"--pid=\$$\" | tr -d ' ' >\"$work_sid_file\"" >> "$work_script_file"
echo "ps -o lstart --no-headers \"--pid=\$$\" | tr -d ' ' >\"$work_starttime_file\"" >> "$work_script_file"
echo "sync \"$work_sid_file\" \"$work_starttime_file\" >\"/dev/null\"" >> "$work_script_file"
echo "$work_cmd >\"$work_stdout_file\" 2>\"$work_stderr_file\"" >> "$work_script_file"
echo "echo \$? >\"$work_exitcode_file\"" >> "$work_script_file"
echo "sync \"$work_stdout_file\" \"$work_stderr_file\" \"$work_exitcode_file\" >\"/dev/null\"" >> "$work_script_file"
nohup cgexec -g "memory,cpu,cpuset:$work_id" su "$work_user" -c "/bin/bash $work_mnt/work.sh" >"/dev/null" 2>&1 &

exit 0
