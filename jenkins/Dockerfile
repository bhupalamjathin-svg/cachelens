# Jenkins LTS (JDK 17) + Docker CLI, so the pipeline's "Docker Image" stage can talk to the host daemon.
FROM jenkins/jenkins:lts-jdk17
USER root
RUN apt-get update \
 && apt-get install -y --no-install-recommends docker.io git \
 && rm -rf /var/lib/apt/lists/*
USER jenkins
