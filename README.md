# VICINITY Open Gateway API #
The standalone VICINITY Open Gateway API enables your IoT infrastructure to interconnect with other IoT infrastructures and services through VICINITY P2P Network by using HTTP REST requests. Among its features there are devices and services registration,  retrieving and setting a property on remote objects, executing an action, or subscribing to an event channel and receiving asynchronously fired event whenever one is published.

# Getting started with VICINITY Open Gateway API #
This “simple:)” get started guide provides step by step approach to integrate IoT infrastructure in VICINITY.           
https://vicinity-get-started.readthedocs.io/en/latest/getstarted.html

# VICINITY Open Gateway REST API description #
For more information about HTTP REST requests please visit complete REST API description.
https://vicinityh2020.github.io/vicinity-gateway-api/#/

# Build binaries #

mvn clean package

# Run static analyzer

Run static analyzer SonarQube

mvn sonar:sonar \
  -Dsonar.projectKey=YOUR-PROJECT-NAME \
  -Dsonar.host.url=YOUR-HOST \
  -Dsonar.login=YOUR-TOKEN
