## License
Copyright 2018 IBM

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations under
the License.

This is being developed for the DITAS Project: https://www.ditas-project.eu/

## ehealth-sample-spark-vdc

VDC implemented in Spark for e-Health sample usecase

## Description

A Spark implementation of a VDC that exposes data for medical doctor and researcher from two data sources - patient biographical data and blood tests. The data returned is compliant with privacy policies, which are enforced by the enforcement engine.


## Functionalities

`POST` `/patient` `/socialId` `/blood-test` `/component` `/testType`

  * **description**: Get timeseries of patient's blood test component.
  * **caller** Requester
  * **input**: RequesterId and purpose
  * **output**: JSON containing the compliant blood tests

`POST` `/blood-test` `/component` `/testType` `/average` `/startAgeRange-endAgeRange`

  * **description**: Returns the average value for a specific blood test 
    component in a specific age range, to be used by researchers. 
  * **caller** Requester
  * **input**: Purpose
  * **output**: JSON containing the average value


## Installation
Clone repository

Create distribution with:
```
sbt universal:packageZipTarball
```


Unzip the archive in target/universal/:
```
tar xvfz ehealth-sample-spark-vdc-1.0.tgz
```
## Execution:

The conf/reference.conf file provide defaults; they are overridden by any settings defined in the application.conf file. Please, copy the contents of the file conf/reference.conf to conf/application.conf and replace with the correct values of your runtime.

* Use the following command to run the application.

```
app/ehealth-sample-spark-vdc-1.0/bin/ehealth-sample-spark-vdc -Dplay.http.secret.key='your-secret' -Dconfig.file='\/conf\/application.conf'
```

* Example call:
```
 curl -X GET "http://<hostname>:9000/blood-test/component/fibrinogen/average/3-18" -H "accept: application/json" -H "Content-Type: application/json"  -H "Purpose: Research"
```

## Documentation:
```
Go to http://<hostname>:9000/docs/swagger.json
```

For Swagger UI - Go to
```
http://<hostname>:9000/docs/
```
and paste the url path to your swagger.json in the text box.

