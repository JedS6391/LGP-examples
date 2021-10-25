# LGP Examples

[![license][license-image]][license-url]

## About

A set of example usages of [`nz.co.jedsimson.lgp:core`](https://github.com/JedS6391/LGP/tree/master/core) and [`nz.co.jedsimson.lgp:lib`](https://github.com/JedS6391/LGP/tree/master/lib).

## Usage

To run the provided examples, you can build a fat JAR using Gradle and then execute the given main class, as follows:

1. Build a fat JAR

```bash
./gradlew jar
```

This will output a JAR to the `build/libs` directory that contains everything necessary to run the examples.

2. Run your chosen problem

```bash
cd build/libs
kotlin -cp LGP-examples-1.0.jar:. nz.co.jedsimson.lgp.examples.kotlin.SimpleFunction
```

To enable logging for the framework, add the following option to the above command: `-Dorg.slf4j.simpleLogger.defaultLogLevel=LOG_LEVEL`

[license-image]: https://img.shields.io/github/license/mashape/apistatus.svg?style=flat
[license-url]: https://github.com/JedS6391/LGP/blob/master/LICENSE
