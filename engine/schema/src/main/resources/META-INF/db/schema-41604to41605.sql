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
-- Schema upgrade from 4.16.0.4 to 4.16.0.5
--;

-- Fix msid in VM stats data.
UPDATE vm_stats stat JOIN mshost ms ON stat.mgmt_server_id = ms.msid SET stat.mgmt_server_id = ms.id WHERE 1;

ALTER TABLE backups ADD backup_volumes TEXT NULL COMMENT 'details of backedup volumes';

-- Populate column backup_volumes in table backups with a GSON
-- formed by concatenating the UUID, type, size, path and deviceId
-- of the volumes of VMs that have some backup offering. 
-- Required for the restore process of a backup using Veeam
-- The Gson result can be in one of this formats:
-- When VM has only ROOT disk: [{"uuid":"<uuid>","type":"<type>","size":<size>,"path":"<path>","deviceId":<deviceId>}]
-- When VM has more tha one disk: [{"uuid":"<uuid>","type":"<type>","size":<size>,"path":"<path>","deviceId":<deviceId>}, {"uuid":"<uuid>","type":"<type>","size":<size>,"path":"<path>","deviceId":<deviceId>}, <>]
UPDATE backups b INNER JOIN vm_instance vm ON b.vm_id = vm.id SET b.backup_volumes = (SELECT CONCAT("[", GROUP_CONCAT( CONCAT("{\"uuid\":\"", v.uuid, "\",\"type\":\"", v.volume_type, "\",\"size\":", v.`size`, ",\"path\":\"", v.path, "\",\"deviceId\":", v.device_id, "}") SEPARATOR ","), "]") FROM volumes v WHERE v.instance_id = vm.id);

ALTER TABLE vm_instance DROP COLUMN backup_volumes;

ALTER TABLE vm_instance ADD backup_name varchar(255) NULL COMMENT 'backup job name when using Veeam provider';

UPDATE vm_instance vm INNER JOIN backup_offering bo ON vm.backup_offering_id = bo.id SET vm.backup_name = CONCAT(vm.instance_name, "-CSBKP-", vm.uuid);


