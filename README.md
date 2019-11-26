# metacfg4j

The `metacfg4j` project (that stands for the `meta configuration for java`) is a library that can be used as the solution by creating a business abstraction or 
may extend an existed implementation to provide such software solutions as: various configuration (application, user's and etc.), CRUD services, DSL, MVP.

## Architecture

This is a high-level abstraction based on the low-level API. It has been written without frameworks and delivered with one dependency:

 &#8658; JSON simple (https://github.com/fangyidong/json-simple)<br/>

This library has the implementation of a simple web-client/server, repositories, services, controllers. The web-server provides implementation of REST methods.
Data is persisted into a DB, by using any configured datasource.
  
## Usage

### Basic Usage

Add a maven dependency into your project:
```xml
<dependency>
    <groupId>com.github.akarazhev.metacfg</groupId>
    <artifactId>metacfg4j</artifactId>
    <version>1.0</version>
</dependency>
```
Instantiate the meta configuration class in your project with the default configuration:
```java
@Bean(destroyMethod = "close")
public MetaConfig metaConfig() {
    return new MetaConfig.Builder().defaultConfig().build();
}
```
If you have the configured data source, you can use it:
```java
@Bean(destroyMethod = "close")
public MetaConfig metaConfig() {
    return new MetaConfig.Builder().dataSource(ds).build();
}
```
NOTE: The web-server will not be started, since it requires the related configuration.

### Advanced Usage

You can instantiate the meta configuration with the custom configuration:
```java
@Bean(destroyMethod = "close")
public MetaConfig metaConfig() {
    // Create the web server config
    final Config webServer = new Config.Builder(Server.Settings.CONFIG_NAME,
        Arrays.asList(
                new Property.Builder(Settings.HOSTNAME, "localhost").build(),
                new Property.Builder(Settings.PORT, 8000).build(),
                new Property.Builder(Settings.BACKLOG, 0).build(),
                new Property.Builder(Settings.KEY_STORE_FILE, "./data/metacfg4j.keystore").build(),
                new Property.Builder(Settings.ALIAS, "alias").build(),
                new Property.Builder(Settings.STORE_PASSWORD, "password").build(),
                new Property.Builder(Settings.KEY_PASSWORD, "password").build()))
        .build();
    // Create the meta configuration
    return new MetaConfig.Builder().
        webServer(webServer).
        build();
    }
```

### Certificate generation

To generate a certificate, you need open a command line and and enter:
```bash
keytool -genkeypair -keyalg RSA -alias alias -keypass password -keystore metacfg4j.keystore -storepass password
```
If you have certificate that's signed by CA (Certificate Authority), please use it.

### Settings

## Build Requirements

 &#8658; Java 8+
 &#8658; Maven 3.6+