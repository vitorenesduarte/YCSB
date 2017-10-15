FROM openjdk

WORKDIR /app
RUN apt-get update && apt-get install -y \
    tar \
    git \
    golang \
    maven
RUN git clone https://github.com/otrack/epaxos
RUN GOPATH=/app/epaxos go get -u github.com/go-redis/redis
RUN GOPATH=/app/epaxos go get -u github.com/google/uuid
RUN GOPATH=/app/epaxos go install client
RUN GOPATH=/app/epaxos go get github.com/sridharv/gojava
RUN GOPATH=/app/epaxos /app/epaxos/bin/gojava -o /app/epaxos/epaxos.jar build bindings
RUN mvn install:install-file -Dfile=/app/epaxos/epaxos.jar -DgroupId=epaxos -DartifactId=epaxos -Dversion=1.0 -Dpackaging=jar

RUN git clone https://github.com/tuanir/YCSB
RUN mvn -f YCSB -pl com.yahoo.ycsb:epaxos-binding -am clean package
RUN tar zxvf YCSB/epaxos/target/ycsb-epaxos-binding-0.13.0-SNAPSHOT.tar.gz -C /app

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

CMD ["sh", "-c", "/app/ycsb-epaxos-binding-0.13.0-SNAPSHOT/bin/ycsb ${TYPE} ${DB} \
    -P /app/ycsb-epaxos-binding-0.13.0-SNAPSHOT/workloads/${WORKLOAD} \
    -threads ${THREADS} \
    -p recordcount=${RECORDCOUNT} \
    -p operationcount=${OPERATIONCOUNT} \
    -p host=${HOST} \
    -p port=${PORT} \
    -p leaderless=${LEADERLESS} \
    -p fast=${FAST}"] \
    ${EXTRA}