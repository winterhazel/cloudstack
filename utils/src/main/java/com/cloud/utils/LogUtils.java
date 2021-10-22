//
// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
//

package com.cloud.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import com.google.gson.Gson;

public class LogUtils {
    public static final Logger LOGGER = Logger.getLogger(LogUtils.class);
    private static final Gson GSON = new Gson();

    public static void initLog4j(String log4jConfigFileName) {
        assert (log4jConfigFileName != null);
        File file = PropertiesUtil.findConfigFile(log4jConfigFileName);
        if (file != null) {
            LOGGER.info("log4j configuration found at " + file.getAbsolutePath());
            DOMConfigurator.configureAndWatch(file.getAbsolutePath());
        } else {
            String nameWithoutExtension = log4jConfigFileName.substring(0, log4jConfigFileName.lastIndexOf('.'));
            file = PropertiesUtil.findConfigFile(nameWithoutExtension + ".properties");
            if (file != null) {
                LOGGER.info("log4j configuration found at " + file.getAbsolutePath());
                DOMConfigurator.configureAndWatch(file.getAbsolutePath());
            }
        }
    }

    public static String logGsonWithoutException(String formatMessage, Object ... objects) {
        List<String> gsons = new ArrayList<>();
        for (Object object : objects) {
            try {
                gsons.add(GSON.toJson(object));
            } catch (Exception e) {
                LOGGER.debug(String.format("Failed to log object [%s] using GSON.", object.getClass().getSimpleName()));
                gsons.add("error to decode");
            }
        }
        try {
            return String.format(formatMessage, gsons.toArray());
        } catch (Exception e) {
            String errorMsg = String.format("Failed to log objects using GSON due to: [%s].", e.getMessage());
            LOGGER.error(errorMsg, e);
            return errorMsg;
        }
    }
}
