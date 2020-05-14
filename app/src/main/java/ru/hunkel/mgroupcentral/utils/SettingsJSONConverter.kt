package utils

import org.json.JSONArray
import org.json.JSONObject

const val TAG = "SettingsJSONConverter"

class SettingsJSONConverter {
    companion object {
        @JvmStatic
        fun packSettings(preferences: Map<String, Any>): String {
            return packToJsonString(
                preferences
            )
        }

        @JvmStatic
        fun unpackSettings(settingsString: String): Map<String, Any> {
            val settings = mutableMapOf<String, Any>()

            val s = JSONArray(settingsString)
            for (index in 0 until s.length()) {
                val jsonObject = s.getJSONObject(index)
                val preferenceName = jsonObject.keys()
                preferenceName.forEach {
                    settings[it] = jsonObject.getString(it)
                }
            }
            return settings
        }

        private fun packToJsonString(preferences: Map<String, Any>): String {
            val jsonArray = JSONArray()
            for (p in preferences) {
                val j = JSONObject()
                j.put(p.key, p.value)
                jsonArray.put(j)
            }
            return jsonArray.toString()
        }
    }
}
