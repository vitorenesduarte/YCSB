<!--
Copyright (c) 2015 YCSB contributors. All rights reserved.

Licensed under the Apache License, Version 2.0 (the "License"); you
may not use this file except in compliance with the License. You
may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the License for the specific language governing
permissions and limitations under the License. See accompanying
LICENSE file.
-->

## Quick Start

This section describes how to run YCSB on MGB-SMap. 

### 1. Install Java and Maven

### 2. Set Up YCSB
1. Git clone YCSB and compile:
  ```
git clone http://github.com/brianfrankcooper/YCSB.git
cd YCSB
mvn clean package
  ```
### 3. Install MGB-SMap

0. Git clone MGB-SMap, compile, generate poms and install
  ```
git clone https://github.com/tuanir/MGB-SMap.git
cd MGB-SMap
make compile
make jars
make poms
make install
  ```

1. Go to YCSB root directory and compile the MGB-SMap bindings:
  ```
mvn -pl com.yahoo.ycsb:mgbsmap-binding -am clean package -DskipTests
  ```

### 4. Testing through YCSB shell

1. In YCSB/bin, run:

  ```
sh ycsb.sh shell vcdmap
  ```
