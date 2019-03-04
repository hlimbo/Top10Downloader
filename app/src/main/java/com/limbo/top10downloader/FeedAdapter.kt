package com.limbo.top10downloader

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class ViewHolder(v: View) {
    val tvName: TextView = v.findViewById(R.id.tvName)
    val tvArtist: TextView = v.findViewById(R.id.tvArtist)
    val tvSummary: TextView = v.findViewById(R.id.tvSummary)
}

// if there are multiple constructors start with the minimum that you need first!
class FeedAdapter(context: Context, private val resource: Int, private val applications: List<FeedEntry>)
    : ArrayAdapter<FeedEntry>(context, resource) {

    // layout inflater -> take the xml representation and produce widgets from them
    // A context is an interface to global information about an application environment, which is an abstract class whose implementation is provided by the Android System
    private val inflater = LayoutInflater.from(context)

    override fun getCount(): Int {
        return applications.size
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // inflates a new view everytime a new view is created...slow and inefficient | creates a new view everytime a view comes in the screen
        //val view = inflater.inflate(resource, parent, false)

        // reuse views ~ don't create new views
        val view: View
        val viewHolder: ViewHolder
        if(convertView == null) {
            view = inflater.inflate(resource, parent, false)
            // findViewById only gets called when view object tag is a viewHolder class instance
            viewHolder = ViewHolder(view)
            // use tag property to store viewHolder in view object
            view.tag = viewHolder
        } else {
            view = convertView
            // grab viewHolder from view.tag property as it returns an object
            viewHolder = view.tag as ViewHolder
        }

//        val tvName: TextView = view.findViewById(R.id.tvName)
//        val tvArtist: TextView = view.findViewById(R.id.tvArtist)
//        val tvSummary: TextView = view.findViewById(R.id.tvSummary)

        val currentApp = applications[position]

        viewHolder.tvName.text = currentApp.name
        viewHolder.tvArtist.text = currentApp.artist
        viewHolder.tvSummary.text = currentApp.summary

        return view
    }
}