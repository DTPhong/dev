FROM openjdk:8
VOLUME /tmp
COPY deploy/*.jar ./
COPY conf/conf.prod ./conf
COPY deploy/lib ./lib
COPY conf/conf.prod ./
RUN $JAVA_HOME/bin/keytool -import -file gke.crt -alias k8s -keystore $JAVA_HOME/jre/lib/security/cacerts -trustcacerts -storepass changeit -noprompt
ENTRYPOINT ["java","-jar","-Dappenv=development","-Dapppath=","match3.service.gamemanager.jar"]


