# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

# Set the working directory
WORKDIR /home/container

# Copy the current directory contents into the container at /home/container
COPY . .

# Build the jar
RUN mvn package -q -Dmaven.test.skip -DskipTests -T2C

# Stage 2: Create the final lightweight image
FROM eclipse-temurin:17.0.11_9-jre-focal

# Set the app to be in production mode
ENV ENVIRONMENT=production

# Set the working directory
WORKDIR /home/container

# Copy the built jar file from the builder stage
COPY --from=builder /home/container/target/PIA-Servers.jar .

# Make port 80 available to the world outside this container
EXPOSE 80
ENV PORT=80

# Run the jar file
CMD java -jar PIA-Servers.jar -Djava.awt.headless=true