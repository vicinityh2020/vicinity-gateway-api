var data = {
  "swagger": "2.0",
  "info": {
    "description": "<p>The standalone VICINITY Open Gateway API enables your IoT infrastructure to Among its features there is retrieving and setting a property on remote interconnect with other IoT infrastructures and Services through VICINITY P2P network by using HTTP REST requests.</p><br> <p>This API is used as specification <ul><li>to expose your IoT infrastructure in VICINITY P2P network and</li><li>to consume (access) IoT infrastructure through VICINITY P2P network.</li></ul></p> <p>The only difference is that if your software component is calling these API endpoints (e.g. service is reading property from remote device) or your IoT infrastructure is providing these endpoints to Open Gateway API (e.g. IoT infrastructure is responding to service request on device property).</p> <p>VICINITY Open Gateway API is divided in the following groups: <ul>\n  <li>authentication: used to login device, service and adapter/agent;</li>\n  <li>discovery: to manage registry of devices and services in VICINITY P2P network;</li>\n  <li>properties: to expose or to consume (access) properties of the particular devices/services;</li>\n  <li>actions: to expose or to consume (access) action of the particular device/service;</li>\n  <li>events: to expose or to consume (access) events of the particular device/service.</li>\n</ul>\n<p>Note, for installation guide please refere the <a href=\"\">VICINITY Getting started guide</a></p>",
    "version": "0.6.4",
    "title": "VICINITY Open Gateway API",
    "termsOfService": "http://swagger.io/terms/",
    "contact": {
      "email": "sales@bavenir.eu"
    },
    "license": {
      "name": "LGPL v3.0",
      "url": "https://www.gnu.org/copyleft/lesser.html"
    }
  },
  "host": "localhost:8181",
  "basePath": "/api",
  "securityDefinitions": {
    "basicAuth": {
      "type": "basic",
      "description": "All endpoints are secured by basic authentication. Usage of the authentication credentials depends on the service group. Registry services uses credentials of adapter or agent (You can receive these credentials from your VICINITY Neighbourhood Manager UI). Properties, actions, events and discovery endpoints uses authentication credentials of devices or services (You can receive these credentials in device/service registration request as <code>oid</code>/<code>password</code>)."
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
      "description": "Authentication endpoints are used to login and logout the devices and services to and from VICINITY P2P network. If device is not logged it will not be reachable from P2P network (e.g. VICINITY Gateway API will not be able to send property request through the P2P network unless the devices is not logged in)."
    },
    {
      "name": "registry",
      "description": "These endpoints are used to manage your devices and service registry in VICINITY Peer-to-peer network. It is very likely that your IoT infrastructure possesses many objects. In order to avoid setting up each one of your objects manually, you can register, update or remove automatically several objects at once."
    },
    {
      "name": "properties",
      "description": "These endpoints (services) are used to access and set properties of the remote devices or service through VICINITY P2P network. If you registered device or service using discovery endpints and these device or service provides values of property you need to implement these endpoints and specify them during the registration of the devices or service."
    },
    {
      "name": "actions",
      "description": "These endpoints (services) are used to access actions of the remote devices and service through VICINITY P2P network. If you registered device or service using discovery endpoints and these device or service provides actions you need to implement these endpoints and specify them during the registration of the devices or service."
    },
    {
      "name": "events",
      "description": "These endpoints (services) are used to access events of the remote devices and service through VICINITY P2P network. If you registered device or service using discovery endpoints and these device or service provides events you need to implement these endpoints and specify them during the registration of the devices or service."
    },
    {
      "name": "discovery",
      "description": "These endpoint enable search for devices and service which are reachable through your VICINITY Open Gateway API. You can search only for devices and services which you have access and proper contract set-up in VICINITY Neighbourhood Manager."
    }
  ],
  "schemes": [
    "https"
  ],
  "paths": {
    "/objects/login": {
      "get": {
        "tags": [
          "authentication"
        ],
        "summary": "Logs your object into the network.",
        "description": "Provides login endpoint for your objects (devices, services). ",
        "operationId": "getObjectsLogin",
        "produces": [
          "application/json"
        ],
        "responses": {
          "200": {
            "description": "Successful operation",
            "schema": {
              "type": "object",
              "properties": {
                "error": {
                  "type": "boolean",
                  "default": false
                },
                "statusCode": {
                  "type": "integer",
                  "default": 200
                },
                "statusCodeReason": {
                  "type": "string",
                  "default": "OK. Login successfull."
                },
                "contentType": {
                  "type": "string",
                  "default": "application/json"
                },
                "message": {
                  "type": "array",
                  "items": {
                    "type": "string"
                  }
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
        "summary": "Logs out your object from the network.",
        "description": "Provides logout endpoint for your objects (devices, services). The connection for given object is terminated.",
        "operationId": "getObjectsLogout",
        "produces": [
          "application/json"
        ],
        "responses": {
          "200": {
            "description": "Successful operation",
            "schema": {
              "type": "object",
              "properties": {
                "error": {
                  "type": "boolean",
                  "default": false
                },
                "statusCode": {
                  "type": "integer",
                  "default": 200
                },
                "statusCodeReason": {
                  "type": "string",
                  "default": "OK. Logout successfull."
                },
                "contentType": {
                  "type": "string",
                  "default": "application/json"
                },
                "message": {
                  "type": "array",
                  "items": {
                    "type": "string"
                  }
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
        "description": "Retrieves a list of all IoT objects identifiers that are visible to that particular object (send object credentials in head HTTP request) based on the permissions set in Neighbourhood Manager Web interface. This includes both your own and foreign devices. In order to make it into the list, it is necessary for the object to be online.\n",
        "operationId": "getObjects",
        "produces": [
          "application/json"
        ],
        "parameters": [
          {
            "name": "thingDescriptions",
            "in": "query",
            "description": "If the parameter is set to true, the output contains the whole thing descriptions, not just ids.",
            "required": false,
            "type": "boolean"
          },
          {
            "name": "page",
            "in": "query",
            "description": "Additional parameter for the thingDescriptions parameter. The output is paging, due to size. An input is an integer value of a page, starting from zero.",
            "required": false,
            "type": "integer"
          }
        ],
        "responses": {
          "200": {
            "description": "Successful operation",
            "schema": {
              "type": "object",
              "properties": {
                "objects": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "oid": {
                        "type": "string"
                      }
                    }
                  }
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
    "/agents/{agid}/objects": {
      "get": {
        "tags": [
          "registry"
        ],
        "summary": "Retrieve a list of object TDs connected through your VICINITY Gateway API.",
        "description": "Retrieves a list of object TDs that are connected to this particular VICINITY Gateway API. The service is athenticated with credentials of agent/adapter requested from VICINITY Neighbourhood Manager.\n",
        "operationId": "getAgentsAgidObjects",
        "produces": [
          "application/json"
        ],
        "parameters": [
          {
            "name": "agid",
            "in": "path",
            "description": "Agent identifier defined in VICINITY Neigbourhood Manager Access Point tab",
            "required": true,
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "Successful operation",
            "schema": {
              "type": "object",
              "properties": {
                "error": {
                  "type": "boolean",
                  "default": false
                },
                "message": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "id": {
                        "type": "object",
                        "properties": {
                          "_id": {
                            "type": "string"
                          },
                          "info": {
                            "type": "object",
                            "properties": {
                              "adapter_id": {
                                "type": "string"
                              },
                              "name": {
                                "type": "string"
                              },
                              "oid": {
                                "type": "string"
                              },
                              "type": {
                                "type": "string"
                              },
                              "actions": {
                                "type": "array",
                                "items": {
                                  "type": "object",
                                  "description": "Flexible schema - see agent documentation"
                                }
                              },
                              "properties": {
                                "type": "array",
                                "items": {
                                  "type": "object",
                                  "description": "Flexible schema - see agent documentation"
                                }
                              },
                              "events": {
                                "type": "array",
                                "items": {
                                  "type": "object",
                                  "description": "Flexible schema - see agent documentation"
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  }
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
          "registry"
        ],
        "summary": "Register a set of new objects.",
        "description": "Agent can use this endpoint to register a set of new objects. Set of new objects are represents like list of thing descriptions (TDs) and needs to be sended in request body.",
        "operationId": "postAgentsAgidObjects",
        "produces": [
          "application/json"
        ],
        "parameters": [
          {
            "name": "body",
            "in": "body",
            "required": true,
            "schema": {
              "type": "object",
              "properties": {
                "agid": {
                  "type": "string"
                },
                "thingDescriptions": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "name": {
                        "type": "string"
                      },
                      "adapter_id": {
                        "type": "string"
                      },
                      "type": {
                        "type": "string"
                      },
                      "actions": {
                        "type": "array",
                        "items": {
                          "type": "object",
                          "description": "Flexible schema - see agent documentation"
                        }
                      },
                      "properties": {
                        "type": "array",
                        "items": {
                          "type": "object",
                          "description": "Flexible schema - see agent documentation"
                        }
                      },
                      "events": {
                        "type": "array",
                        "items": {
                          "type": "object",
                          "description": "Flexible schema - see agent documentation"
                        }
                      }
                    }
                  }
                }
              }
            }
          },
          {
            "name": "agid",
            "in": "path",
            "description": "agent id",
            "required": true,
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "Successful operation",
            "schema": {
              "type": "object",
              "properties": {
                "error": {
                  "type": "boolean",
                  "default": false
                },
                "message": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "oid": {
                        "type": "string"
                      },
                      "password": {
                        "type": "string"
                      },
                      "nm-id": {
                        "type": "string"
                      },
                      "name": {
                        "type": "string"
                      },
                      "error": {
                        "type": "boolean",
                        "default": false
                      }
                    }
                  }
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
          "registry"
        ],
        "summary": "Replace existing set of objects' TDs.",
        "description": "Agent can use this endpoint to update TDs of already registered objects. This call completely replaces previously registered TDs for given objects.",
        "operationId": "putAgentsAgidObjects",
        "produces": [
          "application/json"
        ],
        "parameters": [
          {
            "name": "body",
            "in": "body",
            "required": true,
            "schema": {
              "type": "object",
              "properties": {
                "agid": {
                  "type": "string"
                },
                "thingDescriptions": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "name": {
                        "type": "string"
                      },
                      "oid": {
                        "type": "string"
                      },
                      "adapter_id": {
                        "type": "string"
                      },
                      "type": {
                        "type": "string"
                      },
                      "actions": {
                        "type": "array",
                        "items": {
                          "type": "object",
                          "description": "Flexible schema - see agent documentation"
                        }
                      },
                      "properties": {
                        "type": "array",
                        "items": {
                          "type": "object",
                          "description": "Flexible schema - see agent documentation"
                        }
                      },
                      "events": {
                        "type": "array",
                        "items": {
                          "type": "object",
                          "description": "Flexible schema - see agent documentation"
                        }
                      }
                    }
                  }
                }
              }
            }
          },
          {
            "name": "agid",
            "in": "path",
            "description": "agent id",
            "required": true,
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "Successful operation",
            "schema": {
              "type": "object",
              "properties": {
                "error": {
                  "type": "boolean",
                  "default": false
                },
                "message": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "oid": {
                        "type": "string"
                      },
                      "name": {
                        "type": "string"
                      },
                      "nm-id": {
                        "type": "string"
                      },
                      "error": {
                        "type": "boolean",
                        "default": false
                      },
                      "contracts": {
                        "type": "array",
                        "items": {
                          "type": "boolean",
                          "default": false
                        }
                      },
                      "status": {
                        "type": "string",
                        "default": "Success"
                      }
                    }
                  }
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
    "/agents/{agid}/objects/update": {
      "put": {
        "tags": [
          "registry"
        ],
        "summary": "Update existing set of objects' TDs.",
        "description": "Agent can use this endpoint to update TDs of already registered objects.",
        "operationId": "putAgentsAgidObjectsUpdate",
        "produces": [
          "application/json"
        ],
        "parameters": [
          {
            "name": "body",
            "in": "body",
            "required": true,
            "schema": {
              "type": "object",
              "properties": {
                "agid": {
                  "type": "string"
                },
                "thingDescriptions": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "name": {
                        "type": "string"
                      },
                      "oid": {
                        "type": "string"
                      },
                      "adapter_id": {
                        "type": "string"
                      },
                      "type": {
                        "type": "string"
                      },
                      "actions": {
                        "type": "array",
                        "items": {
                          "type": "object",
                          "description": "Flexible schema - see agent documentation"
                        }
                      },
                      "properties": {
                        "type": "array",
                        "items": {
                          "type": "object",
                          "description": "Flexible schema - see agent documentation"
                        }
                      },
                      "events": {
                        "type": "array",
                        "items": {
                          "type": "object",
                          "description": "Flexible schema - see agent documentation"
                        }
                      }
                    }
                  }
                }
              }
            }
          },
          {
            "name": "agid",
            "in": "path",
            "description": "agent id",
            "required": true,
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "Successful operation",
            "schema": {
              "type": "object",
              "properties": {
                "error": {
                  "type": "boolean",
                  "default": false
                },
                "message": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "oid": {
                        "type": "string"
                      },
                      "success": {
                        "type": "boolean",
                        "default": true
                      }
                    }
                  }
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
    "/agents/{agid}/objects/delete": {
      "post": {
        "tags": [
          "registry"
        ],
        "summary": "Delete set of objects.",
        "description": "An agent can delete a set of objects, that are registered through it.",
        "operationId": "postAgentsAgidObjectsDelete",
        "produces": [
          "application/json"
        ],
        "parameters": [
          {
            "name": "body",
            "in": "body",
            "required": true,
            "schema": {
              "type": "object",
              "properties": {
                "agid": {
                  "type": "string"
                },
                "oids": {
                  "type": "array",
                  "items": {
                    "type": "string"
                  }
                }
              }
            }
          },
          {
            "name": "agid",
            "in": "path",
            "description": "agent id",
            "required": true,
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "Successful operation",
            "schema": {
              "type": "object",
              "properties": {
                "error": {
                  "type": "boolean",
                  "default": false
                },
                "message": {
                  "type": "object",
                  "properties": {
                    "error": {
                      "type": "boolean",
                      "default": false
                    },
                    "message": {
                      "type": "array",
                      "items": {
                        "type": "object",
                        "properties": {
                          "value": {
                            "type": "string"
                          },
                          "result": {
                            "type": "string"
                          },
                          "error": {
                            "type": "boolean",
                            "default": false
                          }
                        }
                      }
                    }
                  }
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
        "description": "Retrieves a value of a given property from a remote object. Only object with valid data contract are available. You can create data contract in VICINITY Neighbourhood Manager. First parameter <code>oid</code> determines remote object and second identify the property af remote object, both parameters are mandatory and can be extracted from TD from <code>/objects</code>.\n\nReception of this request will cause the receiving OGWAPI to fire following request to an Agent - GET http://[agent IP address]:[port]/agent/objects/[destination ID]/properties/[property ID] with one of the parameters being 'sourceId', containing a string with the source identifier. Therefore, an endpoint needs to be implemented on the Agent capable of receving such requests and retrieving given property from the object.",
        "operationId": "getObjectsOidPropertiesPid",
        "produces": [
          "application/json"
        ],
        "parameters": [
          {
            "name": "oid",
            "in": "path",
            "description": "object id",
            "required": true,
            "type": "string"
          },
          {
            "name": "pid",
            "in": "path",
            "description": "property id",
            "required": true,
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "Successful operation",
            "schema": {
              "type": "object",
              "properties": {
                "error": {
                  "type": "boolean",
                  "default": false
                },
                "statusCode": {
                  "type": "number",
                  "default": 200
                },
                "statusCodeReason": {
                  "type": "string"
                },
                "message": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "value": {
                        "type": "number"
                      }
                    }
                  }
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
        "description": "Sets a new value of a property on a remote object. Object which property is setting has to be visible for object, which is requesting. This visibiliti is configurable in neighbourhood manager. First parameter oid determines remote object and secound identify the property of remote object, both parameters are mandatory. Setting value is sending in body of request. \n\nReception of this request will cause the receiving OGWAPI to fire following request to an Agent - PUT http://[agent IP address]:[port]/agent/objects/[destination ID]/properties/[property ID] with one of the parameters being 'sourceId', containing a string with the source identifier. Therefore, an endpoint needs to be implemented on the Agent capable of receving such requests and retrieving given property from the object.\n",
        "operationId": "putObjectsOidPropertiesPid",
        "produces": [
          "application/json"
        ],
        "parameters": [
          {
            "name": "body",
            "in": "body",
            "required": true,
            "schema": {
              "type": "object",
              "properties": {
                "value": {
                  "type": "number",
                  "default": 0
                }
              }
            }
          },
          {
            "name": "oid",
            "in": "path",
            "description": "object id",
            "required": true,
            "type": "string"
          },
          {
            "name": "pid",
            "in": "path",
            "description": "property id",
            "required": true,
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "Successful operation",
            "schema": {
              "type": "object",
              "properties": {
                "error": {
                  "type": "boolean",
                  "default": false
                },
                "statusCode": {
                  "type": "number",
                  "default": 200
                },
                "statusCodeReason": {
                  "type": "string"
                },
                "message": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "data": {
                        "type": "object",
                        "properties": {
                          "echo": {
                            "type": "string"
                          },
                          "oid": {
                            "type": "string"
                          }
                        }
                      },
                      "status": {
                        "type": "string"
                      }
                    }
                  }
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
        "description": "Start execution of an action. The particular execution is called a task, has its own ID (that gets returned) and has multiple states, in which it can be – see the next endpoint. You can add any parameters, just remember that on the other side the request to agent/adapter will automatically have 'sourceOid' parameter added. Therefore, any parameter with the same name will be overwritten. IMPORTANT - The gateway on the other side will automatically queue requests and will pick one at a time, in a standard FIFO fashion, for execution. Therefore, the start of a pending task will cause the receiving OGWAPI to fire following request to an Agent / Adapter - POST http://[agent / adapter IP address]:[port]/agent/objects/[destination ID]/properties/[property ID] with one of the parameters being 'sourceId', containing a string with the source identifier. Therefore, an endpoint needs to be implemented on the Agent / Adapter capable of receving such requests and retrieving given property from the object.",
        "operationId": "postObjectsOidActionsAid",
        "produces": [
          "application/json"
        ],
        "parameters": [
          {
            "name": "oid",
            "in": "path",
            "description": "object id",
            "required": true,
            "type": "string"
          },
          {
            "name": "aid",
            "in": "path",
            "description": "action id",
            "required": true,
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "Successful operation",
            "schema": {
              "type": "object",
              "properties": {
                "error": {
                  "type": "boolean",
                  "default": false
                },
                "statusCode": {
                  "type": "integer",
                  "default": 201
                },
                "statusCodeReason": {
                  "type": "string"
                },
                "message": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "taskId": {
                        "type": "string"
                      }
                    }
                  }
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
        "description": "The Agent/Adapter in charge of executing the task should call this endpoint to let the local OGWAPI know, that the action is completed, or to provide intermediate results if desired. Since there is only one task at a time the Adapter is able to execute, there is no need to specify to OGWAPI which task is done – the queueing of the tasks is necessary only from the ‘outside world’ point of view. The new status is send in a query parameter 'status' - it is a string of one of these values - running, failed, finished. Putting the task into any state other than running will cause the task (and its timers) to stop and next task in queue will start being executed.",
        "operationId": "putObjectsOidActionsAid",
        "produces": [
          "application/json"
        ],
        "parameters": [
          {
            "name": "oid",
            "in": "path",
            "description": "object id",
            "required": true,
            "type": "string"
          },
          {
            "name": "aid",
            "in": "path",
            "description": "action id",
            "required": true,
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "Successful operation",
            "schema": {
              "type": "object",
              "properties": {
                "error": {
                  "type": "boolean",
                  "default": false
                },
                "statusCode": {
                  "type": "number",
                  "default": 201
                },
                "statusCodeReason": {
                  "type": "string"
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
        "description": "When an action is executed, it can take a while, like opening a door or running an algorithm. This particular execution, or run, is called a task. This endpoint returns a status of a task, that has been executed before. A task can be in one of the following states - pending (first state, before it starts being executed), running, finished and failed.",
        "operationId": "getObjectsOidActionsAidTasksTid",
        "produces": [
          "application/json"
        ],
        "parameters": [
          {
            "name": "oid",
            "in": "path",
            "description": "object id",
            "required": true,
            "type": "string"
          },
          {
            "name": "aid",
            "in": "path",
            "description": "action id",
            "required": true,
            "type": "string"
          },
          {
            "name": "tid",
            "in": "path",
            "description": "task id",
            "required": true,
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "Successful operation",
            "schema": {
              "type": "object",
              "properties": {
                "error": {
                  "type": "boolean",
                  "default": false
                },
                "statusCode": {
                  "type": "number",
                  "default": 200
                },
                "statusCodeReason": {
                  "type": "string"
                },
                "message": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "taskId": {
                        "type": "string"
                      },
                      "status": {
                        "type": "string"
                      },
                      "createdAt": {
                        "type": "string",
                        "description": "date"
                      },
                      "startTime": {
                        "type": "string",
                        "description": "date"
                      },
                      "totalTime": {
                        "type": "number"
                      },
                      "returnValue": {
                        "type": "object",
                        "properties": {
                          "value": {
                            "type": "string"
                          }
                        }
                      }
                    }
                  }
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
        "description": "Cancels a task, that is in progress as a result of action being executed. Only a task that is either in running or pending state can be cancelled. IMPORTANT - Reception of this request will cause the receiving OGWAPI to fire following request to an Agent / Adapter - DELETE http://[agent / adapter IP address]:[port]/agent/objects/[destination ID]/actions/[action ID] with one of the parameters being {sourceId}, containing a string with the source identifier. Therefore, an endpoint needs to be implemented on the Agent / Adapter capable of receving such requests and cancelling given action on the object.",
        "operationId": "deleteObjectsOidActionsAidTasksTid",
        "produces": [
          "application/json"
        ],
        "parameters": [
          {
            "name": "oid",
            "in": "path",
            "description": "object id",
            "required": true,
            "type": "string"
          },
          {
            "name": "aid",
            "in": "path",
            "description": "action id",
            "required": true,
            "type": "string"
          },
          {
            "name": "tid",
            "in": "path",
            "description": "task id",
            "required": true,
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "Successful operation",
            "schema": {
              "type": "object",
              "properties": {
                "error": {
                  "type": "boolean",
                  "default": false
                },
                "statusCode": {
                  "type": "number",
                  "default": 200
                },
                "statusCodeReason": {
                  "type": "string"
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
        "produces": [
          "application/json"
        ],
        "parameters": [
          {
            "name": "eid",
            "in": "path",
            "description": "event id",
            "required": true,
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "Successful operation",
            "schema": {
              "type": "object",
              "properties": {
                "error": {
                  "type": "boolean",
                  "default": false
                },
                "statusCode": {
                  "type": "number",
                  "default": 200
                },
                "statusCodeReason": {
                  "type": "string"
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
        "produces": [
          "application/json"
        ],
        "parameters": [
          {
            "name": "eid",
            "in": "path",
            "description": "event id",
            "required": true,
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "Successful operation",
            "schema": {
              "type": "object",
              "properties": {
                "error": {
                  "type": "boolean",
                  "default": false
                },
                "statusCode": {
                  "type": "number",
                  "default": 200
                },
                "statusCodeReason": {
                  "type": "string"
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
        "produces": [
          "application/json"
        ],
        "parameters": [
          {
            "name": "eid",
            "in": "path",
            "description": "event id",
            "required": true,
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "Successful operation",
            "schema": {
              "type": "object",
              "properties": {
                "error": {
                  "type": "boolean",
                  "default": false
                },
                "statusCode": {
                  "type": "number",
                  "default": 200
                },
                "statusCodeReason": {
                  "type": "string"
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
        "produces": [
          "application/json"
        ],
        "parameters": [
          {
            "name": "oid",
            "in": "path",
            "description": "object id",
            "required": true,
            "type": "string"
          },
          {
            "name": "eid",
            "in": "path",
            "description": "event id",
            "required": true,
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "Successful operation",
            "schema": {
              "type": "object",
              "properties": {
                "error": {
                  "type": "boolean",
                  "default": false
                },
                "statusCode": {
                  "type": "integer",
                  "default": 200
                },
                "statusCodeReason": {
                  "type": "string"
                },
                "message": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "active": {
                        "type": "boolean"
                      }
                    }
                  }
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
        "produces": [
          "application/json"
        ],
        "parameters": [
          {
            "name": "oid",
            "in": "path",
            "description": "object id",
            "required": true,
            "type": "string"
          },
          {
            "name": "eid",
            "in": "path",
            "description": "event id",
            "required": true,
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "Successful operation",
            "schema": {
              "type": "object",
              "properties": {
                "error": {
                  "type": "boolean",
                  "default": false
                },
                "statusCode": {
                  "type": "number",
                  "default": 200
                },
                "statusCodeReason": {
                  "type": "string"
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
        "produces": [
          "application/json"
        ],
        "parameters": [
          {
            "name": "oid",
            "in": "path",
            "description": "object id",
            "required": true,
            "type": "string"
          },
          {
            "name": "eid",
            "in": "path",
            "description": "event id",
            "required": true,
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "Successful operation",
            "schema": {
              "type": "object",
              "properties": {
                "error": {
                  "type": "boolean",
                  "default": false
                },
                "statusCode": {
                  "type": "number",
                  "default": 200
                },
                "statusCodeReason": {
                  "type": "string"
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
    "/search/sparql": {
      "post": {
        "tags": [
          "discovery"
        ],
        "summary": "Query the VICINITY P2P Networ by using SPARQL.",
        "description": "Query  the  VICINITY  P2P  Network  by  means  of  a  combination  of  discovery  and  access functions, by using SPARQL and the VICINITY Ontology.",
        "operationId": "postSearchSparql",
        "produces": [
          "application/json"
        ],
        "parameters": [
          {
            "name": "body",
            "in": "body",
            "required": true,
            "description": "Body is SPARQL query in JSON format.\n",
            "schema": {
              "type": "object"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "Successful operation",
            "schema": {
              "type": "object"
            }
          },
          "401": {
            "description": "Unauthorized"
          }
        }
      }
    },
    "/search/semantic": {
      "post": {
        "tags": [
          "discovery"
        ],
        "summary": "Query the available compatible SHAR-Q Semantic Interfaces.",
        "description": "<p>Retrieve all compatible interfaces for a given semantic interface. After obtaining the list of compatible semantic interfaces you can filter the list of TDs of devices or services from the <code>/objects</code> end-point using on semantic interface name. From each filter TD you can get <code>oid</code> of device or service with which you can interact through properties, actions and events endpoints.</p>",
        "operationId": "postSearchSemantic",
        "produces": [
          "application/json"
        ],
        "parameters": [
          {
            "name": "body",
            "in": "body",
            "required": true,
            "schema": {
              "type": "object",
              "properties": {
                "semanticInterface": {
                  "type": "string"
                }
              },
              "example": {
                "semanticInterface": "eu.shar_q.sim:BasicBattery:0.0.1"
              }
            }
          }
        ],
        "responses": {
          "200": {
            "description": "Successful operation",
            "examples": {
              "application/json": {
                "error": false,
                "statusCode": 0,
                "statusCodeReason": "Reason of error",
                "message": [
                  {
                    "semanticInterfaces": [
                      {
                        "oid": "OID1",
                        "semanticInterface": "eu.shar_q.sim:BasicBattery:0.0.1"
                      },
                      {
                        "oid": "OID2",
                        "semanticInterface": "eu.shar_q.sim:BasicBattery:0.0.1"
                      },
                      {
                        "oid": "OID3",
                        "semanticInterface": "eu.shar_q.sim:BatteryExample:0.0.1"
                      }
                    ]
                  }
                ]
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
