FROM openjdk

MAINTAINER 0track

WORKDIR /app
RUN apt-get update && apt-get install -y \
    tar \
    git \
    golang \
    maven \
    build-essential

ADD https://api.github.com/repos/otrack/epaxos/git/refs/heads/master epaxos-version.json
RUN git clone https://github.com/otrack/epaxos
RUN GOPATH=/app/epaxos go get -u github.com/go-redis/redis
RUN GOPATH=/app/epaxos go get -u github.com/google/uuid
RUN GOPATH=/app/epaxos go install client
RUN GOPATH=/app/epaxos go get github.com/sridharv/gojava
RUN GOPATH=/app/epaxos /app/epaxos/bin/gojava -o /app/epaxos/epaxos.jar build bindings
RUN mvn install:install-file -Dfile=/app/epaxos/epaxos.jar -DgroupId=epaxos -DartifactId=epaxos -Dversion=1.0 -Dpackaging=jar

ADD https://api.github.com/repos/vitorenesduarte/VCD-java-client/git/refs/heads/zk vcd-java-client-version.json
RUN git clone -b zk https://github.com/vitorenesduarte/VCD-java-client
RUN mvn -f VCD-java-client clean install -DskipTests

ADD https://api.bitbucket.org/1.0/repositories/tfr/vcd-map vcd-map-version.json
RUN git clone https://bitbucket.org/tfr/vcd-map
RUN make -C vcd-map install

ADD https://api.github.com/repos/otrack/YCSB/git/refs/heads/epaxos ycsb-version.json
RUN git clone -b epaxos https://github.com/otrack/YCSB
RUN mvn -f YCSB -pl com.yahoo.ycsb:epaxos-binding,com.yahoo.ycsb:vcdmap-binding -am clean package -DskipTests
RUN tar zxvf /app/YCSB/epaxos/target/ycsb-epaxos-binding-0.13.0-SNAPSHOT.tar.gz -C /app
RUN tar zxvf /app/YCSB/vcdmap/target/ycsb-vcdmap-binding-0.13.0-SNAPSHOT.tar.gz -C /app
RUN mkdir /app/ycsb-binding-0.13.0-SNAPSHOT
RUN cp -Rf /app/ycsb-epaxos-binding-0.13.0-SNAPSHOT/* /app/ycsb-binding-0.13.0-SNAPSHOT
RUN cp -Rf /app/ycsb-vcdmap-binding-0.13.0-SNAPSHOT/* /app/ycsb-binding-0.13.0-SNAPSHOT

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

RUN rm -Rf /app/YCSB && rm -Rf /root/.m2 && rm -Rf /app/epaxos && rm -Rf /app/vcd-map && rm -Rf VCD-java-client && rm -f /app/*.json && rm -Rf ycsb-epaxos-binding-0.13.0-SNAPSHOT && rm -Rf ycsb-vcdmap-binding-0.13.0-SNAPSHOT

CMD ["sh", "-c", "/app/ycsb-binding-0.13.0-SNAPSHOT/bin/ycsb ${TYPE} ${DB} \
    -P /app/ycsb-binding-0.13.0-SNAPSHOT/workloads/${WORKLOAD} \
    -threads ${THREADS} \
    -p recordcount=${RECORDCOUNT} \
    -p operationcount=${OPERATIONCOUNT} \
    -p host=${HOST} \
    -p port=${PORT} \
    -p leaderless=${LEADERLESS} \
    -p fast=${FAST} \
    ${EXTRA}"]