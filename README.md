# metacfg4j

The `metacfg4j` project (that stands for the `meta configuration for java`) is a library that can be used as a business abstraction or 
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
Instantiate a meta configuration class in your project:
```java
@Bean(destroyMethod = "close")
public MetaConfig metaConfig() {
    return new MetaConfig.Builder().build();
}
```
Documentation is in the progress.

### Advanced Usage

Documentation is in the progress.

## Build Requirements

 &#8658; Java 8+
 &#8658; Maven 3.6+