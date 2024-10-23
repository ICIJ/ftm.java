# ftm.java

This repository is providing a Java (or JVM based) classes and records from the [Follow The Money](https://followthemoney.tech) models.

The goal is to use the YAML source files to generate Java records and classes, then publish a jar that contains the generated models.

The schema files are [here](https://github.com/alephdata/followthemoney/tree/main/followthemoney/schema).

This is a work in progress, for now, only "required" fields are generated in Java code.

To use it, just make `mvn package` it will generate and compile FtM models in a jar.