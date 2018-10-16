package uk.gov.hmcts.ccd.domain.service.aggregated;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_CREATE;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.JurisdictionDisplayProperties;
import uk.gov.hmcts.ccd.domain.model.aggregated.User;
import uk.gov.hmcts.ccd.domain.model.aggregated.UserProfile;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEvent;
import uk.gov.hmcts.ccd.domain.model.definition.CaseState;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

class AuthorisedGetUserProfileOperationTest {
    private final UserProfile userProfile = new UserProfile();
    private final User user = new User();
    private final JurisdictionDisplayProperties test1JurisdictionDisplayProperties = new JurisdictionDisplayProperties();
    private final JurisdictionDisplayProperties test2JurisdictionDisplayProperties = new JurisdictionDisplayProperties();

    private String userToken = "testToken";
    private Set<String> userRoles = Sets.newHashSet("role1", "role2", "role3");
    private List<CaseState> caseStates = Arrays.asList(new CaseState(), new CaseState(), new CaseState());
    private List<CaseEvent> caseEvents = Arrays.asList(new CaseEvent(), new CaseEvent(), new CaseEvent(), new CaseEvent());

    private CaseType notAllowedCaseType = new CaseType();

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccessControlService accessControlService;

    @Mock
    private GetUserProfileOperation getUserProfileOperation;

    private AuthorisedGetUserProfileOperation classUnderTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        userProfile.setUser(user);
        userProfile.setJurisdictions(new JurisdictionDisplayProperties[]{test1JurisdictionDisplayProperties, test2JurisdictionDisplayProperties});

        List<CaseType> caseTypes1 = Arrays.asList(notAllowedCaseType, new CaseType());
        List<CaseType> caseTypes2 = Arrays.asList(new CaseType(), notAllowedCaseType, new CaseType());

        test1JurisdictionDisplayProperties.setCaseTypes(caseTypes1);
        test2JurisdictionDisplayProperties.setCaseTypes(caseTypes2);

        doReturn(userRoles).when(userRepository).getUserRoles();
        doReturn(userProfile).when(getUserProfileOperation).execute(userToken);
        doReturn(true).when(accessControlService).canAccessCaseTypeWithCriteria(any(), any(), any());
        classUnderTest = new AuthorisedGetUserProfileOperation(userRepository, accessControlService, getUserProfileOperation);
    }

    @Test
    @DisplayName("should return only caseTypes the user is allowed to access")
    void execute() {
        doReturn(false).when(accessControlService).canAccessCaseTypeWithCriteria(eq(notAllowedCaseType), eq(userRoles), eq(CAN_CREATE));
        doReturn(caseStates).when(accessControlService).filterCaseStatesByAccess(any(), eq(userRoles), eq(CAN_CREATE));
        doReturn(caseEvents).when(accessControlService).filterCaseEventsByAccess(any(), eq(userRoles), eq(CAN_CREATE));

        UserProfile userProfile = classUnderTest.execute(userToken);

        assertAll(
            () -> assertThat(userProfile.getJurisdictions()[0].getCaseTypes().size(), is(1)),
            () -> assertThat(userProfile.getJurisdictions()[1].getCaseTypes().size(), is(2)),
            () -> assertThat(userProfile.getJurisdictions()[0].getCaseTypes(), everyItem(not(isIn(Arrays.asList(notAllowedCaseType))))),
            () -> assertThat(userProfile.getJurisdictions()[1].getCaseTypes(), everyItem(not(isIn(Arrays.asList(notAllowedCaseType))))),
            () -> assertThat(userProfile.getJurisdictions()[0].getCaseTypes().get(0).getStates().size(), is(3)),
            () -> assertThat(userProfile.getJurisdictions()[0].getCaseTypes().get(0).getEvents().size(), is(4))
        );
    }

    @Test
    @DisplayName("should return empty caseType if the user is not allowed to access any case type")
    void shouldReturnEmptyCaseType() {
        doReturn(false).when(accessControlService).canAccessCaseTypeWithCriteria(any(), eq(userRoles), eq(CAN_CREATE));

        UserProfile userProfile = classUnderTest.execute(userToken);

        assertAll(
            () -> assertThat(userProfile.getJurisdictions()[0].getCaseTypes().size(), is(0)),
            () -> assertThat(userProfile.getJurisdictions()[1].getCaseTypes().size(), is(0))
        );
    }
}
