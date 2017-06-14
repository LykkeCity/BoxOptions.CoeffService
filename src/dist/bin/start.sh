#!/bin/bash
# (c) lykke.com, 2016. All rights reserved.

BINDIR=$(dirname "$0")
BASEDIR=`cd "$BINDIR/.." >/dev/null; pwd`

if [ -z "$JAVACMD" ] ; then
  if [ -z "$JAVA_HOME" ] ; then
    JAVACMD="/usr/bin/java"
  else
    JAVACMD="$JAVA_HOME/bin/java"
  fi
fi

if [ ! -x "$JAVACMD" ] ; then
  echo "Error: JAVA_HOME is not defined correctly." 1>&2
  echo "  We cannot execute $JAVACMD" 1>&2
  exit 1
fi

CLASSPATH=$BASEDIR/lib/box.options-0.1.jar:$BASEDIR/lib/kotlin-stdlib-1.1.2-5.jar:$BASEDIR/lib/JForex-API-2.13.30.jar:$BASEDIR/lib/DDS2-jClient-JForex-3.1.2.jar:$BASEDIR/lib/gson-2.6.2.jar:$BASEDIR/lib/joda-time-2.9.7.jar:$BASEDIR/lib/log4j-1.2.17.jar:$BASEDIR/lib/amqp-client-3.6.5.jar:$BASEDIR/lib/kotlin-runtime-1.0.6.jar:$BASEDIR/lib/JForex-API-2.13.30-sources.jar:$BASEDIR/lib/DDS2-Charts-6.24.23.jar:$BASEDIR/lib/DDS2-TextEditor-1.27.27.jar:$BASEDIR/lib/ui-core-1.6.30.jar:$BASEDIR/lib/lucene-core-3.4.0.jar:$BASEDIR/lib/lucene-highlighter-3.4.0.jar:$BASEDIR/lib/ecj-4.3.1.jar:$BASEDIR/lib/ta-lib-0.4.4dc.jar:$BASEDIR/lib/7zip-4.65.jar:$BASEDIR/lib/jmockit-1.5.jar:$BASEDIR/lib/slf4j-log4j12-1.7.13.jar:$BASEDIR/lib/CleanDocking-2.1.5.jar:$BASEDIR/lib/Login-Form-1.6.19.jar:$BASEDIR/lib/greed-common-318.4.17.jar:$BASEDIR/lib/patterns-1.60.19.jar:$BASEDIR/lib/jakarta-oro-2.0.8.jar:$BASEDIR/lib/lucene-memory-3.4.0.jar:$BASEDIR/lib/lucene-queries-3.4.0.jar:$BASEDIR/lib/javax.inject-1.jar:$BASEDIR/lib/commons-lang3-3.3.2.jar:$BASEDIR/lib/MQL4Connector-2.13.24.jar:$BASEDIR/lib/netty-transport-client-0.4.49.jar:$BASEDIR/lib/auth-protocol-client-1.0.10.jar:$BASEDIR/lib/msg-1.0.121.jar:$BASEDIR/lib/mail-1.4.jar:$BASEDIR/lib/commons-beanutils-1.9.2.jar:$BASEDIR/lib/jakarta-regexp-1.4.jar:$BASEDIR/lib/slf4j-api-1.7.13.jar:$BASEDIR/lib/jna-3.5.0.jar:$BASEDIR/lib/MQL4Converter-2.52.24.jar:$BASEDIR/lib/transport-common-0.5.112.jar:$BASEDIR/lib/netty-transport-4.0.42.Final.jar:$BASEDIR/lib/netty-handler-4.0.42.Final.jar:$BASEDIR/lib/javassist-3.18.1-GA.jar:$BASEDIR/lib/guava-18.0.jar:$BASEDIR/lib/json-20131018.jar:$BASEDIR/lib/srp6a-1.5.3.jar:$BASEDIR/lib/activation-1.1.jar:$BASEDIR/lib/commons-logging-1.1.1.jar:$BASEDIR/lib/commons-collections-3.2.1.jar:$BASEDIR/lib/dev-tools-0.0.16.jar:$BASEDIR/lib/netty-buffer-4.0.42.Final.jar:$BASEDIR/lib/netty-codec-4.0.42.Final.jar:$BASEDIR/lib/netty-common-4.0.42.Final.jar

mkdir $BASEDIR/log 2>/dev/null
cd "$BINDIR"

EXECSTR="$JAVACMD -Xms512m -Xmx1g -DBoxOptionsService -server -Dlog4j.configuration=file:///"$BASEDIR"/cfg/log4j.properties $JAVA_OPTS \
    -classpath "$CLASSPATH" \
    -Dapp.name="start.sh" \
    -Dapp.pid="$$" \
    -Dapp.repo="$REPO" \
    -Dapp.home="$BASEDIR" \
    -Dbasedir="$BASEDIR" \
     com.lykke.box.options.AppStarterKt \
     "$BASEDIR"/cfg/application.properties"

if [[ " $@ " =~ " --console " ]] ; then
    exec $EXECSTR ${@%"--console"}
else
    exec $EXECSTR $@ 1>"$BASEDIR/log/out.log" 2>"$BASEDIR/log/err.log" &
fi
