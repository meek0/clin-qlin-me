FROM maven:3.9.4-amazoncorretto-21 as build-app
WORKDIR /tmp/app
COPY . .
RUN mvn clean install -DskipTests

FROM amazoncorretto:21-alpine as build-jre
WORKDIR /tmp/jre
# required for strip-debug to work
RUN apk add --no-cache binutils
RUN jlink \
         --add-modules ALL-MODULE-PATH \
         --strip-debug \
         --no-man-pages \
         --no-header-files \
         --compress=2 \
         --output slim

FROM alpine:latest
RUN apk add --no-cache curl
WORKDIR /app
ENV JAVA_HOME=/jre
ENV PATH="$PATH:$JAVA_HOME/bin"
COPY --from=build-jre /tmp/jre/slim $JAVA_HOME
COPY --from=build-app /tmp/app/target/clin-qlin-me-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT java $JAVA_OPTS -jar app.jar
