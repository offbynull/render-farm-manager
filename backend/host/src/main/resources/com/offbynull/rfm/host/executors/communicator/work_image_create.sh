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
disk_size="${2}"

work_img="$work_dir/work.img"



# Create image
disk_excess_boundary=$(($disk_size % 1024))
if [ $disk_excess_boundary != 0 ]
then
    disk_size=$(($disk_size + (1024 - $disk_excess_boundary)))
    echo Size not multiple of 1K -- rounding up >&2
fi

mkdir -p "$work_dir"
if [ $? != 0 ]
then
    echo Work directory creation failed >&2
    exit 1
fi
function cleanup_dir {
    rm -rf "$work_dir" >"/dev/null"
}
function cleanup {
    cleanup_dir
}

fallocate -l "$disk_size" "$work_img"
if [ $? != 0 ]
then
    echo Image creation failed >&2
    exit 1
fi

function cleanup_image {
    rm "$work_img" >"/dev/null"
}
function cleanup {
    cleanup_image
    cleanup_dir
}
trap cleanup 0

mkfs -t ext2 -F "$work_img" >"/dev/null"
if [ $? != 0 ]
then
    echo Image formatting failed >&2
    exit 1
fi

sync "$work_img" >"/dev/null"



# Remove trap (assume everything worked successfully at this point)
trap - 0

exit 0
