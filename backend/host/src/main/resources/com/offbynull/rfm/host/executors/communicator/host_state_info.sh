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



# Check required commands are available
req_commands=(echo cat grep wc dd rm mkdir mktemp kill pkill sudo su sleep tr)              # core commands
req_commands+=(systemctl ssh-keygen)                                                        # commands required for priming
req_commands+=(uptime df free uname ps pstree)                                              # commands required by host info (this)
req_commands+=(cgcreate cgexec cgdelete fallocate e2fsck resize2fs mkfs mount umount gzip)  # commands required for containerization
req_commands+=(mountpoint)
for req_command in $req_commands
do
    which "$req_command" > /dev/null
    if [ $? != 0 ]
    then
        echo "Command $req_command missing" >&2
        exit 1
    fi
done



# Check systemd is used
systemd_enabled=$(pstree -p | grep systemd | wc -l)
if [ $systemd_enabled == "0" ]
then
    echo systemd not used >&2
    exit 1
fi



# Get Linux details
uname_output=$(uname -a)
if [ $? != 0 ]
then
    echo Uname info capture failed >&2
    exit 1
fi
echo !UNAME
echo "$uname_output" | wc -l
echo "$uname_output"



# List mounts
df_output=$(df -B 1 -a --output="used,avail,fstype,target")
if [ $? != 0 ]
then
    echo Mount info capture failed >&2
    exit 1
fi
echo !DF
echo "$df_output" | wc -l
echo "$df_output"



# List CPUs
cgroup_cpuset_file="/sys/fs/cgroup/cpuset/cpuset.cpus"
if [ ! -f "$cgroup_cpuset_file" ]
then
    echo Missing cpuset cgroup file >&2
    exit 1
fi
cgroup_cpuset=$(cat "$cgroup_cpuset_file")
echo !CGROUP_CPUSET
echo "$cgroup_cpuset" | wc -l
echo "$cgroup_cpuset"

cpuinfo_output=$(cat /proc/cpuinfo)
if [ $? != 0 ]
then
    echo CPU info capture failed >&2
    exit 1
fi
echo !CPUINFO
echo "$cpuinfo_output" | wc -l
echo "$cpuinfo_output"

procstat_output1=$(cat /proc/stat)
if [ $? != 0 ]
then
    echo /proc/stat capture failed >&2
    exit 1
fi
echo !PROCSTAT1
echo "$procstat_output1" | wc -l
echo "$procstat_output1"

sleep 1
if [ $? != 0 ]
then
    echo Sleep failed >&2
    exit 1
fi

procstat_output2=$(cat /proc/stat)
if [ $? != 0 ]
then
    echo /proc/stat capture failed >&2
    exit 1
fi
echo !PROCSTAT2
echo "$procstat_output2" | wc -l
echo "$procstat_output2"



# List Kernel config options
kernel_configs=$(cat /boot/config-*)
if [ $? != 0 ]
then
    echo Kernel config option capture failed >&2
    exit 1
fi
kernel_configs=$(grep "CONFIG_MEMCG_SWAP_ENABLED" <<< "$kernel_configs")
if [ $? != 0 ]
then
    echo Kernel config option grepping failed >&2
    exit 1
fi
echo !KERNEL_CONFIGS
echo "$kernel_configs" | wc -l
echo "$kernel_configs"



# List Memory (https://serverfault.com/a/903208/461008)
cgroup_mem_usage=$(cat /sys/fs/cgroup/memory/memory.usage_in_bytes)
if [ $? != 0 ]
then
    echo Unable to read mem usage cgroup file >&2
    exit 1
fi
echo !CGROUP_MEM_USAGE
echo "$cgroup_mem_usage" | wc -l
echo "$cgroup_mem_usage"

cgroup_mem_stat=$(cat /sys/fs/cgroup/memory/memory.stat)
if [ $? != 0 ]
then
    echo Unable to read mem stat cgroup file >&2
    exit 1
fi
echo !CGROUP_MEM_STAT
echo "$cgroup_mem_stat" | wc -l
echo "$cgroup_mem_stat"

meminfo_output=$(cat /proc/meminfo)
if [ $? != 0 ]
then
    echo Unable to read meminfo file >&2
    exit 1
fi
echo !MEMINFO
echo "$meminfo_output" | wc -l
echo "$meminfo_output"



# List NVIDIA devices
#  also see https://www.microway.com/hpc-tech-tips/nvidia-smi_control-your-gpus/ -- the topology section is useful
which nvidia-smi >"/dev/null"
nvidia_installed=$?
if [ $nvidia_installed == 0 ]
then
    nvidia_smi=$(nvidia-smi --query-gpu=index,name --format=csv)
    nvidia_devs=$(find /dev | grep /dev/nvidia.*)
    echo !NVIDIA_SMI
    echo "$nvidia_smi" | wc -l
    echo "$nvidia_smi"
    echo !NVIDIA_DEVICES
    echo "$nvidia_devs" | wc -l
    echo "$nvidia_devs"
fi



# List tasks
tasks_output=""
if [ -f "/opt/rfm/tasks" ]
then
    tasks_output=$(cat "/opt/rfm/tasks")
    if [ $? != 0 ]
    then
        echo Failed to read active tasks >&2
        exit 1
    fi
fi
echo !TASKS
echo "$tasks_output" | wc -l
echo "$tasks_output"

exit 0
