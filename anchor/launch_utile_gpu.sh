#!/bin/bash

pi00=192.168.28.200
pi01=192.168.28.201
pi02=192.168.28.202
pi03=192.168.28.203

# for ssh remote access
# 1. ssh-keygen -t rsa
# 2. ssh-copy-id user@host

programFolder=/home/pi/testbed/platform_cc_single_transmission_neon_ten_tones/

echo -e "
		This program is used to launch all the anchor at the same time
		"
		

function launchAll()
{
	echo "launch all the anchors"
	launchOne $pi00
	launchOne $pi01
	launchOne $pi02
	launchOne $pi03
}

function killAll()
{
	echo "launch all the anchors"
	killOne $pi00
	killOne $pi01
	killOne $pi02
	killOne $pi03
}

function beepbeep()
{
	echo "launch beepbeep"
	launchOne $1
	launchOne $2
}

function launchOne()
{
	echo "begin to launch anchor $1"
	ssh -T -p 22 pi@$1 <<remoteSSH
	cd /home/pi/testbed/platform_cc_single_transmission_neon_ten_tones/
	tmux new -s main -d "sudo ./main"
	exit
remoteSSH
echo "launch $1 anchor success -------"
}

function killOne()
{
	echo "kill one anchor program"
	ssh -T -p 22 pi@$1 <<remoteSSH
	sudo pkill main
	tmux kill-session -t main 2> /dev/null
	exit
remoteSSH
}

function updateAnchorConfiguration()
{
	echo "update anchor configuration"
	scp $1 pi@$2:$programFolder
	echo "update anchor configuration ok"
}

function uploadAnchorProgram()
{
	echo "upload program to pis"
	createFolder $pi02
	scp -r $1 pi@$pi02:$programFolder
	echo "uploaded ok ------------"
	makeProgram
	echo "program uploading ok. Yeh!!!"
}

function verifyConnection()
{
	echo "test connection to each pi"
	ssh -f -p 22 pi@$pi01 "pwd"
	echo "connection to pi01:$pi01 ok"
	ssh -f -p 22 pi@$pi02 "pwd"
	echo "connection to pi02:$pi02 ok"
	ssh -f -p 22 pi@$pi03 "pwd"
	echo "connection to pi03:$pi03 ok"
	ssh -f -p 22 pi@$pi04 "pwd"
	echo "connection to pi04:$pi04 ok"
	echo "test connection over -------"	
}

function uploadTest()
{
	echo "upload program to pis"
	createFolder
	scp -r $1 pi@$pi02:/home/pi/testTransfer/cc/
	echo "program uploading ok. Yeh!!!"
}

function createFolder()
{
	ssh -T -p 22 pi@$1 <<remoteSSH
	if [ ! -d "$programFolder" ]; then
		mkdir -p ~$programFolder
		echo "Program folder created!"
	fi
remoteSSH
}

function makeProgram()
{
	echo "make program--------------"
	ssh -T -p 22 pi@$1 <<remoteSSH
	cd $programFolder
	make
remoteSSH
}

function scpFiles()
{
	echo "upload files to pi***************"
	scp $1 pi@$2:$3
	echo "uploading over ##################"
}