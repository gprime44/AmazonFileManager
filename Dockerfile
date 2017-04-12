FROM openjdk:7

VOLUME /data
VOLUME /config

ENV JAVA_OPTS ""
ENV SPRING_CONFIG_LOCATION /config/application.properties

RUN apt-get update && apt-get install -y \
python3 \
python3-appdirs \
python3-dateutil \
python3-requests \
python3-sqlalchemy \
python3-pip \
git \
encfs \
unionfs-fuse \
maven

RUN pip3 install --upgrade git+https://github.com/yadayada/acd_cli.git

WORKDIR /code

ADD pom.xml /code/pom.xml
RUN ["mvn", "dependency:resolve"]
RUN ["mvn", "verify"]

ADD src /code/src
RUN ["mvn", "package"]

CMD ["/bin/sh", "-c", "java $JAVA_OPTS -jar /code/target/app.jar --spring.config.location=$SPRING_CONFIG_LOCATION"]