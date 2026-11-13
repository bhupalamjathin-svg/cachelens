// CacheLens CI pipeline
//
// Trigger : polls GitHub every minute (no public URL needed for a webhook on a laptop).
// Stages  : Checkout -> Test (JUnit) -> Build (versioned JAR) -> Docker Image (versioned tag)
// Post    : publishes test results and archives the JAR as the build artifact.
//
// Enhancements implemented here:
//   1. Automated tests in CI   - the Test stage runs before Build; a failing test stops the pipeline.
//   2. Build versioning        - artifact is cachelens-1.0.<BUILD_NUMBER>-<GIT_SHA>.jar and the Docker
//                                image is tagged with the same string, so every build maps to one commit.

pipeline {
    agent any

    tools {
        // Manage Jenkins -> Tools -> Maven installations -> name it "Maven3" (install automatically)
        maven 'Maven3'
    }

    triggers {
        pollSCM('* * * * *')
    }

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    environment {
        APP = 'cachelens'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_SHA = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    env.VERSION = "1.0.${env.BUILD_NUMBER}-${env.GIT_SHA}"
                    currentBuild.displayName = "#${env.BUILD_NUMBER} ${env.VERSION}"
                }
                echo "Building ${APP} version ${VERSION}"
            }
        }

        stage('Test') {
            steps {
                sh 'mvn -B clean test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Build') {
            steps {
                // tests already ran in the previous stage
                sh "mvn -B package -DskipTests -Drevision=${VERSION}"
                sh "ls -lh target/${APP}-${VERSION}.jar"
            }
        }

        stage('Docker Image') {
            // Runs only when the Jenkins agent can reach a Docker daemon (see jenkins/docker-compose.yml).
            when {
                expression { sh(script: 'docker info > /dev/null 2>&1', returnStatus: true) == 0 }
            }
            steps {
                sh "docker build --build-arg REVISION=${VERSION} -t ${APP}:${VERSION} -t ${APP}:latest ."
                sh "docker images ${APP} --format 'table {{.Repository}}\\t{{.Tag}}\\t{{.Size}}'"
            }
        }
    }

    post {
        success {
            archiveArtifacts artifacts: "target/${APP}-${VERSION}.jar", fingerprint: true
            echo "Archived ${APP}-${VERSION}.jar"
        }
        failure {
            echo 'Build failed - check the Test stage report above.'
        }
    }
}
