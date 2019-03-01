package com.limbo.top10downloader

import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d(TAG, "onCreate called")
        val downloadData = DownloadData()
        downloadData.execute("http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/limit=10/xml")
        Log.d(TAG, "onCreate: done")
    }

    // kotlin's equivalent of static
    companion object {
        private class DownloadData : AsyncTask<String, Void, String>() {
            private val TAG = "DownloadData"
            override fun onPostExecute(result: String?) {
                super.onPostExecute(result)
                Log.d(TAG, "onPostExecute: parameter is $result")
            }

            // runs this task in a background thread
            // can pass multiple arguments in a vararg parameter
            override fun doInBackground(vararg url: String?): String {
                Log.d(TAG, "doInBackground: starts with ${url[0]}")
                val rssFeed = downloadXML(url[0])
                if(rssFeed.isEmpty()) {
                    Log.e(TAG, "doInBackground: Error downloading")
                }
                return rssFeed
            }
        }

        private fun downloadXML(urlPath: String?): String {
            val TAG = "MainActivity"
            val xmlResult = StringBuilder()

            try {
                val url = URL(urlPath)
                val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
                val response = connection.responseCode
                Log.d(TAG, "downloadXML: The response code was $response")

                // note : closing the BufferedReader automatically closes the InputReader which in turn also closes the connection's input stream
                // BufferReader reads 8192 bytes
//                val reader = BufferedReader(InputStreamReader(connection.inputStream))
//                // java way of writing things
//                val inputBuffer = CharArray(500)
//                var charsRead = 0
//                while (charsRead >= 0) {
//                    charsRead = reader.read(inputBuffer) //blocks until data is read from the input buffer
//                    if (charsRead > 0) {
//                        xmlResult.append(String(inputBuffer, 0, charsRead))
//                    }
//                }
//
//                reader.close()

                // kotlin idiomatic way (functional programming style)
                // widely tested in android framework
                // more concise high level readable code
                connection.inputStream.buffered().reader().use { reader ->
                    xmlResult.append(reader.readText())
                }

                Log.d(TAG, "Received ${xmlResult.length} bytes")
                return xmlResult.toString()
//            } catch(e: MalformedURLException) {
//                Log.e(TAG, "downloadXML: Invalid url ${e.message}")
//            } catch(e: IOException) {
//                Log.e(TAG, "downloadXML: 10 exception invalid reading data: ${e.message}")
//            } catch(e: SecurityException) {
//              Log.e(TAG, "downloadXML: Security exception. Needs permissions? ${e.message}")
//            } catch(e: Exception) {
//                Log.e(TAG, "Unknown error: ${e.message}")
//            }
            } catch(e: Exception) {
                val errorMessage: String = when(e) {
                    is MalformedURLException -> "downloadXML: Invalid URL ${e.message}"
                    is IOException -> "downloadXML: IO Exception reading data: ${e.message}"
                    is SecurityException -> {
                        e.printStackTrace()
                        "downloadXML: Security Exception. Needs permission? ${e.message}"
                    }
                    else -> "Unknown error: ${e.message}"
                }
                Log.e(TAG, errorMessage)
            }

            return "" // if it gets to here, there's been a problem, return an empty string
        }
    }
}
