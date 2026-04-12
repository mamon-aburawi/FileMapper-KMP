# FileMapper KMP

![Kotlin](https://img.shields.io/badge/kotlin-multiplatform-blue.svg?logo=kotlin)
![Compose](https://img.shields.io/badge/Compose-Multiplatform-green.svg)
![Android](https://img.shields.io/badge/Android-3DDC84.svg?logo=android&logoColor=white)
![iOS](https://img.shields.io/badge/iOS-000000.svg?logo=apple&logoColor=white)
![Desktop](https://img.shields.io/badge/Desktop-4A4A55.svg)
![Web](https://img.shields.io/badge/Web-654FF0.svg?logo=webassembly&logoColor=white)
![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Version](https://img.shields.io/badge/version-1.0.0-orange.svg)

A lightweight, powerful **Kotlin Multiplatform (KMP)** library designed to seamlessly map files (XLSX, JSON) to Kotlin Data Classes. Supporting Android, iOS, Desktop, and Web (Wasm/JS), it provides both Compose-ready UI triggers and pure logic-based APIs.

-----

## 🚀 Features

| Feature | Description |
| :--- | :--- |
| **Multi-Format** | Support for `.xlsx` and `.json` file types. |
| **Type-Safe Mapping** | Automatically converts file rows/objects into Kotlin Data Classes. |
| **Platform-Agnostic** | Unified API for Android, iOS, Desktop (JVM), and Web (Wasm/JS). |
| **Compose Integration** | Pre-built Composable controller for easy state management. |
| **Flexible Logic** | Pure `suspend` functions for Use Cases and ViewModels. |
| **Excel Control** | Use annotations to map specific columns or ignore unmapped data. |

-----

## 📦 Setup

Add the dependency to your `commonMain` source set in `build.gradle.kts`:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.mamon-aburawi:filemapper-kmp:{last_version}")
        }
    }
}
```

-----

## 🛠 Implementation

### 1\. Define your Data Model

Ensure your data class is annotated with `@Serializable` (from `kotlinx.serialization`) and use `@ColumnName` to link Excel headers.

```kotlin
import kotlinx.serialization.Serializable
import io.mamon.filemapper.ColumnName

@Serializable
data class Employee(
    @ColumnName("Full Name") // Matches Excel Header
    val name: String,
    
    @ColumnName("Email Address")
    val email: String,

    @ColumnName("Age")
    val age: Int 
)
```

### 2\. Usage in Compose (UI)

Use the `rememberFileMapper` controller for a seamless UI experience. It handles picking, importing, and exporting in one place.

```kotlin
val fileMapperController = rememberFileMapper<Employee>(
    fileType = selectedFileType,
    onImportSuccess = { employeeList ->
        mEmployees = employeeList
        statusMessage = "Successfully imported ${employeeList.size} employees."
    },
    onExportSuccess = { savedPath ->
        statusMessage = "Successfully exported to $savedPath"
    },
    onImportFailed = { error ->
        statusMessage = error.getLocalizedMessage()
    },
    onExportFailed = { error ->
        statusMessage = "Export Error: ${error.getLocalizedMessage()}"
    }
)

// Trigger the picker
Button(onClick = { fileMapperController.import() }) {
    Text("Import Employees")
}

// Trigger the export
Button(onClick = { fileMapperController.export(mEmployees, "YourFileName") }) {
    Text("Export to Excel")
}
```

### 3\. Usage in Non-Compose (Logic)

Trigger picking and mapping from a ViewModel or shared logic using the `FileMapperPicker` and `importData` API.

```kotlin
scope.launch {
    // 1. Pick the file using the platform picker
    val file = FileMapperPicker.pickFile(type = selectedFileType)
    
    if (file != null) {
        // 2. Read bytes and import data
        fileMapper.importData<Employee>(
            bytes = file.readBytes(),
            ignoreColumns = emptySet(),
            onSuccess = { employeeList ->
                mEmployees = employeeList
                statusMessage = "Successfully imported ${employeeList.size} employees."
            },
            onFailed = { error ->
                statusMessage = error.getLocalizedMessage()
            }
        )
    }
}
```

-----

## 📑 Annotations & Configuration

| Annotation / Property | Description |
| :--- | :--- |
| `@Serializable` | Required for the engine to parse the data class structure. |
| `@ColumnName("Name")` | Maps a specific Excel header string to a Kotlin property. |
| `ignoreUnmappedColumns` | used if you want to ignore the columns in the excel file that does not mapped in data class. |
| `ignoreColumns` | Set of strings to skip during the export/import process. |

-----

## ⚠️ Errors & Exceptions

The library uses a specialized `FileMapperException` to provide localized feedback.

| Error Code | Meaning |
| :--- | :--- |
| `EMPTY_FILE` | The selected file is empty. |
| `INVALID_FORMAT` | Invalid file format. Expected XLSX or JSON. |
| `READ_FAILED` | Failed to read the selected file. |
| `EXPORT_FAILED` | An error occurred during export. |
| `NO_DATA_ROWS` | The file contains no data rows. |
| `ALL_COLUMNS_IGNORED` | All columns for the target data class are ignored. |
| `MISSING_COLUMNS` | Missing Required Columns. |
| `UNEXPECTED_COLUMNS` | Unexpected Columns found. |
| `UNEXPECTED_ERROR` | An unexpected error occurred. |

### Localized Messages

Retrieve error messages in the user's system language (English/Arabic) automatically:

```kotlin
// Works in both Compose and Non-Compose (Suspend) contexts
val message = exception.getLocalizedMessage() 
```

-----

## 🌟 Support the Library

If this library helped you save time on your KMP project, please consider giving it a ⭐ It helps more developers find the project.



-----

*Developed by [Mamon Aburawi](https://github.com/mamon-aburawi) — 2026*
