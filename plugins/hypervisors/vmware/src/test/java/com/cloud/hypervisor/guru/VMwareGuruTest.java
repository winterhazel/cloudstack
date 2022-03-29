package com.cloud.hypervisor.guru;

import java.util.Date;

import org.apache.cloudstack.backup.BackupVO;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cloud.storage.VolumeApiService;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.vm.VMInstanceVO;
import com.vmware.vim25.VirtualDisk;
import com.vmware.vim25.VirtualDiskFlatVer2BackingInfo;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Logger.class, VMwareGuru.class})
public class VMwareGuruTest {
    @Mock
    Logger logger;

    @Mock
    VolumeDao volumeDao;

    @Mock
    VolumeApiService volumeService;

    @InjectMocks
    VMwareGuru guru;

    @Before
    public void setup() {
        guru = Mockito.spy(VMwareGuru.class);
        guru.volumeService = volumeService;
        guru._volumeDao = volumeDao;
        guru.s_logger = logger;
    }

    @Test
    public void detachVolumeTestWhenVolumePathDontExistsInDb() {
        VirtualDisk virtualDisk = Mockito.mock(VirtualDisk.class);
        VirtualDiskFlatVer2BackingInfo info = Mockito.mock(VirtualDiskFlatVer2BackingInfo.class);
        Mockito.when(virtualDisk.getBacking()).thenReturn(info);
        Mockito.when(info.getFileName()).thenReturn("[ae4e2064cdbf3587908f726a23f9a5a3] i-2-444-VM/6b10e0316c5e441dbaeb23a806679c8d.vmdk");
        Mockito.when(volumeDao.findByPath(Mockito.eq("6b10e0316c5e441dbaeb23a806679c8d"))).thenReturn(null);
        VMInstanceVO vmInstanceVO = new VMInstanceVO();
        BackupVO backupVO = new BackupVO();

        VolumeVO detachVolume = guru.detachVolume(vmInstanceVO, virtualDisk, backupVO);
        Assert.assertEquals(null, detachVolume);
    }

    @Test
    public void detachVolumeTestWhenVolumeExistsButIsOwnedByAnotherInstance() {
        VirtualDisk virtualDisk = Mockito.mock(VirtualDisk.class);
        VirtualDiskFlatVer2BackingInfo info = Mockito.mock(VirtualDiskFlatVer2BackingInfo.class);
        VolumeVO volumeVO = Mockito.mock(VolumeVO.class);
        VMInstanceVO vmInstanceVO = Mockito.mock(VMInstanceVO.class);

        Mockito.when(virtualDisk.getBacking()).thenReturn(info);
        Mockito.when(info.getFileName()).thenReturn("[ae4e2064cdbf3587908f726a23f9a5a3] i-2-444-VM/6b10e0316c5e441dbaeb23a806679c8d.vmdk");
        Mockito.when(volumeDao.findByPath(Mockito.eq("6b10e0316c5e441dbaeb23a806679c8d"))).thenReturn(volumeVO);
        Mockito.when(volumeVO.getInstanceId()).thenReturn(2L);
        Mockito.when(vmInstanceVO.getId()).thenReturn(1L);
        BackupVO backupVO = new BackupVO();

        Mockito.verify(volumeService, Mockito.never()).detachVolumeFromVM(Mockito.any());
        guru.detachVolume(vmInstanceVO, virtualDisk, backupVO);
    }

    @Test
    public void detachVolumeTestWhenVolumeExistsButIsRemoved() {
        VirtualDisk virtualDisk = Mockito.mock(VirtualDisk.class);
        VirtualDiskFlatVer2BackingInfo info = Mockito.mock(VirtualDiskFlatVer2BackingInfo.class);
        VolumeVO volumeVO = Mockito.mock(VolumeVO.class);
        VMInstanceVO vmInstanceVO = Mockito.mock(VMInstanceVO.class);

        Mockito.when(volumeVO.getInstanceId()).thenReturn(1L);
        Mockito.when(volumeVO.getRemoved()).thenReturn(new Date());
        Mockito.when(virtualDisk.getBacking()).thenReturn(info);
        Mockito.when(info.getFileName()).thenReturn("[ae4e2064cdbf3587908f726a23f9a5a3] i-2-444-VM/6b10e0316c5e441dbaeb23a806679c8d.vmdk");
        Mockito.when(volumeDao.findByPath(Mockito.eq("6b10e0316c5e441dbaeb23a806679c8d"))).thenReturn(volumeVO);
        Mockito.when(vmInstanceVO.getId()).thenReturn(1L);
        BackupVO backupVO = new BackupVO();

        Mockito.verify(volumeService, Mockito.never()).detachVolumeFromVM(Mockito.any());
        guru.detachVolume(vmInstanceVO, virtualDisk, backupVO);
    }

