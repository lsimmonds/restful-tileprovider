# restful-tileprovider
A simple service to fetch tiles from a GeoPackage

## Description
Designed to be used as a fallback to a remote GeoServer. In the case of a remote GeoServer being unavailable will
attempt to produce a map from local data. It does this by searching for a comparable GeoPackage database file for a 
given layer. This only works for WMS or OWS calls of format type "image:jpeg"

## Configuration

Basic tileprovider configuration is in the file src\main\resources\bootstrap.properties. It has the following variables:

* server.port - The port the service will be delivered through
* spring.application.name - The application name, can be used as the final path element in url
* geopackage.rootPath - The directory where GeoPackage files will be looked for
* debug - Will draw outlines around individual tiles if set to true
* transparent - If multiple layers are requested will make all but the first layer 50% transparent

## Operation
Tileprovider is a restful service. The call path should be a http call to the port defined in server.port followed by
"/service-instances/" followed by the GeoServer service, wms or ows, or the value of spring.application.name. The
service is designed to take identical calls to the the GeoServer services. You should be able to cut the last last
element of a GeoServer call and concatenate it to the http path up to "service-instances/" and it should work. For
example this wms call:
* http://localhost:8765/geoserver/wms?bbox=-85,25,-80,35&service=WMS&version=1.1.0&request=GetMap&layers=NOMS%3ABlueMarbleA&width=300&height=300&srs=EPSG%3A4326&format=image%2Fjpeg
should work as this Tileprovider call:
* http://localhost:8082/service-instances/wms?bbox=-90,0,-70,50&service=WMS&version=1.1.0&request=GetMap&layers=NOMS%3ABlueMarbleA,cite-ne_10m_admin_1_states_provinces,nurc-Arc_Sample&width=300&height=300&srs=EPSG%3A4326&format=image%2Fjpeg
