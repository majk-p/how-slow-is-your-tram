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