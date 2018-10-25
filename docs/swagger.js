var data = {
  "swagger": "2.0",
  "info": {
    "description": "The standalone Open Gateway API enables your IoT infrastructure to interconnect with other IoT infrastructures by using HTTP REST requests. Among its features there is retrieving and setting a property on remote objects, executing an action, or subscribing to an event channel and receiving asynchronously fired event whenever one is published. After installation and start, the OGWAPI serves the following REST API on your local host's port as configured in the configuration file (default is 8181).",
    "version": "0.6.3",
    "title": "Open Gateway API",
    "termsOfService": "http://swagger.io/terms/",
    "contact": {
      "email": "sales@bavenir.eu"
    },
    "license": {
      "name": "Apache 2.0",
      "url": "http://www.apache.org/licenses/LICENSE-2.0.html"
    }
  },
  "host": "http://localhost:8181",
  "basePath": "/api",
  "securityDefinitions": {
    "basicAuth": {
      "type": "basic"
    }
  },
  "security": [
    {
      "basicAuth": []
    }
  ],
  "tags": [
    {
      "name": "authentication",
      "description": "Endpoints that let you log in your objects.",
      "externalDocs": {
        "description": "There is a Javadoc and cookbook documentation here:",
        "url": "https://github.com/vicinityh2020/vicinity-gateway-api/tree/master/doc"
      }
    },
    {
      "name": "discovery",
      "description": "Endpoints for registering your objects and perform a discovery of new ones.",
      "externalDocs": {
        "description": "There is a Javadoc and cookbook documentation here:",
        "url": "https://github.com/vicinityh2020/vicinity-gateway-api/tree/master/doc"
      }
    },
    {
      "name": "properties",
      "description": "Setting and retrieving properties on remote objects.",
      "externalDocs": {
        "description": "There is a Javadoc and cookbook documentation here:",
        "url": "https://github.com/vicinityh2020/vicinity-gateway-api/tree/master/doc"
      }
    },
    {
      "name": "actions",
      "description": "Starting, stopping and retrieving a status of remotely executed action.",
      "externalDocs": {
        "description": "There is a Javadoc and cookbook documentation here:",
        "url": "https://github.com/vicinityh2020/vicinity-gateway-api/tree/master/doc"
      }
    },
    {
      "name": "events",
      "description": "Subscribing a remote event channel and receiving published events.",
      "externalDocs": {
        "description": "There is a Javadoc and cookbook documentation here:",
        "url": "https://github.com/vicinityh2020/vicinity-gateway-api/tree/master/doc"
      }
    }
  ],
  "schemes": [
    "https",
    "http"
  ],
  "paths": {
    "/objects/login": {
      "get": {
        "tags": [
          "authentication"
        ],
        "summary": "Logs your object into the network.",
        "description": "Provides login endpoint for your objects. This can get confusing in the beginning, because you have to send credentials in every request anyways, making the object logged in automatically. However objects that are not logged in will not be visible/reachable on the network and although an object is logged in automatically after it makes its first request to OGWAPI (provided it has correct credentials), it is wise to explicitly log them in before it happens.",
        "operationId": "getObjectsLogin",
        "consumes": null,
        "produces": [
          "application/json"
        ],
        "parameters": null,
        "responses": {
          "200": {
            "description": "Successful operation",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/definitions/ResultGetObjectsLogin"
                }
              }
            }
          },
          "401": {
            "description": "Unauthorized"
          }
        }
      }
    },
    "/objects/logout": {
      "get": {
        "tags": [
          "authentication"
        ],
        "summary": "Logs your object out of the network.",
        "description": "Provides logout endpoint for your objects. The connection for given object is terminated.",
        "operationId": "getObjectsLogout",
        "consumes": null,
        "produces": [
          "application/json"
        ],
        "parameters": null,
        "responses": {
          "200": {
            "description": "Successful operation",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/definitions/ResultGetObjectsLogout"
                }
              }
            }
          },
          "401": {
            "description": "Unauthorized"
          }
        }
      }
    },
    "/objects": {
      "get": {
        "tags": [
          "discovery"
        ],
        "summary": "Retrieve available objects.",
        "description": "Retrieves a list of all IoT objects that are visible to that particular Agent/Adapter based on the permissions set in Neighbourhood Manager Web interface. This includes both your own and foreign devices. In order to make it into the list, it is necessary for the object to be online.",
        "operationId": "getObjects",
        "consumes": null,
        "produces": [
          "application/json"
        ],
        "parameters": null,
        "responses": {
          "200": {
            "description": "Successful operation",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/definitions/ResultGetObjects"
                }
              }
            }
          },
          "401": {
            "description": "Unauthorized"
          }
        }
      }
    },
    "/agent/{agid}/objects": {
      "get": {
        "tags": [
          "discovery"
        ],
        "summary": "Retrieve a list of object TDs connected to this agent.",
        "description": "Retrieves a list of object TDs that are connected to this particular Agent (identified by his AGID – Agent ID). It is necessary to call this endpoint before an automatic registration is attempted, so you know which objects need to be registered.",
        "operationId": "getAgentsAgidObjects",
        "consumes": null,
        "produces": [
          "application/json"
        ],
        "parameters": null,
        "responses": {
          "200": {
            "description": "Successful operation",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/definitions/ResultGetAgentsAgidObjects"
                }
              }
            }
          },
          "401": {
            "description": "Unauthorized"
          }
        }
      },
      "post": {
        "tags": [
          "discovery"
        ],
        "summary": "Register a set of new objects.",
        "description": "Agent can use this endpoint to register a set of new objects.",
        "operationId": "postAgentsAgidObjects",
        "consumes": null,
        "produces": [
          "application/json"
        ],
        "parameters": null,
        "responses": {
          "200": {
            "description": "Successful operation",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/definitions/ResultPostAgentsAgidObjects"
                }
              }
            }
          },
          "401": {
            "description": "Unauthorized"
          }
        }
      },
      "put": {
        "tags": [
          "discovery"
        ],
        "summary": "Replace existing set of objects' TDs.",
        "description": "Agent can use this endpoint to update TDs of already registered objects. This call completely replaces previously registered TDs for given objects.",
        "operationId": "putAgentsAgidObjects",
        "consumes": null,
        "produces": [
          "application/json"
        ],
        "parameters": null,
        "responses": {
          "200": {
            "description": "Successful operation",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/definitions/ResultPutAgentsAgidObjects"
                }
              }
            }
          },
          "401": {
            "description": "Unauthorized"
          }
        }
      }
    },
    "/agent/{agid}/objects/update": {
      "put": {
        "tags": [
          "discovery"
        ],
        "summary": "Update existing set of objects' TDs.",
        "description": "Agent can use this endpoint to update TDs of already registered objects.",
        "operationId": "putAgentsAgidObjectsUpdate",
        "consumes": null,
        "produces": [
          "application/json"
        ],
        "parameters": null,
        "responses": {
          "200": {
            "description": "Successful operation",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/definitions/ResultPutAgentsAgidObjectsUpdate"
                }
              }
            }
          },
          "401": {
            "description": "Unauthorized"
          }
        }
      }
    },
    "/agent/{agid}/objects/delete": {
      "post": {
        "tags": [
          "discovery"
        ],
        "summary": "Delete set of objects.",
        "description": "An agent can delete a set of objects, that are registered through it.",
        "operationId": "postAgentsAgidObjectsDelete",
        "consumes": null,
        "produces": [
          "application/json"
        ],
        "parameters": null,
        "responses": {
          "200": {
            "description": "Successful operation",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/definitions/ResultPostAgentsAgidObjectsDelete"
                }
              }
            }
          },
          "401": {
            "description": "Unauthorized"
          }
        }
      }
    },
    "/objects/{oid}/properties/{pid}": {
      "get": {
        "tags": [
          "properties"
        ],
        "summary": "Get a property value from a remote object.",
        "description": "Retrieves a value of a given property from a remote object. You can add any parameters, just remember that on the other side the request to agent/adapter will automatically have 'sourceOid' parameter added. Therefore, any parameter with the same name will be overwritten.\nIMPORTANT: Reception of this request will cause the receiving OGWAPI to fire following request to an Agent / Adapter: GET http://[agent / adapter IP address]:[port]/agent/objects/[destination ID]/properties/[property ID] with one of the parameters being 'sourceId', containing a string with the source identifier. Therefore, an endpoint needs to be implemented on the Agent / Adapter capable of receving such requests and retrieving given property from the object.",
        "operationId": "getObjectsOidPropertiesPid",
        "consumes": null,
        "produces": [
          "application/json"
        ],
        "parameters": null,
        "responses": {
          "200": {
            "description": "Successful operation",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/definitions/ResultGetObjectsOidPropertiesPid"
                }
              }
            }
          },
          "401": {
            "description": "Unauthorized"
          }
        }
      },
      "put": {
        "tags": [
          "properties"
        ],
        "summary": "Set a property value on a remote object.",
        "description": "Sets a new value of a property on a remote object. You can add any parameters, just remember that on the other side the request to agent/adapter will automatically have 'sourceOid' parameter added. Therefore, any parameter with the same name will be overwritten.\nIMPORTANT: Reception of this request will cause the receiving OGWAPI to fire following request to an Agent / Adapter: PUT http://[agent / adapter IP address]:[port]/agent/objects/[destination ID]/properties/[property ID] with one of the parameters being 'sourceId', containing a string with the source identifier. Therefore, an endpoint needs to be implemented on the Agent / Adapter capable of receving such requests and retrieving given property from the object.",
        "operationId": "putObjectsOidPropertiesPid",
        "consumes": null,
        "produces": [
          "application/json"
        ],
        "parameters": null,
        "responses": {
          "200": {
            "description": "Successful operation",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/definitions/ResultPutObjectsOidPropertiesPid"
                }
              }
            }
          },
          "401": {
            "description": "Unauthorized"
          }
        }
      }
    },
    "/objects/{oid}/actions/{aid}": {
      "post": {
        "tags": [
          "actions"
        ],
        "summary": "Execute an action on a remote object.",
        "description": "Start execution of an action. The particular execution is called a task, has its own ID (that gets returned) and has multiple states, in which it can be – see the next endpoint. You can add any parameters, just remember that on the other side the request to agent/adapter will automatically have 'sourceOid' parameter added. Therefore, any parameter with the same name will be overwritten.\nIMPORTANT: The gateway on the other side will automatically queue requests and will pick one at a time, in a standard FIFO fashion, for execution. Therefore, the start of a pending task will cause the receiving OGWAPI to fire following request to an Agent / Adapter: POST http://[agent / adapter IP address]:[port]/agent/objects/[destination ID]/properties/[property ID] with one of the parameters being 'sourceId', containing a string with the source identifier. Therefore, an endpoint needs to be implemented on the Agent / Adapter capable of receving such requests and retrieving given property from the object.",
        "operationId": "postObjectsOidActionsAid",
        "consumes": null,
        "produces": [
          "application/json"
        ],
        "parameters": null,
        "responses": {
          "200": {
            "description": "Successful operation",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/definitions/ResultPostObjectsOidActionsAid"
                }
              }
            }
          },
          "401": {
            "description": "Unauthorized"
          }
        }
      },
      "put": {
        "tags": [
          "actions"
        ],
        "summary": "Update status of a task.",
        "description": "The Agent/Adapter in charge of executing the task should call this endpoint to let the local OGWAPI know, that the action is completed, or to provide intermediate results if desired. Since there is only one task at a time the Adapter is able to execute, there is no need to specify to OGWAPI which task is done – the queueing of the tasks is necessary only from the ‘outside world’ point of view. The new status is send in a query parameter 'status' - it is a string of one of these values: running, failed, finished. Putting the task into any state other than running will cause the task (and its timers) to stop and next task in queue will start being executed.",
        "operationId": "putObjectsOidActionsAid",
        "consumes": null,
        "produces": [
          "application/json"
        ],
        "parameters": null,
        "responses": {
          "200": {
            "description": "Successful operation",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/definitions/ResultPutObjectsOidActionsAid"
                }
              }
            }
          },
          "401": {
            "description": "Unauthorized"
          }
        }
      }
    },
    "/objects/{oid}/actions/{aid}/tasks/{tid}": {
      "get": {
        "tags": [
          "actions"
        ],
        "summary": "Retrieve the status or a return value of a given task.",
        "description": "When an action is executed, it can take a while, like opening a door or running an algorithm. This particular execution, or run, is called a task. This endpoint returns a status of a task, that has been executed before. A task can be in one of the following states: pending (first state, before it starts being executed), running, finished and failed.",
        "operationId": "getObjectsOidActionsAidTasksTid",
        "consumes": null,
        "produces": [
          "application/json"
        ],
        "parameters": null,
        "responses": {
          "200": {
            "description": "Successful operation",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/definitions/ResultGetObjectsOidActionsAidTasksTid"
                }
              }
            }
          },
          "401": {
            "description": "Unauthorized"
          }
        }
      },
      "delete": {
        "tags": [
          "actions"
        ],
        "summary": "Cancel a task in progress.",
        "description": "Cancels a task, that is in progress as a result of action being executed. Only a task that is either in running or pending state can be cancelled.\nIMPORTANT: Reception of this request will cause the receiving OGWAPI to fire following request to an Agent / Adapter: DELETE http://[agent / adapter IP address]:[port]/agent/objects/[destination ID]/actions/[action ID] with one of the parameters being 'sourceId', containing a string with the source identifier. Therefore, an endpoint needs to be implemented on the Agent / Adapter capable of receving such requests and cancelling given action on the object.",
        "operationId": "deleteObjectsOidActionsAidTasksTid",
        "consumes": null,
        "produces": [
          "application/json"
        ],
        "parameters": null,
        "responses": {
          "200": {
            "description": "Successful operation",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/definitions/ResultDeleteObjectsOidActionsAidTasksTid"
                }
              }
            }
          },
          "401": {
            "description": "Unauthorized"
          }
        }
      }
    },
    "/events/{eid}": {
      "post": {
        "tags": [
          "events"
        ],
        "summary": "Activates the event channel.",
        "description": "Used by an Agent/Adapter, that is capable of generating events and is willing to send these events to subscribed objects. A call to this endpoint activates the channel – from that moment, other objects in the network are able to subscribe for receiving those messages.",
        "operationId": "postEventsEid",
        "consumes": null,
        "produces": [
          "application/json"
        ],
        "parameters": null,
        "responses": {
          "200": {
            "description": "Successful operation",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/definitions/ResultPostEventsEid"
                }
              }
            }
          },
          "401": {
            "description": "Unauthorized"
          }
        }
      },
      "put": {
        "tags": [
          "events"
        ],
        "summary": "Send an event to subscribed objects.",
        "description": "Used by an Agent/Adapter that is capable of generating events, to send an event to all subscribed objects on the network. The event is put into the body (only JSON is accepted). Also some values can be put into request parameters.",
        "operationId": "putEventsEid",
        "consumes": null,
        "produces": [
          "application/json"
        ],
        "parameters": null,
        "responses": {
          "200": {
            "description": "Successful operation",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/definitions/ResultPutEventsEid"
                }
              }
            }
          },
          "401": {
            "description": "Unauthorized"
          }
        }
      },
      "delete": {
        "tags": [
          "events"
        ],
        "summary": "Deactivate the event channel.",
        "description": "Used by an Agent/Adapter that is capable of generating events to de-activate an event channel. This will prohibit any other new objects to subscribe to that channel.",
        "operationId": "deleteEventsEid",
        "consumes": null,
        "produces": [
          "application/json"
        ],
        "parameters": null,
        "responses": {
          "200": {
            "description": "Successful operation",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/definitions/ResultDeleteEventsEid"
                }
              }
            }
          },
          "401": {
            "description": "Unauthorized"
          }
        }
      }
    },
    "/objects/{oid}/events/{eid}": {
      "get": {
        "tags": [
          "events"
        ],
        "summary": "Retrieve a current status of a remote event channel.",
        "description": "Retrieves a status of a particular event channel – whether it is active or deactivated.",
        "operationId": "getObjectsOidEventsEid",
        "consumes": null,
        "produces": [
          "application/json"
        ],
        "parameters": null,
        "responses": {
          "200": {
            "description": "Successful operation",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/definitions/ResultGetObjectsOidEventsEid"
                }
              }
            }
          },
          "401": {
            "description": "Unauthorized"
          }
        }
      },
      "post": {
        "tags": [
          "events"
        ],
        "summary": "Subscribe to an event channel.",
        "description": "Subscribes the object to an event reception. From that moment, the object calling this endpoint will start receiving events, until the channel is deactivated or the subscription is cancelled.",
        "operationId": "postObjectsOidEventsEid",
        "consumes": null,
        "produces": [
          "application/json"
        ],
        "parameters": null,
        "responses": {
          "200": {
            "description": "Successful operation",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/definitions/ResultPostObjectsOidEventsEid"
                }
              }
            }
          },
          "401": {
            "description": "Unauthorized"
          }
        }
      },
      "delete": {
        "tags": [
          "events"
        ],
        "summary": "Unsubscribe from an event channel.",
        "description": "By calling this endpoint, an object can cancel its own subscription to event channel.",
        "operationId": "deleteObjectsOidEventsEid",
        "consumes": null,
        "produces": [
          "application/json"
        ],
        "parameters": null,
        "responses": {
          "200": {
            "description": "Successful operation",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/definitions/ResultDeleteObjectsOidEventsEid"
                }
              }
            }
          },
          "401": {
            "description": "Unauthorized"
          }
        }
      }
    }
  }
}