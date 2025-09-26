FROM maven:3.9.9-eclipse-temurin-17 AS builder

# Set working directory
WORKDIR /app

# Copy pom and source
COPY pom.xml .
COPY src ./src
COPY ./lib/jade.jar ./lib/jade.jar

# Install local jade.jar into Maven repository
RUN mvn install:install-file \
      -Dfile=./lib/jade.jar \
      -DgroupId=jade \
      -DartifactId=jade \
      -Dversion=4.5.0 \
      -Dpackaging=jar

# Package application (skipping tests for faster builds, adjust if needed)
RUN mvn clean package -DskipTests

FROM nvidia/cuda:12.2.0-runtime-ubuntu22.04

RUN apt-get update && apt-get install -y openjdk-17-jre python3 python3-pip && rm -rf /var/lib/apt/lists/*

COPY src/main/resources/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

WORKDIR /app

# Copy built jar from builder stage
COPY --from=builder /app/target/worker_agent-1.0-SNAPSHOT-shaded.jar app.jar

CMD ["java", "-jar", "app.jar"]