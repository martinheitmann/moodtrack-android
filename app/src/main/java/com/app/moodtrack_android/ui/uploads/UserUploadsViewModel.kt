package com.app.moodtrack_android.ui.uploads

import android.app.Application
import android.app.DownloadManager
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.app.moodtrack_android.model.DocumentFile
import com.app.moodtrack_android.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.internal.notify
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject
import kotlin.math.pow


@HiltViewModel
class UserUploadsViewModel @Inject constructor(
    application: Application,
    private val documentRepository: DocumentRepository
) : AndroidViewModel(application) {
    private val context get() = getApplication<Application>()
    val tag = "UserUploadsViewModel"
    val documents = MutableLiveData<List<DocumentFile>>(emptyList())
    val selectedUri = MutableLiveData<Uri?>(null)

    init {
        getAvailableDocuments()
    }

    fun uploadDocument() {
        viewModelScope.launch(Dispatchers.IO) {
            selectedUri.value?.let { uri ->
                val filename = getUriFilename(uri)
                val mimeType = getMimeType(uri)
                val file = getFileFromUri(uri)
                if (mimeType != null && file != null) {
                    documentRepository.uploadDocument(
                        file = file,
                        mimetype = mimeType,
                        filename = filename
                    )
                    getAvailableDocuments()
                }
            }
        }
    }

    private fun getAvailableDocuments() {
        viewModelScope.launch(Dispatchers.IO) {
            val docs = documentRepository.getDocumentList()
            documents.postValue(docs)
        }
    }

    private fun getFileFromUri(uri: Uri): File? {
        val contentResolver = context.contentResolver
        val inputStream = contentResolver.openInputStream(uri)
        inputStream?.let { istr ->
            val filename = getUriFilename(uri)
            val dest = File(context.filesDir.path + File.separatorChar + filename)
            streamToFile(istr, dest)
            return dest
        } ?: run { return null }
    }

    private fun streamToFile(ins: InputStream, destination: File?) {
        try {
            FileOutputStream(destination).use { os ->
                val buffer = ByteArray(4096)
                var length: Int
                while (ins.read(buffer).also { length = it } > 0) {
                    os.write(buffer, 0, length)
                }
                os.flush()
            }
        } catch (exception: Throwable) {
            Log.e(tag, exception.stackTraceToString())
        }
    }

    fun setUri(uri: Uri) {
        selectedUri.postValue(uri)
    }

    fun getUriFileSize(uri: Uri): String {
        var sizeString = "Unknown"
        val cursor: Cursor? = context.contentResolver.query(
            uri, null, null, null, null, null
        )
        cursor?.use {
            if (it.moveToFirst()) {
                val sizeIndex: Int = it.getColumnIndex(OpenableColumns.SIZE)
                sizeString = it.getString(sizeIndex)
            }
        }
        return sizeString
    }

    fun getUriFilename(uri: Uri): String {
        var nameString = "Unknown"
        val cursor: Cursor? = context.contentResolver.query(
            uri, null, null, null, null, null
        )
        cursor?.use {
            if (it.moveToFirst()) {
                nameString = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            }
        }
        return nameString
    }

    fun getMimeType(uri: Uri): String? {
        val cr = context.contentResolver
        return cr.getType(uri)
    }

    private fun bytesToMegaBytes(bytes: Double): Double {
        return bytes / (10.0.pow(6.0))
    }

    private fun bytesToKiloBytes(bytes: Double): Double {
        return bytes / (10.0.pow(3.0))
    }

    fun createFileSizeString(bytes: Double): String {
        var size = bytesToMegaBytes(bytes)
        var suffix = "Mb"
        if (size < 1) {
            size = bytesToKiloBytes(bytes)
            suffix = "Kb"
        }
        return "$size$suffix"
    }

    fun removeSelectedUri() {
        selectedUri.postValue(null)
    }

    fun deleteDocument(index: Int): Unit {
        val document = documents.value?.get(index)
        document?.let { doc ->
            viewModelScope.launch(Dispatchers.IO) {
                documentRepository.deleteDocument(doc.id)
            }
            getAvailableDocuments()
        }
    }

    fun downloadDocument(
        index: Int, onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        Log.d(tag, "downloadDocument called")
        val document = documents.value?.get(index)
        document?.let { doc ->
            viewModelScope.launch(Dispatchers.IO) {
                documentRepository.getDocument(doc.id)?.let { fDoc ->
                    Log.d(tag, "decodeDocFile fDoc was not null")
                    val tempFile = decodedFile(fDoc)
                    tempFile?.let { file ->
                        Log.d(tag, "decodedFile bytes was not null: ${file.name}")
                        val extension = MimeTypeMap.getFileExtensionFromUrl(file.name.replace(" ", ""))
                        extension?.let { ext ->
                            Log.d(tag, "decodeDocFile ext was not null: $ext")
                            val type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
                            type?.let { mimetype ->
                                Log.d(tag, "decodeDocFile mimetype was not null: $mimetype")
                                insertFileIntoDownloads(file, mimetype)
                                onSuccess("Nedlasting fullført. Sjekk nedlastingsmappen i filutforskeren.")
                            } ?: run {
                                onFailure("Kunne ikke identifisere filtypen utfra utvidelsen.")
                            }
                        } ?: run {
                            onFailure("Kan ikke hente filutvidelsen fra den nedlastede filen.")
                        }
                    } ?: run {
                        onFailure("Noe galt skjedde under konvertering av det nedlastet dokumentet.")
                    }
                } ?: run {
                    onFailure("Noe galt skjedde under nedlasting av dokumentet.")
                }
            }
        } ?: run {
            onFailure("Noe er galt med appen eller dokumentet, prøv igjen snenere.")
        }
    }

    private fun decodedFile(doc: DocumentFile): File? {
        Log.d(tag, "decodeDocFile called")
        return try {
            val bytes: ByteArray = Base64.decode(doc.data, Base64.NO_WRAP)
            val destination = File.createTempFile("dokument", doc.filename)
            val outputStream = FileOutputStream(destination)
            outputStream.write(bytes)
            outputStream.close()
            destination
        } catch (exception: Throwable) {
            Log.d(tag, exception.stackTraceToString())
            null
        }
    }

    private fun insertFileIntoDownloads(file: File, mimeType: String) {
        Log.d(tag, "insertFileIntoDownloads called")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues()
            contentValues.put(MediaStore.Downloads.TITLE, file.name)
            contentValues.put(MediaStore.Downloads.DISPLAY_NAME, file.name)
            contentValues.put(MediaStore.Downloads.MIME_TYPE, mimeType)
            contentValues.put(MediaStore.Downloads.SIZE, file.length())

            val resolver: ContentResolver = context.contentResolver
            val fileUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            try {
                fileUri?.let { uri ->
                    resolver.openOutputStream(uri)?.let { outputStream ->
                        outputStream.write(file.readBytes())
                        outputStream.close()
                    }
                }
            } catch (exception: Throwable) {
                fileUri?.let { uri -> resolver.delete(uri, null, null) }
            }
        } else {
            val downloadManager: DownloadManager =
                context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.addCompletedDownload(
                file.name, file.name, true,
                mimeType, file.path, file.length(), true
            )
        }
    }
}