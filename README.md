## Cypher-RS

Neo4j server-extension that allows to configure fixed REST-Endpoints for Cypher queries.

You can `PUT` cypher queries to an endpoint with a certain url-suffix and then later execute those queries by running

* `GET` with query parameters, only readonly queries
* `POST` with JSON (map, list of maps) payload for parameters
* `POST` with CSV payload for parameters, with optional batch-size and delimiter

### Create

    Verb: PUT
    URL: /cypher-rs/<yourEndpoint>
    Headers:
        Content-type: plain/text
    Body:
        <youtCypherRequest>
        
#### Examples

    PUT /cypher-rs/users
    Content-type: plain/text
    Body: match (n:User) where n.name={name} return n
    
    --> 201 Location: /cypher-rs/users

    PUT /cypher-rs/create-user
    Content-type: plain/text
    Body: create (n:Node {name:{name},age:{age},male:{male}})
    
    --> 201 Location: /cypher-rs/create-user

### Query

    Verb: GET
    URL: /cypher-rs/<yourEndpoint>
    Headers:
        Content-type: application/x-www-form-urlencoded

#### Example

    GET /cypher-rs/users?name=Andres
    Content-type: application/x-www-form-urlencoded
    
    --> 200 {"name":"Andres","age":21,"male":true,"children":["Cypher","L.","N."]}


### POST JSON-Data

    Verb: POST
    URL: /cypher-rs/<yourEndpoint>
    Headers:
        Content-type: application/json
    Body:
        <jsonData>

#### Examples

    POST /cypher-rs/users 
    Content-type:application/json
    Body: {"name":"Andres"}
    
    --> 200 {"name":"Andres","age":21,"male":true,"children":["Cypher","L.","N."]}

    POST /cypher-rs/users content-type:application/json [{"name":"Andres"},{"name":"Peter"}]
    
    --> 200 [{"name":"Andres","age":21,"male":true,"children":["Cypher","L.","N."]},
             {"name":"Peter","age":32,"male":true,"children":["Neo4j","O.","K."]}]

### POST CSV Data

    Verb: POST
    URL: /cypher-rs/<yourEndpoint>
    Headers:
        Content-type: text/plain
    Body:
        <csvData>

#### Examples

    POST /cypher-rs/create-user
    Content-type:text/plain
    Body: name,age,male\nAndres,21,true
    
    --> 200 {"nodes_created":1,"labels_added":1,"properties_set":3,"rows":1}

    POST /cypher-rs/create-user?delim=\t&batch=20000
    Content-type:text/plain
    Body: name\tage\tmale\nAndres\t21\ttrue
    
    --> 200 {"nodes_created":1,"labels_added":1,"properties_set":3,"rows":1}

### Delete

    Request verb: DELETE
    Request URL: /cypher-rs/<yourEndpoint>

#### Example

    DELETE /cypher-rs/users

    --> 200 

### Types of results:

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


### Ideas

* all endpoints should be able to generate CSV when supplied with the accept header
* replace Jackson with faster Gson?
* performance tests
