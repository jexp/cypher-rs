## Cypher-RS

Neo4j server-extension that allows to configure fixed REST-Endpoints for Cypher queries.

You can `PUT` cypher queries to an endpoint with a certain url-suffix and then later execute those queries by running

* `GET` with query parameters, only readonly queries
* `POST` with JSON (map, list of maps) payload for parameters
* `POST` with CSV payload for parameters, with optional batch-size and delimiter

### Examples

#### Endpoints

Create

    PUT /cypher-rs/users "match (n:User) where n.name={name} return n"
    --> 201 Location: /cypher-rs/users

    PUT /cypher-rs/create-user "create (n:Node {name:{name},age:{age},male:{male}})"
    --> 201 Location: /cypher-rs/create-user

Query

    GET /cypher-rs/users?name=Andres
    --> 200 {"name":"Andres","age":21,"male":true,"children":["Cypher","L.","N."]}


POST JSON-Data

    POST /cypher-rs/users content-type:application/json {"name":"Andres"}
    --> 200 {"name":"Andres","age":21,"male":true,"children":["Cypher","L.","N."]}

    POST /cypher-rs/users content-type:application/json [{"name":"Andres"},{"name":"Peter"}]
    --> 200 [{"name":"Andres","age":21,"male":true,"children":["Cypher","L.","N."]},
             {"name":"Peter","age":32,"male":true,"children":["Neo4j","O.","K."]}]

POST CSV Data

    POST /cypher-rs/create-user content-type:text/plain "name,age,male\nAndres,21,true"
    --> 200 {"nodes_created":1,"labels_added":1,"properties_set":3,"rows":1}

    POST /cypher-rs/create-user?delim=\t&batch=20000 content-type:text/plain "name\tage\tmale\nAndres\t21\ttrue"
    --> 200 {"nodes_created":1,"labels_added":1,"properties_set":3,"rows":1}

Delete

    DELETE /cypher-rs/users

#### Types of results:

single column, single row

    {"name":"Andres","age":21,"male":true,"children":["Cypher","L.","N."]}

single column, multiple rows

    [
     {"name":"Andres","age":21,"male":true,"children":["Cypher","L.","N."]},
     {"name":"Peter","age":32,"male":true,"children":["Neo4j","O.","K."]}
    ]

multiple columns, single row (column names are keys)

    {"user": "Andres", "friends": ["Peter","Michael"]}

multiple columns, multiple rows (column names are keys)

    [
      {"user": "Andres", "friends": ["Peter","Michael"]},
      {"user": "Michael", "friends": ["Peter","Andres"]},
      {"user": "Peter", "friends": ["Andres","Michael"]}
    ]

### Configuration

Build with `mvn clean install dependency:copy-dependencies`

Copy files `cp target/cypher-rs-2.0-SNAPSHOT.jar target/dependency/opencsv-2.3.jar path/to/server/plugins`

Add this line to `path/to/server/conf/neo4j-server.properties`

    org.neo4j.server.thirdparty_jaxrs_classes=org.neo4j.cypher_rs=/cypher-rs


### Notes

There is some magic happening with converting query parameters to cypher parameters, as query-parameters are all strings
things that look like a number are converted to numbers and collections (aka multiple query parameters) are converted into
lists.


!!! Ideas

* all endpoints should be able to generate CSV when supplied with the accept header
* replace Jackson with faster Gson?
* performance tests
