package br.com.belloinfo.saap_mvp.infrastructure.web.controller;

import br.com.belloinfo.saap_mvp.application.service.AppointmentActionTokenService;
import br.com.belloinfo.saap_mvp.application.usecase.AcceptWaitlistOfferUseCase;
import br.com.belloinfo.saap_mvp.application.usecase.DeclineWaitlistOfferUseCase;
import br.com.belloinfo.saap_mvp.domain.repository.WaitlistEntryRepository;
import com.twilio.security.RequestValidator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/notifications/whatsapp")
@RequiredArgsConstructor
public class WhatsAppWebhookController {

    private static final String WEBHOOK_PATH = "/api/v1/notifications/whatsapp/webhook";

    private final WaitlistEntryRepository waitlistEntryRepository;
    private final AcceptWaitlistOfferUseCase acceptWaitlistOfferUseCase;
    private final DeclineWaitlistOfferUseCase declineWaitlistOfferUseCase;
    private final AppointmentActionTokenService tokenService;

    @Value("${app.notifications.twilio.auth-token:}")
    private String authToken;

    @Value("${app.notifications.twilio.webhook-base-url:}")
    private String webhookBaseUrl;

    @Value("${app.notifications.twilio.webhook-validate-signature:true}")
    private boolean validateSignature;

    @PostMapping(value = "/webhook", produces = MediaType.TEXT_XML_VALUE)
    public ResponseEntity<String> receiveButtonReply(
            HttpServletRequest request,
            @RequestParam(name = "From", required = false) String from,
            @RequestParam(name = "ButtonPayload", required = false) String buttonPayload,
            @RequestParam Map<String, String> allParams,
            @org.springframework.web.bind.annotation.RequestHeader(name = "X-Twilio-Signature", required = false) String signature
    ) {
        if (validateSignature && !isValidSignature(allParams, signature)) {
            log.warn("Assinatura Twilio invalida no webhook do WhatsApp");
            return ResponseEntity.status(403).build();
        }

        if (from == null || from.isBlank() || buttonPayload == null || buttonPayload.isBlank()) {
            return twiml("");
        }

        String phone = from.replace("whatsapp:", "");

        if (!"waitlist_accept".equals(buttonPayload) && !"waitlist_decline".equals(buttonPayload)) {
            return twiml("");
        }

        var entryOpt = waitlistEntryRepository.findMostRecentOfferedByPatientPhone(phone);
        if (entryOpt.isEmpty()) {
            log.warn("Nenhuma oferta de vaga ativa encontrada para o telefone {}", maskPhone(phone));
            return twiml("Nao encontramos uma oferta de vaga ativa para responder.");
        }

        UUID entryId = entryOpt.get().getId();

        try {
            if ("waitlist_accept".equals(buttonPayload)) {
                String token = tokenService.generateToken(entryId, "accept-waitlist");
                acceptWaitlistOfferUseCase.execute(token);
                return twiml("Vaga da fila de espera aceita e agendamento confirmado com sucesso!");
            }

            String token = tokenService.generateToken(entryId, "decline-waitlist");
            declineWaitlistOfferUseCase.execute(token);
            return twiml("Vaga da fila de espera recusada com sucesso.");
        } catch (IllegalArgumentException e) {
            log.warn("Token invalido ao processar resposta de botao do WhatsApp para entry {}: {}", entryId, e.getMessage());
            return twiml(e.getMessage());
        } catch (IllegalStateException e) {
            log.warn("Estado invalido ao processar resposta de botao do WhatsApp para entry {}: {}", entryId, e.getMessage());
            return twiml(e.getMessage());
        } catch (Exception e) {
            log.error("Erro ao processar resposta de botao do WhatsApp para entry {}: {}", entryId, e.getMessage(), e);
            return twiml("Nao foi possivel processar sua resposta. Entre em contato com a clinica.");
        }
    }

    private boolean isValidSignature(Map<String, String> params, String signature) {
        if (authToken == null || authToken.isBlank() || webhookBaseUrl == null || webhookBaseUrl.isBlank()) {
            log.warn("Validacao de assinatura Twilio nao configurada (auth-token ou webhook-base-url ausente)");
            return false;
        }
        if (signature == null || signature.isBlank()) {
            return false;
        }
        String url = webhookBaseUrl.replaceAll("/+$", "") + WEBHOOK_PATH;
        return new RequestValidator(authToken).validate(url, params, signature);
    }

    private String maskPhone(String phone) {
        return phone.length() > 4 ? "***" + phone.substring(phone.length() - 4) : "***";
    }

    private ResponseEntity<String> twiml(String message) {
        String body = message.isBlank()
                ? "<Response/>"
                : "<Response><Message>" + escapeXml(message) + "</Message></Response>";
        return ResponseEntity.ok(body);
    }

    private String escapeXml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
