#!/bin/bash

# set -v
TAGNAME=moneytab-webbot

echo "Start build ${TAGNAME} docker image ..."

docker build -t ${TAGNAME} .

RET=$?

if [ ${RET} -ne 0 ]; then
	echo "Build failed !!!"
	exit 1
fi

echo "Build Completed."