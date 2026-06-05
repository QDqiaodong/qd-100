ARG DOCKER_REGISTRY=docker.m.daocloud.io

FROM ${DOCKER_REGISTRY}/library/maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

ARG MAVEN_MIRROR_URL=https://repo.maven.apache.org/maven2
ARG MAVEN_REPO=/root/.m2/repository
ARG MAVEN_ARTIFACT_THREADS=16

COPY .mvn/ .mvn/
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=${MAVEN_REPO},sharing=locked \
    mvn -B -ntp \
      -Drepo.public.url=${MAVEN_MIRROR_URL} \
      -Dmaven.artifact.threads=${MAVEN_ARTIFACT_THREADS} \
      -Dmaven.repo.local=${MAVEN_REPO} \
      -DskipTests \
      package

FROM ${DOCKER_REGISTRY}/library/eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
