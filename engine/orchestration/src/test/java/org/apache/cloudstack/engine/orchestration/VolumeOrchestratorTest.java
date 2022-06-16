package org.apache.cloudstack.engine.orchestration;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cloud.storage.StoragePool;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.template.TemplateManager;
import com.cloud.utils.exception.CloudRuntimeException;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Logger.class, VolumeOrchestrator.class})
public class VolumeOrchestratorTest {
    @Mock
    VMTemplatePoolDao vmTemplatePoolDaoMock;

    @Mock
    VMTemplateDao vmTemplateDaoMock;

    @Mock
    TemplateManager templateManagerMock;

    @Mock
    Logger logger;

    @Mock
    StoragePool storageMock;

    @Mock
    VolumeInfo volMock;

    DataStore dataStoreMock;

    @Mock
    VMTemplateVO vmTemplateMock;

    @InjectMocks
    VolumeOrchestrator orchestratorMock;

    @Before
    public void setup() {
        orchestratorMock = Mockito.spy(VolumeOrchestrator.class);
        orchestratorMock.templateStoragePoolDao = vmTemplatePoolDaoMock;
        orchestratorMock.tmpltDao =  vmTemplateDaoMock;
        orchestratorMock._tmpltMgr = templateManagerMock;
        Mockito.when(storageMock.getId()).thenReturn(1l);
        Mockito.when(storageMock.getUuid()).thenReturn("storage-uuid");
        Mockito.when(volMock.getTemplateId()).thenReturn(2l);
        Mockito.when(volMock.getUuid()).thenReturn("uuid");
        Mockito.when(vmTemplateMock.getUuid()).thenReturn("template-uuid");
        Mockito.when(vmTemplateDaoMock.findById(Mockito.eq(2l))).thenReturn(vmTemplateMock);
    }

    @Test
    public void copyTemplateOfVolumeToNewStoragePoolTestTemplateAlreadyInNewStoragePool() {
        PowerMockito.mockStatic(Logger.class);
        PowerMockito.when(Logger.getLogger(Mockito.eq(VolumeOrchestrator.class))).thenReturn(logger);
        VMTemplateStoragePoolVO result = Mockito.mock(VMTemplateStoragePoolVO.class);
        Mockito.when(vmTemplatePoolDaoMock.findByPoolTemplate(Mockito.eq(1l), Mockito.eq(2l), Mockito.anyString())).thenReturn(result);
        orchestratorMock.copyTemplateOfVolumeToNewStoragePool(storageMock, volMock, dataStoreMock);
        Mockito.verify(logger, Mockito.never()).debug(Mockito.any());
        Mockito.verify(logger, Mockito.never()).error(Mockito.any());
    }

    @Test
    public void copyTemplateOfVolumeToNewStoragePoolTestException() {
        PowerMockito.mockStatic(Logger.class);
        PowerMockito.when(Logger.getLogger(Mockito.eq(VolumeOrchestrator.class))).thenReturn(logger);
        orchestratorMock.s_logger = logger;
        Mockito.when(vmTemplatePoolDaoMock.findByPoolTemplate(Mockito.eq(1l), Mockito.eq(2l), Mockito.anyString())).thenReturn(null);
        Mockito.when(vmTemplateDaoMock.findById(Mockito.eq(2l))).thenReturn(vmTemplateMock);
        CloudRuntimeException exception = Mockito.mock(CloudRuntimeException.class);
        Mockito.when(exception.getMessage()).thenReturn("Error test");
        Mockito.when(templateManagerMock.prepareTemplateForCreate(Mockito.any(), Mockito.any())).thenThrow(exception);
        orchestratorMock.copyTemplateOfVolumeToNewStoragePool(storageMock, volMock, dataStoreMock);
        Mockito.verify(logger, Mockito.times(1)).debug("Template [2] used by volume [uuid] does not exist in storage pool [storage-uuid]. Copying it to this storage.");
        Mockito.verify(logger, Mockito.times(1)).error("Failed to copy template [template-uuid], used by volume [uuid], to storage pool [storage-uuid] due to [Error test].", exception);
    }

    @Test
    public void copyTemplateOfVolumeToNewStoragePoolTestSuccess() {
        PowerMockito.mockStatic(Logger.class);
        PowerMockito.when(Logger.getLogger(Mockito.eq(VolumeOrchestrator.class))).thenReturn(logger);
        orchestratorMock.s_logger = logger;
        Mockito.when(vmTemplatePoolDaoMock.findByPoolTemplate(Mockito.eq(1l), Mockito.eq(2l), Mockito.anyString())).thenReturn(null);
        Mockito.when(templateManagerMock.prepareTemplateForCreate(Mockito.any(), Mockito.any())).thenReturn(null);
        orchestratorMock.copyTemplateOfVolumeToNewStoragePool(storageMock, volMock, dataStoreMock);
        Mockito.verify(logger, Mockito.times(1)).debug("Template [2] used by volume [uuid] does not exist in storage pool [storage-uuid]. Copying it to this storage.");
    }
}