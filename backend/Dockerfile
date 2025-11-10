# ---- Stage 1: Build ----
FROM gradle:9.1-jdk21 AS builder
WORKDIR /app

# Copy Gradle project files
COPY build.gradle settings.gradle gradlew ./
COPY src ./src
COPY gradle ./gradle

# Build the runnable JAR (including dependencies)
RUN ./gradlew shadowJar --no-daemon

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:21-jdk
WORKDIR /app

# Install OpenCV dependencies (Linux native libs)
# RUN apt-get update && apt-get install -y libopencv-core-dev && rm -rf /var/lib/apt/lists/*
# RUN apk add --no-cache libstdc++ gcc g++
#RUN dnf install -y \
#    libstdc++ \
#    && rm -rf /var/cache/dnf/*
RUN apt-get update && apt-get install -y \
    libstdc++6 \
    libgcc-s1 \
    && rm -rf /var/lib/apt/lists/*

# Copy the built JAR from the builder stage
COPY --from=builder /app/build/libs/*.jar app.jar
COPY colourPHash.bin colourPHash.bin
COPY orbDatabase.bin orbDatabase.bin

# Expose the Javalin port
EXPOSE 7000

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
