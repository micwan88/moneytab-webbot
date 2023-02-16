FROM ubuntu:22.04

ENV USER_HOME=/root
ENV EXTDIR=/media
ENV WORKDIR=${EXTDIR}/moneytab
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
RUN mkdir -p ${EXTDIR} \
    && mkdir -p ${WORKDIR} \
    && mkdir -p ${TOOLSDIR} \
    && mkdir -p ${GRADLE_HOME} \
    && mkdir -p ${GRADLE_USER_HOME} \
    && mkdir -p ${M2_HOME}

# install some common tools
RUN apt-get update && apt-get install -y locales net-tools iputils-ping openjdk-8-jdk unzip git wget vim software-properties-common \
	&& rm -rf /var/lib/apt/lists/* \
	&& apt-get clean

# Set the locale
RUN sed -i '/en_US.UTF-8/s/^# //g' /etc/locale.gen \
    && locale-gen
ENV LANG en_US.UTF-8
ENV LANGUAGE en_US:en
ENV LC_ALL en_US.UTF-8

# Set timezone noninteractive
RUN apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get install -yq tzdata && \
    ln -fs /usr/share/zoneinfo/Asia/Hong_Kong /etc/localtime && \
    dpkg-reconfigure -f noninteractive tzdata

# change to tools directory and download required build tools
WORKDIR ${TOOLSDIR}

# download gradle 8.0
RUN wget https://services.gradle.org/distributions/gradle-8.0-bin.zip && unzip gradle-8.0-bin.zip

# download maven 3.9.0
RUN wget https://dlcdn.apache.org/maven/maven-3/3.9.0/binaries/apache-maven-3.9.0-bin.tar.gz && tar -zxvf apache-maven-3.9.0-bin.tar.gz

# set mozilla repos as firefox store instead of snap
RUN add-apt-repository -y ppa:mozillateam/ppa \
    && echo "\nPackage: * \nPin: release o=LP-PPA-mozillateam \nPin-Priority: 1001" | tee /etc/apt/preferences.d/mozilla-firefox

# install firefox
RUN apt-get update && apt-get install -y firefox \
	&& rm -rf /var/lib/apt/lists/* \
	&& apt-get clean

# change to working folder
WORKDIR ${WORKDIR}

COPY deploy-webbot.sh ${WORKDIR}

# build and deploy
RUN ./deploy-webbot.sh

# set docker image entry point
CMD ["./runWebbot.sh"]
