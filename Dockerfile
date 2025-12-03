# BUILD STAGE
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# copy Maven wrapper and pom.xml
COPY mvnw .
COPY pom.xml .

# make mvnw executable and build
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# copy source code and build
COPY src ./src
RUN ./mvnw clean package -DskipTests

# RUNTIME STAGE
FROM eclipse-temurin:21-jre

WORKDIR /app

# copy the JAR file from build stage
COPY --from=builder /app/target/java-tcp-programming-1.0-SNAPSHOT.jar app.jar

# expose default port (can be overridden)
EXPOSE 4269

# set entrypoint
ENTRYPOINT ["java", "-jar", "app.jar"]

