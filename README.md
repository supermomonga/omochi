# omochi-slackbot

A slack bot written in Clojure to provide benri features.

## Usage

### Leiningen

1. Create `profiles.clj`. `profiles.clj.sample` may helps you.
2. `lein run`

### Docker compose

1. Create `docker-compose.yml`. `docker-compose.yml.sample` may helps you.
2. `docker-compose build`
2. `docker-compose up`

### Kubernetes

1. Create `omochi-pod.json`. `omoshi-pod.json.sample` may helps you.
2. Create GKE cluster and get credentials. (see GKE document)
2. `kubectl create -f omochi-pod.json`



## License

This software is released under the MIT License, see `LICENSE`.

