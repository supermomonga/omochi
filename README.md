# omochi-slackbot

A slack bot written in Clojure to provide benri features.

## Features & Status

- [X] simple pattern responder
- [X] eval clojure code
- [ ] CINDERELLA GIRLS profile search
- [ ] quickhelp

## Usage

### Leiningen

1. Create `profiles.clj`. `profiles.clj.sample` may helps you.
2. Create `.java.policy` file into your home directory. `.java.policy.sample` may helps you.
3. `lein run`

### Docker compose

1. Create `docker-compose.yml`. `docker-compose.yml.sample` may helps you.
2. `docker-compose build`
2. `docker-compose up`

### heroku

Ensure to have following environment variables:

- `SLACK_API_TOKEN`: Bot integration api key
- `BOT_NAME`: Display name for bot

[![Deploy](https://www.herokucdn.com/deploy/button.svg)](https://heroku.com/deploy?template=https://github.com/supermomonga/omochi)


## License

This software is released under the MIT License, see `LICENSE`.

