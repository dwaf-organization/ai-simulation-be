#!/bin/bash

./gradlew build -x test

aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin 121435512483.dkr.ecr.ap-northeast-2.amazonaws.com

docker build --build-arg DEPENDENCY=build/dependency -t 121435512483.dkr.ecr.ap-northeast-2.amazonaws.com/simulation/backend:v1 --platform linux/amd64 .
docker tag 121435512483.dkr.ecr.ap-northeast-2.amazonaws.com/simulation/backend:v1 121435512483.dkr.ecr.ap-northeast-2.amazonaws.com/simulation/backend:latest

docker push 121435512483.dkr.ecr.ap-northeast-2.amazonaws.com/simulation/backend:v1
docker push 121435512483.dkr.ecr.ap-northeast-2.amazonaws.com/simulation/backend:latest