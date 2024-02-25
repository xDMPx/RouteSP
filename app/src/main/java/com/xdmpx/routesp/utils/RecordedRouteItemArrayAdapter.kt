package com.xdmpx.routesp.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.xdmpx.routesp.R

data class RecordedRouteItem(
    val routeID: Int, val routeTitleText: String, val routeDetailsText: String
)

class RecordedRouteItemArrayAdapter(
    context: Context, private val routeItemsList: ArrayList<RecordedRouteItem>
) : ArrayAdapter<RecordedRouteItem>(context, 0, routeItemsList) {

    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        var convertView = view
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(
                R.layout.recorded_route_list_item, parent, false
            )
        }

        val titleText = convertView?.findViewById(R.id.title) as TextView
        val subtitleText = convertView.findViewById(R.id.description) as TextView
        titleText.text = routeItemsList[position].routeTitleText
        subtitleText.text = routeItemsList[position].routeDetailsText

        return convertView
    }

}