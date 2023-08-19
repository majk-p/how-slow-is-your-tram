---
theme: mp-theme
size: 16:9
transition: slide 
# see https://github.com/marp-team/marp-cli/blob/main/docs/bespoke-transitions/README.md#built-in-transitions
marp: true
---

<!-- _class: intro -->
# How slow is your tram? :tram:

###### using STTP, CE3, FS2 and scala-cli

![bg right:30% ](img/tram.jpg)

--- 

# About me :wave:

- https://michal.pawlik.dev 📄
- [@majkp@hostux.social](https://hostux.social/@majkp) 🔌

---

<!-- _class: divider -->

# Derailment

---

<!-- _class: divider -->

![bg 80%](img/tram-derailment.jpg)

---

# Derailment

Happens now and then, along with other incidents makes the transportation slow

---

<!-- _class: divider yellow-background -->

# How bad can it be?

Let's find out

---

# Plan

1) Find data source of vehicle positions
2) Fetch and parse
3) Fetch some more
4) Calculate diffs
5) Produce statistics
6) Profit

---

# Find data

First shot: [Wrocław Open Data](https://www.wroclaw.pl/open-data/dataset/lokalizacjapojazdowkomunikacjimiejskiejnatrasie_data)

---

# Wroclaw Open Data

```bash
$ curl -s https://www.wroclaw.pl/open-data/datastore/dump/17308285-3977-42f7-81b7-fdd168c210a2 | head | column -t -s,
_id  Nr_Boczny  Nr_Rej  Brygada  Nazwa_Linii  Ostatnia_Pozycja_Szerokosc  Ostatnia_Pozycja_Dlugosc  Data_Aktualizacji
1    0          None                          51.1059417724609            17.0331401824951          2023-08-12 15:14:34.863000
2    1900       None    None     None         51.0670280456543            17.0984840393066          2023-08-11 12:07:08.877000
3    2206       None    01517                 51.1253318786621            17.0414428710938          2023-08-12 16:09:33.540000
4    2208       None    00309                 51.1245498657227            17.0415744781494          2023-08-12 16:08:45.613000
5    2212       None                          51.079460144043             17.0047359466553          2023-08-12 16:08:49.817000
6    2218       None    02320                 51.1252136230469            17.040599822998           2023-08-12 16:08:45.647000
7    2228       None    00807                 51.1247787475586            17.0387763977051          2023-08-12 16:09:30.653000
8    2237       None    00307                 51.1240081787109            17.0415725708008          2023-08-12 16:09:10.590000
9    2238       None    None     None         51.1238594055176            17.0406036376953          2023-04-30 02:21:09.067000
```
###### Data fetched on 12.08.2023

---

# Some of it is pretty old 👴

```bash
$ curl -s https://www.wroclaw.pl/open-data/datastore/dump/17308285-3977-42f7-81b7-fdd168c210a2 | tail -n +2 | sort -t, -k 8,8 | head | column -t -s,      
34   2316  None                  51.1242408752441  17.0405292510986  2022-06-23 21:32:15.773000
557  8403  None     N10    N     51.1076545715332  17.0392875671387  2022-07-05 15:48:04.690000
324  4639  None                  51.0772018432617  17.0711059570312  2022-08-22 01:11:25.087000
292  4606  None     11308  112   51.0749778747559  17.0063076019287  2022-08-22 13:28:43.300000
379  5483  None     None   None  51.095060667      16.961589167      2022-09-20 23:35:26.007000
459  7400  None     10001        51.1481552124023  17.0230255126953  2022-09-25 04:40:37.697000
329  4807  None     None   None  51.1492080688477  17.0239391326904  2022-09-26 09:21:55.910000
398  7004  DW3987J               54.5531539916992  17.7985401153564  2022-10-03 15:53:05.910000
331  4810  None                  51.0665893554688  17.0993785858154  2022-10-13 09:22:55.650000
332  4811  None                  51.0667343139648  17.0992298126221  2022-10-13 09:32:45.753000
```

* Inconsistent
* Poor refresh rate
* Confusing old records

---

# Wrong way 🚧

![bg right:60% width:800px](./img/tram-detrailment-2.jpg)

---

<!-- _class: divider -->

# We need a better source

---

# Interactive map

https://mpk.wroc.pl/strefa-pasazera/zaplanuj-podroz/mapa-pozycji-pojazdow

![height:400px](./img/interactive-map.png)

---

![bg height:90%](./img/api-docs.png)

---

# Investigate 🕵️

![height:400px](./img/inspector.png)


---

# Investigate 🕵️

```bash
curl -s 'https://mpk.wroc.pl/bus_position' \
  -H 'accept: application/json, text/javascript, */*; q=0.01' \
  -H 'content-type: application/x-www-form-urlencoded; charset=UTF-8' \
  --data-raw 'busList%5Bbus%5D%5B%5D=110&busList%5Btram%5D%5B%5D=31&busList%5Btram%5D%5B%5D=33' \
  --compressed | jq       

[
  {
    "name": "31",
    "type": "tram",
    "y": 17.051546,
    "x": 51.076923,
    "k": 22471783
  },
  {
    "name": "31",
    "type": "tram",
    "y": 17.049835,
    "x": 51.081802,
    "k": 22472415
  },
 /* ... */
]  
```

---

# Finally data!


```json
[
  {
    "name": "31",
    "type": "tram",
    "y": 17.051546,
    "x": 51.076923,
    "k": 22471783
  },
 /* ... */
]    
```

---

# What does it mean?

* `name` - line name like `31`, `33`, `110`
* `type` - one of `tram`, `bus`
* `y` - latitude
* `x` - longitude
* `k` - most tricky one, looks like a vehicle id

---

<!-- _class: divider -->

# Plan

1. Find data source of vehicle positions ✅
2. Fetch and parse
3. Fetch some more
4. Calculate diffs
5. Produce statistics
6. Profit

---


<!-- _class: divider yellow-background -->


# Coding time 💻

---

# Fetch and parse

Shape of our reques
* HTTP POST
* List of vehicles like `busList[bus][]=110&busList[tram][]=31&busList[tram][]=33`
* `content-type: application/x-www-form-urlencoded`
* Expect JSON output

---

<!-- _class: line-numbers -->

# STTP Client

Start with some imports

```scala
//> using dep "com.softwaremill.sttp.client3::fs2:3.8.16"
//> using dep "com.softwaremill.sttp.client3::circe:3.8.16"
//> using dep "com.softwaremill.sttp.client3::core:3.8.16"
//> using toolkit typelevel:latest

import cats.effect.*
import cats.implicits.given
import io.circe.Codec
import sttp.client3.*
import sttp.client3.circe.*
import sttp.client3.httpclient.fs2.HttpClientFs2Backend
import sttp.model.MediaType
```

---

<!-- _class: line-numbers -->

# STTP

```scala
val apiUri = uri"https://mpk.wroc.pl/bus_position"
val trams = List("31", "33")
val buses = List("110")

def payload(buses: List[String], trams: List[String]) = 
  (trams.map(v => s"busList[tram][]=$v") ++ buses.map(v => s"busList[bus][]=$v")).mkString("&")
```

---


# STTP

<!-- _class: line-numbers -->

```scala
val apiUri = uri"https://mpk.wroc.pl/bus_position"
val trams = List("31", "33")
val buses = List("110")

def payload(buses: List[String], trams: List[String]) = 
  (trams.map(v => s"busList[tram][]=$v") ++ buses.map(v => s"busList[bus][]=$v")).mkString("&")

def request(backend: SttpBackend[IO, Any]) = 
  basicRequest
    .post(apiUri)
    .body(payload(buses, trams)) // Something like busList[bus][]=110&busList[tram][]=31&busList[tram][]=33
    .contentType(MediaType.ApplicationXWwwFormUrlencoded)
    .response(asJson[List[Record]])
    .send(backend)
    .map(_.body)                 // We are only interested in the result
    .rethrow                     // Fail `IO` on all errors, we are being simple here
```

---

# STTP

<!-- _class: line-numbers -->

```scala
val apiUri = uri"https://mpk.wroc.pl/bus_position"
val trams = List("31", "33")
val buses = List("110")

def payload(buses: List[String], trams: List[String]) = 
  (trams.map(v => s"busList[tram][]=$v") ++ buses.map(v => s"busList[bus][]=$v")).mkString("&")

case class Record(
  name: String,
  x: Double,
  y: Double,
  k: Int
) derives Codec.AsObject      // This means the compiler will generate JSON Encoder and Decoder 

def request(backend: SttpBackend[IO, Any]): IO[List[Record]] = // Note the return type
  basicRequest
    .post(apiUri)
    .body(payload(buses, trams)) // Something like busList[bus][]=110&busList[tram][]=31&busList[tram][]=33
    .contentType(MediaType.ApplicationXWwwFormUrlencoded)
    .response(asJson[List[Record]])
    .send(backend)
    .map(_.body)                 // We are only interested in the result
    .rethrow                     // Fail `IO` on all errors, we are being simple here
```

---

# Let's run it

* Our `request` returns an `IO`, so we need a way to execute it
* It requires `SttpBackend` so we need to create one

---

# Let's run it

The easiest way to execute an `IO` is to create a `Main` class that handles it for us

```scala
object Main extends IOApp.Simple {
  def run: IO[Unit] = ??? // our logic goes here 
}
```

---

# Let's run it

<!-- _class: line-numbers -->

Let's create a backend, execute the request and print the result

```scala
object Main extends IOApp.Simple {
  def run = 
    HttpClientFs2Backend
      .resource[IO]()
      .use(backend => request(backend))
      .flatMap(IO.println)
}
```

---

# Let's run it

<!-- _class: line-numbers -->

```scala
object Main extends IOApp.Simple {
  def run = 
    HttpClientFs2Backend
      .resource[IO]()
      .use(backend => request(backend))
      .flatMap(IO.println)
}
```

Execution result

```bash
$ scala-cli sttp-client.scala

List(
  Record(31,51.141502,16.95872,22475890), Record(31,51.110912,17.02159,22475017), Record(31,51.07934,17.050734,22475050),
  Record(31,51.12252,17.011976,22475871), Record(31,51.097458,17.03275,22475942), Record(110,51.096992,17.037682,22312466),
  Record(33,51.112633,16.99349,22476133), Record(33,51.107376,17.035055,22476039), Record(33,51.11388,17.1032,22476064),
  Record(33,51.10771,17.040272,22476110)
)
```

---

# Nice, we've got the data!

We're back on track

![bg right:60% width:800px](./img/tram-skoda-on-rails.jpg)

---

<!-- _class: divider -->

# Plan

1. Find data source of vehicle positions ✅
2. Fetch and parse ✅
3. Fetch some more
4. Calculate diffs
5. Produce statistics
6. Profit

---

<!-- _class: divider yellow-background -->

# Refactoring time! 🚧

---

# Refactoring time! 🚧

The model is bound to what the API gives.

Let's decouple before we move on.

---

# Refactoring time! 🚧

First step: hide the API behind an interface

```scala
trait MpkWrocApiClient[F[_]] {
  def vehicles(): F[Seq[MpkWrocApiClient.Record]]
}
```

---

# Refactoring time! 🚧

<!-- _class: line-numbers -->

Our existing code

```scala
val apiUri = uri"https://mpk.wroc.pl/bus_position"
val trams = List("31", "33")
val buses = List("110")

def payload(buses: List[String], trams: List[String]) = 
  (trams.map(v => s"busList[tram][]=$v") ++ buses.map(v => s"busList[bus][]=$v")).mkString("&")

case class Record(
  name: String,
  x: Double,
  y: Double,
  k: Int
) derives Codec.AsObject      // This means the compiler will generate JSON Encoder and Decoder 

def request(backend: SttpBackend[IO, Any]): IO[List[Record]] = // Note the return type
  basicRequest
    .post(apiUri)
    .body(payload(buses, trams)) // Something like busList[bus][]=110&busList[tram][]=31&busList[tram][]=33
    .contentType(MediaType.ApplicationXWwwFormUrlencoded)
    .response(asJson[List[Record]])
    .send(backend)
    .map(_.body)                 // We are only interested in the result
    .rethrow                     // Fail `IO` on all errors, we are being simple here
```

---

# Refactoring time! 🚧

<!-- _class: line-numbers -->

Just wrap the `request` method

```scala
trait MpkWrocApiClient[F[_]] {
  def vehicles(): F[Seq[MpkWrocApiClient.Record]]
}

object MpkWrocApiClient {
  def apply[F[_]](using ev: MpkWrocApiClient[F]): MpkWrocApiClient[F] = ev

  private val apiUri = uri"https://mpk.wroc.pl/bus_position"

  def instance(backend: SttpBackend[IO, Any])(buses: List[String], trams: List[String]): MpkWrocApiClient[IO] = 
    new MpkWrocApiClient[IO] {
      def vehicles(): IO[Seq[MpkWrocApiClient.Record]] = 
        basicRequest
          .post(apiUri)
          .body(payload(buses, trams))
          .contentType(MediaType.ApplicationXWwwFormUrlencoded)
          .response(asJson[List[Record]])
          .send(backend)
          .map(_.body)
          .rethrow
    }

  case class Record(
    name: String,
    x: Double,
    y: Double,
    k: Int
  ) derives Codec.AsObject

  private def payload(buses: List[String], trams: List[String]) = 
    (trams.map(v => s"busList[tram][]=$v") ++ buses.map(v => s"busList[bus][]=$v")).mkString("&")

}
```

---

# Refactoring time! 🚧

First step done ✅

```scala
trait MpkWrocApiClient[F[_]] {
  def vehicles(): F[Seq[MpkWrocApiClient.Record]]
}
```

We no longer have to care how the data is fetched!

---

# Generic API

Let's have our own data model and hide the implementation details

```scala
trait Vehicles[F[_]] {
  def list(): F[Seq[Vehicle]]
}
```

* Notice the `Vehicle` type - it needs better fields than `x`, `y`, `k`

---

<!-- _class: line-numbers -->

# `Vehicle` model

```scala
case class Vehicle(
  lineName: Vehicle.LineName,
  measuredAt: Instant,
  position: Position,
  id: Vehicle.Id
)
```

###### It could be called `VehiclePosition` or `VehicleMeasurement` but let's stick with `Vehicle` for clarity

---

<!-- _class: line-numbers -->

# `Vehicle` model

```scala
case class Vehicle(
  lineName: Vehicle.LineName,
  measuredAt: Instant,
  position: Position,
  id: Vehicle.Id
)
```

```scala
case class Position(latitude: Double, longitude: Double)

object Vehicle {
  case class Id(value: String) extends AnyVal
  case class LineName(value: String) extends AnyVal
}
```

---

# `Vehicles` service

Now that we have a model, let's implement the service

```scala
trait Vehicles[F[_]] {
  def list(): F[Seq[Vehicle]]
}
```

---

<!-- _class: line-numbers -->

# `Vehicles` service

```scala
trait Vehicles[F[_]] {
  def list(): F[Seq[Vehicle]]
}

object Vehicles {

  def mpkApiAdapter(client: MpkWrocApiClient[IO]): Vehicles[IO] = 
    new Vehicles[IO] {                                            // Creates anonymous instance for `IO` effect
      def list(): IO[Seq[Vehicle]] = 
        for {
          now <- IO.realTime                                      // Measure the time of measurement
          records <- client.vehicles()                            // Use the client to call vehicles
        } yield {
          records
            .map{ record =>                                       // map every resulting row from MPK API
              Vehicle(                                            // and turn it into our `Vehicle` model
                lineName = Vehicle.LineName(record.name),
                measuredAt = Instant.ofEpochMilli(now.toMillis),
                position = Position(record.x, record.y),
                id = Vehicle.Id(record.k.toString)
              )
            }
        }
    }
}
```

---

# One last thing

In the next step we'll be looking into the distance covered between two measurements

---

# Distance 

```scala
case class Vehicle(
  lineName: Vehicle.LineName,
  measuredAt: Instant,
  position: Position,
  id: Vehicle.Id
){
  // Don't worry about the details, it's a shameless copy-paste from StackOverflow 😅
  def distance(other: Vehicle): Double = {
    val earthRadius = 6371000 // Earth's radius in meters
    val lat1 = toRadians(position.latitude)
    val lon1 = toRadians(position.longitude)
    val lat2 = toRadians(other.position.latitude)
    val lon2 = toRadians(other.position.longitude)

    val dlon = lon2 - lon1
    val dlat = lat2 - lat1

    val a = pow(sin(dlat / 2), 2) + cos(lat1) * cos(lat2) * pow(sin(dlon / 2), 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    val distance = earthRadius * c

    distance
  }
}
```

---

# Distance

The point is, we can now calculate distance covered by vehicle

```scala
val measurement1: Vehicle = ???
val measurement2: Vehicle = ???

measurement1.distance(measurement2) // like this
```

---

# Back to the plan!

---

<!-- _class: divider -->

# Plan

1. Find data source of vehicle positions ✅
2. Fetch and parse ✅
3. Fetch some more 🛠️
4. Calculate diffs 🛠️
5. Produce statistics
6. Profit

---

# Streams 

![bg right:60%](img/stream.jpeg)


---

# Recap from streaming

* Represent sequences of data elements
* Can be finite or infinite, synchronous or asynchronous
* Offer operations for transforming, filtering, and aggregating data
* Declarative and lazy approach to data processing


---

# FS2: Functional Streams for Scala

Short intro

---

# FS2: Functional Streams for Scala

* Create an infinite stream of natural numbers `1, 2, 3, 4, 5, ...`
* Keep only the odd ones `1, 3, 5, 7, ...`
* Group them by 3 elements `(1, 3, 5), (7, 9, 11), ...`
* Add each group `9, 15, 21, ...`
* Take first `10`


---

## How many lines of code would it take without streams?

Think of it for a second

---

# FS2: Functional Streams for Scala

Here's how you do this

---

# FS2: Functional Streams for Scala

<!-- _class: line-numbers -->


```scala
val stream: Stream[IO, Int] = 
  Stream
    .iterate(1)(_ + 1)  // Create an infinite stream of natural numbers
    .filter(_ % 2 != 0) // Filter for odd numbers: 1, 3, 5, 7, 9, ...
    .sliding(3)         // Slides over each 3 elements: (1, 3, 5), (3, 5, 7), (5, 7, 9), ...
    .map { chunk =>
      chunk(0) + chunk(1) + chunk(2) // Add them together: 9, 15, 21, ...
    }
    .take(10)           // Fetch the first ten results
```

---

# To the real use case!

---

# To the real use case!

We want to build a stream that:

* Is infinite
* Acts on given time interval like every `N` seconds
* When the time comes - it lists vehicles using `Vehicles[IO].list()`
* Joins previous and current result by vehicle `id`
  - Because vehicles are not sorted and some might go missing
* Calculate the distance using `Vehicle.distance` and the elapsed time
* Build a map of `(Vehicle.LineName, Vehicle.Id) -> VehicleStats`
  - Where `VehicleStats` are distance, time and avg speed


---

# Step by step

Infinite stream that lists vehicles every `N` seconds

---

<!-- _class: line-numbers -->

# Step by step

Infinite stream that lists vehicles every `N` seconds

```scala
val interval = 7.seconds // 7 is a fine value for `N`

def stats(vehicles: Vehicles[IO]): IO[Map[(LineName, Id), VehicleStats]] = 
  fs2.Stream
    .fixedRateStartImmediately[IO](interval)
    .evalMap(_ => vehicles.list())
    // TBC
```

Easy right?

---

# Step by step

The next step - slide over data, take current and previous measurement and calculate the diff

---

# Step by step

The next step - slide over data, take current and previous measurement and calculate the diff

<!-- _class: line-numbers -->

```scala
def stats(vehicles: Vehicles[IO]): IO[Map[(LineName, Id), VehicleStats]] = 
  fs2.Stream
    .fixedRateStartImmediately[IO](interval)
    .evalMap(_ => vehicles.list())
    .sliding(2)
    .map(chunk => calculateDiff(chunk(0), chunk(1))) // That needs explaining
    // TBC
```

---

# Step by step

<!-- _class: line-numbers -->

Important part
```scala
def calculateDiff(snapshot1: Seq[Vehicle], snapshot2: Seq[Vehicle]): Seq[VehiclePositionDiff] =
  snapshot1
    .join(snapshot2)
    .map(
      (v1, v2) => 
        VehiclePositionDiff(v1.lineName, v1.id, secondDuration(v1.measuredAt, v2.measuredAt), v1.distance(v2))
    )

case class VehiclePositionDiff(line: Vehicle.LineName, id: Vehicle.Id, secondsDuration: Double, metersDistance: Double)
```

---

# Step by step

<!-- _class: line-numbers -->

Important part
```scala
def calculateDiff(snapshot1: Seq[Vehicle], snapshot2: Seq[Vehicle]): Seq[VehiclePositionDiff] =
  snapshot1
    .join(snapshot2)
    .map(
      (v1, v2) => 
        VehiclePositionDiff(v1.lineName, v1.id, secondDuration(v1.measuredAt, v2.measuredAt), v1.distance(v2))
    )

case class VehiclePositionDiff(line: Vehicle.LineName, id: Vehicle.Id, secondsDuration: Double, metersDistance: Double)
```

Boring stuff
```scala
extension (snapshot: Seq[Vehicle]) {
  def join(snapshot2: Seq[Vehicle]): Seq[(Vehicle, Vehicle)] = 
    snapshot.flatMap{ v1 => snapshot2.collect { case v2 if v2.id == v1.id => (v1, v2)} }
}

def secondDuration(start: Instant, end: Instant) = 
  (end.toEpochMilli() - start.toEpochMilli()).toDouble / 1000
```

---

# Step by step

Where are we again?

<!-- _class: line-numbers -->

```scala
def stats(vehicles: Vehicles[IO]): IO[Map[(LineName, Id), VehicleStats]] = 
  fs2.Stream
    .fixedRateStartImmediately[IO](interval)
    .evalMap(_ => vehicles.list())
    .sliding(2)
    .map(chunk => calculateDiff(chunk(0), chunk(1))) // That needs explaining
    // TBC
```

Our stream lists vehicles every `interval` and calculates a list of diffs

---

# Step by step

- Is infinite ✅
- Acts on given time interval like every `N` seconds ✅
- When the time comes - it lists vehicles using `Vehicles[IO].list()` ✅
- Joins previous and current result by vehicle `id` ✅
  - Because vehicles are not sorted and some might go missing
- Calculate the distance using `Vehicle.distance` and the elapsed time ✅
- Build a map of `(Vehicle.LineName, Vehicle.Id) -> VehicleStats`
  - Where `VehicleStats` are distance, time and avg speed

---

# Step by step

Let's calculate the stats

---

<!-- _class: line-numbers -->

# Easy!

```scala
def stats(vehicles: Vehicles[IO]): IO[Map[(LineName, Id), VehicleStats]] = 
  fs2.Stream
    .fixedRateStartImmediately[IO](interval)
    .evalMap(_ => vehicles.list())
    .sliding(2)
    .map(chunk => calculateDiff(chunk(0), chunk(1)))
    .take(numberOfSamples)
    .fold(Map[(Vehicle.LineName, Vehicle.Id), VehicleStats]()){ case (previousSummary, listOfDiffs) =>
      val currentDiffSummary = 
        listOfDiffs
          .groupBy(d => (d.line, d.id))
          .view
          .mapValues(_.head)
          .mapValues(diff => VehicleStats(diff.metersDistance, diff.secondsDuration) )
          .toMap
      Monoid.combine(previousSummary, currentDiffSummary)
    }
    .compile
    .toList
    .map(_.head)
```
---

<!-- _class: line-numbers -->

# Explanation

```scala
def stats(vehicles: Vehicles[IO]): IO[Map[(LineName, Id), VehicleStats]] = 
  fs2.Stream
    .fixedRateStartImmediately[IO](interval)
    .evalMap(_ => vehicles.list())
    .sliding(2)
    .map(chunk => calculateDiff(chunk(0), chunk(1)))
    .take(numberOfSamples)
    .fold(Map[(Vehicle.LineName, Vehicle.Id), VehicleStats]()){ case (previousSummary, listOfDiffs) =>
      val currentDiffSummary = 
        listOfDiffs                       // just a `Seq[VehiclePositionDiff]` so (line, id, distance, duration)
          .groupBy(d => (d.line, d.id))   // group by line and id
          .view
          .mapValues(_.head)              // take head of (line, id) -> List[diff] since it's always single element
          .mapValues(diff => VehicleStats(diff.metersDistance, diff.secondsDuration) ) // make the diff
          .toMap                          // Render a proper Map[(line, id) -> VehicleStats]
      Monoid.combine(previousSummary, currentDiffSummary) // Magic 🌠
    }
    .compile
    .toList
    .map(_.head)
```

--- 

# Let's run it!

<!-- _class: line-numbers -->

```scala
object Main extends IOApp.Simple {
  def run = 
    HttpClientFs2Backend
      .resource[IO]()
      .use(backend => program(backend) *> IO.println("Program finished"))

  private val trams = List("8", "16", "18", "20", "31", "33")
  private val buses = List("110", "124", "145", "149")

  def program(backend: SttpBackend[IO, Any]) = for {
    _ <- IO.println("Initializing client")
    client = MpkWrocApiClient.instance(backend)(buses, trams)
    vehicles = Vehicles.mpkApiAdapter(client)
    stats <- StatsCalculator.stats(vehicles)
    _ <- IO.println("Results:")
    _ <- IO.println(stats.mkString("\n"))
  } yield ()

}
```

---

# Results

Captured at 19.08.2023 23:59

```scala
$ scala-cli main.scala
Compiling project (Scala 3.3.0, JVM)
Compiled project (Scala 3.3.0, JVM)
Initializing client
Results:
(LineName(8),Id(22516252)) -> VehicleStats(distance = 314 m, duration = 56 s, avgSpeed = 20.213189082553846 km/h)
(LineName(20),Id(22515917)) -> VehicleStats(distance = 443 m, duration = 56 s, avgSpeed = 28.5119529250625 km/h)
(LineName(20),Id(22515179)) -> VehicleStats(distance = 40 m, duration = 56 s, avgSpeed = 2.5404996160117994 km/h)
(LineName(18),Id(22514827)) -> VehicleStats(distance = 147 m, duration = 56 s, avgSpeed = 9.422170260015774 km/h)
(LineName(31),Id(22515449)) -> VehicleStats(distance = 236 m, duration = 56 s, avgSpeed = 15.154027356445154 km/h)
(LineName(20),Id(22515337)) -> VehicleStats(distance = 277 m, duration = 56 s, avgSpeed = 17.81978845252574 km/h)
(LineName(33),Id(22515611)) -> VehicleStats(distance = 19 m, duration = 56 s, avgSpeed = 1.2391629631487628 km/h)
(LineName(8),Id(22516472)) -> VehicleStats(distance = 85 m, duration = 56 s, avgSpeed = 5.445973045490719 km/h)
(LineName(33),Id(22515654)) -> VehicleStats(distance = 46 m, duration = 56 s, avgSpeed = 2.9398921547170436 km/h)
(LineName(31),Id(22515530)) -> VehicleStats(distance = 390 m, duration = 56 s, avgSpeed = 25.099798011115187 km/h)
(LineName(31),Id(22514759)) -> VehicleStats(distance = 142 m, duration = 56 s, avgSpeed = 9.139924459411425 km/h)
(LineName(145),Id(22325620)) -> VehicleStats(distance = 438 m, duration = 56 s, avgSpeed = 28.168021843769804 km/h)
(LineName(8),Id(22516229)) -> VehicleStats(distance = 76 m, duration = 56 s, avgSpeed = 4.893826680968721 km/h)
(LineName(31),Id(22515483)) -> VehicleStats(distance = 471 m, duration = 56 s, avgSpeed = 30.27673252637013 km/h)
(LineName(31),Id(22514740)) -> VehicleStats(distance = 295 m, duration = 56 s, avgSpeed = 18.977040274024887 km/h)
Program finished
```

---

# Therefore...

---

<!-- _class: divider yellow-background -->

# How slow is your tram? :tram:

---

<!-- _class: divider yellow-background -->

### How slow is your tram? :tram:

# Too slow!

---

# But we have learned something!

* Data is not easy to find
* Sttp and Circe play well together for HTTP APIs
* Streams are not that hard
* Smartly applied `Monoid` can save you some code

---

# Thank you!

<style scoped>
/* Styling for centering (required in default theme) */
h1, h2, h3, h4, h5, p, ul, li {
  text-align: center;
}
</style>

Keep in touch! 🤝

Blog: [blog.michal.pawlik.dev](https://blog.michal.pawlik.dev)
Linkedin: [Michał Pawlik](https://www.linkedin.com/in/michał-pawlik/)
Github: [majk-p](https://github.com/majk-p)
Mastodon: [@majkp@hostux.social](https://hostux.social/@majkp)

---

# Bonus!

---

# Bonus: Monoid magic

Thanks to how monoids compose, you can combine two maps for free
```scala
//> using toolkit typelevel:latest

import cats.Monoid

val a = Map[String, Int]("foo" -> 1, "bar" -> 5)
val b = Map[String, Int]("bar" -> 3, "baz" -> 0)

println(
    Monoid.combine(a, b)
)
```

---

# Bonus: Monoid magic

Thanks to how monoids compose, you can combine two maps for free
```scala
//> using toolkit typelevel:latest

import cats.Monoid

val a = Map[String, Int]("foo" -> 1, "bar" -> 5)
val b = Map[String, Int]("bar" -> 3, "baz" -> 0)

println(
    Monoid.combine(a, b)
)
```

The result is
```sh
$ scala-cli monoid.sc 
Compiling project (Scala 3.3.0, JVM)
Compiled project (Scala 3.3.0, JVM)
Map(bar -> 8, baz -> 0, foo -> 1)
```

---

# Bonus: Monoid magic

So when we provide a `Monoid` for `VehicleStats` like this

```scala
given Monoid[VehicleStats] with {

  override def combine(x: VehicleStats, y: VehicleStats): VehicleStats = 
    VehicleStats(
      x.metersDistance + y.metersDistance,
      x.secondsDuration + y.secondsDuration
    )

  override def empty: VehicleStats = VehicleStats(0,0)
}
```

This works for free
```scala
Monoid.combine(previousSummary, currentDiffSummary) // combine previous stats with current one together
```


<!-- 

Agenda

* Derailment
* Is it really that bad? 
* Data sources - open-data wroclaw
* Failure with open data https://www.wroclaw.pl/open-data/dataset/lokalizacjapojazdowkomunikacjimiejskiejnatrasie_data
* Interactive map https://mpk.wroc.pl/strefa-pasazera/zaplanuj-podroz/mapa-pozycji-pojazdow NO API!?
* STTP  - let's download the data
* Decode JSON
* Internal model
* Make sense of data
* Monoid for the win - combine data for free
* FS2 to gather some more data
* Best and worst line
* Future improvements - S3 to store data? More statistics
* Summary 

-->