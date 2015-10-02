FROM gfredericks/leiningen
MAINTAINER supermomonga

COPY ./ /app

WORKDIR /app

RUN lein uberjar

EXPOSE 8080

CMD java -cp target/omochi-standalone.jar clojure.main -m omochi.core --name omochi --adapter slack