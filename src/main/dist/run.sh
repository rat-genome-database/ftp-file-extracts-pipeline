#!/usr/bin/env bash
#
# FTP file extracts pipeline
#
. /etc/profile
APPNAME=ftpFileExtracts

# abort the script if any of stages below will fail
set -e

APPDIR=/home/rgddata/pipelines/$APPNAME
cd $APPDIR

# some modules, like annotations, require a lot of RAM to run; so we set it to 16GB, as of Oct 2019
java -Dspring.config=$APPDIR/../properties/default_db2.xml \
    -Dlog4j.configuration=file://$APPDIR/properties/log4j.properties \
    -Xmx16G \
    -jar lib/$APPNAME.jar "$@" 2>&1
