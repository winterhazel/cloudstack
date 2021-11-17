-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements.  See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership.  The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliance
-- with the License.  You may obtain a copy of the License at
--
--   http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing,
-- software distributed under the License is distributed on an
-- "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
-- KIND, either express or implied.  See the License for the
-- specific language governing permissions and limitations
-- under the License.

--;
-- Schema upgrade from 4.15.0.0 to 4.15.1.0
--;
-- Correct guest OS names
UPDATE `cloud`.`guest_os` SET display_name='Fedora Linux (32 bit)' WHERE id=320;
UPDATE `cloud`.`guest_os` SET display_name='Mandriva Linux (32 bit)' WHERE id=323;
UPDATE `cloud`.`guest_os` SET display_name='OpenSUSE Linux (32 bit)' WHERE id=327;

-- PR#4699 Drop the procedure `ADD_GUEST_OS_AND_HYPERVISOR_MAPPING` if it already exist.
DROP PROCEDURE IF EXISTS `cloud`.`ADD_GUEST_OS_AND_HYPERVISOR_MAPPING`;

-- PR#4699 Create the procedure `ADD_GUEST_OS_AND_HYPERVISOR_MAPPING` to add guest_os and guest_os_hypervisor mapping.
CREATE PROCEDURE `cloud`.`ADD_GUEST_OS_AND_HYPERVISOR_MAPPING` (
    IN guest_os_category_id bigint(20) unsigned,
    IN guest_os_display_name VARCHAR(255),
    IN guest_os_hypervisor_hypervisor_type VARCHAR(32),
    IN guest_os_hypervisor_hypervisor_version VARCHAR(32),
    IN guest_os_hypervisor_guest_os_name VARCHAR(255)
)
BEGIN	
	INSERT  INTO cloud.guest_os (uuid, category_id, display_name, created) 
	SELECT 	UUID(), guest_os_category_id, guest_os_display_name, now()
	FROM    DUAL
	WHERE 	not exists( SELECT  1 
	                    FROM    cloud.guest_os
	                    WHERE   cloud.guest_os.category_id = guest_os_category_id
	                    AND     cloud.guest_os.display_name = guest_os_display_name)	
						
;	INSERT  INTO cloud.guest_os_hypervisor (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created) 
	SELECT 	UUID(), guest_os_hypervisor_hypervisor_type, guest_os_hypervisor_hypervisor_version, guest_os_hypervisor_guest_os_name, guest_os.id, now()
	FROM 	cloud.guest_os
	WHERE 	guest_os.category_id = guest_os_category_id
	AND 	guest_os.display_name = guest_os_display_name
	AND	NOT EXISTS (SELECT  1 
	                    FROM    cloud.guest_os_hypervisor as hypervisor
	                    WHERE   hypervisor_type = guest_os_hypervisor_hypervisor_type			
	                    AND     hypervisor_version = guest_os_hypervisor_hypervisor_version
	                    AND     hypervisor.guest_os_id = guest_os.id
	                    AND     hypervisor.guest_os_name = guest_os_hypervisor_guest_os_name)    
;END;

-- PR#4699 Call procedure `ADD_GUEST_OS_AND_HYPERVISOR_MAPPING` to add new data to guest_os and guest_os_hypervisor.
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (10, 'Ubuntu 20.04 LTS', 'KVM', 'default', 'Ubuntu 20.04 LTS');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (10, 'Ubuntu 21.04', 'KVM', 'default', 'Ubuntu 21.04');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (9, 'pfSense 2.4', 'KVM', 'default', 'pfSense 2.4');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (9, 'OpenBSD 6.7', 'KVM', 'default', 'OpenBSD 6.7');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (9, 'OpenBSD 6.8', 'KVM', 'default', 'OpenBSD 6.8');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'AlmaLinux 8.3', 'KVM', 'default', 'AlmaLinux 8.3');

-- Add support for SUSE Linux Enterprise Desktop 12 SP3 (64-bit) for Xenserver 8.1.0
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (5, 'SUSE Linux Enterprise Desktop 12 SP3 (64-bit)', 'Xenserver', '8.1.0', 'SUSE Linux Enterprise Desktop 12 SP3 (64-bit)');
-- Add support for SUSE Linux Enterprise Desktop 12 SP4 (64-bit) for Xenserver 8.1.0
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (5, 'SUSE Linux Enterprise Desktop 12 SP4 (64-bit)', 'Xenserver', '8.1.0', 'SUSE Linux Enterprise Desktop 12 SP4 (64-bit)');
-- Add support for SUSE Linux Enterprise Server 12 SP4 (64-bit) for Xenserver 8.1.0
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (5, 'SUSE Linux Enterprise Server 12 SP4 (64-bit)', 'Xenserver', '8.1.0', 'SUSE Linux Enterprise Server 12 SP4 (64-bit)');
-- Add support for Scientific Linux 7 for Xenserver 8.1.0
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (9, 'Scientific Linux 7', 'Xenserver', '8.1.0', 'Scientific Linux 7');
-- Add support for NeoKylin Linux Server 7 for Xenserver 8.1.0
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (9, 'NeoKylin Linux Server 7', 'Xenserver', '8.1.0', 'NeoKylin Linux Server 7');
-- Add support CentOS 8 for Xenserver 8.1.0
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'Xenserver', '8.1.0', 'CentOS 8', 297, now(), 0);
-- Add support for Debian Buster 10 for Xenserver 8.1.0
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'Xenserver', '8.1.0', 'Debian Buster 10', 292, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'Xenserver', '8.1.0', 'Debian Buster 10', 293, now(), 0);
-- Add support for SUSE Linux Enterprise 15 (64-bit) for Xenserver 8.1.0
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'Xenserver', '8.1.0', 'SUSE Linux Enterprise 15 (64-bit)', 291, now(), 0);

