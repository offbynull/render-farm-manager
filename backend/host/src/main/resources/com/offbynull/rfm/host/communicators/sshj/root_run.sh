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



if [ $# == 0 ]
then
    echo Command missing >&2
    exit 1
fi



if [ $UID != "0" ]
then
    echo "NoRoot" >&2
    # Not root -- re-run this script through sudo
    # -S flag reads from stdin
    # -k flag makes it so it always asks for password (no caching)
    # -p flag gives a custom prompt -- using $'somestring' (ANSI C Quoting) allows you to use escape characters like \n for newline
    self_cmd="/bin/bash ${0} ${@}"
    sudo -S -k -p $'SudoPasswd\n' $self_cmd
else
    echo "YesRoot" >&2
    # Root -- run the command
    boottime=$(cat /proc/stat | grep btime | cut -f 2 -d ' ')
    if [ $boottime != $1 ] && [ -1 != $1 ]
    then
        echo "BootUpdate" >&2
        echo "$boottime" >&2
        exit 1;
    else
        echo "Ok" >&2
        echo "$boottime" >&2
        ${@:2}
    fi
fi