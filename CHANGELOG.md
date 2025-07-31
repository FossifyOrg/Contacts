# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.2.4] - 2025-07-31
### Changed
- Updated translations

### Fixed
- Fixed issue with contacts not displaying when syncing via DAVx⁵ ([#339])

## [1.2.3] - 2025-07-23
### Changed
- All contact exports now use the vCard 4.0 format
- Preference category labels now use sentence case
- Updated translations

### Fixed
- Filtering contacts now works correctly on the favorites tab ([#78])

## [1.2.2] - 2025-06-17
### Changed
- Updated translations

### Fixed
- Fixed invisible preferred number indicator in light themes ([#289])

## [1.2.1] - 2025-06-03
### Changed
- Updated translations

### Fixed
- Fixed crash on startup due to private contacts ([#281])
- Fixed crash when creating new contacts

## [1.2.0] - 2025-05-31
### Added
- Support for structured addresses ([#30])
- Dialog for choosing contact source when editing a merged contact ([#201])

### Changed
- Updated translations

## [1.1.0] - 2024-10-28
### Added
- Added an option to display formatted phone numbers
- Added a favorite button for contacts in groups

### Changed
- Replaced checkboxes with switches
- Other minor bug fixes and improvements
- Added more translations

### Removed
- Removed support for Android 7 and older versions

### Fixed
- Fixed issue with contacts not displaying on Android 14 and above
- Fixed data loss when deleting contacts with identical names
- Fixed corrupted automatic backups
- Fixed low-quality photo exports in vCards
- Fixed overlap between the floating action button and list items

## [1.0.1] - 2024-01-17
### Fixed
- Fixed vcf importer

## [1.0.0] - 2024-01-17
### Added
- Initial release

[#30]: https://github.com/FossifyOrg/Contacts/issues/30
[#78]: https://github.com/FossifyOrg/Contacts/issues/78
[#201]: https://github.com/FossifyOrg/Contacts/issues/201
[#281]: https://github.com/FossifyOrg/Contacts/issues/281
[#289]: https://github.com/FossifyOrg/Contacts/issues/289
[#339]: https://github.com/FossifyOrg/Contacts/issues/339

[Unreleased]: https://github.com/FossifyOrg/Contacts/compare/1.2.4...HEAD
[1.2.4]: https://github.com/FossifyOrg/Contacts/compare/1.2.3...1.2.4
[1.2.3]: https://github.com/FossifyOrg/Contacts/compare/1.2.2...1.2.3
[1.2.2]: https://github.com/FossifyOrg/Contacts/compare/1.2.1...1.2.2
[1.2.1]: https://github.com/FossifyOrg/Contacts/compare/1.2.0...1.2.1
[1.2.0]: https://github.com/FossifyOrg/Contacts/compare/1.1.0...1.2.0
[1.1.0]: https://github.com/FossifyOrg/Contacts/compare/1.0.1...1.1.0
[1.0.1]: https://github.com/FossifyOrg/Contacts/compare/1.0.0...1.0.1
[1.0.0]: https://github.com/FossifyOrg/Contacts/releases/tag/1.0.0
