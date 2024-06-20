brooklime
===

[![Maven Central](https://img.shields.io/maven-central/v/com.io7m.brooklime/com.io7m.brooklime.svg?style=flat-square)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.io7m.brooklime%22)
[![Maven Central (snapshot)](https://img.shields.io/nexus/s/com.io7m.brooklime/com.io7m.brooklime?server=https%3A%2F%2Fs01.oss.sonatype.org&style=flat-square)](https://s01.oss.sonatype.org/content/repositories/snapshots/com/io7m/brooklime/)
[![Codecov](https://img.shields.io/codecov/c/github/io7m-com/brooklime.svg?style=flat-square)](https://codecov.io/gh/io7m-com/brooklime)
![Java Version](https://img.shields.io/badge/17-java?label=java&color=e65cc3)

![com.io7m.brooklime](./src/site/resources/brooklime.jpg?raw=true)

| JVM | Platform | Status |
|-----|----------|--------|
| OpenJDK (Temurin) Current | Linux | [![Build (OpenJDK (Temurin) Current, Linux)](https://img.shields.io/github/actions/workflow/status/io7m-com/brooklime/main.linux.temurin.current.yml)](https://www.github.com/io7m-com/brooklime/actions?query=workflow%3Amain.linux.temurin.current)|
| OpenJDK (Temurin) LTS | Linux | [![Build (OpenJDK (Temurin) LTS, Linux)](https://img.shields.io/github/actions/workflow/status/io7m-com/brooklime/main.linux.temurin.lts.yml)](https://www.github.com/io7m-com/brooklime/actions?query=workflow%3Amain.linux.temurin.lts)|
| OpenJDK (Temurin) Current | Windows | [![Build (OpenJDK (Temurin) Current, Windows)](https://img.shields.io/github/actions/workflow/status/io7m-com/brooklime/main.windows.temurin.current.yml)](https://www.github.com/io7m-com/brooklime/actions?query=workflow%3Amain.windows.temurin.current)|
| OpenJDK (Temurin) LTS | Windows | [![Build (OpenJDK (Temurin) LTS, Windows)](https://img.shields.io/github/actions/workflow/status/io7m-com/brooklime/main.windows.temurin.lts.yml)](https://www.github.com/io7m-com/brooklime/actions?query=workflow%3Amain.windows.temurin.lts)|

## brooklime

The `brooklime` package implements a Java API to the
[Sonatype Nexus](https://www.sonatype.com/product-nexus-repository) repository
manager. It exclusively implements the _staging_ workflow used in the
professional version of Nexus, and is suitable for deploying
content to [Maven Central](https://search.maven.org).

## Features

* Clean Java 17 API.
* Simple command-line interface.
* Creates staging repositories.
* Uploads content to staging repositories.
* Closes staging repositories.
* Releases staging repositories.
* Drops staging repositories.
* High-coverage automated test suite.
* ISC license.

## Usage

See the [documentation](https://www.io7m.com/software/brooklime).