    @Test
    public void detachVolumeTestWhenVolumeExistsButDetachFail() {
        PowerMockito.mockStatic(Logger.class);
        PowerMockito.when(Logger.getLogger(Mockito.eq(VMwareGuru.class))).thenReturn(logger);
        VirtualDisk virtualDisk = Mockito.mock(VirtualDisk.class);
        VirtualDiskFlatVer2BackingInfo info = Mockito.mock(VirtualDiskFlatVer2BackingInfo.class);
        VolumeVO volumeVO = Mockito.mock(VolumeVO.class);
        VMInstanceVO vmInstanceVO = Mockito.mock(VMInstanceVO.class);
        BackupVO backupVO = Mockito.mock(BackupVO.class);

        Mockito.when(volumeVO.getInstanceId()).thenReturn(1L);
        Mockito.when(volumeVO.getUuid()).thenReturn("123");
        Mockito.when(backupVO.getUuid()).thenReturn("321");
        Mockito.when(vmInstanceVO.getInstanceName()).thenReturn("test1");
        Mockito.when(vmInstanceVO.getUuid()).thenReturn("1234");
        Mockito.when(virtualDisk.getBacking()).thenReturn(info);
        Mockito.when(info.getFileName()).thenReturn("[ae4e2064cdbf3587908f726a23f9a5a3] i-2-444-VM/6b10e0316c5e441dbaeb23a806679c8d.vmdk");
        Mockito.when(volumeDao.findByPath(Mockito.eq("6b10e0316c5e441dbaeb23a806679c8d"))).thenReturn(volumeVO);
        Mockito.when(vmInstanceVO.getId()).thenReturn(1L);
        Mockito.when(volumeService.detachVolumeFromVM(Mockito.any())).thenReturn(null);

        guru.detachVolume(vmInstanceVO, virtualDisk, backupVO);
        Mockito.verify(volumeService, Mockito.times(1)).detachVolumeFromVM(Mockito.any());
        Mockito.verify(logger, Mockito.times(1)).warn("Failed to detach volume [uuid: 123] from VM [uuid: 1234, name: test1], during the backup restore process (as this volume does not exist in the metadata of backup [uuid: 321]).");
    }

    @Test
    public void detachVolumeTestWhenVolumeExistsAndDetachDontFail() {
        PowerMockito.mockStatic(Logger.class);
        PowerMockito.when(Logger.getLogger(Mockito.eq(VMwareGuru.class))).thenReturn(logger);
        VirtualDisk virtualDisk = Mockito.mock(VirtualDisk.class);
        VirtualDiskFlatVer2BackingInfo info = Mockito.mock(VirtualDiskFlatVer2BackingInfo.class);
        VolumeVO volumeVO = Mockito.mock(VolumeVO.class);
        VMInstanceVO vmInstanceVO = Mockito.mock(VMInstanceVO.class);
        BackupVO backupVO = Mockito.mock(BackupVO.class);

        Mockito.when(volumeVO.getInstanceId()).thenReturn(1L);
        Mockito.when(volumeVO.getUuid()).thenReturn("123");
        Mockito.when(backupVO.getUuid()).thenReturn("321");
        Mockito.when(vmInstanceVO.getInstanceName()).thenReturn("test1");
        Mockito.when(vmInstanceVO.getUuid()).thenReturn("1234");
        Mockito.when(virtualDisk.getBacking()).thenReturn(info);
        Mockito.when(info.getFileName()).thenReturn("[ae4e2064cdbf3587908f726a23f9a5a3] i-2-444-VM/6b10e0316c5e441dbaeb23a806679c8d.vmdk");
        Mockito.when(volumeDao.findByPath(Mockito.eq("6b10e0316c5e441dbaeb23a806679c8d"))).thenReturn(volumeVO);
        Mockito.when(vmInstanceVO.getId()).thenReturn(1L);
        Mockito.when(volumeService.detachVolumeFromVM(Mockito.any())).thenReturn(volumeVO);

        guru.detachVolume(vmInstanceVO, virtualDisk, backupVO);
        Mockito.verify(volumeService, Mockito.times(1)).detachVolumeFromVM(Mockito.any());
        Mockito.verify(logger, Mockito.times(1)).debug("Volume [uuid: 123] detached with success from VM [uuid: 1234, name: test1], during the backup restore process (as this volume does not exist in the metadata of backup [uuid: 321]).");
    }
}
