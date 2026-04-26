# ManualEntryApp

ManualEntryApp is an Android application designed to assist truck drivers in calculating manual entries for their tachograph (tacho) cards. The app helps drivers accurately record the time and date of removal and insertion of their tacho cards, making it easier to document how they traveled to and from their base to their truck.

## Features

- Simple and intuitive user interface
- Input fields for time and date of tacho card removal and insertion
- Calculates the required manual entry for the journey between base and truck
- Helps ensure compliance with driving regulations
- Supports multiple languages (see `res/values-*` folders)

## Use Case

Truck drivers are often required to manually record periods when their tacho card is not inserted in the tachograph, such as when traveling from their base to their truck or vice versa. ManualEntryApp streamlines this process by providing a straightforward way to calculate and document these entries, reducing the risk of errors and saving time.

## Getting Started

### Prerequisites
- Android Studio (latest version recommended)
- Android device or emulator running Android 5.0 (Lollipop) or higher

### Building and Running
1. Clone this repository:
   ```sh
   git clone <repository-url>
   ```
2. Open the project in Android Studio.
3. Build the project using Gradle.
4. Run the app on an emulator or physical device.

## Project Structure
- `app/src/main/java/com/example/manualentryapp/` - Main application source code
- `app/src/main/res/` - Resources (layouts, drawables, strings, etc.)
- `app/src/main/AndroidManifest.xml` - App manifest
- `build.gradle.kts` - Project build configuration

## Contributing
Contributions are welcome! Please open issues or submit pull requests for improvements and bug fixes.

## Disclaimer
This application is provided as an experimental tool to assist with manual tachograph entries. The author does not guarantee the accuracy or legal compliance of the results. Use at your own risk. The author holds no responsibility for any fines, penalties, or legal consequences resulting from the use of this application. Always verify manual entries according to local regulations and consult with relevant authorities if in doubt.

## License
This project is licensed under the MIT License.

## Author
Tomass Sauliškalns - Saulīte
