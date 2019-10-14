#!/usr/bin/env bash
#
# Copyright (C) 2015-2016 Orange
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# http://www.apache.org/licenses/LICENSE-2.0
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

set -ev

echo "CIRCLE_BRANCH: <$CIRCLE_BRANCH> - CIRCLE_TAG: <$CIRCLE_TAG>"

#Workaround OpenJdk8 regression which crashes JVM. See https://stackoverflow.com/a/53085816
export _JAVA_OPTIONS="-Djdk.net.URLClassPath.disableClassPathURLCheck=true"

#Download dependencies
mvn -q help:evaluate -Dexpression=project.version --settings settings.xml
# Capture execution of maven command - It looks like grep cannot be used like this on circle
export VERSION=$(mvn help:evaluate -Dexpression=project.version --settings settings.xml |grep '^[0-9].*')

echo "Current version extracted from pom.xml: $VERSION"

echo "Compiling and packaging artefacts"

mvn clean package --settings settings.xml