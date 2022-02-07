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
-- Schema upgrade from 4.16.0.3 to 4.16.0.4
--;

-- Create table to persist VM stats.
DROP TABLE IF EXISTS `cloud`.`vm_stats`;
CREATE TABLE `cloud`.`vm_stats` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `vm_id` bigint unsigned NOT NULL,
  `mgmt_server_id` bigint unsigned NOT NULL,
  `timestamp` datetime NOT NULL,
  `vm_stats_data` text NOT NULL,
  PRIMARY KEY (`id`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Update name for global configuration vm.stats.increment.metrics
Update configuration set name='vm.stats.increment.metrics' where name='vm.stats.increment.metrics.in.memory';
