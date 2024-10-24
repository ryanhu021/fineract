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
package org.apache.fineract.portfolio.savings.api;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.bulkimport.service.BulkImportWorkbookPopulatorService;
import org.apache.fineract.infrastructure.bulkimport.service.BulkImportWorkbookService;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.exception.UnrecognizedQueryParamException;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.core.service.Page;
import org.apache.fineract.infrastructure.core.service.SearchParameters;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.savings.SavingsApiConstants;
import org.apache.fineract.portfolio.savings.data.SavingsAccountData;
import org.apache.fineract.portfolio.savings.service.SavingsAccountChargeReadPlatformService;
import org.apache.fineract.portfolio.savings.service.SavingsAccountReadPlatformService;
import org.apache.fineract.useradministration.domain.AppUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.ws.rs.core.UriInfo;

@ExtendWith(MockitoExtension.class)
public class SavingsAccountsApiResourceTest {

    private static final MockedStatic<SearchParameters> searchParametersMockedStatic = mockStatic(
            SearchParameters.class);

    @Mock
    private SavingsAccountReadPlatformService savingsAccountReadPlatformService;
    @Mock
    private PlatformSecurityContext context;
    @Mock
    private DefaultToApiJsonSerializer<SavingsAccountData> toApiJsonSerializer;
    @Mock
    private PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    @Mock
    private ApiRequestParameterHelper apiRequestParameterHelper;
    @Mock
    private SavingsAccountChargeReadPlatformService savingsAccountChargeReadPlatformService;
    @Mock
    private BulkImportWorkbookService bulkImportWorkbookService;
    @Mock
    private BulkImportWorkbookPopulatorService bulkImportWorkbookPopulatorService;
    @Mock
    private AppUser appUser;
    @Mock
    private UriInfo uriInfo;
    @Mock
    private SearchParameters searchParameters;

    private SavingsAccountsApiResource underTest;

    Integer clientBirthMonth = 10;
    Integer clientBirthDay = 20;
    Page<SavingsAccountData> savingsAccountDataPage = new Page<>(Collections.emptyList(), 0);

    @BeforeEach
    public void setUp() {
        underTest = new SavingsAccountsApiResource(savingsAccountReadPlatformService, context, toApiJsonSerializer,
                commandsSourceWritePlatformService, apiRequestParameterHelper, savingsAccountChargeReadPlatformService,
                bulkImportWorkbookService, bulkImportWorkbookPopulatorService);
        when(context.authenticatedUser()).thenReturn(appUser);
        searchParametersMockedStatic
                .when(() -> SearchParameters.forSavings(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(searchParameters);
    }

    @AfterEach
    public void resetMocks() {
        searchParametersMockedStatic.reset();
    }

    @Test
    public void testRetrieveAll() {
        // when
        when(savingsAccountReadPlatformService.retrieveAll(any())).thenReturn(savingsAccountDataPage);

        underTest.retrieveAll(uriInfo, null, null, null, null, null, null, null, null);

        // then
        verify(context.authenticatedUser(), times(1))
                .validateHasReadPermission(SavingsApiConstants.SAVINGS_ACCOUNT_RESOURCE_NAME);
        searchParametersMockedStatic.verify(
                () -> SearchParameters.forSavings(any(), any(), any(), any(), any(), any(), any(), any()));
        verify(savingsAccountReadPlatformService, times(1)).retrieveAll(searchParameters);
        verify(toApiJsonSerializer, times(1)).serialize(any(), eq(savingsAccountDataPage), any());
    }

    @Test
    public void testRetrieveAllWithClientBirthMonthAndDay() {
        // when
        underTest.retrieveAll(uriInfo, null, null, null, null, null, null, clientBirthMonth, clientBirthDay);

        // then
        searchParametersMockedStatic.verify(() -> SearchParameters.forSavings(any(), any(), any(), any(), any(), any(),
                eq(clientBirthMonth), eq(clientBirthDay)));
    }

    @Test
    public void testRetrieveAllWithInvalidClientBirthMonth() {
        // given
        Integer invalidBirthMonth = 13;

        // then
        assertThrows(UnrecognizedQueryParamException.class, () -> {
            // when
            underTest.retrieveAll(uriInfo, null, null, null, null, null, null, invalidBirthMonth, clientBirthDay);
        });
    }

    @Test
    public void testRetrieveAllWithInvalidClientBirthDay() {
        // given
        Integer invalidBirthDay = 32;

        // then
        assertThrows(UnrecognizedQueryParamException.class, () -> {
            // when
            underTest.retrieveAll(uriInfo, null, null, null, null, null, null, clientBirthMonth, invalidBirthDay);
        });
    }

    @Test
    public void testRetrieveAllWithOnlyClientBirthMonth() {
        // then
        assertThrows(UnrecognizedQueryParamException.class, () -> {
            // when
            underTest.retrieveAll(uriInfo, null, null, null, null, null, null, clientBirthMonth, null);
        });
    }

    @Test
    public void testRetrieveAllWithOnlyClientBirthDay() {
        // then
        assertThrows(UnrecognizedQueryParamException.class, () -> {
            // when
            underTest.retrieveAll(uriInfo, null, null, null, null, null, null, null, clientBirthDay);
        });
    }
}
