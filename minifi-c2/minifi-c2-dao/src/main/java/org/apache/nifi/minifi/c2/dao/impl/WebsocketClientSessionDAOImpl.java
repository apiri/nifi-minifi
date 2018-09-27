package org.apache.nifi.minifi.c2.dao.impl;

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
 * Created on 3/15/17.
 */


import org.apache.nifi.minifi.c2.dao.WebsocketClientSessionDAO;

import javax.websocket.Session;
import java.util.ArrayList;
import java.util.List;

public class WebsocketClientSessionDAOImpl
        implements WebsocketClientSessionDAO {

    private List<Session> sessions = new ArrayList<Session>();

    public void addWebsocketClientSession(Session session) {
        sessions.add(session);
    }

    public void removeWebsocketClientSession(Session session) {
        sessions.remove(session);
    }

    public List<Session> getActiveSessions() {
        return sessions;
    }
}
