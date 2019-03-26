#!/bin/sh
APPDIR=~/pipelines/ftpFileExtracts/
$APPDIR/run.sh -interactions -species=Rat
$APPDIR/run.sh -interactions -species=Mouse
$APPDIR/run.sh -interactions -species=Human
$APPDIR/run.sh -interactions -species=Dog
$APPDIR/run.sh -interactions -species=Pig
echo 'DONE'
