# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.6.0] - 2026-01-30
### Added
- Added support for custom fonts

### Changed
- Tapping contact photo in lists now launches the contact details page ([#452])
- Updated translations

### Fixed
- Fixed incorrect spacing between prefix and last name ([#157])

## [1.5.0] - 2025-12-16
### Changed
- Updated translations

### Fixed
- Fixed invisible navigation bars in contact viewer ([#415])
- Fixed search highlighting for characters with accents and diacritics ([#12])

## [1.4.0] - 2025-10-29
### Changed
- Compatibility updates for Android 15 & 16
- Search query is now preserved when switching tabs
- Updated translations

## [1.3.0] - 2025-10-09
### Added
- Support for importing contacts from vCards shared by other apps ([#321])

### Changed
- Updated translations

### Fixed
- Fixed search not matching full phone numbers

## [1.2.5] - 2025-09-09
### Changed
- Updated translations

### Fixed
- Fixed contacts edits being silently discarded when using navigation arrow ([#360])

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

[#12]: https://github.com/FossifyOrg/Contacts/issues/12
[#30]: https://github.com/FossifyOrg/Contacts/issues/30
[#78]: https://github.com/FossifyOrg/Contacts/issues/78
[#157]: https://github.com/FossifyOrg/Contacts/issues/157
[#201]: https://github.com/FossifyOrg/Contacts/issues/201
[#281]: https://github.com/FossifyOrg/Contacts/issues/281
[#289]: https://github.com/FossifyOrg/Contacts/issues/289
[#321]: https://github.com/FossifyOrg/Contacts/issues/321
[#339]: https://github.com/FossifyOrg/Contacts/issues/339
[#360]: https://github.com/FossifyOrg/Contacts/issues/360
[#415]: https://github.com/FossifyOrg/Contacts/issues/415
[#452]: https://github.com/FossifyOrg/Contacts/issues/452

[Unreleased]: https://github.com/FossifyOrg/Contacts/compare/1.6.0...HEAD
[1.6.0]: https://github.com/FossifyOrg/Contacts/compare/1.5.0...1.6.0
[1.5.0]: https://github.com/FossifyOrg/Contacts/compare/1.4.0...1.5.0
[1.4.0]: https://github.com/FossifyOrg/Contacts/compare/1.3.0...1.4.0
[1.3.0]: https://github.com/FossifyOrg/Contacts/compare/1.2.5...1.3.0
[1.2.5]: https://github.com/FossifyOrg/Contacts/compare/1.2.4...1.2.5
[1.2.4]: https://github.com/FossifyOrg/Contacts/compare/1.2.3...1.2.4
[1.2.3]: https://github.com/FossifyOrg/Contacts/compare/1.2.2...1.2.3
[1.2.2]: https://github.com/FossifyOrg/Contacts/compare/1.2.1...1.2.2
[1.2.1]: https://github.com/FossifyOrg/Contacts/compare/1.2.0...1.2.1
[1.2.0]: https://github.com/FossifyOrg/Contacts/compare/1.1.0...1.2.0
[1.1.0]: https://github.com/FossifyOrg/Contacts/compare/1.0.1...1.1.0
[1.0.1]: https://github.com/FossifyOrg/Contacts/compare/1.0.0...1.0.1
[1.0.0]: https://github.com/FossifyOrg/Contacts/releases/tag/1.0.0
