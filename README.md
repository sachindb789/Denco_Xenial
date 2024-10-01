# PunchRetriever

## Overview

The `PunchRetriever` class is a Java application that retrieves punch data and employee details from the Xenial API and inserts the data into a MySQL database. It utilizes Apache HttpClient for HTTP requests and Jackson for JSON processing.

## Table of Contents

- [Dependencies](#dependencies)
- [Configuration](#configuration)
- [Endpoints](#endpoints)
- [Process](#process)
- [Usage](#usage)
- [License](#license)

## Dependencies

Ensure the following dependencies are included in your `pom.xml` (for Maven):

### Maven

```xml
<dependency>
    <groupId>org.apache.httpcomponents</groupId>
    <artifactId>httpclient</artifactId>
    <version>4.5.14</version>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-core</artifactId>
    <version>2.15.2</version>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.15.2</version>
</dependency>
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
    <version>2.0.0</version>
</dependency>
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.33</version>
</dependency>
