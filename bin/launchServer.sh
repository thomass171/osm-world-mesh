#!/bin/bash
#
# Launches osm world mesh services server
#

OWNDIR=`dirname $0`
source $OWNDIR/common.sh || exit 1

SPRING_ARGS=--spring.main.banner-mode=off
SPRING_ARGS="${SPRING_ARGS} --spring.datasource.url=jdbc:postgresql://192.168.98.151:5432/worldmesh?currentSchema=worldmesh"
SPRING_ARGS="${SPRING_ARGS} --spring.datasource.username=worldmesh"
SPRING_ARGS="${SPRING_ARGS} --spring.datasource.password=worldmesh"

cd $OWNDIR/../services
checkrc cd
mvn spring-boot:run -Dspring-boot.run.arguments="$SPRING_ARGS" -Dspring-boot.run.jvmArguments="-Dviewer2d.enabled=false"