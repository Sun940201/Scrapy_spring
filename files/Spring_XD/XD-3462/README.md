# spring-cloud-dataflow

The Spring Cloud Data Flow project provides orchestration for data microservices, including
both [stream](https://github.com/spring-cloud/spring-cloud-stream) and
[task](https://github.com/spring-cloud/spring-cloud-task) processing modules.

## Components

The [Core](https://github.com/spring-cloud/spring-cloud-dataflow/tree/master/spring-cloud-dataflow-core)
domain module includes the concept of a **stream** that is a composition of spring-cloud-stream
modules in a linear pipeline from a *source* to a *sink*, optionally including *processor* modules
in between. The domain also includes the concept of a **task**, which may be any process that does
not run indefinitely, including [Spring Batch](https://github.com/spring-projects/spring-batch) jobs.

The [Module Registry](https://github.com/spring-cloud/spring-cloud-dataflow/tree/master/spring-cloud-dataflow-module-registry)
maintains the set of available modules, and their mappings to Maven coordinates.

The [Module Deployer SPI](https://github.com/spring-cloud/spring-cloud-dataflow/tree/master/spring-cloud-dataflow-module-deployers/spring-cloud-dataflow-module-deployer-spi)
provides the abstraction layer for deploying the modules of a given stream across a variety of runtime environments, including:
* [Local](https://github.com/spring-cloud/spring-cloud-dataflow/tree/master/spring-cloud-dataflow-module-deployers/spring-cloud-dataflow-module-deployer-local)
* [Lattice](https://github.com/spring-cloud/spring-cloud-dataflow/tree/master/spring-cloud-dataflow-module-deployers/spring-cloud-dataflow-module-deployer-lattice)
* [Cloud Foundry](https://github.com/spring-cloud/spring-cloud-dataflow/tree/master/spring-cloud-dataflow-module-deployers/spring-cloud-dataflow-module-deployer-cloudfoundry)
* [Yarn](https://github.com/spring-cloud/spring-cloud-dataflow/tree/master/spring-cloud-dataflow-module-deployers/spring-cloud-dataflow-module-deployer-yarn)

The [Admin](https://github.com/spring-cloud/spring-cloud-dataflow/tree/master/spring-cloud-dataflow-admin) provides a REST API and UI. It is an executable Spring Boot application that is profile aware, so that the proper implementation of the Module Deployer SPI will be instantiated based on the environment within which the Admin application itself is running.

The [Shell](https://github.com/spring-cloud/spring-cloud-dataflow/tree/master/spring-cloud-dataflow-shell) connects to the Admin's REST API and supports a DSL that simplifies the process of defining a stream and managing its lifecycle.

The instructions below describe the process of running both the Admin and the Shell across different runtime environments.

## Running Singlenode

1\. start Redis locally via `redis-server`

2\. clone this repository and build from the root directory:

```
git clone https://github.com/spring-cloud/spring-cloud-dataflow.git
cd spring-cloud-dataflow
mvn clean package
```

3\. launch the admin:

```
$ java -jar spring-cloud-dataflow-admin/target/spring-cloud-dataflow-admin-1.0.0.BUILD-SNAPSHOT.jar
```

4\. launch the shell:

```
$ java -jar spring-cloud-dataflow-shell/target/spring-cloud-dataflow-shell-1.0.0.BUILD-SNAPSHOT.jar
```

thus far, only the following commands are supported in the shell when running singlenode:
* `stream list`
* `stream create`
* `stream deploy`

## Running on Lattice

1\. start Redis on Lattice (running as root):

```
ltc create redis redis -r
```

2\. launch the admin, with a mapping for port 9393 and extra memory (the default is 128MB):

```
ltc create admin springcloud/dataflow-admin -p 9393 -m 512
```

3\. launching the shell is the same as above, but once running must be
configured to point to the admin that is running on Lattice:

```
server-unknown:>admin config server http://admin.192.168.11.11.xip.io
Successfully targeted http://admin.192.168.11.11.xip.io
dataflow:>
```

all stream commands are supported in the shell when running on Lattice:
* `stream list`
* `stream create`
* `stream deploy`
* `stream undeploy`
* `stream all undeploy`
* `stream destroy`
* `stream all destroy`

## Running on Cloud Foundry

Spring Cloud Data Flow can be used to deploy modules in a Cloud Foundry
environment. When doing so, the [Admin](https://github.com/spring-cloud/spring-cloud-dataflow/tree/master/spring-cloud-dataflow-admin) application can either run itself on Cloud Foundry, or on another installation (_e.g._ a simple laptop).

The required configuration amounts to the same, and is merely related to providing credentials to the Cloud Foundry instance, so that the admin can spawn applications itself. Any Spring Boot compatible configuration mechanism can be used (passing program arguments, editing configuration files before building the application, using [Spring Cloud Config](https://github.com/spring-cloud/spring-cloud-config), using environment variables, _etc._), although although some may prove more adequate than others when running _on_ Cloud Foundry.

1\. provision a redis service instance on Cloud Foundry.
Your mileage may vary depending on your Cloud Foundry installation. Use `cf marketplace` to discover which plans are available to you. For example when using [Pivotal Web Services](https://run.pivotal.io/):
```
cf create-service rediscloud 30mb redis
```

2\. build packages

```
$ mvn clean package
```

3a\. push the admin application on Cloud Foundry, configure it (see below) and start it

```
cf push s-c-dataflow-admin --no-start -p spring-cloud-dataflow-admin/target/spring-cloud-dataflow-admin-1.0.0.BUILD-SNAPSHOT.jar
cf bind-service s-c-dataflow-admin redis
... configure it ...
cf start s-c-dataflow-admin
```

alternatively,

3b\. run the admin application locally, targeting your Cloud Foundry installation (see below for configuration)

```
java -jar spring-cloud-dataflow-admin/target/spring-cloud-dataflow-admin-1.0.0.BUILD-SNAPSHOT.jar [--option1=value1] [--option2=value2] [etc.]
```

4\. run the shell and optionally target the Admin application if not running on the same host (will typically be the case if deployed on Cloud Foundry as **3a.**)
```
$ java -jar spring-cloud-dataflow-shell/target/spring-cloud-dataflow-shell-1.0.0.BUILD-SNAPSHOT.jar
```
```
server-unknown:>admin config server http://s-c-dataflow-admin.cfapps.io
Successfully targeted http://s-c-dataflow-admin.cfapps.io
dataflow:>
```

At step **3.**, either running _on_ Cloud Foundry or _targeting_ Cloud Foundry, the following pieces of configuration must be provided, for example using `cf env s-c-dataflow-admin CLOUDFOUNDRY_DOMAIN mydomain.cfapps.io` (note the use of underscores) when running _in_ Cloud Foundry

```
# Default values cited after the equal sign.
# Example values, typical for Pivotal Web Services, cited as a comment

# url of the CF API (used when using cf login -a for example), e.g. https://api.run.pivotal.io
cloudfoundry.apiEndpoint=

# name of the space into which modules will be deployed
cloudfoundry.space=<same as admin when running on CF or 'development'>

# name of the organization that owns the space above, e.g. youruser-org
cloudfoundry.organization=

# the root domain to use when mapping routes, e.g. cfapps.io
cloudfoundry.domain=

# Comma separated set of service instance names to bind to the module.
# Amongst other things, this should include a service that will be used
# for Spring Cloud Stream binding
cloudfoundry.services=redis

# url used for obtaining an OAuth2 token, e.g. https://uaa.run.pivotal.io/oauth/token
security.oauth2.client.access-token-uri=

# url used to grant user authorizations, e.g. https://login.run.pivotal.io/oauth/authorize
security.oauth2.client.user-authorization-uri=

# username and password of the user to use to create apps (modules)
security.oauth2.client.username=
security.oauth2.client.password=
```

## Running on Hadoop YARN

Current YARN configuration is set to use localhost meaning this can only be run against local cluster. Also all commands need to be run from a project root.

1\. build packages

```
$ mvn clean package
```

2\. start Redis locally via `redis-server`

3\. optionally wipe existing data on `hdfs`

```
$ hdfs dfs -rm -R /app/app
```

4\. start `spring-cloud-dataflow-admin` with `yarn` profile

```
$ java -Dspring.profiles.active=yarn -Ddataflow.yarn.app.appmaster.path=spring-cloud-dataflow-yarn/spring-cloud-dataflow-yarn-appmaster/target -Ddataflow.yarn.app.container.path=spring-cloud-dataflow-yarn/spring-cloud-dataflow-yarn-container/target -jar spring-cloud-dataflow-admin/target/spring-cloud-dataflow-admin-1.0.0.BUILD-SNAPSHOT.jar
```

5\. start `spring-cloud-dataflow-shell`

```
$ java -jar spring-cloud-dataflow-shell/target/spring-cloud-dataflow-shell-1.0.0.BUILD-SNAPSHOT.jar

dataflow:>stream create --name "ticktock" --definition "time --fixedDelay=5|log" --deploy

dataflow:>stream list
  Stream Name  Stream Definition        Status
  -----------  -----------------------  --------
  ticktock     time --fixedDelay=5|log  deployed

dataflow:>stream destroy --name "ticktock"
Destroyed stream 'ticktock'
```

YARN application is pushed and started automatically during a stream deployment process. This application instance is not automatically closed which can be done from CLI:

```
$ java -jar spring-cloud-dataflow-yarn/spring-cloud-dataflow-yarn-client/target/spring-cloud-dataflow-yarn-client-1.0.0.BUILD-SNAPSHOT.jar shell
Spring YARN Cli (v2.3.0.M2)
Hit TAB to complete. Type 'help' and hit RETURN for help, and 'exit' to quit.

$ submitted
  APPLICATION ID                  USER          NAME                            QUEUE    TYPE       STARTTIME       FINISHTIME  STATE    FINALSTATUS  ORIGINAL TRACKING URL
  ------------------------------  ------------  ----------------------------------  -------  --------  --------------  ----------  -------  -----------  --------------------------
  application_1439803106751_0088  jvalkealahti  spring-cloud-dataflow-yarn-app_app  default  DATAFLOW  01/09/15 09:02  N/A         RUNNING  UNDEFINED    http://192.168.122.1:48913

$ shutdown -a application_1439803106751_0088
shutdown requested
```

Properties `dataflow.yarn.app.appmaster.path` and `dataflow.yarn.app.container.path` can be used with both `spring-cloud-dataflow-admin` and `and spring-cloud-dataflow-yarn-client` to define directory for `appmaster` and `container` jars. Values for those default to `.` which then assumes all needed jars are in a same working directory.

## Building from Source

 $ mvn clean install

### Building reference documentation

You can build the reference documentation with the command below:

 $ mvn clean install -pl spring-boot-docs -Pfull

> TIP: The generated documentation is available from `spring-cloud-dataflow-docs/target/contents/reference`

