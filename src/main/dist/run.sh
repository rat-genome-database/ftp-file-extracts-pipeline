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

java -Dspring.config=$APPDIR/../properties/default_db.xml \
    -Dlog4j.configuration=file://$APPDIR/properties/log4j.properties \
    -jar $APPNAME.jar "$@" 2>&1
