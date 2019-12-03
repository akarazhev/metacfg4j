# metacfg4j

[![][Build Status img]][Build Status]
[![][Coverage Status img]][Coverage Status]
[![][license img]][license]
[![][Maven Central img]][Maven Central]
[![][Javadocs img]][Javadocs]

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
public MetaConfig metaConfig() {
    return new MetaConfig.Builder().defaultConfig().build();
}
```
If you have the configured data source, you can use it:
```java
public MetaConfig metaConfig() {
    return new MetaConfig.Builder().dataSource(getDataSource()).build();
}
```
NOTE: The web-server will not be started, since it requires the related configuration.

### Advanced Usage

You can instantiate the meta configuration with the custom configuration:
```java
public MetaConfig metaConfig() {
    // Create the web server config
    final Config webServer = new Config.Builder(Server.Settings.CONFIG_NAME,
        Arrays.asList(
                new Property.Builder(Server.Settings.HOSTNAME, "localhost").build(),
                new Property.Builder(Server.Settings.PORT, 8000).build(),
                new Property.Builder(Server.Settings.BACKLOG, 0).build(),
                new Property.Builder(Server.Settings.KEY_STORE_FILE, "./data/metacfg4j.keystore").build(),
                new Property.Builder(Server.Settings.ALIAS, "alias").build(),
                new Property.Builder(Server.Settings.STORE_PASSWORD, "password").build(),
                new Property.Builder(Server.Settings.KEY_PASSWORD, "password").build()))
        .build();
    // Create the meta configuration
    return new MetaConfig.Builder().
        webServer(webServer).
        dataSource(getDataSource()).
        build();
    }
```

It's possible to configure the meta configuration as a client:
```java
public MetaConfig metaConfig() {
    // Create the web client config
    final Config webClient = new Config.Builder(WebClient.Settings.CONFIG_NAME,
        Arrays.asList(
                new Property.Builder(WebClient.Settings.URL, "https://localhost:8000/api/metacfg").build(),
                new Property.Builder(WebClient.Settings.ACCEPT_ALL_HOSTS, true).build()))
        .build();
    // Create the meta configuration
    return new MetaConfig.Builder().webClient(webClient).build();
}
```
NOTE: you need to call the close method in the end of processing.

### Certificate generation

To generate a certificate, you need open a command line and and enter:
```bash
keytool -genkeypair -keyalg RSA -alias alias -keypass password -keystore metacfg4j.keystore -storepass password
```
If you have certificate that's signed by CA (Certificate Authority), please use it.

### Settings

#### Web Server settings

The following settings are available:

 * `config-server` - the name of the configuration. <br/>
 * `hostname` - the server hostname. <br/>
 * `port` - the server port. <br/>
 * `backlog` - the server backlog. <br/>
 * `keyStoreFile` - the path to a certificate. <br/>
 * `alias` - the alias of a certificate. <br/>
 * `storePassword` - the store password of a certificate. <br/>
 * `keyPassword` - the key password of a certificate. <br/>

#### REST API

The API is available by the https protocol:

`POST api/metacfg/accept_config/CONFIG_NAME` - calls the logic for the config. <br/>
`GET api/metacfg/config_names` - returns a list of config names. <br/>
`GET api/metacfg/config?names=CONFIG_NAMES_IN_BASE64` - returns a list of configs. <br/>
`PUT api/metacfg/config` - creates or updates a config. <br/>
`DELETE api/metacfg/config?names=CONFIG_NAMES_IN_BASE64` - removes a list of configs. <br/>

## Build Requirements

 &#8658; Java 8+ <br/>
 &#8658; Maven 3.6+ <br/>
 
 ## Contribution
 
Contribution is always welcome. Please perform changes and submit pull requests from the `dev` branch instead of `master`.  
Please set your editor to use spaces instead of tabs, and adhere to the apparent style of the code you are editing.