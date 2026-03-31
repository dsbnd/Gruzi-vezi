#!/bin/bash

./gradlew clean bootJar -Pspring.profiles.active=narayana

/usr/bin/scp -P 2222 build/libs/gruzi_vezi-1.0-SNAPSHOT.jar s409792@cs.ifmo.ru:~/