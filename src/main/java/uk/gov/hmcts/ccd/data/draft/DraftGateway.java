package uk.gov.hmcts.ccd.data.draft;

import uk.gov.hmcts.ccd.domain.model.draft.CreateCaseDraft;
import uk.gov.hmcts.ccd.domain.model.draft.Draft;
import uk.gov.hmcts.ccd.domain.model.draft.UpdateCaseDraft;

public interface DraftGateway {

    Long save(CreateCaseDraft draft);

    Draft get(String draftId);

    Draft update(UpdateCaseDraft draft, String draftId);

}
