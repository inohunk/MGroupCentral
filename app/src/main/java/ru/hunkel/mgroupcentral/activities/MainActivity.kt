package ru.hunkel.mgroupcentral.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.item_app.view.*
import ru.hunkel.mgroupcentral.R
import ru.hunkel.mgroupcentral.database.dao.entities.Module
import ru.hunkel.mgroupcentral.managers.MGroupDatabaseManager

const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    lateinit var mAppRecyclerView: RecyclerView
    private lateinit var mAppAdapter: AppListAdapter
    lateinit var mDatabaseManager: MGroupDatabaseManager

    private var mModulesList: MutableList<Module> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        update_modules_list_button.setOnClickListener {
            syncListWithDatabase()
        }

        mDatabaseManager = MGroupDatabaseManager(this)
        mAppRecyclerView = app_list
        mAppRecyclerView.layoutManager = LinearLayoutManager(this)

        mModulesList = mDatabaseManager.actionGetAllModules().toMutableList()

        mAppRecyclerView.adapter = AppListAdapter(mModulesList)
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

        fun updateItems(list: List<Module>) {
            moduleList.clear()
            moduleList = list.toMutableList()
            notifyDataSetChanged()
        }
    }

    private class AppListViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(module: Module) {
            itemView.app_id_text_view.text = "id: ${module.id}"
            itemView.app_package_text_view.text = "package: ${module.appPackage}"
        }
    }

    private fun syncListWithDatabase() {
        mModulesList = mDatabaseManager.actionGetAllModules().toMutableList()
        mAppAdapter.updateItems(mModulesList)
    }
}
