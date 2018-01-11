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
backup_dir="${2}"

work_img="$work_dir/work.img"
backup_img="$backup_dir/work.img.gz"



# Ensure backup directory exists
mkdir -p "$backup_dir"
if [ $? != 0 ]
then
    echo Image backup directory creation failed >&2
    exit 1
fi



## Zero out remaining space on image with 0 (improves compressibility) COMMENTED OUT BECAUSE TASK MAY STILL BE RUNNING WHILE THIS IS INVOKED
#zero_tmp_id=$(cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w 8 | head -n 1)
#zero_file="$work_mnt/zerofill_$zero_tmp_id.dat"
#cat /dev/zero > zero_file
#sync &> /dev/null
#rm zero_file &> /dev/null
#sync &> /dev/null



# Compress image
sync "$work_img" >"/dev/null"  # sync just incase
gzip -c "$work_img" >"$backup_img"
if [ $? != 0 ]
then
    echo Image backup compression failed >&2
    exit 1
fi

exit 0
