#!/usr/bin/env bash
# Staging environment: 31.171.247.162
# Private key for ssh: /opt/keypairs/ditas-testbed-keypair.pem
version="da0.0.1"
# TODO state management? We are killing without careing about any operation the conainer could be doing.
ssh -T -i /opt/keypairs/ditas-testbed-keypair.pem cloudsigma@31.171.247.162 << 'ENDSSH'
echo $version
# Ensure that a previously running instance is stopped (-f stops and removes in a single step)
# || true - "docker stop" failt with exit status 1 if image doen't exists, what makes the Pipeline fail. the "|| true" forces the command to exit with 0.
sudo docker rm -f ehealth-sample-spark-vdc-$version || true
sudo docker pull ditas/ehealth-sample-spark-vdc:$version
sudo docker run -p 50005:9000 -d --name ehealth-sample-spark-vdc-$version ditas/ehealth-sample-spark-vdc:$version
ENDSSH
