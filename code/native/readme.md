# MPK data client

query json object:

```
https://www.wroclaw.pl/open-data/api/action/datastore_search?resource_id=17308285-3977-42f7-81b7-fdd168c210a2&limit=5000
```


data sample
```json
{
    "help": "https://www.wroclaw.pl/open-data/api/3/action/help_show?name=datastore_search",
    "success": true,
    "result": {
        "resource_id": "17308285-3977-42f7-81b7-fdd168c210a2_v2",
        "fields": [
            {
                "type": "int4",
                "id": "_id"
            },
            {
                "type": "int8",
                "id": "Nr_Boczny"
            },
            {
                "type": "text",
                "id": "Nr_Rej"
            },
            {
                "type": "text",
                "id": "Brygada"
            },
            {
                "type": "text",
                "id": "Nazwa_Linii"
            },
            {
                "type": "float8",
                "id": "Ostatnia_Pozycja_Szerokosc"
            },
            {
                "type": "float8",
                "id": "Ostatnia_Pozycja_Dlugosc"
            },
            {
                "type": "text",
                "id": "Data_Aktualizacji"
            }
        ],
        "records": [
            {
                "Ostatnia_Pozycja_Dlugosc": 17.0407390594482,
                "_id": 1,
                "Nazwa_Linii": "",
                "Brygada": "",
                "Nr_Rej": "None",
                "Data_Aktualizacji": "2023-08-04 20:34:47.807000",
                "Nr_Boczny": "0",
                "Ostatnia_Pozycja_Szerokosc": 51.1238784790039
            },
            {
                "Ostatnia_Pozycja_Dlugosc": 16.9951725006104,
                "_id": 2,
                "Nazwa_Linii": "111",
                "Brygada": "11101",
                "Nr_Rej": "None",
                "Data_Aktualizacji": "2023-08-04 20:35:11.627000",
                "Nr_Boczny": "1900",
                "Ostatnia_Pozycja_Szerokosc": 51.1725845336914
            },
            {
                "Ostatnia_Pozycja_Dlugosc": 17.0464000701904,
                "_id": 3,
                "Nazwa_Linii": "16",
                "Brygada": "00807",
                "Nr_Rej": "None",
                "Data_Aktualizacji": "2023-08-04 20:35:14.657000",
                "Nr_Boczny": "2206",
                "Ostatnia_Pozycja_Szerokosc": 51.1021461486816
            },
            {
                "Ostatnia_Pozycja_Dlugosc": 17.0239334106445,
                "_id": 4,
                "Nazwa_Linii": "3",
                "Brygada": "01026",
                "Nr_Rej": "None",
                "Data_Aktualizacji": "2023-08-04 20:35:02.700000",
                "Nr_Boczny": "2208",
                "Ostatnia_Pozycja_Szerokosc": 51.1138725280762
            },
            {
                "Ostatnia_Pozycja_Dlugosc": 17.0048007965088,
                "_id": 5,
                "Nazwa_Linii": "",
                "Brygada": "",
                "Nr_Rej": "None",
                "Data_Aktualizacji": "2023-08-04 20:34:17.533000",
                "Nr_Boczny": "2212",
                "Ostatnia_Pozycja_Szerokosc": 51.079460144043
            }
        ],
        "_links": {
            "start": "/api/action/datastore_search?limit=5&resource_id=17308285-3977-42f7-81b7-fdd168c210a2",
            "next": "/api/action/datastore_search?offset=5&limit=5&resource_id=17308285-3977-42f7-81b7-fdd168c210a2"
        },
        "limit": 5,
        "total": 680
    }
}
```

with the single record described as 

```json
{
    "Ostatnia_Pozycja_Dlugosc": 17.0407390594482,
    "_id": 1,
    "Nazwa_Linii": "",
    "Brygada": "",
    "Nr_Rej": "None",
    "Data_Aktualizacji": "2023-08-04 20:34:47.807000",
    "Nr_Boczny": "0",
    "Ostatnia_Pozycja_Szerokosc": 51.1238784790039
}
```



---

Request

```
curl 'https://mpk.wroc.pl/bus_position?busList' \
  -H 'authority: mpk.wroc.pl' \
  -H 'accept: application/json, text/javascript, */*; q=0.01' \
  --data-raw 'busList%5Bbus%5D%5B%5D=110&busList%5Btram%5D%5B%5D=16&busList%5Btram%5D%5B%5D=18&busList%5Btram%5D%5B%5D=31&busList%5Btram%5D%5B%5D=33' 
```

```json
[
    {
        "name": "31",
        "type": "tram",
        "y": 17.033848,
        "x": 51.099865,
        "k": 22472425
    },
    {
        "name": "31",
        "type": "tram",
        "y": 17.024809,
        "x": 51.11622,
        "k": 22471852
    },
    {
        "name": "18",
        "type": "tram",
        "y": 17.046537,
        "x": 51.077057,
        "k": 22472445
    },
    {
        "name": "18",
        "type": "tram",
        "y": 16.985214,
        "x": 51.129864,
        "k": 22471840
    },
    {
        "name": "18",
        "type": "tram",
        "y": 17.027561,
        "x": 51.117695,
        "k": 22471881
    },
    {
        "name": "18",
        "type": "tram",
        "y": 17.032692,
        "x": 51.10728,
        "k": 22471821
    },
    {
        "name": "16",
        "type": "tram",
        "y": 17.058214,
        "x": 51.110912,
        "k": 22471927
    },
    {
        "name": "16",
        "type": "tram",
        "y": 17.060015,
        "x": 51.11141,
        "k": 22471906
    }
]
```