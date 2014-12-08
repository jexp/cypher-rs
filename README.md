## Cypher-RS

*[Presentation about Cypher-RS](http://slideshare.net/neo4j/document-oriented-access-to-graphs)*

Neo4j server-extension that allows to configure fixed REST-Endpoints for Cypher queries.

You can `PUT` cypher queries to an endpoint with a certain url-suffix and then later execute those queries by running

* `GET` with query parameters, only readonly queries
* `POST` with JSON (map, list of maps) payload for parameters
* `POST` with CSV payload for parameters, with optional batch-size and delimiter

### CREATE ENDPOINT

    Verb: PUT
    URL: /cypher-rs/<yourEndpoint>
    Headers:
        Content-type: plain/text
    Body:
        <yourCypherRequest>
        
#### Examples

    PUT /cypher-rs/users
    Content-type: plain/text
    Body: match (n:User) where n.name={name} return n
    
    --> 201 Location: /cypher-rs/users

    PUT /cypher-rs/create-user
    Content-type: plain/text
    Body: create (n:Node {name:{name},age:{age},male:{male}})
    
    --> 201 Location: /cypher-rs/create-user

### QUERY ENDPOINT

    Verb: GET
    URL: /cypher-rs/<yourEndpoint>

#### Example

    GET /cypher-rs/users?name=Andres

    --> 200 {"name":"Andres","age":21,"male":true,"children":["Cypher","L.","N."]}
    
    GET /cypher-rs/users?name=NotExists

    --> 204

### POST JSON-DATA TO ENDPOINT

    Verb: POST
    URL: /cypher-rs/<yourEndpoint>
    Headers:
        Content-type: application/json
    Body:
        <jsonData>

#### Examples

    POST /cypher-rs/users 
    Content-type: application/json
    Body: {"name":"Andres"}
    
    --> 200 {"name":"Andres","age":21,"male":true,"children":["Cypher","L.","N."]}

    POST /cypher-rs/users 
    Content-type: application/json
    Body: {"name":"NotExists"}
    
    --> 204

    POST /cypher-rs/users
    Content-type: application/json
    Body: [{"name":"Andres"},{"name":"Peter"},{"name":"NotExists"]
    
    --> 200 [{"name":"Andres","age":21,"male":true,"children":["Cypher","L.","N."]},
             {"name":"Peter","age":32,"male":true,"children":["Neo4j","O.","K."]},
             null]


### POST CSV DATA TO ENDPOINT

    Verb: POST
    URL: /cypher-rs/<yourEndpoint>
    Headers:
        Content-type: text/plain
    Body:
        <csvData>

#### Examples

    POST /cypher-rs/create-user
    Content-type: text/plain
    Body: name,age,male\nAndres,21,true
    
    --> 200 {"nodes_created":1,"labels_added":1,"properties_set":3,"rows":1}

    POST /cypher-rs/create-user?delim=\t&batch=20000
    Content-type: text/plain
    Body: name\tage\tmale\nAndres\t21\ttrue
    
    --> 200 {"nodes_created":1,"labels_added":1,"properties_set":3,"rows":1}

### DELETE ENDPOINT

    Verb: DELETE
    URL: /cypher-rs/<yourEndpoint>

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

Copy files `cp target/cypher-rs-2.1-SNAPSHOT.jar target/dependency/opencsv-2.3.jar path/to/server/plugins`

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

### Concrete Example for the Movie-Graph in Neo4j-Server

#### A "movie and cast" endpoint

* parameter: `title`
* result: a document with the movie title and a collection of cast names

````bash
curl -i -XPUT -H content-type:text/plain \
 -d'MATCH (movie:Movie {title:{title}})
    OPTIONAL MATCH (movie)<-[:ACTED_IN]-(actor)
    RETURN {title:movie.title, cast: collect(actor.name)} as movie' \
 http://localhost:7474/cypher-rs/movie

// use it

curl http://localhost:7474/cypher-rs/movie?title=The%20Matrix
````

#### A "co-actors" endpoint

* parameter: `name`
* result: a document of the requested actor and actors who co-acted in the same movies, ordered by frequency

````bash
curl -i -XPUT -H content-type:text/plain \
 -d'MATCH (actor:Person {name:{name}})-[:ACTED_IN*2..2]-(co_actor)
    WITH actor.name as name, {name:co_actor.name, count: count(*)} as co_actors
    ORDER BY count(*) DESC
    RETURN {name:name, co_actors: collect(co_actors)} as result' \
 http://localhost:7474/cypher-rs/co-actor

// use it

curl -i http://localhost:7474/cypher-rs/co-actor?name=Keanu%20Reeves
````

#### A "create movie only" endpoint

* parameters: `title` and `released`
* result: document with the movie's properties
* uses `MERGE` as "get-or-create" operation

````bash
curl -i -XPUT -H content-type:text/plain \
-d'MERGE (movie:Movie {title:{title}}) ON CREATE SET movie.released={released} RETURN movie' \
 http://localhost:7474/cypher-rs/create-movie

// use it

curl -i -XPOST -H content-type:application/json -d'{"title":"Forrest Gump","released":1994}' http://localhost:7474/cypher-rs/create-movie
````

#### A "create movie with cast" endpoint

* parameters: `title`, `released` for the movies, `actors` for the actor names
* result: document with the movie's properties
* uses `MERGE` as "get-or-create" operation for both movie and actors

````bash
curl -i -XPUT -H content-type:text/plain \
-d'MERGE (movie:Movie {title:{title}}) ON CREATE SET movie.released={released}
   FOREACH (name in {actors} | MERGE (actor:Person {name:name}) MERGE (actor)-[:ACTED_IN]->(movie))
   RETURN movie' \
 http://localhost:7474/cypher-rs/create-movie2

// use it

curl -i -XPOST -H content-type:application/json -d'{"title":"Forrest Gump","released":1994, "actors":["Tom Hanks","Robin Wright","Gary Sinise"]}' http://localhost:7474/cypher-rs/create-movie2

curl http://localhost:7474/cypher-rs/movie?title=Forrest%20Gump
```
