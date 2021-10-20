FROM 730502903637.dkr.ecr.us-east-1.amazonaws.com/gradle:7.2.0 as builder
ENV WORKSPACE=/home/builder
WORKDIR $WORKSPACE

# Install Data Dog Agent
RUN yum -y install git wget
RUN wget -O dd-java-agent.jar 'https://dtdg.co/latest-java-tracer' --no-check-certificate

COPY settings.gradle.kts settings.gradle.kts
COPY app/build.gradle.kts app/build.gradle.kts
# Copy git folder for GIT_SHA. Please comment below .git copy if there is no git repo yet
COPY .git .git

# Download dependencies first (Docker cache optimization)
RUN gradle --stacktrace -i clean app:dependencies

# Build and run tests
COPY app app

RUN gradle --stacktrace -i app:build

RUN echo $(ls -a app/build/libs)

# Set the version number for Data Dog. Please comment below line if there is no git repo yet
RUN git rev-parse --short HEAD >> VERSION

FROM 730502903637.dkr.ecr.us-east-1.amazonaws.com/openjdk:11.0.9.1 as prod
ENV WORKSPACE=/home/builder

COPY --from=builder ${WORKSPACE}/dd-java-agent.jar .
#Please comment below line if there is no git repo yet
COPY --from=builder ${WORKSPACE}/VERSION .
COPY --from=builder ${WORKSPACE}/app/build/libs/app.jar .
COPY --from=builder ${WORKSPACE}/app/src/main/resources .

# Setup to run the application during deployment
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]