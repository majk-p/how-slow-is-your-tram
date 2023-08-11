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