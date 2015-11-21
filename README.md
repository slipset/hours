# hours
[![Circle CI](https://circleci.com/gh/slipset/hours.svg?style=svg)](https://circleci.com/gh/slipset/hours)
## Prerequisites

You will need [Leiningen][] 2.0.0 or above installed.

[leiningen]: https://github.com/technomancy/leiningen

## Running


### Creating the development database

Hours needs a PostgreSQL database to connect to. Migrations are done
automatically by the application, but you need to create a database
and credentials for the application to use.

    psql
    CREATE USER hours LOGIN;
    ALTER ROLE hours PASSWORD 'mypassword';
    CREATE DATABASE hours WITH OWNER hours;
    \u hours
    CREATE EXTENSION "uuid-ossp";

### Setting up the environment

Hours uses environment variables for its configuration.
The following variables need to be set for the application
to function.


Variable   | Description | Default
-----------|-------------|----------
JDBC_CONNECTION_URL | The JDBC-url used for connecting to the database. See [PostgreSQL JDBC Connections docs](https://jdbc.postgresql.org/documentation/head/connect.html) for more information | None
HOURS_OAUTH2_CLIENT_ID | The OAuth2 client id from Google used for authenticating the users | None
HOURS_OAUTH2_SECRET | The OAuth2 secret key provided by Google | None
HOURS_URI | The base uri for the application. Used to construct the callback uri for oauth2 authentication process | None

#### Creating OAuth credentials

Go to [Google Developers console](https://console.developers.google.com) and follow their instructions.
The callback uri must end with `/oauth2callback`

### Initial run for database migrations

To run the migrations, start the main-method in handler.clj. The easiest way is to run:

    lein run


### Running the server

To start a web server for the application, run:

    lein ring server

## License

Copyright © 2015 FIXME
