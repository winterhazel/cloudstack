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

package com.cloud.network;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderVO;
import com.cloud.network.dao.PhysicalNetworkVO;
import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.VlanDao;
import com.cloud.domain.dao.DomainDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.net.Ip;
import com.cloud.network.Network.Provider;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class NetworkModelTest {

    @Mock
    private DataCenterDao dataCenterDao;
    @Mock
    private PhysicalNetworkDao physicalNetworkDao;
    @Mock
    private PhysicalNetworkServiceProviderDao physicalNetworkServiceProviderDao;
    @Mock
    private NetworkService networkService;

    @InjectMocks
    @Spy
    private NetworkModelImpl networkModel = new NetworkModelImpl();

    @Mock
    private DataCenterVO zone1;
    @Mock
    private DataCenterVO zone2;
    @Mock
    private PhysicalNetworkVO physicalNetworkZone1;
    @Mock
    private PhysicalNetworkVO physicalNetworkZone2;
    @Mock
    private PhysicalNetworkServiceProviderVO providerVO;
    @Mock
    private DomainDao domainDao;
    @Mock
    private NetworkDao networkDao;
    @Mock
    private AccountDao accountDao;

    private static final long ZONE_1_ID = 1L;
    private static final long ZONE_2_ID = 2L;
    private static final long PHYSICAL_NETWORK_1_ID = 1L;
    private static final long PHYSICAL_NETWORK_2_ID = 2L;

    private static final String IPV6_CIDR = "fd59:16ba:559b:243d::/64";
    private static final String IPV6_GATEWAY = "fd59:16ba:559b:243d::1";
    private static final String START_IPV6 = "fd59:16ba:559b:243d:0:0:0:2";
    private static final String END_IPV6 = "fd59:16ba:559b:243d:ffff:ffff:ffff:ffff";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(dataCenterDao.listEnabledZones()).thenReturn(Arrays.asList(zone1, zone2));
        when(physicalNetworkDao.listByZoneAndTrafficType(ZONE_1_ID, Networks.TrafficType.Guest)).
                thenReturn(Collections.singletonList(physicalNetworkZone1));
        when(physicalNetworkDao.listByZoneAndTrafficType(ZONE_2_ID, Networks.TrafficType.Guest)).
                thenReturn(Collections.singletonList(physicalNetworkZone2));
        when(physicalNetworkServiceProviderDao.findByServiceProvider(
                PHYSICAL_NETWORK_1_ID, Network.Provider.ConfigDrive.getName())).thenReturn(null);
        when(physicalNetworkServiceProviderDao.findByServiceProvider(
                PHYSICAL_NETWORK_2_ID, Network.Provider.ConfigDrive.getName())).thenReturn(null);

        when(zone1.getNetworkType()).thenReturn(DataCenter.NetworkType.Advanced);
        when(zone1.getId()).thenReturn(ZONE_1_ID);

        when(zone2.getNetworkType()).thenReturn(DataCenter.NetworkType.Advanced);
        when(zone2.getId()).thenReturn(ZONE_2_ID);

        when(physicalNetworkZone1.getId()).thenReturn(PHYSICAL_NETWORK_1_ID);
        when(physicalNetworkZone2.getId()).thenReturn(PHYSICAL_NETWORK_2_ID);
    }

    @Test
    public void testGetSourceNatIpAddressForGuestNetwork() {
        NetworkModelImpl modelImpl = new NetworkModelImpl();
        IPAddressDao ipAddressDao = mock(IPAddressDao.class);
        modelImpl._ipAddressDao = ipAddressDao;
        List<IPAddressVO> fakeList = new ArrayList<IPAddressVO>();
        IPAddressVO fakeIp = new IPAddressVO(new Ip("75.75.75.75"), 1, 0xaabbccddeeffL, 10, false);
        fakeList.add(fakeIp);
        SearchBuilder<IPAddressVO> fakeSearch = mock(SearchBuilder.class);
        modelImpl.IpAddressSearch = fakeSearch;
        VlanDao fakeVlanDao = mock(VlanDao.class);
        when(fakeVlanDao.findById(anyLong())).thenReturn(mock(VlanVO.class));
        modelImpl._vlanDao = fakeVlanDao;
        when(fakeSearch.create()).thenReturn(mock(SearchCriteria.class));
        when(ipAddressDao.search(any(SearchCriteria.class), (Filter) isNull())).thenReturn(fakeList);
        when(ipAddressDao.findById(anyLong())).thenReturn(fakeIp);
        Account fakeAccount = mock(Account.class);
        when(fakeAccount.getId()).thenReturn(1L);
        Network fakeNetwork = mock(Network.class);
        when(fakeNetwork.getId()).thenReturn(1L);
        PublicIpAddress answer = modelImpl.getSourceNatIpAddressForGuestNetwork(fakeAccount, fakeNetwork);
        Assert.assertNull(answer);
        IPAddressVO fakeIp2 = new IPAddressVO(new Ip("76.75.75.75"), 1, 0xaabb10ddeeffL, 10, true);
        fakeList.add(fakeIp2);
        when(ipAddressDao.findById(anyLong())).thenReturn(fakeIp2);
        answer = modelImpl.getSourceNatIpAddressForGuestNetwork(fakeAccount, fakeNetwork);
        Assert.assertNotNull(answer);
        Assert.assertEquals(answer.getAddress().addr(), "76.75.75.75");

    }

    @Test
    public void testVerifyDisabledConfigDriveEntriesOnZonesBothEnabledZones() {
        networkModel.verifyDisabledConfigDriveEntriesOnEnabledZones();
        verify(networkModel, times(2)).addDisabledConfigDriveEntriesOnZone(any(DataCenterVO.class));
    }

    @Test
    public void testVerifyDisabledConfigDriveEntriesOnZonesOneEnabledZone() {
        when(dataCenterDao.listEnabledZones()).thenReturn(Collections.singletonList(zone1));

        networkModel.verifyDisabledConfigDriveEntriesOnEnabledZones();
        verify(networkModel).addDisabledConfigDriveEntriesOnZone(any(DataCenterVO.class));
    }

    @Test
    public void testVerifyDisabledConfigDriveEntriesOnZonesNoEnabledZones() {
        when(dataCenterDao.listEnabledZones()).thenReturn(null);

        networkModel.verifyDisabledConfigDriveEntriesOnEnabledZones();
        verify(networkModel, never()).addDisabledConfigDriveEntriesOnZone(any(DataCenterVO.class));
    }

    @Test
    public void testAddDisabledConfigDriveEntriesOnZoneBasicZone() {
        when(zone1.getNetworkType()).thenReturn(DataCenter.NetworkType.Basic);

        networkModel.addDisabledConfigDriveEntriesOnZone(zone1);
        verify(physicalNetworkDao, never()).listByZoneAndTrafficType(ZONE_1_ID, Networks.TrafficType.Guest);
        verify(networkService, never()).
                addProviderToPhysicalNetwork(anyLong(), eq(Provider.ConfigDrive.getName()), isNull(Long.class), isNull(List.class));
    }

    @Test
    public void testAddDisabledConfigDriveEntriesOnZoneAdvancedZoneExistingConfigDrive() {
        when(physicalNetworkServiceProviderDao.findByServiceProvider(
                PHYSICAL_NETWORK_1_ID, Network.Provider.ConfigDrive.getName())).thenReturn(providerVO);

        networkModel.addDisabledConfigDriveEntriesOnZone(zone1);
        verify(networkService, never()).
                addProviderToPhysicalNetwork(anyLong(), eq(Provider.ConfigDrive.getName()), isNull(Long.class), isNull(List.class));
    }

    @Test
    public void testAddDisabledConfigDriveEntriesOnZoneAdvancedZoneNonExistingConfigDrive() {
        networkModel.addDisabledConfigDriveEntriesOnZone(zone1);
        verify(networkService).
                addProviderToPhysicalNetwork(anyLong(), eq(Provider.ConfigDrive.getName()), isNull(Long.class), isNull(List.class));
    }

    @Test
    public void checkIp6ParametersTestAllGood() {
        networkModel.checkIp6Parameters(START_IPV6, END_IPV6, IPV6_GATEWAY,IPV6_CIDR);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void checkIp6ParametersTestCidr32() {
        String ipv6cidr = "fd59:16ba:559b:243d::/32";
        String endipv6 = "fd59:16ba:ffff:ffff:ffff:ffff:ffff:ffff";
        networkModel.checkIp6Parameters(START_IPV6, endipv6, IPV6_GATEWAY,ipv6cidr);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void checkIp6ParametersTestCidr63() {
        String ipv6cidr = "fd59:16ba:559b:243d::/63";
        String endipv6 = "fd59:16ba:559b:243d:ffff:ffff:ffff:ffff";
        networkModel.checkIp6Parameters(START_IPV6, endipv6, IPV6_GATEWAY,ipv6cidr);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void checkIp6ParametersTestCidr65() {
        String ipv6cidr = "fd59:16ba:559b:243d::/65";
        String endipv6 = "fd59:16ba:559b:243d:7fff:ffff:ffff:ffff";
        networkModel.checkIp6Parameters(START_IPV6, endipv6, IPV6_GATEWAY,ipv6cidr);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void checkIp6ParametersTestCidr120() {
        String ipv6cidr = "fd59:16ba:559b:243d::/120";
        String endipv6 = "fd59:16ba:559b:243d:0:0:0:ff";
        networkModel.checkIp6Parameters(START_IPV6, endipv6, IPV6_GATEWAY,ipv6cidr);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void checkIp6ParametersTestNullGateway() {
        networkModel.checkIp6Parameters(START_IPV6, END_IPV6, null,IPV6_CIDR);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void checkIp6ParametersTestNullCidr() {
        networkModel.checkIp6Parameters(START_IPV6, END_IPV6, IPV6_GATEWAY,null);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void checkIp6ParametersTestNullCidrAndNulGateway() {
        networkModel.checkIp6Parameters(START_IPV6, END_IPV6, null,null);
    }

    @Test
    public void checkIp6ParametersTestNullStartIpv6() {
        networkModel.checkIp6Parameters(null, END_IPV6, IPV6_GATEWAY,IPV6_CIDR);
    }

    @Test
    public void checkIp6ParametersTestNullEndIpv6() {
        networkModel.checkIp6Parameters(START_IPV6, null, IPV6_GATEWAY,IPV6_CIDR);
    }

    @Test
    public void checkIp6ParametersTestNullStartAndEndIpv6() {
        networkModel.checkIp6Parameters(null, null, IPV6_GATEWAY,IPV6_CIDR);
    }

    @Test
    public void checkAccountAccessToNetworkTestAccountRootAdmin() {
        AccountVO accountVO = new AccountVO();
        accountVO.setType(Account.Type.ADMIN);
        NetworkVO networkVO = Mockito.mock(NetworkVO.class);
        Mockito.when(networkVO.getAccountId()).thenReturn(2l);
        networkModel.checkAccountAccessToNetwork(accountVO, networkVO);
    }

    @Test
    public void checkAccountAccessToNetworkTestAccountIsTheSameInNetwork() {
        AccountVO accountVO = new AccountVO();
        accountVO.setType(Account.Type.NORMAL);
        accountVO.setId(1l);
        NetworkVO networkVO = Mockito.mock(NetworkVO.class);
        Mockito.when(networkVO.getAccountId()).thenReturn(1l);

        networkModel.checkAccountAccessToNetwork(accountVO, networkVO);
    }

    @Test
    public void checkAccountAccessToNetworkTestAccountIsDomainAdminOfTheNetwork() {
        AccountVO accountVO = new AccountVO();
        accountVO.setType(Account.Type.DOMAIN_ADMIN);
        accountVO.setId(1l);
        NetworkVO networkVO = Mockito.mock(NetworkVO.class);
        Mockito.when(networkVO.getAccountId()).thenReturn(2l);
        Mockito.when(domainDao.isChildDomain(1l, 2l)).thenReturn(true);
        Mockito.when(accountDao.findById(2l)).thenReturn(new AccountVO());
        networkModel.checkAccountAccessToNetwork(accountVO, networkVO);
    }

    @Test(expected = PermissionDeniedException.class)
    public void checkAccountAccessToNetworkTestAccountIsDomainAdminButDoesNotHaveAccessToNetwork() {
        AccountVO accountVO = new AccountVO();
        accountVO.setType(Account.Type.DOMAIN_ADMIN);
        accountVO.setId(1l);
        NetworkVO networkVO = Mockito.mock(NetworkVO.class);
        Mockito.when(networkVO.getAccountId()).thenReturn(2l);
        Mockito.when(domainDao.isChildDomain(1l, 2l)).thenReturn(false);
        Mockito.when(networkDao.listBy(1l, 2l)).thenReturn(null);
        networkModel.checkAccountAccessToNetwork(accountVO, networkVO);
    }

    @Test(expected = PermissionDeniedException.class)
    public void checkAccountAccessToNetworkTestAccountDoesNotHaveAccessToNetwork() {
        AccountVO accountVO = new AccountVO();
        accountVO.setType(Account.Type.NORMAL);
        accountVO.setId(1l);
        NetworkVO networkVO = Mockito.mock(NetworkVO.class);
        Mockito.when(networkVO.getAccountId()).thenReturn(2l);
        Mockito.when(domainDao.isChildDomain(1l, 2l)).thenReturn(false);
        Mockito.when(networkDao.listBy(1l, 2l)).thenReturn(null);
        networkModel.checkAccountAccessToNetwork(accountVO, networkVO);
    }

    @Test
    public void checkAccountAccessToNetworkTestAccountHaveAccessToNetwork() {
        AccountVO accountVO = new AccountVO();
        accountVO.setType(Account.Type.NORMAL);
        accountVO.setId(1l);
        NetworkVO networkVO = Mockito.mock(NetworkVO.class);
        Mockito.when(networkVO.getId()).thenReturn(3l);
        Mockito.when(networkVO.getAccountId()).thenReturn(2l);
        List<NetworkVO> list = new ArrayList<>();
        list.add(networkVO);
        Mockito.when(domainDao.isChildDomain(1l, 2l)).thenReturn(false);
        Mockito.when(networkDao.listBy(1l, 3l)).thenReturn(list);
        networkModel.checkAccountAccessToNetwork(accountVO, networkVO);
    }
}
