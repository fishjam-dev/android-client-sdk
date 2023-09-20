
# Jellyfish Android Client

Android client library for [Jellyfish](https://github.com/jellyfish-dev/jellyfish).

## Documentation

Documentation is available [here](https://jellyfish-dev.github.io/android-client-sdk/).

## Installation

Add jitpack repo to your build.gradle:

```gradle
 allprojects {
  repositories {
   ...
   maven { url 'https://jitpack.io' }
  }
 }
```

Add the dependency:

```gradle
 dependencies {
   implementation 'com.github.jellyfish-dev:android-client-sdk:<<version>>'
 }
```

## Usage

Make sure you have:

- Running [Jellyfish](https://github.com/jellyfish-dev/jellyfish) server.
- Created room and token of peer in that room.
  You can use [dashboard](https://jellyfish-dev.github.io/jellyfish-dashboard/) example to create room and peer token.

You can refer to our minimal example on how to use this library.

## Development

1. Set `JELLYFISH_SOCKET_URL` in `~/.gradle/gradle.properties` to your dev backend.
2. Run `./gradlew formatKotlin` to format code.
3. Run `release-it` to release. Follow the prompts, it should add a commit and a git tag and jitpack should pick it up automatically and put the new version in the jitpack repo.

## Contributing

We welcome contributions to this SDK. Please report any bugs or issues you find or feel free to make a pull request with your own bug fixes and/or features.`

## Jellyfish Ecosystem

|             |                                                                                                                                                                                                                                                              |
| ----------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Client SDKs | [React](https://github.com/jellyfish-dev/react-client-sdk), [React Native](https://github.com/jellyfish-dev/react-native-client-sdk), [iOs](https://github.com/jellyfish-dev/ios-client-sdk), [Android](https://github.com/jellyfish-dev/android-client-sdk) |
| Server SDKs | [Elixir](https://github.com/jellyfish-dev/elixir_server_sdk), [Python](https://github.com/jellyfish-dev/python-server-sdk), [OpenAPI](https://jellyfish-dev.github.io/jellyfish-docs/api_reference/rest_api)                                                 |
| Services    | [Videoroom](https://github.com/jellyfish-dev/jellyfish_videoroom) - an example videoconferencing app written in elixir <br/> [Dashboard](https://github.com/jellyfish-dev/jellyfish-dashboard) - an internal tool used to showcase Jellyfish's capabilities   |
| Resources   | [Jellyfish Book](https://jellyfish-dev.github.io/book/) - theory of the framework, [Docs](https://jellyfish-dev.github.io/jellyfish-docs/), [Tutorials](https://github.com/jellyfish-dev/jellyfish-clients-tutorials)                                        |
| Membrane    | Jellyfish is based on [Membrane](https://membrane.stream/), [Discord](https://discord.gg/nwnfVSY)                                                                                                                                                            |
| Compositor  | [Compositor](https://github.com/membraneframework/membrane_video_compositor_plugin) - Membrane plugin to transform video                                                                                                                                     |
| Protobufs   | If you want to use Jellyfish on your own, you can use our [protobufs](https://github.com/jellyfish-dev/protos)                                                                                                                                               |

## Copyright and License

Copyright 2022, [Software Mansion](https://swmansion.com/?utm_source=git&utm_medium=readme&utm_campaign=jellyfish)

[![Software Mansion](https://logo.swmansion.com/logo?color=white&variant=desktop&width=200&tag=membrane-github)](https://swmansion.com/?utm_source=git&utm_medium=readme&utm_campaign=jellyfish)

Licensed under the [Apache License, Version 2.0](LICENSE)
