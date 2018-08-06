#!/usr/bin/env bash
# Staging environment: 31.171.247.162
# Private key for ssh: /opt/keypairs/ditas-testbed-keypair.pem

# TODO state management? We are killing without careing about any operation the conainer could be doing.

ssh -i /opt/keypairs/ditas-testbed-keypair.pem cloudsigma@31.171.247.162 << 'ENDSSH'
# Ensure that a previously running instance is stopped (-f stops and removes in a single step)
# || true - "docker stop" failt with exit status 1 if image doen't exists, what makes the Pipeline fail. the "|| true" forces the command to exit with 0.
sudo docker rm -f ehealth-sample-spark-vdc || true
sudo docker pull ditas/ehealth-sample-spark-vdc:latest
sudo docker run -p 50005:9000 -d  --name ehealth-sample-spark-vdc ditas/ehealth-sample-spark-vdc:latest
sh "cp /home/cloudsigma/configurations/ehealth-sample-spark-vdc/.application.conf .application.conf"
sudo docker exec -i ehealth-sample-spark-vdc sh -c 'cat > /app/ehealth-sample-spark-vdc-1.0-SNAPSHOT/conf/application.conf' < ./application.conf
sudo docker exec -d ehealth-sample-spark-vdc /app/ehealth-sample-spark-vdc-1.0-SNAPSHOT/bin/ehealth-sample-spark-vdc -Dplay.http.secret.key='wspl4r' -Dconfig.file='app/ehealth-sample-spark-vdc-1.0-SNAPSHOT/conf/application.conf'
ENDSSH
