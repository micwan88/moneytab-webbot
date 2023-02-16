FROM ubuntu:22.04

ENV WEBBOT_VERSION=1.0.0-SNAPSHOT
ENV USER_HOME=/root
ENV EXTDIR=/media
ENV TOOLSDIR=/opt
ENV JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
ENV GRADLE_HOME=${TOOLSDIR}/gradle-8.0
ENV GRADLE_USER_HOME=${USER_HOME}/gradle
ENV M2_HOME=${TOOLSDIR}/apache-maven-3.9.0
ENV PATH="${JAVA_HOME}/bin:${PATH}:${GRADLE_HOME}/bin:${M2_HOME}/bin"

LABEL maintainer="Michael Wan"

# declare data is external volume
VOLUME ${EXTDIR}

# init directories it if not exist
RUN mkdir -p ${EXTDIR} && mkdir -p ${TOOLSDIR} \
    && mkdir -p ${GRADLE_HOME} \
    && mkdir -p ${GRADLE_USER_HOME} \
    && mkdir -p ${M2_HOME}

WORKDIR ${TOOLSDIR}

# install some common tools
RUN apt-get update && apt-get install -y locales net-tools iputils-ping openjdk-8-jdk unzip git wget vim \
	&& rm -rf /var/lib/apt/lists/* \
	&& apt-get clean

# Set the locale
RUN sed -i '/en_US.UTF-8/s/^# //g' /etc/locale.gen \
    && locale-gen
ENV LANG en_US.UTF-8
ENV LANGUAGE en_US:en
ENV LC_ALL en_US.UTF-8

# download gradle 8.0
RUN wget https://services.gradle.org/distributions/gradle-8.0-bin.zip && unzip gradle-8.0-bin.zip

# download maven 3.9.0
RUN wget https://dlcdn.apache.org/maven/maven-3/3.9.0/binaries/apache-maven-3.9.0-bin.tar.gz && tar -zxvf apache-maven-3.9.0-bin.tar.gz

# install firefox
RUN apt-get update && apt-get install -y firefox/jammy \
	&& rm -rf /var/lib/apt/lists/* \
	&& apt-get clean

# change to target folder
WORKDIR ${EXTDIR}

# getting source
RUN git clone https://github.com/micwan88/moneytab-webbot.git

WORKDIR ${EXTDIR}/moneytab-webbot

# build
RUN gradle clean build -x test \
    && cp app/build/distributions/moneytab-webbot-${WEBBOT_VERSION}.zip ${EXTDIR}/

WORKDIR ${EXTDIR}

RUN unzip moneytab-webbot-${WEBBOT_VERSION}.zip

# set docker image entry point
CMD ["/bin/bash"]