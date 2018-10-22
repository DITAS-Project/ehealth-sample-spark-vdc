/**
 * Copyright 2018 IBM
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 * This is being developed for the DITAS Project: https://www.ditas-project.eu/
 */

# ehealth-sample-spark-vdc
VDC implemented in Spark for e-Health sample usecase

Description:
```
Generate a compliant result by applying the rewritten query send from the enforcement engine on a set of corresponding data tables.
```

Create distribution with:
```
sbt universal:packageZipTarball
```


Unzip the archive in target/universal/:
```
tar xvfz ehealth-sample-spark-vdc-1.0-SNAPSHOT.tgz
```

Run the application:
```
app/ehealth-sample-spark-vdc-1.0-SNAPSHOT/bin/ehealth-sample-spark-vdc -Dplay.http.secret.key='your-secret' -Dconfig.file='\/conf\/application.conf'
```

Please note: conf/reference.conf file includes default settings for the configuration file.  

Example call:
```
 curl -X GET "http://<hostname>:9000/blood-test/component/fibrinogen/average/3-18" -H "accept: application/json" -H "Content-Type: application/json"  -H "Purpose: Research"
```

Documentation:
Go to http://<hostname>:9000/docs/ and paste the path to your swagger.json (http://<hostname>:9000/docs/swagger.json) in the text box.

