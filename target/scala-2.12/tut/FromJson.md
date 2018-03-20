
# Reading from JSON

## "Parse JSON from a String"

```scala
scala> import io.circe._
import io.circe._

scala> import io.circe.parser._
import io.circe.parser._

scala> val exampleJson = """
     |   {
     |     "foo": {
     |       "bar": {
     |         "baz": true
     |       }
     |     }
     |   }
     |   """
exampleJson: String =
"
  {
    "foo": {
      "bar": {
        "baz": true
      }
    }
  }
  "

scala> val parseResult = parse(exampleJson)
parseResult: Either[io.circe.ParsingFailure,io.circe.Json] =
Right({
  "foo" : {
    "bar" : {
      "baz" : true
    }
  }
})
```

## "I want to get a value at this path"

The easiest method is using optics:
```scala
scala> import io.circe.optics.JsonPath._
import io.circe.optics.JsonPath._

scala> val bazPath = root.foo.bar.baz.boolean
bazPath: monocle.Optional[io.circe.Json,Boolean] = monocle.POptional$$anon$1@3508e97e

scala> parseResult.map(json => bazPath.getOption(json))
res0: scala.util.Either[io.circe.ParsingFailure,Option[Boolean]] = Right(Some(true))
```

## "I want to convert this JSON object to this case class"

```scala
scala> val jsonWithPerson = """
     |   { "name": "Doctor Who", "age": 100000000 }
     | """
jsonWithPerson: String =
"
  { "name": "Doctor Who", "age": 100000000 }
"

scala> final case class Person(name: String, age: Int)
defined class Person
```

We have a few options to solve this.

The first thing we need to do is parse the JSON.

```scala
scala> val personParseResult = parse(jsonWithPerson)
personParseResult: Either[io.circe.ParsingFailure,io.circe.Json] =
Right({
  "name" : "Doctor Who",
  "age" : 100000000
})

scala> // For brevity's sake I'll get the JSON out of the Either.
     | // (Not recommended for production code)§
     | import cats.syntax.either._
import cats.syntax.either._

scala> val parsedPerson = personParseResult.getOrElse(Json.Null)
parsedPerson: io.circe.Json =
{
  "name" : "Doctor Who",
  "age" : 100000000
}
```

Luckily, our JSON fields match the case class fields.
We can write a decoder!

### forProduct helpers - for flat objects
These methodd lets us map the JSON field names to case class fields.
The types are resolved by the compiler.

```scala
scala> import io.circe.Decoder
import io.circe.Decoder

scala> val decodePerson = Decoder.forProduct2("name", "age")(Person.apply)
decodePerson: io.circe.Decoder[Person] = io.circe.ProductDecoders$$anon$2@2f12f894

scala> decodePerson.decodeJson(parsedPerson)
res3: io.circe.Decoder.Result[Person] = Right(Person(Doctor Who,100000000))
```

### Using a cursor
Cursors are the old/standard way of decoding in Circe and Argonaut, the library
that inspired it.

You basically have a way to navigate to a part of the JSON document.
More complex examples to come later.
```scala
scala> val decodePersonCursor = new Decoder[Person] {
     |   final def apply(c: HCursor): Decoder.Result[Person] = 
     |     for {
     |       name <- c.downField("name").as[String]
     |       age <- c.downField("age").as[Int]
     |     } yield {
     |       Person(name, age)
     |     }
     | }
decodePersonCursor: io.circe.Decoder[Person] = $anon$1@17a37d9a

scala> import io.circe.literal._
import io.circe.literal._

scala> val newPersonJson = json"""{ "name": "Young person", "age": 100 }"""
newPersonJson: io.circe.Json =
{
  "name" : "Young person",
  "age" : 100
}

scala> decodePersonCursor.decodeJson(newPersonJson)
res4: io.circe.Decoder.Result[Person] = Right(Person(Young person,100))
```

Note I created new JSON using the literal module. This is very useful for
creating small snippets of typechecked JSON.


### Using Shapeless magic
```scala
scala> import io.circe.generic.auto._
import io.circe.generic.auto._

scala> decode[Person]("""{ "name": "Very easygoing person", "age": 1 }""")
res5: Either[io.circe.Error,Person] = Right(Person(Very easygoing person,1))
```
