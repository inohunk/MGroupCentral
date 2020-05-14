package ru.hunkel.mgroupcentral.activities

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_app.view.*
import kotlinx.android.synthetic.main.settings_activity.*
import ru.hunkel.mgroupcentral.R
import ru.hunkel.mgroupcentral.database.entities.Module
import ru.hunkel.mgroupcentral.managers.MGroupDatabaseManager
import ru.hunkel.mgroupcentral.utils.MODULE_TITLES

class GeneralSettingsActivity : AppCompatActivity() {

    lateinit var mAppRecyclerView: RecyclerView
    private lateinit var mAppAdapter: AppListAdapter
    lateinit var mDatabaseManager: MGroupDatabaseManager

    private var mModulesList: MutableList<Module> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

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

    private inner class AppListAdapter(private var moduleList: MutableList<Module>) :
        RecyclerView.Adapter<AppListAdapter.AppListViewHolder>() {
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
            val isExpanded: Boolean = moduleList.get(position).expanded
            holder.expandableLayout.visibility = if (isExpanded) View.VISIBLE else View.GONE
        }

        fun updateItems(list: List<Module>) {
            moduleList.clear()
            moduleList = list.toMutableList()
            notifyDataSetChanged()
        }

        fun getItems() = moduleList

        private inner class AppListViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
            lateinit var expandableLayout: LinearLayout

            fun bind(module: Module) {
                itemView.module_package_text_view.text = MODULE_TITLES[module.appPackage]
                expandableLayout = view.expandable_layout

                itemView.setOnClickListener {
                    try {
                        val module = mAppAdapter.getItems()[adapterPosition]

                        val intent = Intent()
                        intent.component = ComponentName(
                            module.appPackage,
                            "ru.ogpscenter.ogpstracker.ui.EventsListActivity"
                        )
                        startActivity(intent)
                    } catch (ex: Exception) {
                        Log.e(TAG, ex.message)
                    }
                    val module: Module = moduleList.get(adapterPosition)
//                    if (module.appPackage == "ru.ogpscenter.ogpstracker") it.registration_text_view.visibility =
//                        View.VISIBLE
//                    module.expanded = !module.expanded
//                    notifyItemChanged(adapterPosition)

                }

//                itemView.registration_text_view.setOnClickListener {
//                    try {
//                        val module = mAppAdapter.getItems()[adapterPosition]
//
//                        val intent = Intent()
//                        intent.component = ComponentName(
//                            module.appPackage,
//                            "ru.ogpscenter.ogpstracker.ui.EventsListActivity"
//                        )
//                        startActivity(intent)
//                    } catch (ex: Exception) {
//                        Log.e(TAG, ex.message)
//                    }
//                }

                itemView.module_settings_button.setOnClickListener {
                    try {
                        val module = mAppAdapter.getItems()[adapterPosition]

                        val intent = Intent()
                        intent.component = ComponentName(
                            module.appPackage,
                            module.appSettings
                        )
                        startActivity(intent)
                    } catch (ex: Exception) {
                        Log.e(TAG, ex.message)
                    }
                }
            }
        }
    }

    private fun syncListWithDatabase() {
        mModulesList = mDatabaseManager.actionGetAllModules().toMutableList()
        mAppAdapter.updateItems(mModulesList)
    }
}