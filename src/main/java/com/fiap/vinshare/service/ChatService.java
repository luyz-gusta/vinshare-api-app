package com.fiap.vinshare.service;

import com.fiap.vinshare.domain.dto.chat.ChatMessageResponseDTO;
import com.fiap.vinshare.domain.dto.chat.ChatSessionResponseDTO;
import com.fiap.vinshare.domain.dto.chat.SendMessageRequestDTO;
import com.fiap.vinshare.domain.dto.chat.SuggestedActionDTO;
import com.fiap.vinshare.domain.entities.ChatMessage;
import com.fiap.vinshare.domain.entities.ChatMessageRole;
import com.fiap.vinshare.domain.entities.ChatSession;
import com.fiap.vinshare.domain.entities.Customer;
import com.fiap.vinshare.domain.entities.User;
import com.fiap.vinshare.domain.entities.Vehicle;
import com.fiap.vinshare.infra.errors.exceptions.ResourceNotFoundException;
import com.fiap.vinshare.infra.security.InputSanitizer;
import com.fiap.vinshare.repositories.ChatMessageRepository;
import com.fiap.vinshare.repositories.ChatSessionRepository;
import com.fiap.vinshare.repositories.CustomerRepository;
import com.fiap.vinshare.repositories.VehicleRepository;
import com.fiap.vinshare.repositories.WarrantyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final String SYSTEM_PROMPT = """
            Você é o assistente virtual da Ford VIN Share. Atenda em português brasileiro,
            seja conciso (até 4 frases por resposta) e proativo. Use o contexto do cliente
            para personalizar. Quando uma ação fizer sentido (agendar, ver garantia),
            mencione a ação de forma clara. Nunca peça CPF, RG ou dados pessoais.
            Nunca invente datas, modelos ou status; se algo não estiver no contexto,
            diga que vai verificar.
            """;

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final CustomerRepository customerRepository;
    private final VehicleRepository vehicleRepository;
    private final WarrantyRepository warrantyRepository;
    private final AiChatClient aiChatClient;
    private final InputSanitizer sanitizer;

    @Transactional
    public ChatSessionResponseDTO openSession(User user) {
        ChatSession session = sessionRepository.save(
                ChatSession.builder().user(user).build());
        return ChatSessionResponseDTO.builder()
                .sessionId(session.getId())
                .startedAt(session.getStartedAt())
                .build();
    }

    @Transactional
    public ChatMessageResponseDTO sendMessage(UUID sessionId, SendMessageRequestDTO req, User user) {
        ChatSession session = sessionRepository.findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> ResourceNotFoundException.of("Sessão de chat", sessionId));

        ChatMessage userMessage = ChatMessage.builder()
                .session(session)
                .role(ChatMessageRole.USER)
                .content(sanitizer.sanitize(req.message()))
                .build();
        messageRepository.save(userMessage);

        List<Map<String, String>> history = messageRepository
                .findAllBySessionIdOrderByCreatedAtAsc(session.getId()).stream()
                .map(m -> Map.of(
                        "role", m.getRole() == ChatMessageRole.ASSISTANT ? "assistant" : "user",
                        "content", m.getContent()))
                .toList();

        String systemWithContext = SYSTEM_PROMPT + buildPseudonimizedContext(user);
        log.debug("Chat: chamando IA para sessão {} (histórico de {} mensagens)",
                session.getId(), history.size());

        String reply = aiChatClient.complete(systemWithContext, history);
        List<SuggestedActionDTO> actions = inferActions(reply);

        ChatMessage assistantMessage = ChatMessage.builder()
                .session(session)
                .role(ChatMessageRole.ASSISTANT)
                .content(reply)
                .suggestedActions(actions.stream().map(this::actionToMap).toList())
                .build();
        messageRepository.save(assistantMessage);

        return toDTO(assistantMessage);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponseDTO> listMessages(UUID sessionId, User user) {
        ChatSession session = sessionRepository.findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> ResourceNotFoundException.of("Sessão de chat", sessionId));
        return messageRepository.findAllBySessionIdOrderByCreatedAtAsc(session.getId()).stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Monta o contexto enviado ao prompt sem CPF, sem nome completo, sem e-mail.
     * Usa apenas primeiro nome, modelo, ano e status de garantia.
     * Atende Cyber Frente 4 (pseudonimização).
     */
    private String buildPseudonimizedContext(User user) {
        Optional<Customer> custOpt = customerRepository.findByUser(user);
        if (custOpt.isEmpty()) return "";

        Customer customer = custOpt.get();
        String firstName = customer.getFullName() == null ? "Cliente"
                : customer.getFullName().split("\\s+")[0];

        StringBuilder sb = new StringBuilder("\n\nContexto do cliente: ");
        sb.append("primeiro nome=").append(firstName).append(". ");

        List<Vehicle> vehicles = vehicleRepository.findAllByCustomerId(customer.getId());
        if (!vehicles.isEmpty()) {
            Vehicle v = vehicles.get(0);
            sb.append("Veículo: ").append(v.getModel()).append(" ").append(v.getYear());
            warrantyRepository.findByVehicleId(v.getId()).ifPresent(w -> sb
                    .append(". Garantia: ").append(w.getStatus())
                    .append(", vence em ").append(w.getEndDate()));
            sb.append(". KM atual=").append(v.getCurrentKm());
        }
        return sb.toString();
    }

    private List<SuggestedActionDTO> inferActions(String reply) {
        String lower = reply.toLowerCase();
        List<SuggestedActionDTO> actions = new ArrayList<>();
        if (lower.contains("agend") || lower.contains("revisão") || lower.contains("revisao")) {
            actions.add(SuggestedActionDTO.builder()
                    .type("OPEN_SCHEDULING")
                    .label("Agendar revisão")
                    .target("fordapp://scheduling")
                    .build());
        }
        if (lower.contains("garantia")) {
            actions.add(SuggestedActionDTO.builder()
                    .type("OPEN_WARRANTY")
                    .label("Ver garantia")
                    .target("fordapp://warranty")
                    .build());
        }
        return actions;
    }

    private Map<String, Object> actionToMap(SuggestedActionDTO a) {
        Map<String, Object> m = new HashMap<>();
        m.put("type", a.type());
        m.put("label", a.label());
        m.put("target", a.target());
        return m;
    }

    private ChatMessageResponseDTO toDTO(ChatMessage m) {
        List<SuggestedActionDTO> actions = m.getSuggestedActions() == null
                ? List.of()
                : m.getSuggestedActions().stream()
                        .map(map -> SuggestedActionDTO.builder()
                                .type(asString(map.get("type")))
                                .label(asString(map.get("label")))
                                .target(asString(map.get("target")))
                                .build())
                        .toList();
        return ChatMessageResponseDTO.builder()
                .id(m.getId())
                .role(m.getRole())
                .content(m.getContent())
                .suggestedActions(actions)
                .createdAt(m.getCreatedAt())
                .build();
    }

    private String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
