package io.mamon.filemapper.engine


import kotlin.text.Regex


@PublishedApi
internal object FileMapperXlsxEngine {

    fun parseXlsxFilesAsSequence(files: Map<String, ByteArray>): Sequence<Map<String, String>> = sequence {
            val rawSharedStringsXml = files["xl/sharedStrings.xml"]?.decodeToString() ?: ""
            val rawSheetXml = files["xl/worksheets/sheet1.xml"]?.decodeToString() ?: return@sequence

            val sharedStringsXml = rawSharedStringsXml.replace(Regex("""<(?:[a-zA-Z0-9]+:)?t\b[^>]*/>"""), "<t></t>")
            val sheetXml = rawSheetXml.replace(Regex("""<(?:[a-zA-Z0-9]+:)?v\b[^>]*/>"""), "<v></v>")

            val sharedStrings = mutableListOf<String>()

            val siRegex = Regex("""<(?:[a-zA-Z0-9]+:)?si\b[^>]*>([\s\S]*?)</(?:[a-zA-Z0-9]+:)?si>""")
            val tTagRegex = Regex("""<(?:[a-zA-Z0-9]+:)?t\b[^>]*>([\s\S]*?)</(?:[a-zA-Z0-9]+:)?t>""")

            siRegex.findAll(sharedStringsXml).forEach { siMatch ->
                val siContent = siMatch.value
                val text = tTagRegex.findAll(siContent)
                    .joinToString("") { it.groupValues[1].decodeXml() }
                    .replace(Regex("[\\p{C}\\p{Z}]"), " ")
                    .trim()
                sharedStrings.add(text)
            }

            val rowRegex = Regex("""<(?:[a-zA-Z0-9]+:)?row[^>]*>([\s\S]*?)</(?:[a-zA-Z0-9]+:)?row>""")
            val cellRegex = Regex("""<(?:[a-zA-Z0-9]+:)?c\b[^>]*/>|<(?:[a-zA-Z0-9]+:)?c\b[^>]*>[\s\S]*?</(?:[a-zA-Z0-9]+:)?c>""")
            val colLetterRegex = Regex("""r="([A-Z]+)\d+"""")
            val typeRegex = Regex("""t="([^"]+)"""")
            val valueRegex = Regex("""<(?:[a-zA-Z0-9]+:)?v[^>]*>([\s\S]*?)</(?:[a-zA-Z0-9]+:)?v>""")

            val headersMap = mutableMapOf<String, String>()
            var isFirstRow = true

            rowRegex.findAll(sheetXml).forEach { rowMatch ->
                val rowContent = rowMatch.groupValues[1]
                val rowCells = mutableMapOf<String, String>()

                cellRegex.findAll(rowContent).forEach { cellMatch ->
                    val cellXml = cellMatch.value
                    val colLetter = colLetterRegex.find(cellXml)?.groupValues?.get(1) ?: ""
                    val type = typeRegex.find(cellXml)?.groupValues?.get(1)
                    val value = valueRegex.find(cellXml)?.groupValues?.get(1)?.trim() ?: ""

                    val finalValue = when (type) {
                        "s" -> {
                            val idx = value.toIntOrNull() ?: -1
                            if (idx in sharedStrings.indices) sharedStrings[idx] else value
                        }
                        "b" -> {
                            if (value == "1") "true" else "false"
                        }
                        else -> {
                            value.replace(Regex("[\\p{C}\\p{Z}]"), "")
                        }
                    }

                    if (colLetter.isNotEmpty()) {
                        rowCells[colLetter] = finalValue
                    }
                }

                if (rowCells.isNotEmpty()) {
                    if (isFirstRow) {
                        val sortedLetters = rowCells.keys.sortedWith(compareBy({ it.length }, { it }))
                        sortedLetters.forEach { letter ->
                            headersMap[letter] = rowCells[letter] ?: ""
                        }
                        isFirstRow = false
                    } else {
                        val mappedRow = mutableMapOf<String, String>()
                        headersMap.forEach { (letter, name) ->
                            mappedRow[name] = rowCells[letter] ?: ""
                        }
                        yield(mappedRow)
                    }
                }
            }
        }


    private fun String.decodeXml() = this
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")


    fun createXlsxFiles(headers: List<String>, dataMatrix: List<List<String>>): Map<String, ByteArray> {
        val sharedStrings = mutableListOf<String>()
        val stringMap = mutableMapOf<String, Int>()


        fun getStringIndex(value: String): Int {
            return stringMap.getOrPut(value) {
                sharedStrings.add(value)
                sharedStrings.size - 1
            }
        }

        val sheetXml = buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
            append("""<sheetData>""")

            // Write Headers (Row 1)
            append("""<row r="1">""")
            headers.forEachIndexed { colIndex, header ->
                val cellRef = "${getExcelColumnName(colIndex)}1"
                val strIdx = getStringIndex(header)
                append("""<c r="$cellRef" t="s"><v>$strIdx</v></c>""")
            }
            append("""</row>""")

            dataMatrix.forEachIndexed { rowIndex, rowData ->
                val excelRow = rowIndex + 2
                append("""<row r="$excelRow">""")
                rowData.forEachIndexed { colIndex, value ->
                    val cellRef = "${getExcelColumnName(colIndex)}$excelRow"

                    // Check if value is a number to store it natively, otherwise store as string
                    val doubleVal = value.toDoubleOrNull()
                    if (doubleVal != null) {
                        append("""<c r="$cellRef" t="n"><v>$value</v></c>""")
                    } else {
                        val strIdx = getStringIndex(value)
                        append("""<c r="$cellRef" t="s"><v>$strIdx</v></c>""")
                    }
                }
                append("""</row>""")
            }

            append("""</sheetData></worksheet>""")
        }

        val sharedStringsXml = buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            append("""<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="${sharedStrings.size}" uniqueCount="${sharedStrings.size}">""")
            sharedStrings.forEach { str ->
                // Escape XML characters safely
                val safeStr = str.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&apos;")
                append("""<si><t>$safeStr</t></si>""")
            }
            append("""</sst>""")
        }

        val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                <Default Extension="xml" ContentType="application/xml"/>
                <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                <Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>
            </Types>""".trimIndent()

        val rootRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
            </Relationships>""".trimIndent()

        val workbook = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                <sheets>
                    <sheet name="Sheet1" sheetId="1" r:id="rId1"/>
                </sheets>
            </workbook>""".trimIndent()

        val workbookRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/>
            </Relationships>""".trimIndent()

        return mapOf(
            "[Content_Types].xml" to contentTypes.encodeToByteArray(),
            "_rels/.rels" to rootRels.encodeToByteArray(),
            "xl/workbook.xml" to workbook.encodeToByteArray(),
            "xl/_rels/workbook.xml.rels" to workbookRels.encodeToByteArray(),
            "xl/sharedStrings.xml" to sharedStringsXml.encodeToByteArray(),
            "xl/worksheets/sheet1.xml" to sheetXml.encodeToByteArray()
        )
    }

    private fun getExcelColumnName(columnIndex: Int): String {
        var index = columnIndex
        var columnName = ""
        while (index >= 0) {
            columnName = ('A' + (index % 26)) + columnName
            index = (index / 26) - 1
        }
        return columnName
    }



}



