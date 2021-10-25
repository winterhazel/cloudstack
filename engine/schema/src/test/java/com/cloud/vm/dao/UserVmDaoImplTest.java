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

package com.cloud.vm.dao;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine.State;

@RunWith(MockitoJUnitRunner.class)
public class UserVmDaoImplTest {

    @Spy
    @InjectMocks
    private UserVmDaoImpl userVmDaoImplMock = new UserVmDaoImpl();

    @Test
    public void createSearchBuilderWithStateCriteriaTestReturnSearchBuilder() {
        SearchBuilder<UserVmVO> expected = Mockito.mock(SearchBuilder.class);
        UserVmVO userVmVoEntityMock = Mockito.mock(UserVmVO.class);
        Mockito.doReturn(State.Running).when(userVmVoEntityMock).getState();
        Mockito.doReturn(userVmVoEntityMock).when(expected).entity();
        Mockito.doReturn(expected).when(userVmDaoImplMock).createSearchBuilder();

        int index = 0;
        for (SearchCriteria.Op op : SearchCriteria.Op.values()) {
            index++;
            SearchBuilder<UserVmVO> actual = userVmDaoImplMock.createSearchBuilderWithStateCriteria(op);

            Mockito.verify(userVmDaoImplMock, Mockito.times(index)).createSearchBuilder();
            assertEquals(expected, actual);
            Mockito.verify(expected).and(Mockito.eq("state"), Mockito.eq(State.Running),  Mockito.eq(op));
        }
    }
}