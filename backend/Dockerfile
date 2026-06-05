ARG DOCKER_REGISTRY=docker.m.daocloud.io

FROM ${DOCKER_REGISTRY}/library/maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

ARG MAVEN_MIRROR_URL=https://maven.aliyun.com/repository/public

COPY .mvn/ .mvn/
COPY pom.xml .
RUN mvn -Drepo.public.url=${MAVEN_MIRROR_URL} dependency:go-offline -q

COPY src ./src
RUN mvn -Drepo.public.url=${MAVEN_MIRROR_URL} package -DskipTests -q

FROM ${DOCKER_REGISTRY}/library/eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
