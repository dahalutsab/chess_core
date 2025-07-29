FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

COPY target/chess_engine-0.0.1-SNAPSHOT.jar app.jar

# Create all possible directories
RUN mkdir -p /home/ubuntu/chess_engine && \
    mkdir -p /Users/chess_engine && \
    mkdir -p /windows_chess_engine

# Environment variables with correct paths
ENV WINDOWS_PATH=/windows_chess_engine/
ENV LINUX_PATH=/home/ubuntu/chess_engine/
ENV MAC_PATH=/Users/chess_engine/

# Entrypoint script
RUN echo '#!/bin/sh\n\
if [ -d "$WINDOWS_PATH" ]; then\n\
    export ACTUAL_FILEPATH="$WINDOWS_PATH"\n\
elif [ -d "$MAC_PATH" ]; then\n\
    export ACTUAL_FILEPATH="$MAC_PATH"\n\
else\n\
    export ACTUAL_FILEPATH="$LINUX_PATH"\n\
fi\n\
exec java -Dapp.filepath.windows="$WINDOWS_PATH" \
          -Dapp.filepath.linux="$LINUX_PATH" \
          -Dapp.filepath.mac="$MAC_PATH" \
          -Dapp.filepath.actual="$ACTUAL_FILEPATH" \
          -jar app.jar' > /app/entrypoint.sh && \
    chmod +x /app/entrypoint.sh

EXPOSE 8181
ENTRYPOINT ["/app/entrypoint.sh"]