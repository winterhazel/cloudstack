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
package org.apache.cloudstack.network.tungsten.agent.api;

import java.util.Objects;

public class ListTungstenNetworkCommand extends TungstenCommand {
    private final String projectFqn;
    private final String networkUuid;

    public ListTungstenNetworkCommand(final String projectFqn, final String networkUuid) {
        this.projectFqn = projectFqn;
        this.networkUuid = networkUuid;
    }

    public String getProjectFqn() {
        return projectFqn;
    }

    public String getNetworkUuid() {
        return networkUuid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ListTungstenNetworkCommand that = (ListTungstenNetworkCommand) o;
        return Objects.equals(projectFqn, that.projectFqn) && Objects.equals(networkUuid, that.networkUuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), projectFqn, networkUuid);
    }
}
