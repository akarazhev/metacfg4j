# metacfg4j

The `metacfg4j` project (that stands for the `meta configuration for java`) is a library that can be used as a solution to add a business abstraction or 
may extend an existed implementation to provide such software solutions as: various configuration (application, user's and etc.), CRUD services.

## Architecture

This is a high-level abstraction based on the low-level API. It has been written without frameworks and delivered with two dependencies:

 &#8658; Embedded H2db (https://www.h2database.com)<br/>
 &#8658; JSON simple (https://github.com/fangyidong/json-simple)<br/>

This library has an implementation of a simple web-server and a repository, service, controller. Web-server provides implementation of REST methods.
Data is persisted into the embedded H2 DataBase.
  
## Usage

### Basic Usage

Add a maven dependency into your project:
```xml
<dependency>
    <groupId>com.github.akarazhev.metacfg</groupId>
    <artifactId>metacfg4j</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```
Instantiate the meta configuration class in your project with the default configuration:
```java
@Bean(destroyMethod = "close")
public MetaConfig metaConfig() {
    return new MetaConfig.Builder().build();
}
```
If you have the configured data source, you can use it:
```java
@Bean(destroyMethod = "close")
public MetaConfig metaConfig() {
    return new MetaConfig.Builder().dataSource(ds).build();
}
```
Or you can instantiate the meta configuration with the custom configuration:
```java
@Bean(destroyMethod = "close")
public MetaConfig metaConfig() {
    // Create the DB server config
    final Config dbServer = new Config.Builder(H2dbServer.Settings.CONFIG_NAME,
        Arrays.asList(
                new Property.Builder(H2dbServer.Settings.TYPE, H2dbServer.Settings.TYPE_TCP).build(),
                new Property.Builder(H2dbServer.Settings.ARGS, "-tcp", "-tcpPort", "8043").build())).
        build();
    // Create the connection pool config
    final Config connectionPool = new Config.Builder(ConnectionPools.Settings.CONFIG_NAME,
        Arrays.asList(
                new Property.Builder(ConnectionPools.Settings.URL, "jdbc:h2:./data/metacfg4j").build(),
                new Property.Builder(ConnectionPools.Settings.USER, "sa").build(),
                new Property.Builder(ConnectionPools.Settings.PASSWORD, "sa").build())).
        build();
    // Create the web server config
    final Config webServer = new Config.Builder(ConfigServer.Settings.CONFIG_NAME,
        Arrays.asList(
                new Property.Builder(ConfigServer.Settings.PORT, 8000).build(),
                new Property.Builder(ConfigServer.Settings.BACKLOG, 0).build()))
        .build();
    // Create the meta configuration
    return new MetaConfig.Builder().
        dbServer(dbServer).
        connectionPool(connectionPool).
        webServer(webServer).
        build();
    }
```

### Advanced Usage

Documentation is in the progress.

## Build Requirements

 &#8658; Java 8+
 &#8658; Maven 3.6+