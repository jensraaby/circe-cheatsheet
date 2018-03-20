
# Reading from JSON

## "Parse JSON from a String"

```tut
import io.circe._
import io.circe.parser._

val exampleJson = """
  {
    "foo": {
      "bar": {
        "baz": true
      }
    }
  }
  """

val parseResult = parse(exampleJson)
```

## "I want to get a value at this path"

The easiest method is using optics:
```tut
import io.circe.optics.JsonPath._

val bazPath = root.foo.bar.baz.boolean

parseResult.map(json => bazPath.getOption(json))

```

## "I want to convert this JSON object to this case class"

```tut
val jsonWithPerson = """
  { "name": "Doctor Who", "age": 100000000 }
"""

final case class Person(name: String, age: Int)

```

We have a few options to solve this.

The first thing we need to do is parse the JSON.

```tut
val personParseResult = parse(jsonWithPerson)

// For brevity's sake I'll get the JSON out of the Either.
// (Not recommended for production code)§
import cats.syntax.either._
val parsedPerson = personParseResult.getOrElse(Json.Null)
```

Luckily, our JSON fields match the case class fields.
We can write a decoder!

### forProduct helpers - for flat objects
This method lets to map the JSON field names to case class fields.

```tut
import io.circe.Decoder

val decodePerson = Decoder.forProduct2("name", "age")(Person.apply)

decodePerson.decodeJson(parsedPerson)
```

### Using a cursor
Cursors are the old/standard way of decoding in Circe and Argonaut, the library
that inspired it.

You basically have a way to navigate to a part of the JSON document.
More complex examples to come later.
```tut

val decodePersonCursor = new Decoder[Person] {
  final def apply(c: HCursor): Decoder.Result[Person] = 
    for {
      name <- c.downField("name").as[String]
      age <- c.downField("age").as[Int]
    } yield {
      Person(name, age)
    }
}

import io.circe.literal._
val newPersonJson = json"""{ "name": "Young person", "age": 100 }"""
decodePersonCursor.decodeJson(newPersonJson)
```


## "I want to get an array of objects in the JSON to be an array of JSON
