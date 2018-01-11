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
work_state_expected="${2}"



# Check
if [ ! -f "$work_dir/work.state" ]
then
    echo Work state does not exist >&2
    exit 3
fi
work_state=$( cat "$work_dir/work.state" )
if [ $? != 0 ]
then
    echo Unable to extract work state >&2
    exit 1
fi
if [ "$work_state" != "$work_state_expected" ]
then
    echo Incorrect work state: "$work_state" vs "work_state_expected" >&2
    exit 3
fi

exit 0
