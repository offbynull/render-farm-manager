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



# Shrink image
e2fsck -f -y "$work_img" >&2  # https://www.systutorials.com/docs/linux/man/8-fsck.ext3/
case "$?" in
    "0") #0=no errors
        ;;
    "1") #1=corrected errors
        ;;
    *)
        echo Image check failed >&2
        exit 1
        ;;
esac

resize2fs -M "$work_img" >&2  # resize image + image's filesystem to minimum
if [ $? != 0 ]
then
    echo Image resize failed >&2
    exit 1
fi

sync "$work_img" >"/dev/null"

exit 0
