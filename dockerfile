# Start with a base image containing Java runtime
FROM openjdk:21-jdk

WORKDIR /app

COPY build/libs/communication-backend-0.0.1-SNAPSHOT.jar app.jar

# Add Maintainer Info
# LABEL maintainer="your.email@example.com"

# Add a volume pointing to /tmp
# VOLUME /tmp

# Make port 8080 available to the world outside this container
EXPOSE 8080

# # The application's jar file
# ARG JAR_FILE=target/spring-boot-app-1.0.0.jar

# # Add the application's jar to the container
# ADD ${JAR_FILE} app.jar

# Run the jar file
# ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
ENTRYPOINT ["java", "-jar", "app.jar"]