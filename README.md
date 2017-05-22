# Getting started with VICINITY Open Gateway API #

### VICINITY Open Gateway API requirements ###

### Installing VICINITY Open Gateway API from source code ###

### Installing VICINITY Open Gateway API from using JAR package ###

***
# Configure the VICINITY Adapter #
------
## Register the VICINITY Adapter in VICINITY ##
The VICINITY System integrator should log in [VICINITY](http://vicinity.bavenir.eu) to setup new VICINITY Adapter instance. The VICINITY will generate the VICINITY Adapter API.

The current VICINITY Adapter configuration can be VISIBLE through `adapters` endpoint of VICINITY Gateway API:
```
#!shell

curl -X GET https://virtserver.swaggerhub.com/voravec/your-api/1.0.0/adapters -H "access-control-allow-origin: *" -H "accept: application/json" -H "content-type: application/json" -H "authorization: Basic <security token>"
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
curl -X GET <VICINITY Gateway API base>/adapters/1dae4326-44ae-4b98-bb75-15aa82516cc3 -H "access-control-allow-origin: *" -H "accept: application/json" -H "content-type: application/json" -H "authorization: Basic <security token>"

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
### Register and Expose the IoT objects provided by the integrated ecosystem ###


***
### Discover and consume of IoT objects ###


***
### Exposing IoT objects in VICINITY Gateway API ###


***
### VICINITY Open Gateway API Specification ###