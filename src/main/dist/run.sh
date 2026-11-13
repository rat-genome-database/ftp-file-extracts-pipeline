#!/usr/bin/env bash
#
# FTP file extracts pipeline
#
. /etc/profile
APPNAME="ftp-file-extracts-pipeline"

# abort the script if any of stages below will fail
set -e

APPDIR=/home/rgddata/pipelines/$APPNAME
cd $APPDIR

# some modules, like annotations, require a lot of RAM to run;
# 31G, not 32G: from 32G up the JVM turns off compressed object pointers and every reference doubles in size
java -Dspring.config=$APPDIR/../properties/default_db2.xml \
    -Dlog4j.configurationFile=file://$APPDIR/properties/log4j2.xml \
    -Xmx31G \
    -jar lib/$APPNAME.jar "$@" 2>&1
