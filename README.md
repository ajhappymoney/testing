# Production Insights #

**Production Insights** is a new tool which provides easy access to production data. It would be a one stop shop which will centralize all the lead information so developers don’t have to go to different places to monitor the loan funnel.

## One-Time Setup
The repo is not ready to run locally immediately after being cloned. From the project root, execute `cp application.local.yml.example ./app/src/main/resources/application-local.yml` to create an active profile for local development. Open the file and fill in the parts of the local configuration that are purposefully kept out of source control. The Engineering Productivity owner should be able to supply these values for you.
## Build and Run
At the top level of the code base, use the gradle wrapper to build and run.

Build project.  Add clean before build keyword to start from scratch.
```shell script
./gradlew build
```
Build and run the application locally.
```shell script
./gradlew bootRun
```

To run the application on docker, run the following command which will create the build.
```shell script
docker compose build
```

After this, you'll be able to run the application using docker-compose:
```shell script
docker compose up -d
```

### Endpoints

|             | URL                    | Description                  |
|-------------|------------------------|------------------------------|
| API(local)         | http://localhost:8080/chart | Loads Dashboard page| 
| API(staging)      | https://production-observability.stage.aws-ue1.happymoney.com/chart | Loads Dashboard page |

## Getting help

If you have questions, concerns, bug reports, etc, please contact [Hrishi Kadam](mailto:hrishi@happymoney.com), [Pablo Delgado](mailto:pdelgado@happymoney.com), [Leena Venugopal](mailto:lvenugopal@happymoney.com)
