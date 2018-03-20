enablePlugins(TutPlugin)

val circeVersion = "0.9.2"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser",
  "io.circe" %% "circe-literal",
  "io.circe" %% "circe-optics"
).map(_ % circeVersion)


