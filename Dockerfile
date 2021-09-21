FROM 730502903637.dkr.ecr.us-east-1.amazonaws.com/openjdk:11.0.9.1
ENV SRC_PATH=/usr/src
ENV APP_HOME=/home/app
RUN mkdir -p $APP_HOME
RUN yum -y install aws-cli

COPY .git $SRC_PATH/.git
COPY settings.gradle.kts $SRC_PATH
COPY app $SRC_PATH/app
WORKDIR $SRC_PATH

# Run the build and unit tests
RUN gradle clean build \
    && mv app/build/libs/app.jar $APP_HOME

# Setup to run the application during deployment
EXPOSE 8080
WORKDIR $APP_HOME
CMD ["java", "-jar", "app.jar"]