package ru.hunkel.mgroupcentral

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*
import ru.hunkel.mgroupcentral.database.dao.entities.Module

class MainActivity : AppCompatActivity() {
    lateinit var mAppRecyclerView: RecyclerView
    private lateinit var mAppAdapter: AppListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mAppRecyclerView = app_list
        //TODO data to list
        mAppRecyclerView.adapter = AppListAdapter(mutableListOf())
        mAppAdapter = mAppRecyclerView.adapter as AppListAdapter
    }


    private class AppListAdapter(private var moduleList: MutableList<Module>) :
        RecyclerView.Adapter<AppListViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppListViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_app, parent, false)
            return AppListViewHolder(view)
        }

        override fun getItemCount(): Int {
            return moduleList.size
        }

        override fun onBindViewHolder(holder: AppListViewHolder, position: Int) {
            holder.bind(moduleList[position])
        }
    }

    private class AppListViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(module: Module) {

        }
    }
}
