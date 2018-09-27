package org.apache.nifi.minifi.c2.dao;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * Created on 6/18/17.
 */


public class DBConstants {

    public static final String GARCON_DB_NAME = "GARCON";
    public static final String DEVICE_TABLE = "DEVICE";
    public static final String NIFI_DEVICE_TABLE = "NIFI_NODE";
    public static final String MINIFI_DEVICE_TABLE = "MINIFI_DEVICE";
    public static final String MINIFI_HEARTBEATS_TABLE = "MINIFI_CPP_HEARTBEATS";

    public static final String C2_DEVICE_TABLE = "C2_DEVICE";
    public static final String C2_QUEUE_METRICS = "C2_QUEUE_METRICS";
    public static final String C2_PROCESS_METRICS = "C2_PROCESS_METRICS";
    public static final String C2_HEARTBEATS = "C2_HEARTBEATS";
    public static final String C2_OPERATIONS = "C2_OPERATIONS";
    public static final String C2_COMPONENT_STATUS = "C2_COMPONENT_STATUS";
    public static final String C2_DEVICE_CONFIG = "C2_DEVICE_CONFIG";
    public static final String C2_DEVICE_CONFIG_MAPPING = "C2_DEVICE_CONFIG_MAPPING";
}