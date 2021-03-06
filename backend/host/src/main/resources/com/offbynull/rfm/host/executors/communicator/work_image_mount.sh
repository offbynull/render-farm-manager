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

work_img="$work_dir/work.img"
work_mnt="$work_dir/work_mnt"



# Mount image
mkdir -p "$work_mnt"
if [ $? != 0 ]
then
    echo Mount directory creation failed >&2
    exit 1
fi

mount -o loop,rw,sync "$work_img" "$work_mnt" >"/dev/null"
if [ $? != 0 ]
then
    echo Image mounting failed >&2
    exit 1
fi

exit 0
