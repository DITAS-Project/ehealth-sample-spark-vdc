# ehealth-sample-spark-vdc
VDC implemented in Spark for e-Health sample usecase

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

Example call:
```
curl http://localhost:9000/patient/XGXCLS09X31T865C/blood-test/component/haemoglobin
```
