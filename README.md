# LGP Examples

[![license][license-image]][license-url]

## About

A set of example usages of [`nz.co.jedsimson.lgp.LGP`](https://github.com/JedS6391/LGP) and [`nz.co.jedsimson.lgp.LGP-lib`](https://github.com/JedS6391/LGP-lib).

## Usage

To run the provided examples, you can build a fat JAR using Gradle and then execute the given main class, as follows:

1. Build a fat JAR

```bash
./gradlew jar
```

This will output a JAR to build/libs that contains everything necessary to run the examples.

2. Run your chosen problem

```bash
cd build/libs
kotlin -cp LGP-examples-1.0.jar:. nz.co.jedsimson.lgp.examples.kotlin.SimpleFunction
```

[license-image]: https://img.shields.io/github/license/mashape/apistatus.svg?style=flat
[license-url]: https://github.com/JedS6391/LGP/blob/master/LICENSE
