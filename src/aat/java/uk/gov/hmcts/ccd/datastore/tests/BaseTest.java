package uk.gov.hmcts.ccd.datastore.tests;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static java.lang.Boolean.TRUE;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.ccd.datastore.tests.helper.CaseTestDataLoaderExtension;
import uk.gov.hmcts.ccd.datastore.tests.helper.idam.AuthenticatedUser;

@ExtendWith(AATExtension.class)
@TestPropertySource(locations = "classpath:functional-application.properties")
public abstract class BaseTest {
    protected final AATHelper aat;

    protected BaseTest(AATHelper aat) {
        this.aat = aat;
        RestAssured.baseURI = aat.getTestUrl();
        RestAssured.useRelaxedHTTPSValidation();
    }

    protected Supplier<RequestSpecification> asAutoTestCaseworker() {
        return asAutoTestCaseworker(TRUE);
    }

    protected Supplier<RequestSpecification> asAutoTestCaseworker(boolean withUserParam) {
        return authenticateAndCreateRequestSpecification(aat.getCaseworkerAutoTestEmail(),
            aat.getCaseworkerAutoTestPassword(), withUserParam);
    }

    protected Supplier<RequestSpecification> asPrivateCaseworker(boolean withUserParam) {
        return authenticateAndCreateRequestSpecification(aat.getPrivateCaseworkerEmail(),
            aat.getPrivateCaseworkerPassword(), withUserParam);
    }

    protected Supplier<RequestSpecification> asRestrictedCaseworker(boolean withUserParam) {
        return authenticateAndCreateRequestSpecification(aat.getRestrictedCaseworkerEmail(),
            aat.getRestrictedCaseworkerPassword(), withUserParam);
    }

    protected Supplier<RequestSpecification> asPrivateCaseworkerSolicitor(boolean withUserParam) {
        return authenticateAndCreateRequestSpecification(aat.getPrivateCaseworkerSolicitorEmail(),
            aat.getPrivateCaseworkerSolicitorPassword(), withUserParam);
    }

    protected Supplier<RequestSpecification> asPrivateCaseworkerSolicitor1(boolean withUserParam) {
        return authenticateAndCreateRequestSpecification(aat.getPrivateCaseworkerSolicitor1Email(),
            aat.getPrivateCaseworkerSolicitor1Password(), withUserParam);
    }

    protected Supplier<RequestSpecification> asPrivateCaseworkerSolicitor2(boolean withUserParam) {
        return authenticateAndCreateRequestSpecification(aat.getPrivateCaseworkerSolicitor2Email(),
            aat.getPrivateCaseworkerSolicitor2Password(), withUserParam);
    }

    protected Supplier<RequestSpecification> asPrivateCaseworkerLocalAuthority(boolean withUserParam) {
        return authenticateAndCreateRequestSpecification(aat.getPrivateCaseworkerLocalAuthorityEmail(),
            aat.getPrivateCaseworkerLocalAuthorityPassword(), withUserParam);
    }

    protected Supplier<RequestSpecification> asPrivateCaseworkerLocalAuthority1(boolean withUserParam) {
        return authenticateAndCreateRequestSpecification(aat.getPrivateCaseworkerLocalAuthority1Email(),
            aat.getPrivateCaseworkerLocalAuthority1Password(), withUserParam);
    }

    protected Supplier<RequestSpecification> asPrivateCaseworkerLocalAuthority2(boolean withUserParam) {
        return authenticateAndCreateRequestSpecification(aat.getPrivateCaseworkerLocalAuthority2Email(),
            aat.getPrivateCaseworkerLocalAuthority2Password(), withUserParam);
    }

    protected Supplier<RequestSpecification> asPrivateCaseworkerPanelMember(boolean withUserParam) {
        return authenticateAndCreateRequestSpecification(aat.getPrivateCaseworkerPanelMemberEmail(),
            aat.getPrivateCaseworkerPanelMemberPassword(), withUserParam);
    }

