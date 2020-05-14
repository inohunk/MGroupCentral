package io

import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter

const val DEFAULT_FILENAME = "beacons"
const val TAG = "Logger"

class Logger(
    //Root directory
    private val rootPath: String,
    private val fileName: String = Build.MODEL
) {

    //Current file path
    private var currentFilePath: String = ""

    //Shows if stream is open
    private var isCanWritten = false

    //Opened file
    private var file: File? = null

    //Current file counter
    private var currentFileCounter = -1

    //Output stream writer
    private var outputStreamWriter: OutputStreamWriter? = null

    companion object {
        @JvmStatic
        fun getFilePaths(rootPath: String): Array<String> {
//            val list = mutableListOf<String>()
//            var currentFileCounter = 0
            val file = File(rootPath)
            return file.list()!!
//            while (true) {
//                val file = File(rootPath, "${DEFAULT_FILENAME}_${Build.MODEL}_$currentFileCounter")
//                if (file.exists().not()) {
//                    break
//                }
//                list.add(file.absolutePath)
//                currentFileCounter++
//            }
//            return list
        }
    }

    /*
        Public methods
     */
    fun write(msg: String) {
        doWrite(msg)
    }

    fun close() {
        outputStreamWriter?.close()
        isCanWritten = false
    }


    /*
        Private methods
     */
    private fun doWrite(msg: String) {
        if (isCanWritten) {
            outputStreamWriter?.write(msg)
            outputStreamWriter?.flush()
            Log.i(TAG, "Written to file")
        } else {
            val fileCounter = getLastFileCounter()
            file = File(rootPath, "${DEFAULT_FILENAME}_${fileName}_${fileCounter}")
            if (file!!.exists().not()) {
                file!!.createNewFile()
            }
            currentFilePath = file!!.absolutePath
            Log.i(TAG, currentFilePath)
            Log.i(TAG, fileCounter.toString())
            outputStreamWriter = OutputStreamWriter(FileOutputStream(file, true))
            isCanWritten = true
            write(msg)
        }
    }

    private fun getLastFileCounter(): Int {
        currentFileCounter = 0
        while (true) {
            val file = File(rootPath, "${DEFAULT_FILENAME}_${fileName}_$currentFileCounter")
            if (file.exists().not()) {
                return currentFileCounter
            }
            currentFileCounter++
        }
    }
}