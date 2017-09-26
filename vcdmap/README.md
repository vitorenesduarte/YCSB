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

This section describes how to run YCSB on VCDMap. 

### 1. Install Java and Maven

### 2. Set Up YCSB
1. Git clone YCSB and compile:
  ```
git clone http://github.com/brianfrankcooper/YCSB.git
cd YCSB
mvn clean package
  ```
### 3. Install VCDMap

1. Go to VCDMap root directory and install the client:
  ```
mvn install:install-file -Dfile=vcdmapclient/target/vcdmapclient-0.1.0-SNAPSHOT.jar -DpomFile=vcdmapclient/target/vcdmapclient-0.1.0-SNAPSHOT.pom
  ```
2. Go to VCDMap root directory and install the server:
  ```
mvn install:install-file -Dfile=vcdmapserver/target/vcdmapserver-0.1.0-SNAPSHOT.jar -DpomFile=vcdmapserver/target/vcdmapserver-0.1.0-SNAPSHOT.pom
  ```
3. Go to YCSB root directory and compile the VCDMap bindings:
  ```
mvn -Dcheckstyle.skip -X -pl com.yahoo.ycsb:vcdmap-binding -am clean package
  ```

### 4. Testing through YCSB shell

1. In YCSB/bin, run:

  ```
sh ycsb.sh shell vcdmap
  ```
