FROM openjdk

MAINTAINER 0track

WORKDIR /app
RUN apt-get update && apt-get install -y \
    tar \
    iputils-ping \
    git \
    golang \
    maven \
    build-essential

ADD https://api.github.com/repos/otrack/epaxos/git/refs/heads/master epaxos-version.json
RUN git clone https://github.com/otrack/epaxos
RUN GOPATH=/app/epaxos go get -u github.com/go-redis/redis
RUN GOPATH=/app/epaxos go get -u github.com/emirpasic/gods/maps/treemap
RUN GOPATH=/app/epaxos go get -u github.com/google/uuid
RUN GOPATH=/app/epaxos go install client
RUN GOPATH=/app/epaxos go get github.com/sridharv/gojava
RUN GOPATH=/app/epaxos /app/epaxos/bin/gojava -o /app/epaxos/epaxos.jar build bindings
RUN mvn install:install-file -Dfile=/app/epaxos/epaxos.jar -DgroupId=epaxos -DartifactId=epaxos -Dversion=1.0 -Dpackaging=jar

ADD https://api.github.com/repos/vitorenesduarte/VCD-java-client/git/refs/heads/master vcd-java-client-version.json
RUN git clone -b master https://github.com/vitorenesduarte/VCD-java-client
RUN mvn -f VCD-java-client clean install -DskipTests

ADD https://api.github.com/repos/tuanir/MGB-SMap/git/refs/heads/master mgb-smap-version.json
RUN git clone https://github.com/tuanir/MGB-SMap.git
RUN make -C MGB-SMap install

ADD https://api.github.com/repos/otrack/YCSB/git/refs/heads/epaxos ycsb-version.json
RUN git clone -b epaxos https://github.com/otrack/YCSB
RUN mvn -f YCSB -pl com.yahoo.ycsb:epaxos-binding,com.yahoo.ycsb:mgbsmap-binding -am clean package -DskipTests

#RUN mvn -f YCSB -pl com.yahoo.ycsb:cassandra-binding,com.yahoo.ycsb:cassandra-binding -am clean package -DskipTests

RUN tar zxvf /app/YCSB/epaxos/target/ycsb-epaxos-binding-0.13.0-SNAPSHOT.tar.gz -C /app
RUN tar zxvf /app/YCSB/mgbsmap/target/ycsb-mgbsmap-binding-0.13.0-SNAPSHOT.tar.gz -C /app
#RUN tar zxvf /app/YCSB/cassandra/target/ycsb-cassandra-binding-0.13.0-SNAPSHOT.tar.gz -C /app

RUN mkdir /app/ycsb-binding-0.13.0-SNAPSHOT
RUN cp -Rf /app/ycsb-epaxos-binding-0.13.0-SNAPSHOT/* /app/ycsb-binding-0.13.0-SNAPSHOT
RUN cp -Rf /app/ycsb-mgbsmap-binding-0.13.0-SNAPSHOT/* /app/ycsb-binding-0.13.0-SNAPSHOT
#RUN cp -Rf /app/ycsb-cassandra-binding-0.13.0-SNAPSHOT/* /app/ycsb-binding-0.13.0-SNAPSHOT

ENV TYPE load
ENV DB epaxos

ENV WORKLOAD workloada
ENV THREADS 1
ENV RECORDCOUNT 1000
ENV OPERATIONCOUNT 1000

ENV HOST localhost
ENV PORT 7087
ENV LEADERLESS false
ENV FAST false
ENV EXTRA ""
ENV SMAPPORT 8980
ENV STATICSMAP false

CMD ["sh", "-c", "/app/ycsb-binding-0.13.0-SNAPSHOT/bin/ycsb ${TYPE} ${DB} \
    -P /app/ycsb-binding-0.13.0-SNAPSHOT/workloads/${WORKLOAD} \
    -threads ${THREADS} \
    -p recordcount=${RECORDCOUNT} \
    -p operationcount=${OPERATIONCOUNT} \
    -p host=${HOST} \
    -p static=${STATICSMAP} \
    -p port=${PORT} \
    -p leaderless=${LEADERLESS} \
    -p fast=${FAST} \
    -p smapport=${SMAPPORT} \
    ${EXTRA}"]
