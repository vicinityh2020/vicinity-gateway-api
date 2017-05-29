# Getting started with VICINITY Open Gateway API #

### VICINITY Open Gateway API requirements ###
VICINITY Open Gateway API is able to run on any operating system which supports JAVA JDK 8.

Connectivity requirements are: 

* VICINITY Open Gateway API is running on port 8181 by default (can be configured in configuration file)
* VICINITY Open Gateway API needs to have connectivity to VICINITY Communication Server on port 5222 (can be configured in configuration file)

Hardware requirements are:

* CPU: 1GHz;
* RAM: 1GB;
* HDD: 150 MB (JDK excluded);

### Installing VICINITY Open Gateway API from source code ###
VICINITY Open Gateway API source code should be checkout from repository:
```
#!shell
git clone git@cpsgit.informatik.uni-kl.de:VICINITY/vicinity-open-gateway-api.git

```
Afterwards source needs to be build:
```
#!shell

mvn clean install
```
### Installing VICINITY Open Gateway API from using JAR package ###
TBD
### Configure VICINITY Open Gateway API ###

VICINITY Open Gateway API is configured by `config\GatewayConfig.xml`. This configuration includes:

* configuration of the logging mechanism by setting `configuration\logging\file` to existing log file configuration;
* configuration of the communication server;
* configuration of the VICINITY Open Gateway API endpoints;

***
# Configure the VICINITY Agent #
------
## Register the VICINITY Agent in VICINITY ##
The VICINITY System integrator should log in [VICINITY](http://vicinity.bavenir.eu) to setup new VICINITY Agent instance. The VICINITY will generate the VICINITY Agent API.

The current VICINITY Agent configuration can be VISIBLE through `adapters` endpoint of VICINITY Gateway API:
```
#!shell

curl -X GET https://virtserver.swaggerhub.com/voravec/your-api/1.0.0/adapters 
-H "access-control-allow-origin: *" 
-H "accept: application/json" 
-H "content-type: application/json" 
-H "authorization: Basic <security token>"
```
which returns the set of adapters:
```
#!json
[
  {
    "adid": "1dae4326-44ae-4b98-bb75-15aa82516cc3"
  }
]
```
Followed by the single adapter configuration end-point `adapters/{aid}`:
```
#!shell
curl -X GET <VICINITY Gateway API base>/adapters/1dae4326-44ae-4b98-bb75-15aa82516cc3 
-H "access-control-allow-origin: *" 
-H "accept: application/json" 
-H "content-type: application/json" 
-H "authorization: Basic <security token>"
```
which returns VICINITY Adapter configuration:
```
#!json

{
  "type": "generic.adapter.vicinity.eu",
  "name": "My VICINITY Adapter",
  "id": "5603ff1b-e6cc-4897-8045-3724e8a3a56c",
  "adid": "1dae4326-44ae-4b98-bb75-15aa82516cc3",
  "eventUri": "adapter007.vicinity.exemple.org/eventHandler",
  "desicoveryService": "enabled"
}
```
***
## Update the VICINITY Agent configuration in VICINITY ##
The VICINITY Agent should update its configuration if needed, it is possible to update it through [VICINITY](http://vicinity.bavenir.eu) only.

***
# Register and Expose the IoT objects provided by the integrated ecosystem #
Before IoT objects (device and service) can be accessed and shared within VICINITY they needs to be registered through `adapter\{aid}\objects` where the IoT objects configuration needs to be provided.

We would like to expose the thermometer which has two interaction patterns one property `temp1` and one action `switch`. These patterns can be accessed:
* http://adapter.vicinity.example.com/objects/thermostate/properties/temp1
* http://adapter.vicinity.example.com/objects/thermostate/actions/switch.


```javascript
  {
    "type": "Thermostate",
    "base": "http://adapter.vicinity.example.com/objects/thermostate"
    "vicinityIdentity":{
        "secrete":"kjasdnlkj3n4lkjn3lk4n2lkndlidneiu"
    }
    "properties": [
      {
        "type": [
          "Property"
        ],
        "pid": "temp1",
        "monitors": "Temperature",
        "output": {
          "units": "Celsius",
          "datatype": "float"
        },
        "writable": false,
        "links": [
          {
            "href": "http://adapter.vicinity.example.com/objects/thermostate/properties/temp1",
            "mediaType": "application/json"
          }
        ]
      }
    ],
    "actions": [
      {
        "type": [
          "Action"
        ],
        "aid": "switch",
        "affects": "OnOffStatus",
        "links": [
          {
            "href": "http://adapter.vicinity.example.com/objects/thermostate/actions/switch",
            "mediaType": "application/json"
          }
        ],
        "input": {
          "units": "Adimensional",
          "datatype": "boolean"
        }
      }
    ],
    "location": {
      "latitude": 34.43234,
      "longitude": -3.869
    }
  }

```
The registration of the thermometer should look like this:

```
#!shell
curl -X POST https://virtserver.swaggerhub.com/voravec/your-api/1.0.0/adapters/1dae4326-44ae-4b98-bb75-15aa82516cc3/objects 
-H "access-control-allow-origin: *" 
-H "accept: application/json" 
-H "content-type: application/json" 
-H "authorization: Basic dGVzdDp0ZXN0" 

-d "[ { \"type\": \"Thermostate\", \"base\": \"http://gateway.vicinity.example.com/objects/0729a580-2240-11e6-9eb5-0002a5d5c51b\",
 \"oid\": \"0729a580-2240-11e6-9eb5-0002a5d5c51b\",
 \"owner\": \"d27ad211-cf1f-4cc9-9c22-238e7b46677d\", \"properties\":
 [ { \"type\": [ \"Property\" ], \"pid\": \"temp1\", \"monitors\": \"Temperature\",
\"output\": { \"units\": \"Celsius\", \"datatype\": \"float\" },
 \"writable\": false, \"links\": [ { \"href\": \"properties/temp1\",
 \"mediaType\": \"application/json\" } ] } ], \"actions\": [ { \"type\": [ \"Action\" ], \"aid\": \"switch\", \"affects\": \"OnOffStatus\", 
\"links\": [ { \"href\": \"actions/switch\", \"mediaType\": \"application/json\" } ],
 \"input\": { \"units\": \"Adimensional\", \"datatype\": \"boolean\" } } ],
 \"location\": { \"latitude\": 34.43234, \"longitude\": -3.869 } }]"
```
with the following response of IoT object ids:
```
#!json
[
  {
    "oid": "0729a580-2240-11e6-9eb5-0002a5d5c51b"
  }
]
```


***
# Discover and consume of IoT objects #


***
# Exposing IoT objects in VICINITY Gateway API #


***
# VICINITY Open Gateway API Specification #