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
pwd
DB_OPTS="-Dspring.config=$APPDIR/../properties/default_db.xml"
LOG4J_OPTS="-Dlog4j.configuration=file://$APPDIR/properties/log4j.properties"
declare -x "FTP_FILE_EXTRACTS_OPTS=$DB_OPTS $LOG4J_OPTS"
bin/$APPNAME "$@" 2>&1