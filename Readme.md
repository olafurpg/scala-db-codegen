# scala-db-codegen

Generate Scala code from your database.
Only tested with postgresql, but could in theory work with any jdbc compliant database.

## CLI

```shell
$ db-codegen --help
db-codegen 0.1.0
Usage: db-codegen [options]
  --usage
        Print usage and exit
  --help | -h
        Print help message and exit
  --user  <value>
        user on database server
  --password  <value>
        password for user on database server
  --url  <value>
        jdbc url
  --schema  <value>
        schema on database
  --jdbc-driver  <value>
        only tested with postgresql
  --imports  <value>
        top level imports of generated file
  --package  <value>
        package name for generated classes
  --type-map  <value>
        Which types should write to which types? Format is: numeric,BigDecimal;int8,Long;...
  --excluded-tables  <value>
        Do not generate classes for these tables.
  --file  <value>
        Write generated code to this filename. Prints to stdout if not set.
```

## Standalone library

TODO, look at source for now.


