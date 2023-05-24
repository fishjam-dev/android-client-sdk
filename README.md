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
  You can use [dashboard](https://github.com/jellyfish-dev/jellyfish-react-client/tree/main/examples/dashboard) example to create room and peer token.

You can refer to our minimal example on how to use this library. 

## Development
1. Set `JELLYFISH_SOCKET_URL` in `~/.gradle/gradle.properties` to your dev backend.
2. Run `./gradlew formatKotlin` to format code.
3. Run `release-it` to release. Follow the prompts, it should add a commit and a git tag and jitpack should pick it up automatically and put the new version in the jitpack repo.

## Credits

This project has been built and is maintained thanks to the support from [Software Mansion](https://swmansion.com).

<img alt="Software Mansion" src="https://logo.swmansion.com/logo?color=white&variant=desktop&width=150&tag=react-native-reanimated-github"/>