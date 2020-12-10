package uk.gov.hmcts.ccd.domain.service.message;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.CaseAuditEventRepository;
import uk.gov.hmcts.ccd.data.message.MessageCandidateRepository;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.IdamUser;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.model.std.MessageInformation;
import uk.gov.hmcts.ccd.domain.model.std.MessageQueueCandidate;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@Qualifier("caseEventMessageService")
public class CaseEventMessageService implements MessageService {

    private static final ObjectMapper mapper = new ObjectMapper();
    private final UserRepository userRepository;
    private final MessageCandidateRepository messageCandidateRepository;
    private final CaseAuditEventRepository caseAuditEventRepository;
    private static final String CASE_EVENT_MESSAGE_TYPE = "CASE_EVENT";

    @Inject
    public CaseEventMessageService(@Qualifier(CachedUserRepository.QUALIFIER) final UserRepository userRepository,
                                   final MessageCandidateRepository messageCandidateRepository,
                                   CaseAuditEventRepository caseAuditEventRepository) {
        this.userRepository = userRepository;
        this.messageCandidateRepository = messageCandidateRepository;
        this.caseAuditEventRepository = caseAuditEventRepository;
    }

    @Override
    public void handleMessage(CaseEventDefinition caseEventDefinition,
                              CaseDetails caseDetails, String oldState) {
        final MessageQueueCandidate messageQueueCandidate = new MessageQueueCandidate();
        if (Boolean.TRUE.equals(caseEventDefinition.getPublish())) {

            MessageInformation messageInformation = populateMessageInformation(caseEventDefinition,
                caseDetails, oldState);
            JsonNode node = mapper.convertValue(messageInformation, JsonNode.class);

            messageQueueCandidate.setMessageInformation(node);
            messageQueueCandidate.setMessageType(CASE_EVENT_MESSAGE_TYPE);
            messageQueueCandidate.setTimeStamp(LocalDateTime.now(ZoneOffset.UTC));
            messageCandidateRepository.set(messageQueueCandidate);
        }
    }

    private MessageInformation populateMessageInformation(CaseEventDefinition caseEventDefinition,
                                                          CaseDetails caseDetails, String oldState) {

        final MessageInformation messageInformation = new MessageInformation();
        final IdamUser user = userRepository.getUser();
        List<AuditEvent> auditEvent = caseAuditEventRepository.findByCase(caseDetails);

        messageInformation.setCaseId(caseDetails.getReference().toString());
        messageInformation.setJurisdictionId(caseDetails.getJurisdiction());
        messageInformation.setCaseTypeId(caseDetails.getCaseTypeId());
        messageInformation.setEventInstanceId(auditEvent.get(0).getId());
        messageInformation.setEventTimestamp(caseDetails.getLastModified());
        messageInformation.setEventId(caseEventDefinition.getId());
        messageInformation.setUserId(user.getId());
        messageInformation.setPreviousStateId(oldState);
        messageInformation.setNewStateId(caseDetails.getState());

        return messageInformation;
    }
}
