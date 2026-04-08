package io.mamon.filemapper

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.mamon.filemapper.provider.FileMapperPicker
import kotlinx.coroutines.launch
import kotlin.time.Clock



val employees = List(0) {
    Employee(
        id = it,
        name = "Employee $it",
        department = "Engineering",
        isFavorite = false,
    )
}




@Composable
fun App() {
    MaterialTheme {
        val fileMapper = remember { FileMapper() }
        val scope = rememberCoroutineScope()


        var selectedFileType by remember { mutableStateOf(FileMapperType.XLSX) }
        var mEmployees by remember { mutableStateOf<List<Employee>>(emptyList()) }
        var statusMessage by remember { mutableStateOf("Ready") }

        val flexRenderController = rememberFileMapper<Employee>(
            fileType = selectedFileType,
            onImportSuccess = { employeeList ->
                mEmployees = employeeList
                statusMessage = "Successfully imported ${employeeList.size} employees."
            },
            onExportSuccess = {
                statusMessage = "Successfully exported to $it"
            },
            onImportFailed = { error ->
                statusMessage = error.getLocalizedMessage()
            },
            onExportFailed = {
                statusMessage = "Export Error: ${it.message}"
            }
        )



        DataManagementScreen(
            selectedFileType = selectedFileType,
            onFileTypeSelected = { selectedFileType = it },
            statusMessage = statusMessage,
            employees = mEmployees,
            onImportClick = {


                scope.launch {
                    val file = FileMapperPicker.pickFile(type = selectedFileType)
                    if (file != null){
                        fileMapper.importData<Employee>(
                            bytes = file.readBytes(),
                            fileType = selectedFileType , // should be removed
                            ignoreColumns = emptySet(),
                            onSuccess = {
                                mEmployees = it
                                statusMessage = "Successfully imported ${it.size} employees."
                            },
                            onFailed = {
                                statusMessage = it.getLocalizedMessage()
                            }
                        )
                    }
                }

            },
            onExportClick = {

                flexRenderController.export(
                    data = mEmployees,
                    fileName = "ExportedEmployees" + Clock.System.now().toEpochMilliseconds().toString()
                )

            }
        )


    }
}




@Composable
fun DataManagementScreen(
    selectedFileType: FileMapperType,
    onFileTypeSelected: (FileMapperType) -> Unit,
    statusMessage: String,
    employees: List<Employee>,
    onImportClick: () -> Unit,
    onExportClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Data Management",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text(text = "Format: ", fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.width(8.dp))

            RadioButton(
                selected = selectedFileType == FileMapperType.XLSX,
                onClick = { onFileTypeSelected(FileMapperType.XLSX) }
            )
            Text(text = "Excel")

            Spacer(modifier = Modifier.width(16.dp))

            RadioButton(
                selected = selectedFileType == FileMapperType.JSON,
                onClick = { onFileTypeSelected(FileMapperType.JSON) }
            )
            Text(text = "JSON")
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onImportClick,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "Import ${selectedFileType.name}")
            }

            Button(
                onClick = onExportClick,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "Export ${selectedFileType.name}")
            }
        }

        Text(
            text = statusMessage,
            color = if (statusMessage.contains("Error") || statusMessage.contains("Failed"))
                MaterialTheme.colorScheme.error
            else
                MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            style = MaterialTheme.typography.bodyMedium
        )

        HorizontalDivider()


        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Employee Directory (${employees.size})",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(employees) { emp ->
                ListItem(
                    headlineContent = { Text(emp.name) },
                    supportingContent = {
                     Column {
                         Text("Department: ${emp.department}")
                         Text("isFavorite: ${emp.isFavorite}")
                     }
                                        },
                    leadingContent = {
                        Text(
                            text = "#${emp.id}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )

                    },
                    tonalElevation = 2.dp,
                    shadowElevation = 1.dp
                )
            }
        }
    }
}


@Composable
@Preview(showBackground = true)
fun DataManagementPreview(){
    DataManagementScreen(
        selectedFileType = FileMapperType.XLSX,
        onFileTypeSelected = {},
        statusMessage = "Ready",
        employees = employees,
        onImportClick = {},
        onExportClick = {}
    )
}
