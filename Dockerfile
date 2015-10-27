FROM gfredericks/leiningen
MAINTAINER supermomonga

RUN mkdir /app
COPY ./project.clj /app/project.clj
WORKDIR /app
RUN lein deps

COPY ./ /app

COPY ./.java.policy /root/.java.policy

RUN lein uberjar

EXPOSE 8080

CMD lein run