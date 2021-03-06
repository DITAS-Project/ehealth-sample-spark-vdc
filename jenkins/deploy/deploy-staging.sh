#!/usr/bin/env bash
# Staging environment: 31.171.247.162
# Private key for ssh: /opt/keypairs/ditas-testbed-keypair.pem

# TODO state management? We are killing without careing about any operation the conainer could be doing.

ssh -i /opt/keypairs/ditas-testbed-keypair.pem cloudsigma@31.171.247.162 << 'ENDSSH'
# Ensure that a previously running instance is stopped (-f stops and removes in a single step)
# || true - "docker stop" failt with exit status 1 if image doen't exists, what makes the Pipeline fail. the "|| true" forces the command to exit with 0.
sudo docker rm -f ehealth-sample-spark-vdc || true
sudo docker pull ditas/ehealth-sample-spark-vdc:latest
sudo docker create --name  ehealth-sample-spark-vdc  -p 50005:9000 ditas/ehealth-sample-spark-vdc:latest
sudo docker cp /home/cloudsigma/configurations/ehealth-sample-spark-vdc/applicationEhealth.conf  ehealth-sample-spark-vdc:/app/ehealth-sample-spark-vdc-1.0/conf/application.conf
sudo docker start ehealth-sample-spark-vdc
ENDSSH

