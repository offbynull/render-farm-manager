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
script_args=( "${@:1}" )



# Ensure folder exists
mkdir -p "/opt/rfm"
if [ $? != 0 ]
then
    echo Base directory creation failed >&2
    exit 1
fi



# Lock and run
(
    # Lock + add trap to unlock if script ungracefully terminates
    function cleanup {
        flock -u 9
    }
    trap cleanup 0

    flock -x 9


    # Note how the bash arguments are being formed when bash is called. It needs to be done like this -- form a new array with all arguments
    # and pass that array in. If it isn't, the args won't get passed into the new bash process properly.
    bash_args=( "-c" "${script_args[@]}" )
    bash "${bash_args[@]}"
    ret=$?

    flock -u 9  # the bash invocation above may launch other bash instances async... these prevent unlocking, so force unlock at the end
    exit $ret
) 9>"/opt/rfm/system.lock"
