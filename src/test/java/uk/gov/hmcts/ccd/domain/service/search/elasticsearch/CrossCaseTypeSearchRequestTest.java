package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadSearchRequest;

class CrossCaseTypeSearchRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("should throw exception when query node not found")
    void shouldThrowExceptionWhenQueryNodeNotFound() {
        String query = "{}";
        assertThrows(BadSearchRequest.class, () -> new CrossCaseTypeSearchRequest.Builder()
            .withCaseTypes(singletonList("caseType"))
            .withSearchRequest(objectMapper.readValue(query, JsonNode.class))
            .build());
    }

    @Nested
    @DisplayName("builder")
    class Builder {

        @Test
        @DisplayName("should build non-multi-case-type search request")
        void shouldBuildNonMultiCaseTypeSearch() throws Exception {
            String query = "{\"query\":{}}";
            JsonNode jsonNode = objectMapper.readTree(query);
            List<String> caseTypeIds = singletonList("CT");
            CrossCaseTypeSearchRequest request = new CrossCaseTypeSearchRequest.Builder()
                .withSearchRequest(jsonNode)
                .withCaseTypes(caseTypeIds)
                .build();

            assertThat(request.isMultiCaseTypeSearch(), is(false));
            assertThat(request.getSearchRequestJsonNode(), is(jsonNode));
            assertThat(request.getCaseTypeIds(), hasSize(1));
            assertThat(request.getAliasFields().isEmpty(), is(true));
        }

        @Test
        @DisplayName("should build multi-case-type search request")
        void shouldBuildMultiCaseTypeSearch() throws Exception {
            String query = "{\"_source\":[\"alias.name\"], \"query\":{}}";
            JsonNode jsonNode = objectMapper.readTree(query);
            List<String> caseTypeIds = Arrays.asList("CT", "PT");
            CrossCaseTypeSearchRequest request = new CrossCaseTypeSearchRequest.Builder()
                .withSearchRequest(jsonNode)
                .withCaseTypes(caseTypeIds)
                .withMultiCaseTypeSearch(true)
                .build();

            assertThat(request.isMultiCaseTypeSearch(), is(true));
            assertThat(request.getSearchRequestJsonNode(), is(jsonNode));
            assertThat(request.getCaseTypeIds(), is(caseTypeIds));
            assertThat(request.getAliasFields(), hasItem("name"));
        }

        @Test
        @DisplayName("should add metadata source fields for single case type search requests with source")
        void shouldAddMetadataSourceFields() throws Exception {
            String query = "{\"_source\":[\"data.name\"], \"query\":{}}";
            JsonNode jsonNode = objectMapper.readTree(query);
            List<String> caseTypeIds = singletonList("CT");

            final CrossCaseTypeSearchRequest request = new CrossCaseTypeSearchRequest.Builder()
                .withSearchRequest(jsonNode)
                .withCaseTypes(caseTypeIds)
                .build();

            final JsonNode sourceNode = request.getSearchRequestJsonNode().get("_source");
            List<String> sourceFields = new ObjectMapper().readValue(sourceNode.traverse(), new TypeReference<ArrayList<String>>(){});

            assertAll(
                () -> assertThat(request.isMultiCaseTypeSearch(), is(false)),
                () -> assertThat(request.getSearchRequestJsonNode(), is(jsonNode)),
                () -> assertThat(request.getCaseTypeIds(), is(caseTypeIds)),
                () -> assertThat(sourceFields, hasItem("data.name")),
                () -> assertThat(sourceFields, hasItem(MetaData.CaseField.CASE_REFERENCE.getDbColumnName())),
                () -> assertThat(sourceFields, hasItem(MetaData.CaseField.LAST_STATE_MODIFIED_DATE.getDbColumnName())),
                () -> assertThat(sourceFields, hasItem(MetaData.CaseField.CREATED_DATE.getDbColumnName())),
                () -> assertThat(sourceFields, hasItem(MetaData.CaseField.CASE_TYPE.getDbColumnName())),
                () -> assertThat(sourceFields, hasItem(MetaData.CaseField.JURISDICTION.getDbColumnName())),
                () -> assertThat(sourceFields, hasItem(MetaData.CaseField.SECURITY_CLASSIFICATION.getDbColumnName())),
                () -> assertThat(sourceFields, hasItem(MetaData.CaseField.LAST_MODIFIED_DATE.getDbColumnName())),
                () -> assertThat(sourceFields, hasItem(MetaData.CaseField.STATE.getDbColumnName()))
            );
        }
    }

}
