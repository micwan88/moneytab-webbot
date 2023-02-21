#!/bin/bash

set -o pipefail

WEBBOT_VERSION=1.0.0-SNAPSHOT

if [ ! -d "${REPO_DIR}" ]; then
    # getting source
    git clone https://github.com/micwan88/moneytab-webbot.git
else
    cd ${REPO_DIR}

    git pull
fi

if [ -d "${WORKDIR}/moneytab-webbot-${WEBBOT_VERSION}" ]; then
    rm -rf "${WORKDIR}/moneytab-webbot-${WEBBOT_VERSION}"
fi

cd ${REPO_DIR}

# build the artifact
gradle clean build -x test

# stop the gradle after build
gradle --stop

# copy artifact to working folder
cp app/build/distributions/moneytab-webbot-${WEBBOT_VERSION}.zip ${WORKDIR}/

cd ${WORKDIR}

# unzip artifact
unzip moneytab-webbot-${WEBBOT_VERSION}.zip

# make link with libs
ln -s moneytab-webbot-${WEBBOT_VERSION}/lib lib

# copy required files
cp moneytab-webbot-${WEBBOT_VERSION}/*.sh .
#cp moneytab-webbot-${WEBBOT_VERSION}/app.properties .
cp moneytab-webbot-${WEBBOT_VERSION}/log4j2.xml .