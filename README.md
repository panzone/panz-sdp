# panz-sdp

This is the project I've developed for my Distributed Systems class using Java, Jersey and gson. We have several sensors that gathers different environmental data and we want to provide a way to handle and access these informations to a user using a client application.

The project is implemented with three different applications:

* A sensor, which can be a sensor for temperature, acceleration and light
* The gateway, that handles the communications between the sensor network and the user
* A command line application for the end user

Each of these three applications have their own directory and subproject. For more details about the project read progettoSDP2016.pdf (in Italian).

## Sensor

A sensor, in this project, is a little application that generates periodically informations (called measurements). A sensor has a little buffer for contains these informations.

## Sensor network

The network that connects the sensors is a token ring. For handling the network, the sensors uses a message system. For more details about the message types, read Documentazione_rete.pdf (in Italian).

Each sensor, when it receives the token, adds the measurements in its internal buffer to the token itself and it sends the token to the next sensor in the token ring. Since the token has limited memory, when the token is full the sensor sends the measurements to the gateway using a specific REST interface, cleaning up the token.

The network is able to understand when a sensor disappear because of errors or crashed and it can correct itself without external guidance.

## Gateway

The gateway is implemented as a REST server. There are two interfaces: sensor and client. The gateway also stores the measurements received from the sensor network.

## Command line application

This is a simple application that connects to the gateway for obtain informations about the measurements and the sensor network. It also support push notifications.

## Building

All three projects have a gradle configuration for building, so it should be as simple as a
```bash
gradle build
```

on the directory of the subproject.

The gateway was tested with tomcat, but it should work with any web server.

## Execution

The gateway is implemented as a war, so you can use a server like tomcat for executing it.

The client doesn't require any argument: during the first start it will require the necessary data to connect to the gateway server.

The sensor requires several argument, following this template:
```
java -jar Sensor.jar sensor_type sensor_name sensor_port gateway_address [debug]
```
with

* sensor_type the type of the sensor (luminosita, temperatura, accelerometro)
* sensor_name is an arbitrary, unique string for identify the sensor
* sensor_port is the port that the sensor will use for receiving connections
* gateway_address is the address of the gateway
* the optional argument debug prints informations about the sensor during execution.
