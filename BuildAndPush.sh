#!/bin/bash

./gradlew bootJar -Pspring.profiles.active=prod

scp -P 2222 -i ~/.ssh/helios/main ./build/libs/gruzi_vezi-1.0-SNAPSHOT.jar s408303@cs.ifmo.ru:~/