-- Add XenServer 8.2.0 hypervisor capabilities
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(uuid, hypervisor_type, hypervisor_version, max_guests_limit, max_data_volumes_limit, max_hosts_per_cluster, storage_motion_supported) values (UUID(), 'XenServer', '8.2.0', 1000, 253, 64, 1);

-- Copy XenServer 8.1.0 hypervisor guest OS mappings to XenServer 8.2.0
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) SELECT UUID(),'Xenserver', '8.2.0', guest_os_name, guest_os_id, utc_timestamp(), 0 FROM `cloud`.`guest_os_hypervisor` WHERE hypervisor_type='Xenserver' AND hypervisor_version='8.1.0';

-- Add support for Ubuntu Focal Fossa 20.04 for Xenserver 8.2.0
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (10, 'Ubuntu 20.04 LTS', 'Xenserver', '8.2.0', 'Ubuntu Focal Fossa 20.04');

ALTER TABLE `cloud`.`s2s_customer_gateway` ADD COLUMN `ike_version` varchar(5) NOT NULL DEFAULT 'ike' COMMENT 'one of ike, ikev1, ikev2';
ALTER TABLE `cloud`.`s2s_customer_gateway` ADD COLUMN `split_connections` int(1) NOT NULL DEFAULT 0;

-- Add support for VMware 7.0
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities` (uuid, hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled, max_data_volumes_limit, max_hosts_per_cluster, storage_motion_supported, vm_snapshot_enabled) values (UUID(), 'VMware', '7.0', 1024, 0, 59, 64, 1, 1);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) SELECT UUID(),'VMware', '7.0', guest_os_name, guest_os_id, utc_timestamp(), 0  FROM `cloud`.`guest_os_hypervisor` WHERE hypervisor_type='VMware' AND hypervisor_version='6.7';

-- Add support for darwin19_64Guest from VMware 7.0
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (7, 'macOS 10.15 (64 bit)', 'VMware', '7.0', 'darwin19_64Guest');

-- Add support for debian11_64Guest from VMware 7.0
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (2, 'Debian GNU/Linux 11 (64-bit)', 'VMware', '7.0', 'debian11_64Guest');

-- Add support for debian11Guest from VMware 7.0
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (2, 'Debian GNU/Linux 11 (32-bit)', 'VMware', '7.0', 'debian11Guest');

-- Add support for windows2019srv_64Guest from VMware 7.0
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '7.0', 'windows2019srv_64Guest', 276, now(), 0);


-- Add support for VMware 7.0.1.0
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities` (uuid, hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled, max_data_volumes_limit, max_hosts_per_cluster, storage_motion_supported, vm_snapshot_enabled) values (UUID(), 'VMware', '7.0.1.0', 1024, 0, 59, 64, 1, 1);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) SELECT UUID(),'VMware', '7.0.1.0', guest_os_name, guest_os_id, utc_timestamp(), 0  FROM `cloud`.`guest_os_hypervisor` WHERE hypervisor_type='VMware' AND hypervisor_version='7.0';

-- Add support for amazonlinux3_64Guest from VMware 7.0.1.0
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (7, 'Amazon Linux 3 (64 bit)', 'VMware', '7.0.1.0', 'amazonlinux3_64Guest');

-- Add support for asianux9_64Guest from VMware 7.0.1.0
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (7, 'Asianux Server 9 (64 bit)', 'VMware', '7.0.1.0', 'asianux9_64Guest');

-- Add support for centos9_64Guest from VMware 7.0.1.0
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'CentOS 9', 'VMware', '7.0.1.0', 'centos9_64Guest');

-- Add support for darwin20_64Guest from VMware 7.0.1.0
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (7, 'macOS 11 (64 bit)', 'VMware', '7.0.1.0', 'darwin20_64Guest');

-- Add support for darwin21_64Guest from VMware 7.0.1.0
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (7, 'macOS 12 (64 bit)', 'VMware', '7.0.1.0', 'darwin21_64Guest');

-- Add support for freebsd13_64Guest from VMware 7.0.1.0
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (9, 'FreeBSD 13 (64-bit)', 'VMware', '7.0.1.0', 'freebsd13_64Guest');

-- Add support for freebsd13Guest from VMware 7.0.1.0
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (9, 'FreeBSD 13 (32-bit)', 'VMware', '7.0.1.0', 'freebsd13Guest');

-- Add support for oracleLinux9_64Guest from VMware 7.0.1.0
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (3, 'Oracle Linux 9', 'VMware', '7.0.1.0', 'oracleLinux9_64Guest');

-- Add support for other5xLinux64Guest from VMware 7.0.1.0
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (2, 'Linux 5.x Kernel (64-bit)', 'VMware', '7.0.1.0', 'other5xLinux64Guest');

-- Add support for other5xLinuxGuest from VMware 7.0.1.0
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (2, 'Linux 5.x Kernel (32-bit)', 'VMware', '7.0.1.0', 'other5xLinuxGuest');

-- Add support for rhel9_64Guest from VMware 7.0.1.0
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (4, 'Red Hat Enterprise Linux 9.0', 'VMware', '7.0.1.0', 'rhel9_64Guest');

-- Add support for sles16_64Guest from VMware 7.0.1.0
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (5, 'SUSE Linux Enterprise Server 16 (64-bit)', 'VMware', '7.0.1.0', 'sles16_64Guest');

-- Add support for windows2019srvNext_64Guest from VMware 7.0.1.0
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (6, 'Windows Server 2019 (64-bit)', 'VMware', '7.0.1.0', 'windows2019srvNext_64Guest');
