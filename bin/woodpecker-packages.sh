#!/bin/bash

# woodpecker's target dir
WOODPECKER_TARGET_DIR=../target/wpclient-agent

# woodpecker's properties
WOODPECKER_PROPERTIES=../src/main/resources/woodpecker.properties

# exit shell with err_code
# $1 : err_code
# $2 : err_msg
exit_on_err()
{
    [[ ! -z "${2}" ]] && echo "${2}" 1>&2
    exit ${1}
}

# maven package the greys
mvn clean package -Dmaven.test.skip=true -f ../pom.xml \
|| exit_on_err 1 "package woodpecker failed."

# reset the target dir
mkdir -p ${WOODPECKER_TARGET_DIR}

# copy jar to TARGET_DIR
cp ../target/wpclient-agent-1.0-SNAPSHOT-jar-with-dependencies.jar ${WOODPECKER_TARGET_DIR}/wpclient-agent.jar

# copy woodpecker.properties to TARGET_DIR
chmod 777 ${WOODPECKER_PROPERTIES}
cp ${WOODPECKER_PROPERTIES} ${WOODPECKER_TARGET_DIR}/woodpecker.properties

# zip the greys
cd ../target/
zip -r wpclient-agent.zip wpclient-agent/
cd -
