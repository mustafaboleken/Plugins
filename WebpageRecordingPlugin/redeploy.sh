#!/bin/sh
AMS_DIR=/usr/local/antmedia/
mvn clean install -Dmaven.javadoc.skip=true -Dmaven.test.skip=true -Dgpg.skip=true
OUT=$?

if [ $OUT -ne 0 ]; then
    exit $OUT
fi

rm -r $AMS_DIR/plugins/webpage-recording-plugin*
cp target/webpage-recording-plugin.jar $AMS_DIR/plugins/

OUT=$?

if [ $OUT -ne 0 ]; then
    exit $OUT
fi
#./start-debug.sh
