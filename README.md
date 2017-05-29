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
====================
The semantic gateway API for the VICINITY Nodes

**Version:** 0.0.3d

### /adapters
---
##### ***GET***
**Description:** Returns the list of the adapters.

**Responses**

| Code | Description | Schema |
| ---- | ----------- | ------ |
| 200 | All VICINITY identifiers of all adapters. | [ [AdapterId](#adapterId) ] |
| 401 | Unauthorized access |

**Security**

| Security Schema | Scopes |
| --- | --- |
| basicAuth | |

### /adapters/{adid}
---
##### ***GET***
**Description:** Returns the description of the adapter.

**Parameters**

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| adid | path | VICINITY Identifier of the adapter "1dae4326-44ae-4b98-bb75-15aa82516cc3" | Yes | string (uuid) |

**Responses**

| Code | Description | Schema |
| ---- | ----------- | ------ |
| 200 | Adapter description | [AdapterInfo](#adapterInfo) |
| 401 | Unauthorized access |
| 404 | Adapter does not exist under given identifier. |

**Security**

| Security Schema | Scopes |
| --- | --- |
| basicAuth | |

### /adapters/{adid}/objects
---
##### ***GET***
**Description:** Returns the list of IoT objects registered under adapter.

**Parameters**

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| adid | path | VICINITY Identifier of the adapter  (e.g. 1dae4326-44ae-4b98-bb75-15aa82516cc3) | Yes | string (uuid) |

**Responses**

| Code | Description | Schema |
| ---- | ----------- | ------ |
| 200 | All VICINITY identifiers of objects registered under specified adapter. | [ [ObjectId](#objectId) ] |
| 401 | Unauthorized access |
| 404 | Given VICINITY identifier does not exis |

**Security**

| Security Schema | Scopes |
| --- | --- |
| basicAuth | |

##### ***POST***
**Description:** Register the IoT object(s) of the underlying ecosystem e.g. devices, VA service

**Parameters**

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| adid | path | VICINITY Identifier of the adapter e.g. "1dae4326-44ae-4b98-bb75-15aa82516cc3" | Yes | string (uuid) |
| objects | body | List of IoT object descriptions of the underlying ecosystem. e.g. | Yes | [ [ObjectInfo](#objectInfo) ] |

**Responses**

| Code | Description | Schema |
| ---- | ----------- | ------ |
| 200 | All VICINITY identifiers of objects registered under VICINITY Gateway by this call. | [ [ObjectId](#objectId) ] |
| 400 | Invalid object description |
| 401 | Unauthorized access |

**Security**

| Security Schema | Scopes |
| --- | --- |
| basicAuth | |

##### ***PUT***
**Description:** Update the Adapter description of the adapter.

**Parameters**

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| adid | path | VICINITY Identifier (e.g. 1dae4326-44ae-4b98-bb75-15aa82516cc3) of the adapter to be updated | Yes | string (uuid) |
| description | body | New description for an already registered adapter. | Yes | [AdapterInfo](#adapterInfo) |

**Responses**

| Code | Description |
| ---- | ----------- |
| 204 | Description updated |
| 400 | Invalid object description |
| 401 | Unauthorized access |
| 404 | Given VICINITY identifier does not exist |

**Security**

| Security Schema | Scopes |
| --- | --- |
| basicAuth | |

### /adapter/{adid}/subscriptions
---
##### ***POST***
**Description:** Subscribes to description changes of the adapter

**Parameters**

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| adid | path | VICINITY identifier of the adapter (e.g. 1dae4326-44ae-4b98-bb75-15aa82516cc3) | Yes | string (uuid) |
| hooks | body | List of hooks to be invoked after adapter description changes | Yes | [ [SubscriptionInfo](#subscriptionInfo) ] |

**Responses**

| Code | Description | Schema |
| ---- | ----------- | ------ |
| 200 | Subscription created | [ [SubscriptionId](#subscriptionId) ] |
| 401 | Unauthorized access |
| 404 | Given identifier does not exist |

**Security**

| Security Schema | Scopes |
| --- | --- |
| basicAuth | |

##### ***GET***
**Description:** Returns all active subscriptions to the adapter

**Parameters**

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| adid | path | VICINITY identifier of the adapter (e.g. 1dae4326-44ae-4b98-bb75-15aa82516cc3) | Yes | string (uuid) |

**Responses**

| Code | Description | Schema |
| ---- | ----------- | ------ |
| 200 | List of subscription resources | [ [SubscriptionId](#subscriptionId) ] |
| 401 | Unauthorized access |
| 404 | Given identifier does not exist |

**Security**

| Security Schema | Scopes |
| --- | --- |
| basicAuth | |

### /objects
---
##### ***GET***
**Description:** Returns all available (both exposed and discovered and all adapeters) IoT objects managed by VICINITY Open Gateway API.

**Parameters**

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| type | query | Filter by object type identifier (from common VICINITY format) | No | string (type) |
| limit | query | Maximum number of objects should be returned. | No | integer |
| own | query | True returns only exposed objects, false return all objects. | No | boolean |

**Responses**

| Code | Description | Schema |
| ---- | ----------- | ------ |
| 200 | All VICINITY Identifiers of IoT objects fullfill the type and maximum constraint and own parameter. | [ [ObjectId](#objectId) ] |
| 401 | Unauthorized access |

**Security**

| Security Schema | Scopes |
| --- | --- |
| basicAuth | |

### /objects/{oid}
---
##### ***GET***
**Description:** Returns the description of an available IoT object

**Parameters**

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| oid | path | VICINITY identifier of the object (e.g. 0729a580-2240-11e6-9eb5-0002a5d5c51b) | Yes | string (uuid) |

**Responses**

| Code | Description | Schema |
| ---- | ----------- | ------ |
| 200 | Object description | [ObjectInfo](#objectInfo) |
| 401 | Unauthorized |
| 404 | Given identifier does not exist |

**Security**

| Security Schema | Scopes |
| --- | --- |
| basicAuth | |

##### ***PUT***
**Description:** Updates the description of an already registered exposed IoT object

**Parameters**

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| oid | path | VICINITY identifier of the object (e.g. 0729a580-2240-11e6-9eb5-0002a5d5c51b) | Yes | string (uuid) |
| description | body | New description for an already registered object | Yes | [ObjectInfo](#objectInfo) |

**Responses**

| Code | Description |
| ---- | ----------- |
| 200 | Description updated |
| 400 | Invalid object description |
| 401 | Unauthorized access |
| 404 | Given identifier does not exist |

**Security**

| Security Schema | Scopes |
| --- | --- |
| basicAuth | |

##### ***DELETE***
**Description:** Unregisters an exposed IoT object

**Parameters**

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| oid | path | VICINITY identifier of the object (e.g. 0729a580-2240-11e6-9eb5-0002a5d5c51b) | Yes | string (uuid) |

**Responses**

| Code | Description |
| ---- | ----------- |
| 204 | Description deleted |
| 400 | Invalid IoT object identifier |
| 401 | Unauthorized access |
| 404 | Given identifier does not exist |

**Security**

| Security Schema | Scopes |
| --- | --- |
| basicAuth | |

### /objects/{oid}/subscriptions
---
##### ***POST***
**Description:** Subscribes to description changes of an exposed IoT object

**Parameters**

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| oid | path | VICINITY identifier of the object (e.g. 0729a580-2240-11e6-9eb5-0002a5d5c51b) | Yes | string (uuid) |
| hooks | body | List of hooks to be invoked after object description changes | Yes | [ [SubscriptionInfo](#subscriptionInfo) ] |

**Responses**

| Code | Description | Schema |
| ---- | ----------- | ------ |
| 200 | Subscription created | [ [SubscriptionId](#subscriptionId) ] |
| 401 | Unauthorized access |
| 404 | Given identifier does not exist |

**Security**

| Security Schema | Scopes |
| --- | --- |
| basicAuth | |

##### ***GET***
**Description:** Returns all active subscriptions to exposed IoT objects

**Parameters**

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| oid | path | VICINITY identifier of the object (e.g. 0729a580-2240-11e6-9eb5-0002a5d5c51b) | Yes | string (uuid) |

**Responses**

| Code | Description | Schema |
| ---- | ----------- | ------ |
| 200 | List of subscription resources | [ [SubscriptionId](#subscriptionId) ] |
| 401 | Unauthorized access |
| 404 | Given identifier does not exist |

**Security**

| Security Schema | Scopes |
| --- | --- |
| basicAuth | |

### /subscriptions
---
##### ***GET***
**Description:** Returns the list of the subscriptions.

**Responses**

| Code | Description | Schema |
| ---- | ----------- | ------ |
| 200 | All VICINITY identifiers of all subscriptions. | [ [SubscriptionId](#subscriptionId) ] |
| 401 | Unauthorized access |

**Security**

| Security Schema | Scopes |
| --- | --- |
| basicAuth | |

### /subscriptions/{sid}
---
##### ***GET***
**Description:** Return subscription

**Parameters**

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| sid | path | VICINITY identifier of the subscription (e.g. 2734acea-9b8f-43f6-b17e-34f9c73e8513) | Yes | string (uuid) |

**Responses**

| Code | Description | Schema |
| ---- | ----------- | ------ |
| 200 | Subscription configuration | [SubscriptionInfo](#subscriptionInfo) |
| 401 | Unauthorized access |
| 404 | Given identifier does not exist |

**Security**

| Security Schema | Scopes |
| --- | --- |
| basicAuth | |

##### ***DELETE***
**Description:** Delete subscription

**Parameters**

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| sid | path | VICINITY identifier of the subscription (e.g. 2734acea-9b8f-43f6-b17e-34f9c73e8513) | Yes | string (uuid) |

**Responses**

| Code | Description |
| ---- | ----------- |
| 204 | Subscription removed |
| 401 | Unauthorized access |
| 404 | Given identifier does not exist |

**Security**

| Security Schema | Scopes |
| --- | --- |
| basicAuth | |

### /logins
---
##### ***POST***
**Description:** Login services and devices;

**Parameters**

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| credentials | body | Service and device credentials | No | [Credentials](#credentials) |

**Responses**

| Code | Description |
| ---- | ----------- |
| 204 | Authentication successfull |
| 401 | Unauthorized access |

### /logout
---
##### ***POST***
**Description:** Logout services and devices;

**Parameters**

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| objects | body | The list of services and devices being logout | No | [ObjectId](#objectId) |

**Responses**

| Code | Description |
| ---- | ----------- |
| 204 | Logout successfull |
| 404 | Unauthorized access |

**Security**

| Security Schema | Scopes |
| --- | --- |
| basicAuth | |

### /feeds
---
##### ***GET***
**Description:** Returns all active discovery feeds

**Responses**

| Code | Description | Schema |
| ---- | ----------- | ------ |
| 200 | List of identifiers of active feeds | [ [FeedId](#feedId) ] |
| 401 | Unauthorized |

**Security**

| Security Schema | Scopes |
| --- | --- |
| basicAuth | |

##### ***POST***
**Description:** Creates a new discovery feed from a given search criteria

**Parameters**

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| criteria | body | Search criteria based on the common VICINITY format | No | [SimpleFeedReq](#simpleFeedReq) |

**Responses**

| Code | Description |
| ---- | ----------- |
| 200 | Feed created (the corresponding VTED is received from the VICINITY Cloud) |
| 400 | Invalid search criteria |
| 401 | Unauthorized access |

**Security**

| Security Schema | Scopes |
| --- | --- |
| basicAuth | |

### /feeds/{fid}
---
##### ***GET***
**Description:** Returns the given feed

**Parameters**

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| fid | path | VICINITY Identifier of the feed (e.g. 66348b54-1609-11e7-93ae-92361f002671) | Yes | string (uuid) |

**Responses**

| Code | Description | Schema |
| ---- | ----------- | ------ |
| 200 | Information about the feed | [FeedInfo](#feedInfo) |
| 401 | Unauthorized access |
| 404 | Given identifier does not exist |

**Security**

| Security Schema | Scopes |
| --- | --- |
| basicAuth | |

##### ***DELETE***
**Description:** Deletes the given feed (all exclusive discovered objects by this feed won't be available)

**Parameters**

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| fid | path | VICINITY Identifier of the feed (e.g. 66348b54-1609-11e7-93ae-92361f002671) | Yes | string (uuid) |

**Responses**

| Code | Description |
| ---- | ----------- |
| 204 | Given feed was deleted |
| 401 | Unauthorized access |
| 404 | Given identifier does not exist |

**Security**

| Security Schema | Scopes |
| --- | --- |
| basicAuth | |

### /objects/{oid}/properties/{pid}
---
##### ***GET***
**Description:** Gets the property value of an available IoT object

**Parameters**

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| oid | path | VICINITY Identifier of the IoT object (e.g. 0729a580-2240-11e6-9eb5-0002a5d5c51b) | Yes | string (uuid) |
| pid | path | Property identifier (as in object description) (e.g. temp1) | Yes | string |

**Responses**

| Code | Description | Schema |
| ---- | ----------- | ------ |
| 200 | Latest property value | [PropertyValue](#propertyValue) |
| 401 | Unauthorized access |
| 404 | Given identifier does not exist |

**Security**

| Security Schema | Scopes |
| --- | --- |
| basicAuth | |

##### ***PUT***
**Description:** Sets the property value of an available IoT object

**Parameters**

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| oid | path | VICINITY identifier of the IoT object (e.g. 0729a580-2240-11e6-9eb5-0002a5d5c51b) | Yes | string (uuid) |
| pid | path | Property identifier (as in object description) (e.g. temp1) | Yes | string |
| object | body |  | Yes | [SetPropertyValue](#setPropertyValue) |

**Responses**

| Code | Description |
| ---- | ----------- |
| 204 | The property was modified |
| 401 | Unauthorized access |
| 404 | Given identifier does not exist |

**Security**

| Security Schema | Scopes |
| --- | --- |
| basicAuth | |

### /objects/{oid}/actions/{aid}
---
##### ***GET***
**Description:** Gets specific action status of an available IoT object

**Parameters**

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| oid | path | VICINITY identifier of the IoT object (e.g. 0729a580-2240-11e6-9eb5-0002a5d5c51b) | Yes | string (uuid) |
| aid | path | Action identifier (as in object description) (e.g. switch) | Yes | string |

**Responses**

| Code | Description | Schema |
| ---- | ----------- | ------ |
| 200 | Latest action status | [ActionValue](#actionValue) |
| 401 | Unauthorized access |
| 404 | Given identifier does not exist |

**Security**

| Security Schema | Scopes |
| --- | --- |
| basicAuth | |

##### ***POST***
**Description:** Performs an action on an available IoT object

**Parameters**

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| oid | path | VICINITY Identifier of the IoT object (e.g. 0729a580-2240-11e6-9eb5-0002a5d5c51b) | Yes | string (uuid) |
| aid | path | Action identifier (as in object description) (e.g. switch) | Yes | string |
| object | body |  | Yes | [SetActionValue](#setActionValue) |

**Responses**

| Code | Description | Schema |
| ---- | ----------- | ------ |
| 200 | A task to perform the action was submitted | string (uuid) |
| 401 | Unauthorized access |
| 404 | Given identifier does not exist |

**Security**

| Security Schema | Scopes |
| --- | --- |
| basicAuth | |

### /objects/{oid}/actions/{aid}/tasks/{tid}
---
##### ***GET***
**Description:** Gets a specific task status to perform an action of an available IoT object

**Parameters**

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| oid | path | VICINITY Identifier of the object (e.g. 0729a580-2240-11e6-9eb5-0002a5d5c51b) | Yes | string (uuid) |
| aid | path | Action identifier (as in object description) (e.g. switch) | Yes | string |
| tid | path | Task identifier (e.g. ca43b079-0818-4c39-b896-699c2d31f2db) | Yes | string (uuid) |

**Responses**

| Code | Description | Schema |
| ---- | ----------- | ------ |
| 200 | Task status | [TaskInfo](#taskInfo) |
| 401 | Unauthorized |
| 404 | Given identifier does not exist |

**Security**

| Security Schema | Scopes |
| --- | --- |
| basicAuth | |

##### ***DELETE***
**Description:** Deletes the given task to perform an action

**Parameters**

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| oid | path | VICINITY Identifier of the object (e.g. 0729a580-2240-11e6-9eb5-0002a5d5c51b) | Yes | string (uuid) |
| aid | path | Action name (as in object description) (e.g. switch) | Yes | string |
| tid | path | Task identifier (e.g. ca43b079-0818-4c39-b896-699c2d31f2db) | Yes | integer |

**Responses**

| Code | Description |
| ---- | ----------- |
| 204 | Given task was deleted |
| 401 | Unauthorized access |
| 404 | Given identifier does not exist |

**Security**

| Security Schema | Scopes |
| --- | --- |
| basicAuth | |

### /sparql
---
##### ***GET***
**Description:** Queries the network as if it were the VICINITY triple store of all objects' data

**Parameters**

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| query | query | SPARQL query | No | string |

**Responses**

| Code | Description |
| ---- | ----------- |
| 200 | SPARQL results (https://www.w3.org/TR/2013/REC-sparql11-results-json-20130321/) |
| 400 | Invalid query |
| 401 | Unauthorized |

**Security**

| Security Schema | Scopes |
| --- | --- |
| basicAuth | |

##### ***POST***
**Description:** Queries the network as if it were the VICINITY triple store of all objects' data

**Parameters**

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| query | body | New description for an already registered object | Yes | string |

**Responses**

| Code | Description |
| ---- | ----------- |
| 200 | SPARQL results (https://www.w3.org/TR/2013/REC-sparql11-results-json-20130321/) |
| 400 | Invalid query |
| 401 | Unauthorized |

**Security**

| Security Schema | Scopes |
| --- | --- |
| basicAuth | |

### Models
---
**AdapterInfo**  

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| type | string |  | Yes |
| name | string |  | Yes |
| adid | string (uuid) |  | Yes |
| eventUri | string (uri) |  | No |
**AdapterId**  

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| adid | string (uuid) |  | Yes |
**SubscriptionInfo**  

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| uri | string (uri) |  | Yes |
**SubscriptionId**  

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| sid | string (uuid) |  | Yes |
**ObjectInfo**  

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| type | string |  | Yes |
| base | string (uri) |  | No |
| oid | string (uuid) |  | Yes |
| owner | string (uuid) |  | Yes |
| properties | [ [ObjectProperty](#objectProperty) ] |  | No |
| actions | [ [ObjectAction](#objectAction) ] |  | No |
| location | [Location](#location) |  | No |
**ObjectId**  

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| oid | string (uuid) |  | Yes |
**ObjectProperty**  

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| type | [ string ] |  | Yes |
| pid | string |  | Yes |
| monitors | string (uri) |  | Yes |
| output | [OutputSchema](#outputSchema) |  | Yes |
| writable | boolean |  | No |
| links | [ [LinkInfo](#linkInfo) ] |  | No |
**ObjectAction**  

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| type | [ string ] |  | Yes |
| aid | string |  | Yes |
| affects | string (uri) |  | Yes |
| links | [ [LinkInfo](#linkInfo) ] |  | No |
| input | [InputSchema](#inputSchema) |  | Yes |
**FeedInfo**  

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| id | string (uuid) |  | No |
| criteria | object | Set of key-value filters defined for the current discovery feed (based on MongoDB query language) | No |
| created | dateTime | Creation date | No |
| results | [ [ObjectInfo](#objectInfo) ] | List of discovered IoT objects | No |
| ttl | integer | Number of seconds that the corresponding feed must be active | No |
**FeedId**  

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| fid | string (uuid) |  | Yes |
**PropertyValue**  

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| value | object | Can be anything: string, number, array, object, etc. | Yes |
| timestamp | dateTime | Value timestamp | Yes |
**ActionValue**  

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| value | object | Can be anything: string, number, array, object, etc. | Yes |
| timestamp | dateTime | Value timestamp | Yes |
| status | string |  | No |
**SetPropertyValue**  

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| value | object | Can be anything: string, number, array, object, etc. | Yes |
**SetActionValue**  

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| value | object | Can be anything: string, number, array, object, etc. | Yes |
**OutputSchema**  

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| units | string |  | No |
| datatype | string |  | Yes |
**InputSchema**  

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| units | string |  | No |
| datatype | string |  | Yes |
**TaskInfo**  

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| status | string |  | Yes |
| input | [SetActionValue](#setActionValue) |  | Yes |
| output | [ActionValue](#actionValue) |  | No |
**LinkInfo**  

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| href | string | Absolute URI or relative path (base declared) of the endpoint where an interaction pattern is provided | No |
| mediaType | string | Represent the label used to identify the content type | No |
**SimpleFeedReq**  

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| ttl | integer | Number of seconds that the corresponding feed must be active | No |
| criteria | object | Search criteria (based on MongoDB query language) | No |
**Credentials**  

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| name | string |  | No |
| password | string |  | No |
**SecurityToken**  

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| token | string |  | No |
**Location**  

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| latitude | number | Latitude coordinate | No |
| longitude | number | Longitude coordinate | No |