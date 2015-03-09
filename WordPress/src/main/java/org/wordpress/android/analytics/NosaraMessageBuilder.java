package org.wordpress.android.analytics;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

class NosaraMessageBuilder {

    public static synchronized JSONObject createRequestCommonPropsJSONObject(NosaraDeviceInformation deviceInformation,
                                                                             JSONObject userProperties) {
        JSONObject commonProps = new JSONObject();
        unfolderProperties(deviceInformation.getImmutableDeviceInfo(), "device_info_", commonProps);
        unfolderProperties(deviceInformation.getMutableDeviceInfo(), "device_info_", commonProps);
        unfolderProperties(userProperties, "user_info_", commonProps);
        return commonProps;
    }

    public static synchronized JSONObject createEventJSONObject(NosaraEvent event, JSONObject commonProps) {
        try {
            JSONObject eventJSON = new JSONObject();
            eventJSON.put("_en", event.getEventName());

            eventJSON.put("_via_ua", event.getUserAgent());
            eventJSON.put("_ts", event.getTimeStamp());

            if (event.getUserType() == NosaraClient.NosaraUserType.ANON) {
                eventJSON.put("_ut", "anon");
                eventJSON.put("_ui", event.getUser());
            } else {
                eventJSON.put("_ul", event.getUser());
            }

            unfolderPropertiesNotAvailableInCommon(event.getUserProperties(), "user_info_", eventJSON, commonProps);
            unfolderPropertiesNotAvailableInCommon(event.getDeviceInfo(), "device_info_", eventJSON, commonProps);

           return eventJSON;
        } catch (JSONException err) {
            Log.e(NosaraClient.LOGTAG, "Cannot write the JSON representation of this object", err);
            return null;
        }
    }


    private static void unfolderPropertiesNotAvailableInCommon(JSONObject objectToFlatten, String flattenPrefix,
                                                                 JSONObject targetJSONObject, JSONObject commonProps) {
        if (objectToFlatten == null || targetJSONObject == null) {
            return;
        }

        if (flattenPrefix == null) {
            Log.w(NosaraClient.LOGTAG, " Unfolding props with an empty key. This could be an error!");
            flattenPrefix = "";
        }

        Iterator<String> iter = objectToFlatten.keys();
        while (iter.hasNext()) {
            String key = iter.next();
            String flattenKey = String.valueOf(flattenPrefix + key).toLowerCase();
            try {
                Object value = objectToFlatten.get(key);
                String valueString;
                if (value != null) {
                    valueString = String.valueOf(value);
                } else {
                    valueString = "";
                }

                String valueStringInCommons = null;
                // Check if the same key/value is already available in common props
                if (commonProps != null && commonProps.has(flattenKey)) {
                    Object valueInCommons = commonProps.get(key);
                    if (valueInCommons != null) {
                        valueStringInCommons = String.valueOf(valueInCommons);
                    }
                }

                // Add the value at event level only if it's different from common
                if (valueStringInCommons == null || !valueStringInCommons.equals(valueString)) {
                    targetJSONObject.put(flattenKey, valueString);
                }
            } catch (JSONException e) {
                // Something went wrong!
                Log.e(NosaraClient.LOGTAG, "Cannot write the flatten JSON representation of this object", e);
            }
        }
    }

    // Nosara only strings property values. Don't convert JSON objs by calling toString()
    // otherwise they will be likely un-queryable
    private static void unfolderProperties(JSONObject objectToFlatten, String flattenPrefix, JSONObject targetJSONObject) {
        if (objectToFlatten == null || targetJSONObject == null) {
            return;
        }

        if (flattenPrefix == null) {
            Log.w(NosaraClient.LOGTAG, " Unfolding props with an empty key. This could be an error!");
            flattenPrefix = "";
        }

        Iterator<String> iter = objectToFlatten.keys();
        while (iter.hasNext()) {
            String key = iter.next();
            try {
                Object value = objectToFlatten.get(key);
                String valueString;
                if (value != null) {
                    valueString = String.valueOf(value);
                } else {
                    valueString = "";
                }
                targetJSONObject.put(String.valueOf(flattenPrefix + key).toLowerCase(), valueString);
            } catch (JSONException e) {
                // Something went wrong!
                Log.e(NosaraClient.LOGTAG, "Cannot write the flatten JSON representation of this object", e);
            }
        }
    }
}