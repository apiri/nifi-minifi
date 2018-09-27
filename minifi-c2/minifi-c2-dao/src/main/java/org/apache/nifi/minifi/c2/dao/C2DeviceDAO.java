package org.apache.nifi.minifi.c2.dao;

import org.apache.nifi.device.registry.resource.c2.core.device.DeviceInfo;
import org.apache.nifi.minifi.c2.dao.impl.C2DeviceMapper;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

import java.util.List;

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
 * Created on 7/11/17.
 */

@RegisterMapper(C2DeviceMapper.class)
public abstract class C2DeviceDAO {

    @SqlUpdate("INSERT INTO " + DBConstants.C2_DEVICE_TABLE + "(DEVICE_ID, HOSTNAME, IP, MACHINE_ARCH, PHYSICAL_MEM, VCORES) " +
            "VALUES (:deviceId, :hostname, :ip, :machineArch, :physicalMem, :vcores)")
    public abstract void registerC2Device(@Bind("deviceId") String deviceId, @Bind("hostname") String hostname,
            @Bind("ip") String ip, @Bind("machineArch") String machineArch, @Bind("physicalMem") long physicalMem,
            @Bind("vcores") int vcores);

    @SqlUpdate("UPDATE " + DBConstants.C2_DEVICE_TABLE + " SET HOSTNAME = :hostname, IP = :ip, MACHINE_ARCH = :machineArch, " +
            "PHYSICAL_MEM = :physicalMem, VCORES = :vcores where DEVICE_ID = :deviceId")
    public abstract void updateC2Device(@Bind("hostname") String hostname, @Bind("ip") String ip, @Bind("machineArch") String machineArch,
            @Bind("physicalMem") long physicalMem, @Bind("vcores") int vcores, @Bind("deviceId") String deviceId);

    @SqlQuery("SELECT COUNT(*) FROM " + DBConstants.C2_DEVICE_TABLE)
    public abstract long totalNumDevices();

    @SqlQuery("SELECT * FROM " + DBConstants.C2_DEVICE_TABLE + " WHERE DEVICE_ID = :deviceId")
    public abstract DeviceInfo getDeviceById(@Bind("deviceId") String deviceId);

    @SqlQuery("SELECT * FROM " + DBConstants.C2_DEVICE_TABLE + " LIMIT :limit")
    public abstract List<DeviceInfo> getDeviceWithLimit(@Bind("limit") int limit);
}
