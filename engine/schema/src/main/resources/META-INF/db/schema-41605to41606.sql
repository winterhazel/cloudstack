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
-- Schema upgrade from 4.16.0.5 to 4.16.0.6
--;

-- Fix migrateVolume permissions.
DELETE role_perm
FROM role_permissions role_perm
INNER JOIN roles ON role_perm.role_id = roles.id
WHERE roles.role_type != 'Admin' AND is_default = 1 AND role_perm.rule = 'migrateVolume';


-- Add cloud.account.removed to quota_summary_view --
DROP VIEW IF EXISTS `cloud_usage`.`quota_summary_view`;
CREATE VIEW `cloud_usage`.`quota_summary_view` AS
    SELECT  quota_account.account_id,
            quota_account.quota_balance,
            quota_account.quota_balance_date,
            quota_account.quota_enforce,
            quota_account.quota_min_balance,
            quota_account.quota_alert_date,
            quota_account.quota_alert_type,
            quota_account.last_statement_date,
            account.uuid as account_uuid,
            account.account_name,
            account.state as account_state,
            account.removed as account_removed,
            domain.id as domain_id,
            domain.uuid as domain_uuid,
            domain.name as domain_name,
            domain.path as domain_path,
            domain.removed as domain_removed
    FROM    cloud_usage.quota_account quota_account
    INNER   JOIN cloud.account account ON (account.id = quota_account.account_id)
    INNER   JOIN cloud.domain domain on (domain.id = account.domain_id);