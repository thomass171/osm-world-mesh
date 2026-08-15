#
#
export VERBOSELOG=0

MR=$HOME/.m2/repository
VERSION=1.0.0-SNAPSHOT

#for module in core java-common java-native engine maze outofbrowser-common tools traffic
#do
 # CLASSPATH=$CLASSPATH:$MR/de/yard/tcp-22/module-$module/$VERSION/module-$module-$VERSION.jar
#done

#CLASSPATH=$CLASSPATH:$MR/org/apache/logging/log4j/log4j-api/2.17.2/log4j-api-2.17.2.jar
#CLASSPATH=$CLASSPATH:$MR/org/apache/logging/log4j/log4j-core/2.17.2/log4j-core-2.17.2.jar
#CLASSPATH=$CLASSPATH:$MR/org/apache/logging/log4j/log4j-slf4j-impl/2.17.2/log4j-slf4j-impl-2.17.2.jar

export CLASSPATH MR

error() {
        echo $*
        exit 1
}

verboselog() {
        if [ $VERBOSELOG = "1" ]
        then
                echo $*
        fi
}

relax() {
        sleep 1
}

checkrc() {
        RC=$?
        if [ $RC != 0 ]
        then
                error $* returned $RC
        fi
}
