/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.portfolio.savings.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fineract.infrastructure.businessdate.domain.BusinessDateType;
import org.apache.fineract.infrastructure.core.domain.ActionContext;
import org.apache.fineract.infrastructure.core.domain.FineractPlatformTenant;
import org.apache.fineract.infrastructure.core.service.Page;
import org.apache.fineract.infrastructure.core.service.PaginationHelper;
import org.apache.fineract.infrastructure.core.service.SearchParameters;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.infrastructure.dataqueries.service.EntityDatatableChecksReadService;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.infrastructure.security.utils.ColumnValidator;
import org.apache.fineract.organisation.office.domain.Office;
import org.apache.fineract.organisation.staff.service.StaffReadPlatformService;
import org.apache.fineract.portfolio.charge.service.ChargeReadPlatformService;
import org.apache.fineract.portfolio.client.service.ClientReadPlatformService;
import org.apache.fineract.portfolio.group.service.GroupReadPlatformService;
import org.apache.fineract.portfolio.savings.data.SavingsAccountData;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountAssembler;
import org.apache.fineract.useradministration.domain.AppUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
public class SavingsAccountReadPlatformServiceTest {

    @Mock
    private PlatformSecurityContext context;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private ClientReadPlatformService clientReadPlatformService;
    @Mock
    private GroupReadPlatformService groupReadPlatformService;
    @Mock
    private SavingsProductReadPlatformService savingProductReadPlatformService;
    @Mock
    private StaffReadPlatformService staffReadPlatformService;
    @Mock
    private SavingsDropdownReadPlatformService dropdownReadPlatformService;
    @Mock
    private ChargeReadPlatformService chargeReadPlatformService;
    @Mock
    private EntityDatatableChecksReadService entityDatatableChecksReadService;
    @Mock
    private ColumnValidator columnValidator;
    @Mock
    private SavingsAccountAssembler savingAccountAssembler;
    @Mock
    private PaginationHelper paginationHelper;
    @Mock
    private DatabaseSpecificSQLGenerator sqlGenerator;
    @Mock
    private AppUser appUser;
    @Mock
    private Office office;
    @Mock
    private SearchParameters searchParameters;

    private SavingsAccountReadPlatformServiceImpl underTest;

    @BeforeEach
    public void setUp() {
        ThreadLocalContextUtil.setTenant(new FineractPlatformTenant(1L, "default", "Default", "Asia/Kolkata", null));
        ThreadLocalContextUtil.setActionContext(ActionContext.DEFAULT);
        ThreadLocalContextUtil
                .setBusinessDates(
                        new HashMap<>(Map.of(BusinessDateType.BUSINESS_DATE, LocalDate.now(ZoneId.systemDefault()))));
        underTest = new SavingsAccountReadPlatformServiceImpl(context, jdbcTemplate, clientReadPlatformService,
                groupReadPlatformService,
                savingProductReadPlatformService, staffReadPlatformService, dropdownReadPlatformService,
                chargeReadPlatformService,
                entityDatatableChecksReadService, columnValidator, savingAccountAssembler, paginationHelper,
                sqlGenerator);

        when(context.authenticatedUser()).thenReturn(appUser);
        when(appUser.getOffice()).thenReturn(office);
        when(office.getHierarchy()).thenReturn("test");

        List<SavingsAccountData> savingsAccountDataList = new ArrayList<>();
        Page<SavingsAccountData> page = new Page<>(savingsAccountDataList, 1);
        when(paginationHelper.fetchPage(any(JdbcTemplate.class), anyString(), any(Object[].class),
                any(RowMapper.class)))
                .thenReturn(page);
    }

    @AfterEach
    public void resetMocks() {
        reset(searchParameters);
    }

    @Test
    public void testRetrieveAllWithNoSearchParameters() {
        // when
        underTest.retrieveAll(searchParameters);

        // then
        verify(paginationHelper, times(1)).fetchPage(any(JdbcTemplate.class), anyString(), any(Object[].class), any());
    }

    @Test
    public void testRetrieveAllWithClientBirthMonthAndDay() {
        // when
        when(searchParameters.getClientBirthMonth()).thenReturn(10);
        when(searchParameters.getClientBirthDay()).thenReturn(20);

        underTest.retrieveAll(searchParameters);

        // then
        verify(paginationHelper, times(1)).fetchPage(any(JdbcTemplate.class),
                argThat(query -> query.contains("and MONTH(c.date_of_birth) = ? and DAY(c.date_of_birth) = ?")),
                argThat(args -> args[2].equals(10) && args[3].equals(20)), any());
    }

    @Test
    public void testRetrieveAllWithClientBirthDayNoMonth() {
        // when
        when(searchParameters.getClientBirthMonth()).thenReturn(null);
        when(searchParameters.getClientBirthDay()).thenReturn(20);

        underTest.retrieveAll(searchParameters);

        // then
        verify(paginationHelper, times(1)).fetchPage(any(JdbcTemplate.class),
                argThat(query -> !query.contains("c.date_of_birth")), any(Object[].class), any());
    }

    @Test
    public void testRetrieveAllWithClientBirthMonthNoDay() {
        // when
        when(searchParameters.getClientBirthMonth()).thenReturn(10);
        when(searchParameters.getClientBirthDay()).thenReturn(null);

        underTest.retrieveAll(searchParameters);

        // then
        verify(paginationHelper, times(1)).fetchPage(any(JdbcTemplate.class),
                argThat(query -> !query.contains("c.date_of_birth")), any(Object[].class), any());
    }
}