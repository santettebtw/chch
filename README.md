# chch

> A simple, Java TCP socket-based chat protocol implemented with a REPL terminal interface.

## Table of Contents

- [Features](#features)
- [Installation](#installation)
- [Docker](#docker)
    - [Build and Publish with Docker](#build-and-publish-with-docker)
    - [Use with Docker](#use-with-docker)
- [Usage](#usage)
    - [CLI Usage](#cli-usage)
    - [Client REPL Usage - / (slash) commands](#client-repl-usage----slash-commands)
- [Architecture](#architecture)
- [Protocol Definition](#protocol-definition)
- [Future Enhancements](#future-enhancements)
- [Authors](#authors)

## Features

- **List channels**: see what channels are available on the server you connected on
- **Join channels**: join channels to structure communication topics
- **Communicate with peers**: communicate with anyone on the same channel
- **Change channels and username**: change channels and username
- **Channel chat history**: chat history of the channels is saved on the server
- **Channel management**: the server can create and delete channels

## Installation

### Prerequisites

- Java 11 or higher
- Maven 3.5 or higher

### Clone

```bash
$ git clone https://github.com/santettebtw/chch.git
$ cd chch
```

### Build

Make sure you have JDK 21 installed and set as your active Java version.

```bash
# you can build the project using the maven wrapper (recommended):
./mvnw clean compile
./mvnw package
```

```bash
# alternatively, if you already have maven installed locally:
mvn clean compile
mvn package
```

### Run

```bash
java -jar target/java-tcp-programming-1.0-SNAPSHOT.jar
```

## Docker

### Build and Publish with Docker

To build and publish the Docker image to GitHub Container Registry, follow these steps:

**1. Build the Docker image:**

```bash
docker build -t chch .
```

**2. Tag the image for GitHub Container Registry:**

Replace `<YOUR_GITHUB_USERNAME>` with your GitHub username:

```bash
docker tag chch ghcr.io/<YOUR_GITHUB_USERNAME>/chch:latest
```

**3. Login to GitHub Container Registry:**

You'll need a Personal Access Token (PAT) with `write:packages` permission. Create one at: https://github.com/settings/tokens (Settings > Developer settings > Personal access tokens > Tokens (classic))

```bash
docker login ghcr.io -u <YOUR_GITHUB_USERNAME>
```

When prompted for the password, enter your Personal Access Token.

**4. Publish the image to GitHub Container Registry:**

```bash
docker push ghcr.io/<YOUR_GITHUB_USERNAME>/chch:latest
```

The image will be private by default. You can change the visibility in the package settings on GitHub at: `https://github.com/<YOUR_GITHUB_USERNAME>?tab=packages`

### Use with Docker

You can run the application using Docker without installing Java or Maven on your computer.

**1. Run the server:**

```bash
docker run -p 4269:4269 ghcr.io/<YOUR_GITHUB_USERNAME>/chch:latest server -p=4269
```

Replace `<YOUR_GITHUB_USERNAME>` with your GitHub username. The `-p 4269:4269` option maps port 4269 from the container to port 4269 on your host machine.

**2. Run the client:**

```bash
docker run -it ghcr.io/<YOUR_GITHUB_USERNAME>/chch:latest client -H=host.docker.internal -p=4269
```

Replace `<YOUR_GITHUB_USERNAME>` with your GitHub username. The `-it` option allows interactive mode for the REPL. Use `host.docker.internal` to connect to a server running on your host machine.

**Note:** If the image is private, you'll need to authenticate first:

```bash
docker login ghcr.io -u <YOUR_GITHUB_USERNAME>
```

## Usage

### CLI Usage

**1. Run the server**:

```bash
java -jar target/java-tcp-programming-1.0-SNAPSHOT.jar server [-p=<port>]
```

**Options**

- `-p`: the port the server listens on (optional, default `4269`)

**Example**

```bash
java -jar target/java-tcp-programming-1.0-SNAPSHOT.jar server -p=5599
```
**2. Run the client**
```bash
java -jar target/java-tcp-programming-1.0-SNAPSHOT.jar client -H=<host> [-p=<port>]
```

**Options**

- `-H`: the host to connect to (required)
- `-p`: the port to connect to (optional, default `4269`)

**Example**

```bash
java -jar target/java-tcp-programming-1.0-SNAPSHOT.jar client -H=localhost -p=5599
```

### Client REPL Usage - / (slash) commands

Once in the client is running, you can use the following `/` (shalsh) commands:
- `/join <channel> <username>`: join a channel under the given username
- `/exit`: exit the REPL
- `/nick`: change your username for the current channel

## Architecture

The channel messge history is currently sored in `data/<channelname>.txt`, that contains plain text representation of the message history per channel.

## Protocol Definition

Please find the protocol definition in [CHCH_PROTOCOL.md](./CHCH_PROTOCOL.md).

## Future Enhancements

- **Better client REPL**: currently really messy, async message recieving was challenge for the time we had
- **Server REPL**: a server REPL to make it easy to create new channels on the fly and maybe add the ability to kick or ban a user
- **Storage**: currently storing in plain text files, could be rethought...

## Authors

- [Maxime Regenass](https://github.com/maxregenassPro)
- [Santiago Sugranes](https://github.com/santettebtw)
