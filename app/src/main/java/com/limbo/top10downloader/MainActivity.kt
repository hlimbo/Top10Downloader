package com.limbo.top10downloader

import android.content.Context
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ListView
import kotlinx.android.synthetic.main.activity_main.*
import java.net.URL
import kotlin.properties.Delegates

// Read about to prevent data from being downloaded again
// Data being downloaded again:
//  * could waste data if person holds limited data plan
// https://developer.android.com/training/efficient-downloads/redundant_redundant.html

class FeedEntry {
    var name: String = ""
    var artist: String = ""
    var releaseDate: String = ""
    var summary: String = ""
    var imageURL: String = ""
    override fun toString(): String {
        return """
            name = $name
            artist = $artist
            releaseDate = $releaseDate
            imageURL = $imageURL
        """.trimIndent()
    }
}

const val FEED_URL = "FEED_URL"
const val FEED_LIMIT = "FEED_LIMIT"
const val DEFAULT_FEED_URL = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/limit=%d/xml"
const val DEFAULT_FEED_LIMIT = 10

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    // this line of code won't work because Android Studio hasn't set the contentView for the layout that references the xmlListView yet
    // as its view is inflated when onCreate method is called
    //private val downloadData = DownloadData(this, xmlListView)
    // private val downloadData by lazy { DownloadData(this, xmlListView) }
    private var downloadData: DownloadData? = null
    private var feedUrl: String = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/limit=%d/xml"
    private var feedLimit = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d(TAG,"onCreate begin")

        // restore state here since onrestoreinstancestate gets called after oncreate gets called and downloading of data happens here..
        if(savedInstanceState != null) {
            Log.d(TAG, "onCreate: restoring instance state")
            with(savedInstanceState) {
                feedUrl = getString(FEED_URL, DEFAULT_FEED_URL)
                feedLimit = getInt(FEED_LIMIT, DEFAULT_FEED_LIMIT)
            }
        }

        // %d in feedUrl is replaced with feedLimit value
        downloadUrl(feedUrl.format(feedLimit))
        Log.d(TAG, "onCreate done")
    }

    private fun downloadUrl(feedUrl: String) {
        Log.d(TAG, "downloadUrl starting AsyncTask")
        // Async Tasks can only be executed once! so we must create a new instance of Async Task if we want to reuse it
        downloadData = DownloadData(this, xmlListView)
        downloadData?.execute(feedUrl)
        Log.d(TAG, "downloadUrl done")
    }

    // called when main menu view is inflated (rendered on android screen)
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.feeds_menu, menu)
        Log.d(TAG, "onCreateOptionsMenu called")
        if(feedLimit == 10) {
            menu?.findItem(R.id.menu10)?.isChecked = true
        } else {
            menu?.findItem(R.id.menu25)?.isChecked = true
        }

        return true
    }

    // Tip: don't go changing the parameters unless you know what you are doing!
    // Prefer to use elvis operator ? safe call operator on nullable types
    // called when an item from the menu list is selected by the user
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val oldFeedUrl = String(feedUrl.toCharArray())
        val oldFeedLimit: Int = feedLimit
        when(item.itemId) {
            R.id.menuFree ->
                feedUrl = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/limit=%d/xml"
            R.id.menuPaid ->
                feedUrl = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/toppaidapplications/limit=%d/xml"
            R.id.menuSongs ->
                feedUrl = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topsongs/limit=%d/xml"
            R.id.menu10, R.id.menu25 -> {
                // radio button checks (radio button unchecks the other radio button if this current radio button is selected)
                if(!item.isChecked) {
                    item.isChecked = true
                    // subtract from the sum of the 10 and 25 to get the other top x page
                    feedLimit = 35 - feedLimit
                    Log.d(TAG, "onOptionsItemSelected: ${item.title} setting feedLimit to $feedLimit")
                } else {
                    Log.d(TAG, "onOptionsItemSelected: ${item.title} setting feedLimit unchanged")
                }
            }
            R.id.menuRefresh -> {
                Log.d(TAG, "onOptionsItemSelected: menuRefresh selected")
            }
            else ->
                return super.onOptionsItemSelected(item)
        }

        // prevents redownloading the same set of data if the same feed url was selected
        if(oldFeedUrl != feedUrl || oldFeedLimit != feedLimit || item.itemId == R.id.menuRefresh) {
            if(item.itemId != R.id.menuRefresh)
                Log.d(TAG, "onOptionsItemSelected: different item was selected: $feedUrl | new feed limit: $feedLimit")
            downloadUrl(feedUrl.format(feedLimit))
        } else {
            Log.d(TAG, "onOptionsItemSelected: same item was selected no need to perform an asynctask to redownload")
        }
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        // cancels the async task if it hasn't loaded all of the data yet..
        // stops async task from running when this activity is destroyed otherwise, this async task will continue running to completion in the new Activity instance
        // but the new activity will not hold reference to the old  instance of the async task (memory leak)
        // will only be cancelled if the first network call made in doInBackground() method returns (function does not run for a long time)
        downloadData?.cancel(true)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        // saves the view hierarchy state (e.g. editText value is saved when screen orientation is changed)
        super.onSaveInstanceState(outState)

        outState?.putString(FEED_URL, feedUrl)
        outState?.putInt(FEED_LIMIT, feedLimit)

        Log.d(TAG, "onSaveInstanceState called")
    }

    // kotlin's equivalent of static
    // changed this into a companion object to avoid memory leaks
    companion object {
        // An AsyncTask allows you to perform background operations and publish results on thet UI thread without having to manipulate threads and/or handlers
        // AsyncTasks should be used for short operations (a few seconds at most)
        // AsyncTask runs on the background thread whose result is published on the UI thread
        // runs in the background so that the Main UI thread is not blocked by the network call being made to retrieve the xml data
        private class DownloadData(context: Context, listView: ListView) : AsyncTask<String, Void, String>() {
            private val TAG = "DownloadData"

            // defines these props as weak references (to prevent memory leaks in garbage collection)
            var propContext: Context by Delegates.notNull()
            var propListView: ListView by Delegates.notNull()

            init {
                propContext = context
                propListView = listView
            }

            override fun onPostExecute(result: String) {
                super.onPostExecute(result)
                // Log.d(TAG, "onPostExecute: parameter is $result")
                val parseApplications = ParseApplications()
                parseApplications.parse(result)

                // 1st paramter: activity -> mainActivity instance
                // 2nd parameter: resource containing the text view which the array adapter will use to put data to put into
                // 3 parameter: array list of data to put into each list item
                val feedAdapter = FeedAdapter(propContext, R.layout.list_record, parseApplications.applications)
                propListView.adapter = feedAdapter
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
            // kotlin allows extension methods to be attached to existing classes
            return URL(urlPath).readText()
        }

        // long way of doing the same thing above ^
//        private fun downloadXML(urlPath: String?) : String {
//            val TAG = "MainActivity"
//            val xmlResult = StringBuilder()
//
//            try {
//                val url = URL(urlPath)
//                val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
//                val response = connection.responseCode
//                Log.d(TAG, "downloadXML: The response code was $response")
//
//                // note : closing the BufferedReader automatically closes the InputReader which in turn also closes the connection's input stream
//                // BufferReader reads 8192 bytes
////                val reader = BufferedReader(InputStreamReader(connection.inputStream))
////                // java way of writing things
////                val inputBuffer = CharArray(500)
////                var charsRead = 0
////                while (charsRead >= 0) {
////                    charsRead = reader.read(inputBuffer) //blocks until data is read from the input buffer
////                    if (charsRead > 0) {
////                        xmlResult.append(String(inputBuffer, 0, charsRead))
////                    }
////                }
////
////                reader.close()
//
//                // kotlin idiomatic way (functional programming style)
//                // widely tested in android framework
//                // more concise high level readable code
//                connection.inputStream.buffered().reader().use { reader ->
//                    xmlResult.append(reader.readText())
//                }
//
//                Log.d(TAG, "Received ${xmlResult.length} bytes")
//                return xmlResult.toString()
////            } catch(e: MalformedURLException) {
////                Log.e(TAG, "downloadXML: Invalid url ${e.message}")
////            } catch(e: IOException) {
////                Log.e(TAG, "downloadXML: 10 exception invalid reading data: ${e.message}")
////            } catch(e: SecurityException) {
////              Log.e(TAG, "downloadXML: Security exception. Needs permissions? ${e.message}")
////            } catch(e: Exception) {
////                Log.e(TAG, "Unknown error: ${e.message}")
////            }
//            } catch(e: Exception) {
//                val errorMessage: String = when(e) {
//                    is MalformedURLException -> "downloadXML: Invalid URL ${e.message}"
//                    is IOException -> "downloadXML: IO Exception reading data: ${e.message}"
//                    is SecurityException -> {
//                        e.printStackTrace()
//                        "downloadXML: Security Exception. Needs permission? ${e.message}"
//                    }
//                    else -> "Unknown error: ${e.message}"
//                }
//                Log.e(TAG, errorMessage)
//            }
//
//            return "" // if it gets to here, there's been a problem, return an empty string
//        }
    }
}
