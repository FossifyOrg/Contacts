# Fossify Contacts

Fossify Contacts is an open-source Android app for managing contacts, favorites, and groups.
The app interface is available in English (United States).

## Features

- Search and organize contacts, including favorites and groups
- Customize which contact fields are displayed
- Import and export contacts in vCard (`.vcf`) format
- Back up contacts automatically
- Choose from light, dark, and themed interfaces

## Build

Build the Foss variant's debug APK with Gradle:

```shell
./gradlew :app:assembleFossDebug
```

The project also provides a `gplay` product flavor. Android SDK components required by the project must be installed to build either variant.

## License

This project is licensed under the terms in [LICENSE](LICENSE).
