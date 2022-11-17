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
-- Schema upgrade from 4.16.0.9 to 4.16.0.10
--;

UPDATE  cloud.configuration
SET     description = 'Comma separated list of email addresses which are going to receive alert emails.'
WHERE   name = 'alert.email.addresses';

UPDATE  cloud.configuration
SET     description = "Use SSL method used to encrypt copy traffic between zones. Also ensures that the certificate assigned to the zone is used when generating links for external access."
WHERE   name = 'secstorage.encrypt.copy';