    protected Supplier<RequestSpecification> asPrivateCaseworkerPanelMember1(boolean withUserParam) {
        return authenticateAndCreateRequestSpecification(aat.getPrivateCaseworkerPanelMember1Email(),
            aat.getPrivateCaseworkerPanelMember1Password(), withUserParam);
    }

    protected Supplier<RequestSpecification> asPrivateCaseworkerCitizen(boolean withUserParam) {
        return authenticateAndCreateRequestSpecification(aat.getPrivateCaseworkerCitizenEmail(),
            aat.getPrivateCaseworkerCitizenPassword(), withUserParam);
    }

    protected Supplier<RequestSpecification> asPrivateCaseworkerCitizen1(boolean withUserParam) {
        return authenticateAndCreateRequestSpecification(aat.getPrivateCaseworkerCitizen1Email(),
            aat.getPrivateCaseworkerCitizen1Password(), withUserParam);
    }

    protected Supplier<RequestSpecification> asPrivateCaseworkerCitizen2(boolean withUserParam) {
        return authenticateAndCreateRequestSpecification(aat.getPrivateCaseworkerCitizen2Email(),
            aat.getPrivateCaseworkerCitizen2Password(), withUserParam);
    }

    protected Supplier<RequestSpecification> asPrivateCrossCaseTypeCaseworker(boolean withUserParam) {
        return authenticateAndCreateRequestSpecification(aat.getPrivateCrossCaseTypeCaseworkerEmail(),
            aat.getPrivateCrossCaseTypeCaseworkerPassword(),
                                                         withUserParam);
    }

    protected Supplier<RequestSpecification> asPrivateCrossCaseTypeSolicitor(boolean withUserParam) {
        return authenticateAndCreateRequestSpecification(aat.getPrivateCrossCaseTypeSolicitorEmail(),
            aat.getPrivateCrossCaseTypeSolicitorPassword(),
                                                         withUserParam);
    }

    protected Supplier<RequestSpecification> asRestrictedCrossCaseTypeCaseworker(boolean withUserParam) {
        return authenticateAndCreateRequestSpecification(aat.getRestrictedCrossCaseTypeCaseworkerEmail(),
            aat.getRestrictedCrossCaseTypeCaseworkerPassword(),
                                                         withUserParam);
    }


    private Supplier<RequestSpecification> authenticateAndCreateRequestSpecification(
        String username, String password, Boolean withUserParam) {
        AuthenticatedUser caseworker = aat.getIdamHelper().authenticate(username, password);
        String s2sToken = aat.getS2SHelper().getToken();

        return () -> {
            RequestSpecification request = RestAssured.given()
                .header("Authorization", "Bearer " + caseworker.getAccessToken())
                .header("ServiceAuthorization", s2sToken);

            return withUserParam ? request.pathParam("user", caseworker.getId()) : request;
        };
    }

    protected RequestSpecification asAutoTestImporter() {
        AuthenticatedUser caseworker = aat.getIdamHelper().authenticate(aat.getImporterAutoTestEmail(),
                                                                        aat.getImporterAutoTestPassword());

        String s2sToken = aat.getS2SHelper().getToken();

        return RestAssured.given(new RequestSpecBuilder()
                                     .setBaseUri(aat.getDefinitionStoreUrl())
                                     .build())
            .header("Authorization", "Bearer " + caseworker.getAccessToken())
            .header("ServiceAuthorization", s2sToken);
    }

    protected void importCaseDefinitionToGiveUserCRUDPrivileges() throws InterruptedException {
        new CaseTestDataLoaderExtension().importDefinition(
            "src/aat/resources/CCD_CNP_RDM5118_5122_Extended_updated_to_CRUD.xlsx");
        TimeUnit.SECONDS.sleep(0);
    }

    protected void importCaseDefinitionToGiveUserOriginalPrivileges() throws InterruptedException {
        new CaseTestDataLoaderExtension().importDefinition(
            "src/aat/resources/CCD_CNP_RDM5118_5122_Extended_updated_to_original.xlsx");
        TimeUnit.SECONDS.sleep(30);
    }
}
