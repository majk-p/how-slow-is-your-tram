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

<!-- class: line-numbers -->

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
import sttp.model.MediaType
```

---

<!-- class: line-numbers -->

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

```scala
val apiUri = uri"https://mpk.wroc.pl/bus_position"
val trams = List("31", "33")
val buses = List("110")

def payload(buses: List[String], trams: List[String]) = 
  (trams.map(v => s"busList[tram][]=$v") ++ buses.map(v => s"busList[bus][]=$v")).mkString("&")

basicRequest
  .post(apiUri)
  .body(payload(buses, trams)) // Something like busList[bus][]=110&busList[tram][]=31&busList[tram][]=33
  .contentType(MediaType.ApplicationXWwwFormUrlencoded)
  .response(asJson[List[Record]]) // `Record` type needs to be defined`
  .send(backend)
  .map(_.body)                 // We are only interested in the result
  .rethrow                     // Fail `IO` on all errors, we are being simple here
```

---

# STTP

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

basicRequest
  .post(apiUri)
  .body(payload(buses, trams)) // Something like busList[bus][]=110&busList[tram][]=31&busList[tram][]=33
  .contentType(MediaType.ApplicationXWwwFormUrlencoded)
  .response(asJson[List[Record]])
  .send(backend)
  .map(_.body)                 // We are only interested in the result
  .rethrow                     // Fail `IO` on all errors, we are being simple here
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